/*
 * Copyright 2011-2022 B2i Healthcare Pte Ltd, http://b2i.sg
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.b2international.snowowl.snomed.core.ecl;

import static com.b2international.index.revision.Revision.Expressions.id;
import static com.b2international.index.revision.Revision.Expressions.ids;
import static com.b2international.index.revision.Revision.Fields.ID;
import static com.b2international.snowowl.snomed.core.ecl.EclExpression.isAnyExpression;
import static com.b2international.snowowl.snomed.core.ecl.EclExpression.isEclConceptReference;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedComponentDocument.Expressions.activeMemberOf;
import static com.b2international.snowowl.snomed.datastore.index.entry.SnomedComponentDocument.Fields.ACTIVE_MEMBER_OF;
import static com.google.common.collect.Sets.newHashSet;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.util.PolymorphicDispatcher;

import com.b2international.commons.CompareUtils;
import com.b2international.commons.exceptions.NotImplementedException;
import com.b2international.index.Hits;
import com.b2international.index.query.*;
import com.b2international.index.query.Expressions.ExpressionBuilder;
import com.b2international.index.revision.RevisionSearcher;
import com.b2international.snomed.ecl.Ecl;
import com.b2international.snomed.ecl.ecl.*;
import com.b2international.snowowl.core.api.SnowowlRuntimeException;
import com.b2international.snowowl.core.date.DateFormats;
import com.b2international.snowowl.core.date.EffectiveTimes;
import com.b2international.snowowl.core.domain.BranchContext;
import com.b2international.snowowl.core.domain.IComponent;
import com.b2international.snowowl.core.events.Request;
import com.b2international.snowowl.core.events.util.Promise;
import com.b2international.snowowl.snomed.common.SnomedConstants.Concepts;
import com.b2international.snowowl.snomed.core.domain.SnomedConcept;
import com.b2international.snowowl.snomed.core.tree.Trees;
import com.b2international.snowowl.snomed.datastore.SnomedDescriptionUtils;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedConceptDocument;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedDescriptionIndexEntry;
import com.b2international.snowowl.snomed.datastore.index.entry.SnomedDocument;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.*;

/**
 * Evaluates the given ECL expression {@link String} or parsed {@link ExpressionConstraint} to an executable {@link Expression query expression}.
 * <p>
 * <i>NOTE: This request implementation is currently not working in remote environments, when the request need to be sent over the network, because
 * the {@link Expression expression API} is not serializable.</i>
 * </p>
 * 
 * @since 5.4
 */
final class SnomedEclEvaluationRequest implements Request<BranchContext, Promise<Expression>> {

	private static final long serialVersionUID = 5891665196136989183L;
	
	private final PolymorphicDispatcher<Promise<Expression>> dispatcher = PolymorphicDispatcher.createForSingleTarget("eval", 2, 2, this);
	
	private static final Map<String, String> ACCEPTABILITY_ID_TO_FIELD = Map.of(
		Concepts.REFSET_DESCRIPTION_ACCEPTABILITY_PREFERRED, "preferredIn",
		Concepts.REFSET_DESCRIPTION_ACCEPTABILITY_ACCEPTABLE, "acceptableIn"
	);

	@Nullable
	@JsonProperty
	private String expression;
	
	@NotNull
	@JsonProperty
	private String expressionForm = Trees.INFERRED_FORM;

	SnomedEclEvaluationRequest() {
	}

	void setExpression(String expression) {
		this.expression = expression;
	}
	
	void setExpressionForm(String expressionForm) {
		this.expressionForm = expressionForm;
	}

	@Override
	public Promise<Expression> execute(BranchContext context) {
		final ExpressionConstraint expressionConstraint = context.service(EclParser.class).parse(expression);
		return doEval(context, expressionConstraint);
	}

	Promise<Expression> doEval(BranchContext context, final ExpressionConstraint expressionConstraint) {
		ExpressionConstraint rewritten = new SnomedEclRewriter().rewrite(expressionConstraint);
		return evaluate(context, rewritten);
	}
	
	private Promise<Expression> evaluate(BranchContext context, EObject expression) {
		return dispatcher.invoke(context, expression);
	}

	protected Promise<Expression> eval(BranchContext context, EObject eObject) {
		return throwUnsupported(eObject);
	}

	/**
	 * Handles ANY simple expression constraints
	 * @see https://confluence.ihtsdotools.org/display/DOCECL/6.1+Simple+Expression+Constraints  
	 */
	protected Promise<Expression> eval(BranchContext context, Any any) {
		return Promise.immediate(Expressions.matchAll());
	}
	
	/**
	 * Handles EclConceptReference/Self simple expression constraints
	 * @see https://confluence.ihtsdotools.org/display/DOCECL/6.1+Simple+Expression+Constraints
	 */
	protected Promise<Expression> eval(BranchContext context, EclConceptReference concept) {
		return Promise.immediate(id(concept.getId()));
	}
	
