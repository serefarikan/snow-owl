/*
 * Copyright 2021-2022 B2i Healthcare Pte Ltd, http://b2i.sg
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
package com.b2international.snowowl.core.request;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.b2international.snowowl.core.ServiceProvider;
import com.b2international.snowowl.core.domain.PageableCollectionResource;
import com.b2international.snowowl.core.events.AsyncRequest;
import com.b2international.snowowl.core.events.util.Promise;
import com.b2international.snowowl.eventbus.IEventBus;
import com.google.common.collect.Streams;

/**
 * @since 8.0
 * 
 * @param <B>
 * @param <C>
 * @param <R>
 */
public abstract class SearchPageableCollectionResourceRequestBuilder<B extends SearchPageableCollectionResourceRequestBuilder<B, C, R>, C extends ServiceProvider, R extends PageableCollectionResource<?>> 
		extends SearchResourceRequestBuilder<B, C, R> {

	public final Stream<R> stream(C context) {
		return Streams.stream(new SearchResourceRequestIterator<B, R>(getSelf(), (builder) -> builder.build().execute(context)));
	}

	public final Stream<R> stream(ServiceProvider context, Function<B, AsyncRequest<R>> build) {
		return Streams.stream(new SearchResourceRequestIterator<B, R>(getSelf(), (builder) -> build.apply(builder).getRequest().execute(context)));
	}

	public final Stream<R> streamAsync(ServiceProvider context, Function<B, AsyncRequest<R>> build) {
		return streamAsync(context.service(IEventBus.class), build);
	}

	public final Stream<R> streamAsync(IEventBus bus, Function<B, AsyncRequest<R>> build) {
		return Streams.stream(new SearchResourceRequestIterator<B, R>(getSelf(), (builder) -> build.apply(builder).execute(bus).getSync(3, TimeUnit.MINUTES)));
	}

	public final <T> Promise<Collection<T>> transformAsync(ServiceProvider context, Function<B, AsyncRequest<R>> build, Function<R, Stream<T>> transform) {
		return transformAsync(context.service(IEventBus.class), build, transform);
	}
	
	public final <T> Promise<Collection<T>> transformAsync(IEventBus bus, Function<B, AsyncRequest<R>> build, Function<R, Stream<T>> transform) {
		return Promise.wrap(() -> {
			return streamAsync(bus, build)
					.map(transform)
					.flatMap(t -> t)
					.collect(Collectors.toList());
		});
	}
}
