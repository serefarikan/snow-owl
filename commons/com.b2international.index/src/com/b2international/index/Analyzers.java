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
package com.b2international.index;

/**
 * Supported analyzers are listed here. Use with {@link Text} annotation on String fields.
 * 
 * @since 5.0
 */
public enum Analyzers {

	/**
	 * The default term analyzer, by default escapes all whitespaces and any character from the default delimiters list and includes a leading and
	 * trailing marker from the Unicode Private Use Area characters to mark the beginning and end of a text.
	 */
	DEFAULT,
	
	/**
	 * Keyword analyzer present in Elasticsearch by default, does not modify the given text at all, uses it as is for both indexing and search.
	 */
	KEYWORD,

	/**
	 * Indexes the term as is for exact match searches with ASCII folding and in lower-case form.
	 * 
	 * <pre>
	 * "exact_match": {
	 *   "tokenizer": "keyword",
	 *   "filter": [
	 *     "asciifolding",
	 *     "lowercase"
	 *   ]
	 * }
	 * </pre>
	 */
	EXACT,
	
	/**
	 * <pre>
	 * "tokenized": {
	 *   "tokenizer": "whitespace",
	 *   "filter": [
	 *     "asciifolding",
	 *     "lowercase",
	 *     "possessive",
	 *     "word_splitter",
	 *     "unique_token"
	 *   ]
	 * }
	 * 
	 * "word_splitter": {
	 *   "type": "word_delimiter",
	 *   "split_on_case_change": "false",
	 *   "split_on_numerics": "false",
	 *   "preserve_original": "true",
	 *   "stem_english_possessive": "false",
	 *   "type_table": [", => DIGIT", ". => DIGIT"]
	 * }
	 * </pre>
	 */
	TOKENIZED,
	
	/**
	 * <pre>
	 * "tokenized_synonyms": {
	 *   "tokenizer": "whitespace",
	 *   "filter": [
	 *     "asciifolding",
	 *     "lowercase",
	 *     "possessive",
	 *     "synonyms",
	 *     "word_splitter",
	 *     "unique_token"
	 *   ]
	 * }
	 * 
	 * "word_splitter": {
	 *   "type": "word_delimiter",
	 *   "split_on_case_change": "false",
	 *   "split_on_numerics": "false",
	 *   "preserve_original": "true",
	 *   "stem_english_possessive": "false",
	 *   "type_table": [", => DIGIT", ". => DIGIT"]
	 * }
	 * </pre>
	 */
	TOKENIZED_SYNONYMS,
	
	/**
	 * <pre>
	 * "tokenized_ignore_stopwords": {
	 *   "tokenizer": "whitespace",
	 *   "filter": [
	 *     "asciifolding",
	 *     "lowercase",
	 *     "stop_words",
	 *     "possessive",
	 *     "word_splitter",
	 *     "unique_token"
	 *   ]
	 * }
	 * </pre>
	 */
	TOKENIZED_IGNORE_STOPWORDS,
	
	/**
	 * <pre>
	 * "tokenized_synonyms_ignore_stopwords": {
	 *   "tokenizer": "whitespace",
	 *   "filter": [
	 *     "asciifolding",
	 *     "lowercase",
	 *     "stop_words",
	 *     "possessive",
	 *     "synonyms",
	 *     "word_splitter",
	 *     "unique_token"
	 *   ]
	 * }
	 * </pre>
	 */
	TOKENIZED_SYNONYMS_IGNORE_STOPWORDS,
	
	/**
	 * <pre>
	 * "stemming": {
	 *   "tokenizer" : "whitespace",
	 *   "filter" : [
	 *     "asciifolding",
	 *     "lowercase",
	 *     "possessive",
	 *     "word_splitter",
	 *     "unique_token",
	 *     "english_stemmer"
	 *   ]
	 * }
	 * </pre>
	 */
	STEMMING,

	/**
	 * <pre>
	 * "search_stemming": {
	 *   "tokenizer" : "whitespace",
	 *   "filter" : [
	 *     "asciifolding",
	 *     "lowercase",
	 *     "possessive",
	 *     "unique_token",
	 *     "english_stemmer"
	 *   ]
	 * }
	 * </pre>
	 */
	SEARCH_STEMMING,

	/**
	 * <pre>
	 * "prefix": {
	 *   "tokenizer": "whitespace",
	 *   "filter": [
	 *     "asciifolding",
	 *     "lowercase",
	 *     "possessive",
	 *     "word_splitter",
	 *     "unique_token",
	 *     "edge_ngram"
	 *   ]
	 * }
	 * </pre>
	 */
	PREFIX,

	/**
	 * <pre>
	 * "case_sensitive": {
	 *   "tokenizer" : "whitespace",
	 *   "filter" : [
	 *     "possessive",
	 *     "word_splitter",
	 *     "unique_token"
	 *   ]
	 * }
	 * </pre>
	 */
	CASE_SENSITIVE,
	
	/**
	 * <pre>
	 * "case_sensitive_prefix": {
	 *   "tokenizer": "whitespace",
	 *   "filter": [
	 *     "asciifolding",
	 *     "possessive",
	 *     "word_splitter",
	 *     "unique_token",
	 *     "edge_ngram"
	 *   ]
	 * }
	 * </pre>
	 */
	CASE_SENSITIVE_PREFIX,
	
	/**
	 * <pre>
	 * "case_sensitive_ascii_folding": {
	 *   "tokenizer" : "whitespace",
	 *   "filter" : [
	 *     "asciifolding",
	 *     "possessive",
	 *     "word_splitter",
	 *     "unique_token"
	 *   ]
	 * }
	 * </pre>
	 */
	CASE_SENSITIVE_ASCII,
	
	/**
	 * <pre>
	 * "case_sensitive_ascii_folding_exact_match": {
	 *   "tokenizer" : "keyword",
	 *   "filter" : [
	 *     "asciifolding"
	 *   ]
	 * }
	 * </pre>
	 */
	CASE_SENSITIVE_ASCII_EXACT, 
	
	/**
	 * Use the same analyzer that is configured for the {@link Analyzed#analyzer()} field. 
	 */
	INDEX;
	
	/**
	 * @return the actual analyzer registered in Elasticsearch
	 * @see analysis.json for further details
	 */
	public String getAnalyzer() {
		switch (this) {
			case DEFAULT:                             return "standard";
			case KEYWORD:                             return "keyword";
			case EXACT:                               return "exact";
			case TOKENIZED:                           return "tokenized";
			case TOKENIZED_SYNONYMS:                  return "tokenized_synonyms";
			case TOKENIZED_IGNORE_STOPWORDS:          return "tokenized_ignore_stopwords";
			case TOKENIZED_SYNONYMS_IGNORE_STOPWORDS: return "tokenized_synonyms_ignore_stopwords";
			case STEMMING:                            return "stemming";
			case SEARCH_STEMMING:                     return "search_stemming";
			case CASE_SENSITIVE:                      return "case_sensitive";
			case CASE_SENSITIVE_ASCII:                return "case_sensitive_ascii";
			case CASE_SENSITIVE_ASCII_EXACT:          return "case_sensitive_ascii_exact";
			case CASE_SENSITIVE_PREFIX:               return "case_sensitive_prefix";
			case PREFIX:                              return "prefix";
			default: throw new UnsupportedOperationException("Unsupported analyzer: " + this);
		}
	}
}