	protected Promise<Expression> eval(BranchContext context, EclConceptReferenceSet conceptSet) {
		final Set<String> conceptIds = conceptSet.getConcepts()
			.stream()
			.map(EclConceptReference::getId)
			.collect(ImmutableSet.toImmutableSet());
		
		return Promise.immediate(ids(conceptIds));
	}
	
	/**
	 * Handles MemberOf simple expression constraints
	 * @see https://confluence.ihtsdotools.org/display/DOCECL/6.1+Simple+Expression+Constraints
	 */
	protected Promise<Expression> eval(BranchContext context, MemberOf memberOf) {
		final ExpressionConstraint inner = memberOf.getConstraint();
		if (inner instanceof EclConceptReference) {
			final EclConceptReference concept = (EclConceptReference) inner;
			return Promise.immediate(activeMemberOf(concept.getId()));
		} else if (isAnyExpression(inner)) {
			return Promise.immediate(Expressions.exists(ACTIVE_MEMBER_OF));
		} else if (inner instanceof NestedExpression) {
			return EclExpression.of(inner, expressionForm)
					.resolve(context)
					.then(ids -> activeMemberOf(ids));
		} else {
			return throwUnsupported(inner);
		}
	}
	
	/**
	 * Handles DescendantsOf simple expression constraints
	 * @see https://confluence.ihtsdotools.org/display/DOCECL/6.1+Simple+Expression+Constraints
	 */
	protected Promise<Expression> eval(BranchContext context, final DescendantOf descendantOf) {
		final ExpressionConstraint inner = descendantOf.getConstraint();
		// <* should eval to * MINUS parents IN (ROOT_ID)
		if (isAnyExpression(inner)) {
			return Promise.immediate(Expressions.bool()
					.mustNot(parentsExpression(Collections.singleton(IComponent.ROOT_ID)))
					.build());
		} else {
			return evaluate(context, inner)
					.thenWith(resolveIds(context, inner, expressionForm))
					.then(ids -> Expressions.bool()
							.should(parentsExpression(ids))
							.should(ancestorsExpression(ids))
							.build());
		}
	}
	
	/**
	 * Handles DescendantOrSelfOf simple expression constraints
	 * @see https://confluence.ihtsdotools.org/display/DOCECL/6.1+Simple+Expression+Constraints
	 */
	protected Promise<Expression> eval(BranchContext context, final DescendantOrSelfOf descendantOrSelfOf) {
		final ExpressionConstraint inner = descendantOrSelfOf.getConstraint();
		// <<* should eval to *
		if (isAnyExpression(inner)) {
			return evaluate(context, inner);
		} else {
			return evaluate(context, inner)
					.thenWith(resolveIds(context, inner, expressionForm))
					.then(ids -> Expressions.bool()
							.should(ids(ids))
							.should(parentsExpression(ids))
							.should(ancestorsExpression(ids))
							.build());
		}
	}
	
	/**
	 * Handles ChildOf simple expression constraints
	 * @see https://confluence.ihtsdotools.org/display/DOCECL/6.1+Simple+Expression+Constraints
	 */
	protected Promise<Expression> eval(BranchContext context, final ChildOf childOf) {
		final ExpressionConstraint innerConstraint = childOf.getConstraint();
		// <!* should eval to * MINUS parents in (ROOT_ID)
		if (isAnyExpression(innerConstraint)) {
			return Promise.immediate(Expressions.bool()
					.mustNot(parentsExpression(Collections.singleton(IComponent.ROOT_ID)))
					.build());
		} else {
			return evaluate(context, innerConstraint)
					.thenWith(resolveIds(context, innerConstraint, expressionForm))
					.then(ids -> parentsExpression(ids));
		}
	}
	
	/**
	 * Handles ChildOrSelfOf simple expression constraints
	 * @see https://confluence.ihtsdotools.org/display/DOCECL/6.1+Simple+Expression+Constraints
	 */
	protected Promise<Expression> eval(BranchContext context, final ChildOrSelfOf childOrSelfOf) {
		final ExpressionConstraint innerConstraint = childOrSelfOf.getConstraint();
		// <<!* should eval to * (direct child or self of all concept IDs === all concept IDs)
		if (isAnyExpression(innerConstraint)) {
			return evaluate(context, innerConstraint);
		} else {
			return evaluate(context, innerConstraint)
					.thenWith(resolveIds(context, innerConstraint, expressionForm))
					.then(ids -> Expressions.bool()
							.should(ids(ids))
							.should(parentsExpression(ids))
							.build());
		}
	}
	
	/**
	 * Handles ParentOf simple expression constraints
	 * @see https://confluence.ihtsdotools.org/display/DOCECL/6.1+Simple+Expression+Constraints
	 */
	protected Promise<Expression> eval(BranchContext context, final ParentOf parentOf) {
		return EclExpression.of(parentOf.getConstraint(), expressionForm)
				.resolveConcepts(context)
				.then(concepts -> {
					final Set<String> parents = newHashSet();
					for (SnomedConcept concept : concepts) {
						addParentIds(concept, parents);
					}
					return parents;
				})
				.then(matchIdsOrNone());
	}
	
