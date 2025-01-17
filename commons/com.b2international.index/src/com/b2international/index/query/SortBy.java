/*
 * Copyright 2011-2021 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.index.query;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.collect.Lists.newArrayList;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.elasticsearch.search.sort.ScriptSortBuilder.ScriptSortType;

import com.b2international.index.ScriptExpression;
import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

/**
 * @since 4.7
 */
public abstract class SortBy {
	
	public static enum Order {
		ASC, 
		DESC;
	}

	/**
	 * Special field name for sorting based on the document score (relevance).
	 */
	public static final String FIELD_SCORE = "_score";
	
	/**
	 * Special field name for sorting by the default sort field.
	 */
	public static final String FIELD_DEFAULT = "_default";
	
	/**
	 * Singleton representing document sort based on their default sort field (usually the ID, but in case of scroll we can use _doc to speed things up) in ascending order.
	 */
	public static final SortByField DEFAULT = SortBy.field(FIELD_DEFAULT, Order.ASC);
	
	/**
	 * Singleton representing document sort based on their score in decreasing order (higher score first).
	 */
	public static final SortBy SCORE = SortBy.field(FIELD_SCORE, Order.DESC);
	
	/**
	 * @since 5.0
	 */
	public static final class SortByField extends SortBy {
		private final String field;
		private final Order order;

		private SortByField(String field, Order order) {
			this.field = checkNotNull(field, "field");
			this.order = checkNotNull(order, "order");
		}

		public String getField() {
			return field;
		}
		
		public Order getOrder() {
			return order;
		}

		@Override
		public int hashCode() {
			return Objects.hash(field, order);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) { return true; }
			if (obj == null) { return false; }
			if (getClass() != obj.getClass()) { return false; }
			
			SortByField other = (SortByField) obj;
			if (!Objects.equals(field, other.field)) { return false; }
			if (order != other.order) { return false; }
			return true;
		}

		@Override
		public String toString() {
			return field + " " + order;
		}
	}
	
	/**
	 * @since 6.3
	 */
	public static final class SortByScript extends SortBy implements ScriptExpression {

		private final Order order;
		private final String name;
		private final Map<String, Object> params;
		private final ScriptSortType sortType;

		private SortByScript(String name, Map<String, Object> params, Order order, ScriptSortType sortType) {
			this.name = name;
			this.params = params;
			this.order = order;
			this.sortType = sortType;
		}
		
		public Order getOrder() {
			return order;
		}
		
		@Override
		public String getScript() {
			return name;
		}
		
		@Override
		public Map<String, Object> getParams() {
			return params;
		}
		
		public ScriptSortType getSortType() {
			return sortType;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(name, params, order);
		}
		
		@Override
		public boolean equals(Object obj) {
			if (this == obj) { return true; }
			if (obj == null) { return false; }
			if (getClass() != obj.getClass()) { return false; }
			
			SortByScript other = (SortByScript) obj;
			return Objects.equals(name, other.name) 
					&& Objects.equals(params, other.params)
					&& Objects.equals(order, other.order); 
		}
		
		@Override
		public String toString() {
			return name + " " + params + " " + order;
		}
		
	}
	
	/**
	 * @since 5.0
	 */
	public static final class MultiSortBy extends SortBy {
		private final List<SortBy> items;

		private MultiSortBy(List<SortBy> items) {
			this.items = ImmutableList.copyOf(checkNotNull(items, "items"));
		}
		
		public List<SortBy> getItems() {
			return items;
		}

		@Override
		public int hashCode() {
			return 31 + items.hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) { return true; }
			if (obj == null) { return false; }
			if (getClass() != obj.getClass()) { return false; }
			
			MultiSortBy other = (MultiSortBy) obj;
			return items.equals(other.items);
		}

		@Override
		public String toString() {
			return Joiner.on(", ").join(items);
		}
	}
	
	public static final class Builder {
		private final List<SortBy> sorts = newArrayList();
		
		public Builder sortByField(String field, Order order) {
			sorts.add(field(field, order));
			return this;
		}
		
		public Builder sortByScript(String script, Map<String, Object> arguments, Order order) {
			sorts.add(script(script, arguments, order));
			return this;
		}
		
		public Builder sortByScriptNumeric(String script, Map<String, Object> arguments, Order order) {
			sorts.add(scriptNumeric(script, arguments, order));
			return this;
		}
		
		public SortBy build() {
			if (sorts.isEmpty()) {
				return DEFAULT;
			} else if (sorts.size() == 1) {
				return Iterables.getOnlyElement(sorts);
			} else {
				return new MultiSortBy(sorts);
			}
		}
	}
	
	/**
	 * Creates and returns a new {@link SortBy} instance that sorts matches by the given field in the given order.
	 * @param field - the field to use for sort
	 * @param order - the order to use when sorting matches
	 * @return
	 */
	public static SortByField field(String field, Order order) {
		return new SortByField(field, order);
	}
	
	/**
	 * @param script
	 * @param arguments
	 * @param order
	 * @return
	 */
	public static SortBy script(String script, Map<String, Object> arguments, Order order) {
		return new SortByScript(script, arguments, order, ScriptSortType.STRING);
	}
	
	public static SortBy scriptNumeric(String script, Map<String, Object> arguments, Order order) {
		return new SortByScript(script, arguments, order, ScriptSortType.NUMBER);
	}

	public static Builder builder() {
		return new Builder();
	}

}