	/**
	 * Handles ParentOrSelfOf simple expression constraints
	 * @see https://confluence.ihtsdotools.org/display/DOCECL/6.1+Simple+Expression+Constraints
	 */
	protected Promise<Expression> eval(BranchContext context, final ParentOrSelfOf parentOrSelfOf) {
		return EclExpression.of(parentOrSelfOf.getConstraint(), expressionForm)
				.resolveConcepts(context)
				.then(concepts -> {
					final Set<String> results = newHashSet();
					for (SnomedConcept concept : concepts) {
						results.add(concept.getId());
						addParentIds(concept, results);
					}
					return results;
				})
				.then(matchIdsOrNone());
	}
	
	/**
	 * Handles AncestorOf simple expression constraints
	 * @see https://confluence.ihtsdotools.org/display/DOCECL/6.1+Simple+Expression+Constraints
	 */
	protected Promise<Expression> eval(BranchContext context, final AncestorOf ancestorOf) {
		return EclExpression.of(ancestorOf.getConstraint(), expressionForm)
				.resolveConcepts(context)
				.then(concepts -> {
					final Set<String> ancestors = newHashSet();
					for (SnomedConcept concept : concepts) {
						addParentIds(concept, ancestors);
						addAncestorIds(concept, ancestors);
					}
					return ancestors;
				})
				.then(matchIdsOrNone());
	}
	
	/**
	 * Handles AncestorOrSelfOf simple expression constraints
	 * @see https://confluence.ihtsdotools.org/display/DOCECL/6.1+Simple+Expression+Constraints
	 */
	protected Promise<Expression> eval(BranchContext context, final AncestorOrSelfOf ancestorOrSelfOf) {
		final ExpressionConstraint innerConstraint = ancestorOrSelfOf.getConstraint();
		// >>* should eval to *
		if (isAnyExpression(innerConstraint)) {
			return evaluate(context, innerConstraint);
		} else {
			return EclExpression.of(innerConstraint, expressionForm)
					.resolveConcepts(context)
					.then(concepts -> {
						final Set<String> ancestors = newHashSet();
						for (SnomedConcept concept : concepts) {
							ancestors.add(concept.getId());
							addParentIds(concept, ancestors);
							addAncestorIds(concept, ancestors);
						}
						return ancestors;
					})
					.then(matchIdsOrNone());
		}
	}
	
	/**
	 * Handles conjunction binary operator expressions
	 * @see https://confluence.ihtsdotools.org/display/DOCECL/6.4+Conjunction+and+Disjunction
	 */
	protected Promise<Expression> eval(BranchContext context, final AndExpressionConstraint and) {
		if (isAnyExpression(and.getLeft())) {
			return evaluate(context, and.getRight());
		} else if (isAnyExpression(and.getRight())) {
			return evaluate(context, and.getLeft());
		} else if (isEclConceptReference(and.getRight())) {
			// if the right hand is an ID, then iterate and check if the entire tree is ID1 AND ID2 AND IDN
			final Set<String> ids = newHashSet();
			TreeIterator<EObject> it = and.eAllContents();
			while (it.hasNext()) {
				EObject content = it.next();
				// accept only EclConceptReference/Nested and AND expressions, anything else will break the loop
				if (content instanceof EclConceptReference) {
					ids.add(((EclConceptReference) content).getId());
				} else if (content instanceof NestedExpression || content instanceof AndExpressionConstraint) {
					// continue
				} else {
					// remove any IDs collected
					ids.clear();
					break;
				}
			}
			if (!CompareUtils.isEmpty(ids)) {
				return Promise.immediate(Expressions.matchNone());
			}
		}
		return Promise.all(evaluate(context, and.getLeft()), evaluate(context, and.getRight()))
				.then(innerExpressions -> {
					final Expression left = (Expression) innerExpressions.get(0);
					final Expression right = (Expression) innerExpressions.get(1);
					return Expressions.bool()
							.filter(left)
							.filter(right)
							.build();
				});
	}
	
	/**
	 * Handles disjunction binary operator expressions
	 * @see https://confluence.ihtsdotools.org/display/DOCECL/6.4+Conjunction+and+Disjunction
	 */
	protected Promise<Expression> eval(BranchContext context, final OrExpressionConstraint or) {
		if (isAnyExpression(or.getLeft())) {
			return evaluate(context, or.getLeft());
		} else if (isAnyExpression(or.getRight())) {
			return evaluate(context, or.getRight());
		} else if (isEclConceptReference(or.getRight())) {
			// if the right hand is an ID, then iterate and check if the entire tree is ID1 OR ID2 OR IDN
			final Set<String> ids = newHashSet();
			TreeIterator<EObject> it = or.eAllContents();
			while (it.hasNext()) {
				EObject content = it.next();
				// accept only EclConceptReference/Nested and OR expressions, anything else will break the loop
				if (content instanceof EclConceptReference) {
					ids.add(((EclConceptReference) content).getId());
				} else if (content instanceof NestedExpression || content instanceof OrExpressionConstraint) {
					// continue
				} else {
					// remove any IDs collected
					ids.clear();
					break;
				}
			}
			if (!CompareUtils.isEmpty(ids)) {
				return Promise.immediate(ids(ids));
			}
		}
		return Promise.all(evaluate(context, or.getLeft()), evaluate(context, or.getRight()))
				.then(innerExpressions -> {
					final Expression left = (Expression) innerExpressions.get(0);
					final Expression right = (Expression) innerExpressions.get(1);
					return Expressions.bool()
							.should(left)
							.should(right)
							.build();
				});
	}

	/**
	 * Handles exclusion binary operator expressions
	 * 
	 * @see https://confluence.ihtsdotools.org/display/DOCECL/6.5+Exclusion+and+Not+Equals
	 */
	protected Promise<Expression> eval(BranchContext context, final ExclusionExpressionConstraint exclusion) {
		return evaluate(context, exclusion.getRight()).thenWith(right -> {
			if (right.isMatchAll()) {
				// excluding everything should result in no matches
				return Promise.immediate(Expressions.matchNone());
			} else if (right.isMatchNone()) {
				// excluding nothing is just the left query
				return evaluate(context, exclusion.getLeft());
			} else {
				return evaluate(context, exclusion.getLeft()).then(left -> {
					// match left hand side query and not the right hand side query
					return Expressions.bool().filter(left).mustNot(right).build();
				});
			}
		});
	}
	
	/**
	 * Delegates evaluation of Refinement expression constraints to {@link SnomedEclRefinementEvaluator}.
	 */
	protected Promise<Expression> eval(final BranchContext context, final RefinedExpressionConstraint refined) {
		return new SnomedEclRefinementEvaluator(EclExpression.of(refined.getConstraint(), expressionForm)).evaluate(context, refined.getRefinement());
	}
	
	/**
	 * Handles dotted expression constraints (reversed attribute refinement with dot notation)
	 * @see https://confluence.ihtsdotools.org/display/DOCECL/6.2+Refinements
	 */
	protected Promise<Expression> eval(BranchContext context, DottedExpressionConstraint dotted) {
		final Promise<Set<String>> focusConceptIds = SnomedEclRefinementEvaluator.evalToConceptIds(context, dotted.getConstraint(), expressionForm);
		final Promise<Set<String>> typeConceptIds = SnomedEclRefinementEvaluator.evalToConceptIds(context, dotted.getAttribute(), expressionForm);
		return Promise.all(focusConceptIds, typeConceptIds)
			.thenWith(responses -> {
				Set<String> focusConcepts = (Set<String>) responses.get(0);
				Set<String> typeConcepts = (Set<String>) responses.get(1);
				return SnomedEclRefinementEvaluator.evalStatements(context, focusConcepts, typeConcepts, null /* ANY */, false, expressionForm);
			})
			.then(new Function<Collection<SnomedEclRefinementEvaluator.Property>, Set<String>>() {
				@Override
				public Set<String> apply(Collection<SnomedEclRefinementEvaluator.Property> input) {
					return FluentIterable.from(input).transform(SnomedEclRefinementEvaluator.Property::getValue).filter(String.class).toSet();
				}
			})
			.then(matchIdsOrNone());
	}
	
	/**
	 * Handles nested expression constraints by simple evaluation the nested expression.
	 */
	protected Promise<Expression> eval(BranchContext context, final NestedExpression nested) {
		return evaluate(context, nested.getNested());
	}
	
	/**
	 * Handles (possibly) filtered expression constraints by evaluating them along
	 * with the primary ECL expression, and adding the resulting query expressions as
	 * extra required clauses.
	 * 
	 * @param context
	 * @param filtered
	 * @return
	 */
	protected Promise<Expression> eval(BranchContext context, final FilteredExpressionConstraint filtered) {
		final ExpressionConstraint constraint = filtered.getConstraint();
		final FilterConstraint filterConstraint = filtered.getFilter();
		final Domain filterDomain = Ecl.getDomain(filterConstraint);
		final Filter filter = filterConstraint.getFilter();
		
		final Promise<Expression> evaluatedConstraint = evaluate(context, constraint);
		Promise<Expression> evaluatedFilter = evaluate(context, filter);
		if (Domain.DESCRIPTION.equals(filterDomain)) {
			// Find concepts that match the description expression, then use the resulting concept IDs as the expression
			evaluatedFilter = evaluatedFilter.then(ex -> executeDescriptionSearch(context, ex));
		}
		
		if (isAnyExpression(constraint)) {
			// No need to combine "match all" with the filter query expression, return it directly
			return evaluatedFilter;
		}
		
		return Promise.all(evaluatedConstraint, evaluatedFilter).then(results -> {
			final Expressions.ExpressionBuilder builder = Expressions.bool();
			results.forEach(f -> builder.filter((Expression) f));
			return builder.build();
		});
	}
	
	private static Expression executeDescriptionSearch(BranchContext context, Expression descriptionExpression) {
		if (descriptionExpression.isMatchAll()) {
			return Expressions.matchAll();
		} else if (descriptionExpression.isMatchNone()) {
			return SnomedDocument.Expressions.ids(Set.of());
		}
		
		final RevisionSearcher searcher = context.service(RevisionSearcher.class);
		try {
			
			final Query<String> descriptionIndexQuery = Query.select(String.class)
				.from(SnomedDescriptionIndexEntry.class)
				.fields(SnomedDescriptionIndexEntry.Fields.CONCEPT_ID)
				.where(descriptionExpression)
				.limit(Integer.MAX_VALUE)
				.build();
			
			final Hits<String> descriptionHits = searcher.search(descriptionIndexQuery);
			final Set<String> conceptIds = Set.copyOf(descriptionHits.getHits());
			return SnomedDocument.Expressions.ids(conceptIds);

		} catch (IOException e) {
			throw new SnowowlRuntimeException(e);
		}
	}

	protected Promise<Expression> eval(BranchContext context, final PropertyFilter nestedFilter) {
		return throwUnsupported(nestedFilter);
	}
	
	protected Promise<Expression> eval(BranchContext context, final NestedFilter nestedFilter) {
		return evaluate(context, nestedFilter.getNested());
	}
	
	protected Promise<Expression> eval(BranchContext context, final ActiveFilter activeFilter) {
		return Promise.immediate(SnomedDocument.Expressions.active(activeFilter.isActive()));
	}
	
	protected Promise<Expression> eval(BranchContext context, final ModuleFilter moduleFilter) {
		final FilterValue moduleId = moduleFilter.getModuleId();
		return evaluate(context, moduleId)
			.thenWith(resolveIds(context, moduleId, expressionForm))
			.then(SnomedDocument.Expressions::modules);
	}
	
	protected Promise<Expression> eval(BranchContext context, final EffectiveTimeFilter effectiveFilter) {
		final String effectiveTimeAsString = effectiveFilter.getEffectiveTime();
		final long effectiveTime = EffectiveTimes.getEffectiveTime(effectiveTimeAsString, DateFormats.SHORT);
		final String op = effectiveFilter.getOp();
		final Operator eclOperator = Operator.fromString(op);
		
		final Expression expression;
		switch (eclOperator) {
			case EQUALS:
				expression = SnomedDocument.Expressions.effectiveTime(effectiveTime);
				break;
			case GT:
				expression = SnomedDocument.Expressions.effectiveTime(effectiveTime, Long.MAX_VALUE, false, true);
				break;
			case GTE:
				expression = SnomedDocument.Expressions.effectiveTime(effectiveTime, Long.MAX_VALUE, true, true);
				break;
			case LT:
				expression = SnomedDocument.Expressions.effectiveTime(Long.MIN_VALUE, effectiveTime, true, false);
				break;
			case LTE:
				expression = SnomedDocument.Expressions.effectiveTime(Long.MIN_VALUE, effectiveTime, true, true);
				break;
			case NOT_EQUALS:
				expression = Expressions.bool()
					.mustNot(SnomedDocument.Expressions.effectiveTime(effectiveTime))
					.build();
				break;
			default:
				throw new IllegalStateException("Unhandled ECL search operator '" + eclOperator + "'.");
		}

		return Promise.immediate(expression);
	}

	protected Promise<Expression> eval(BranchContext context, final DefinitionStatusIdFilter definitionStatusIdFilter) {
		final String op = definitionStatusIdFilter.getOp();
		final Operator eclOperator = Operator.fromString(op);
		final FilterValue definitionStatus = definitionStatusIdFilter.getDefinitionStatus();
		
		return evalDefinitionStatus(evaluate(context, definitionStatus)
				.thenWith(resolveIds(context, definitionStatus, expressionForm)),
				Operator.NOT_EQUALS.equals(eclOperator));
	}
	
	protected Promise<Expression> eval(BranchContext context, final DefinitionStatusTokenFilter definitionStatusTokenFilter) {
		final String op = definitionStatusTokenFilter.getOp();
		final Operator eclOperator = Operator.fromString(op);

		final Set<String> definitionStatusIds = definitionStatusTokenFilter.getDefinitionStatusTokens()
			.stream()
			.map(DefinitionStatusToken::fromString)
			.filter(Predicates.notNull())
			.map(DefinitionStatusToken::getConceptId)
			.collect(Collectors.toSet());
		
		return evalDefinitionStatus(Promise.immediate(definitionStatusIds), 
				Operator.NOT_EQUALS.equals(eclOperator));
	}
	
	private Promise<Expression> evalDefinitionStatus(final Promise<? extends Iterable<String>> definitionStatusIds, final boolean negate) {
		return definitionStatusIds				
			.then(SnomedConceptDocument.Expressions::definitionStatusIds)
			.then(expression -> negate
				? Expressions.bool().mustNot(expression).build()
				: expression);
	}

	protected Promise<Expression> eval(BranchContext context, final SemanticTagFilter semanticTagFilter) {
		final String op = semanticTagFilter.getOp();
		final Operator eclOperator = Operator.fromString(op);
		
		// XXX: Both concept and description documents support the same field and query
		Expression expression = SnomedDescriptionIndexEntry.Expressions.semanticTags(List.of(semanticTagFilter.getSemanticTag()));
		if (Operator.NOT_EQUALS.equals(eclOperator)) {
			expression = Expressions.bool()
				.mustNot(expression)
				.build();
		}
		
		return Promise.immediate(expression);
	}

	protected Promise<Expression> eval(BranchContext context, final TermFilter termFilter) {
		final List<TypedSearchTermClause> clauses;
		
		SearchTerm searchTerm = termFilter.getSearchTerm();
		if (searchTerm instanceof TypedSearchTerm) {
			clauses = List.of(((TypedSearchTerm) searchTerm).getClause());
		} else if (searchTerm instanceof TypedSearchTermSet) {
			clauses = ((TypedSearchTermSet) searchTerm).getClauses();
		} else {
			return throwUnsupported(searchTerm);
		}
		
		// Filters are combined with an OR ("should") operator
		final Expressions.ExpressionBuilder builder = Expressions.bool();
		
		for (final TypedSearchTermClause clause : clauses) {
			builder.should(toExpression(clause));
		}
		
		return Promise.immediate(builder.build());
	}
	
	protected Expression toExpression(final TypedSearchTermClause clause) {
		final String term = clause.getTerm();

		LexicalSearchType lexicalSearchType = LexicalSearchType.fromString(clause.getLexicalSearchType());
		if (lexicalSearchType == null) {
			lexicalSearchType = LexicalSearchType.MATCH;
		}

		switch (lexicalSearchType) {
			case MATCH:
				final com.b2international.snowowl.core.request.TermFilter match = com.b2international.snowowl.core.request.TermFilter.defaultTermMatch(term);
				return SnomedDescriptionIndexEntry.Expressions.termDisjunctionQuery(match);
			case WILD:
				final String regexTerm = term.replace("*", ".*");
				return SnomedDescriptionIndexEntry.Expressions.matchTermRegex(regexTerm);
			case REGEX:
				return SnomedDescriptionIndexEntry.Expressions.matchTermRegex(term);
			case EXACT:
				return SnomedDescriptionIndexEntry.Expressions.matchTermCaseInsensitive(term);
			default:
				throw new UnsupportedOperationException("Not implemented lexical search type: '" + lexicalSearchType + "'.");
		}
	}

	protected Promise<Expression> eval(BranchContext context, final TypeTokenFilter typeTokenFilter) {
		final Set<String> typeIds = typeTokenFilter.getTokens()
			.stream()
			.map(DescriptionTypeToken::fromString)
			.filter(Predicates.notNull())
			.map(DescriptionTypeToken::getConceptId)
			.collect(Collectors.toSet());
		
		return Promise.immediate(SnomedDescriptionIndexEntry.Expressions.types(typeIds));
	}
	
	protected Promise<Expression> eval(BranchContext context, final TypeIdFilter typeIdFilter) {
		final FilterValue type = typeIdFilter.getType();
		return evaluate(context, type)
			.thenWith(resolveIds(context, type, expressionForm))
			.then(SnomedDescriptionIndexEntry.Expressions::types);
	}
	
	protected Promise<Expression> eval(BranchContext context, final PreferredInFilter preferredInFilter) {
		final FilterValue languageRefSetId = preferredInFilter.getLanguageRefSetId();
		return evaluate(context, languageRefSetId)
			.thenWith(resolveIds(context, languageRefSetId, expressionForm))
			.then(SnomedDescriptionIndexEntry.Expressions::preferredIn);
	}
	
	protected Promise<Expression> eval(BranchContext context, final AcceptableInFilter acceptableInFilter) {
		final FilterValue languageRefSetId = acceptableInFilter.getLanguageRefSetId();
		return evaluate(context, languageRefSetId)
			.thenWith(resolveIds(context, languageRefSetId, expressionForm))
			.then(SnomedDescriptionIndexEntry.Expressions::acceptableIn);
	}
	
	protected Promise<Expression> eval(BranchContext context, final LanguageRefSetFilter languageRefSetFilter) {
		final FilterValue languageRefSetId = languageRefSetFilter.getLanguageRefSetId();
		return evaluate(context, languageRefSetId)
			.thenWith(resolveIds(context, languageRefSetId, expressionForm))
			.then(languageReferenceSetIds -> {
				return Expressions.bool()
					.should(SnomedDescriptionIndexEntry.Expressions.acceptableIn(languageReferenceSetIds))
					.should(SnomedDescriptionIndexEntry.Expressions.preferredIn(languageReferenceSetIds))
					.build();
			});
	}
	
	protected Promise<Expression> eval(BranchContext context, final CaseSignificanceFilter caseSignificanceFilter) {
		final FilterValue caseSignificanceId = caseSignificanceFilter.getCaseSignificanceId();
		return evaluate(context, caseSignificanceId)
			.thenWith(resolveIds(context, caseSignificanceId, expressionForm))
			.then(SnomedDescriptionIndexEntry.Expressions::caseSignificances);
	}
	
	protected Promise<Expression> eval(BranchContext context, final LanguageFilter languageCodeFilter) {
		return Promise.immediate(SnomedDescriptionIndexEntry.Expressions.languageCodes(languageCodeFilter.getLanguageCodes()));
	}
	
	protected Promise<Expression> eval(BranchContext context, final ConjunctionFilter conjunction) {
		return Promise.all(evaluate(context, conjunction.getLeft()), evaluate(context, conjunction.getRight()))
				.then(results -> {
					Expression left = (Expression) results.get(0);
					Expression right = (Expression) results.get(1);
					return Expressions.bool()
							.filter(left)
							.filter(right)
							.build();
				});
	}
	
	protected Promise<Expression> eval(BranchContext context, final DisjunctionFilter disjunction) {
		return Promise.all(evaluate(context, disjunction.getLeft()), evaluate(context, disjunction.getRight()))
				.then(results -> {
					Expression left = (Expression) results.get(0);
					Expression right = (Expression) results.get(1);
					return Expressions.bool()
							.should(left)
							.should(right)
							.build();
				});
	}
	
	protected Promise<Expression> eval(BranchContext context, final DialectAliasFilter dialectAliasFilter) {
		final ListMultimap<String, String> languageMapping = SnomedDescriptionUtils.getLanguageMapping(context);
		final Multimap<String, String> languageRefSetsByAcceptabilityField = HashMultimap.create();
		final ExpressionBuilder dialectQuery = Expressions.bool();
		for (DialectAlias alias : dialectAliasFilter.getDialects()) {
			final Set<String> acceptabilityFields = getAcceptabilityFields(alias.getAcceptability());
			final Collection<String> languageReferenceSetIds = languageMapping.get(alias.getAlias());
			
			// empty acceptabilities or empty language reference set IDs mean that none of the provided values were valid so no match should be returned
			if (acceptabilityFields.isEmpty() || languageReferenceSetIds.isEmpty()) {
				return Promise.immediate(Expressions.matchNone());
			}
			
			for (String acceptabilityField : acceptabilityFields) {
				languageRefSetsByAcceptabilityField.putAll(acceptabilityField, languageReferenceSetIds);
			}
		}
		
		languageRefSetsByAcceptabilityField.asMap().forEach((key, values) -> {
			Operator op = Operator.fromString(dialectAliasFilter.getOp());
			switch (op) {
				case EQUALS: 
					dialectQuery.should(Expressions.matchAny(key, values));
					break;
				case NOT_EQUALS:
					dialectQuery.mustNot(Expressions.matchAny(key, values));
					break;
				default: 
					throw new NotImplementedException("Unsupported dialectAliasFilter operator '%s'", dialectAliasFilter.getOp());
			}
		});
		
		return Promise.immediate(dialectQuery.build());
	}
	
	protected Promise<Expression> eval(BranchContext context, final DialectIdFilter dialectIdFilter) {
		final Multimap<String, String> languageRefSetsByAcceptability = HashMultimap.create();
		final ExpressionBuilder dialectQuery = Expressions.bool();
		for (Dialect dialect : dialectIdFilter.getDialects()) {
			final ExpressionConstraint languageRefSetId = dialect.getLanguageRefSetId();
			final Set<String> evaluatedIds = EclExpression.of(languageRefSetId, expressionForm)
				.resolve(context)
				.getSync();

			final Set<String> acceptabilitiesToMatch = getAcceptabilityFields(dialect.getAcceptability());
			// empty set means that acceptability values are not valid and it should not match any descriptions/concepts
			if (acceptabilitiesToMatch.isEmpty()) {
				return Promise.immediate(Expressions.matchNone());
			}
			
			for (String acceptability : acceptabilitiesToMatch) {
				languageRefSetsByAcceptability.putAll(acceptability, evaluatedIds);
			}
		}
		
		languageRefSetsByAcceptability.asMap().forEach((key, values) -> {
			Operator op = Operator.fromString(dialectIdFilter.getOp());
			switch (op) {
			case EQUALS: 
				dialectQuery.should(Expressions.matchAny(key, values));
				break;
			case NOT_EQUALS:
				dialectQuery.mustNot(Expressions.matchAny(key, values));
				break;
			default: 
				throw new NotImplementedException("Unsupported dialectIdFilter operator '%s'", dialectIdFilter.getOp());
			}
		});
		
		return Promise.immediate(dialectQuery.build());
	}

	private Set<String> getAcceptabilityFields(Acceptability acceptability) {
		// in case of acceptability not defined accept any known acceptability value
		if (acceptability == null) {
			return Set.of("preferredIn", "acceptableIn");
		} else if (acceptability instanceof AcceptabilityIdSet) {
			final AcceptabilityIdSet acceptabilityIdSet = (AcceptabilityIdSet) acceptability;
			return acceptabilityIdSet.getAcceptabilities()
				.getConcepts()
				.stream()
				.map(EclConceptReference::getId)
				.filter(ACCEPTABILITY_ID_TO_FIELD::containsKey)
				.map(ACCEPTABILITY_ID_TO_FIELD::get)
				.collect(Collectors.toSet());
		} else if (acceptability instanceof AcceptabilityTokenSet) {
			final AcceptabilityTokenSet acceptabilityTokenSet = (AcceptabilityTokenSet) acceptability;
			return acceptabilityTokenSet.getAcceptabilities()
				.stream()
				.map(AcceptabilityToken::fromString)
				.filter(Predicates.notNull())
				.map(AcceptabilityToken::getConceptId)
				.filter(ACCEPTABILITY_ID_TO_FIELD::containsKey)
				.map(ACCEPTABILITY_ID_TO_FIELD::get)
				.collect(Collectors.toSet());
		} else {
			throwUnsupported(acceptability);
			return Set.of();
		}
	}

	/**
	 * Handles cases when the expression constraint is not available at all. For instance, script is empty.
	 */
	protected Promise<Expression> eval(BranchContext context, final Void empty) {
		return Promise.immediate(MatchNone.INSTANCE);
	}
	
	/*package*/ static <T> Promise<T> throwUnsupported(EObject eObject) {
		throw new NotImplementedException("Not implemented ECL feature: %s", eObject.eClass().getName());
	}
	
	static boolean canExtractIds(Expression expression) {
		return expression instanceof Predicate && ID.equals(((Predicate) expression).getField());
	}
	
	/*Extract SNOMED CT IDs from the given expression if it is either a String/Long single/multi-valued predicate and the field is equal to RevisionDocument.Fields.ID*/
	/*package*/ static Set<String> extractIds(Expression expression) {
		if (!canExtractIds(expression)) {
			throw new UnsupportedOperationException("Cannot extract ID values from: " + expression);
		}
		if (expression instanceof StringSetPredicate) {
			return ((StringSetPredicate) expression).values();
		} else {
			return Collections.singleton(((StringPredicate) expression).getArgument());
		}
	}
	
	/**
	 * Extracts SNOMED CT IDs from the given expression if it is either single or multi-valued 
	 * String predicate and the field is equal to RevisionDocument.Fields.ID. Otherwise it will 
	 * treat the received value as an ECL expression and evaluates it  completely without using 
	 * the returned index query expression.
	 * 
	 * @param filterValue
	 */
	private static Function<Expression, Promise<Set<String>>> resolveIds(BranchContext context, FilterValue filterValue, String expressionForm) {
		return expression -> {
			try {
				/* 
				 * It should always be possible to extract identifiers from an index query expression derived from 
				 * an EclConceptReferenceSet, and occasionally ExpressionConstraints also have this property.
				 */
				return Promise.immediate(extractIds(expression));
			} catch (UnsupportedOperationException e) {
				/* 
				 * If ID extraction failed, the original filter value must be an ExpressionConstraint that is more
				 * complex. Evaluate it to retrieve the SCTIDs. 
				 */
				return EclExpression.of((ExpressionConstraint) filterValue, expressionForm).resolve(context);
			}
		};
	}
	
	private Expression parentsExpression(Set<String> ids) {
		return Trees.INFERRED_FORM.equals(expressionForm) ? SnomedConceptDocument.Expressions.parents(ids) : SnomedConceptDocument.Expressions.statedParents(ids);
	}

	private Expression ancestorsExpression(Set<String> ids) {
		return Trees.INFERRED_FORM.equals(expressionForm) ? SnomedConceptDocument.Expressions.ancestors(ids) : SnomedConceptDocument.Expressions.statedAncestors(ids);
	}
	
	private void addParentIds(SnomedConcept concept, final Set<String> collection) {
		if (Trees.INFERRED_FORM.equals(expressionForm)) {
			if (concept.getParentIds() != null) {
				for (long parent : concept.getParentIds()) {
					if (IComponent.ROOT_IDL != parent) {
						collection.add(Long.toString(parent));
					}
				}
			}
		} else {
			if (concept.getStatedParentIds() != null) {
				for (long statedParent : concept.getStatedParentIds()) {
					if (IComponent.ROOT_IDL != statedParent) {
						collection.add(Long.toString(statedParent));
					}
				}
			}
		}
	}
	
	private void addAncestorIds(SnomedConcept concept, Set<String> collection) {
		if (Trees.INFERRED_FORM.equals(expressionForm)) {
			if (concept.getAncestorIds() != null) {
				for (long ancestor : concept.getAncestorIds()) {
					if (IComponent.ROOT_IDL != ancestor) {
						collection.add(Long.toString(ancestor));
					}
				}
			}
		} else {
			if (concept.getStatedAncestorIds() != null) {
				for (long statedAncestor : concept.getStatedAncestorIds()) {
					if (IComponent.ROOT_IDL != statedAncestor) {
						collection.add(Long.toString(statedAncestor));
					}
				}
			}
		}		
	}
	
	/*package*/ static Function<Set<String>, Expression> matchIdsOrNone() {
		return ids -> ids.isEmpty() ? Expressions.matchNone() : ids(ids);
	}
}
