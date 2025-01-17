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
package com.b2international.snowowl.eventbus.events;

import java.util.Collections;
import java.util.Map;

import com.b2international.snowowl.eventbus.IEventBus;
import com.b2international.snowowl.eventbus.IHandler;
import com.b2international.snowowl.eventbus.IMessage;

/**
 * @since 4.1
 */
public abstract class BaseEvent implements Event {

	@Override
	public final void send(IEventBus bus) {
		send(bus, (IHandler<IMessage>) null);
	}

	@Override
	public final void publish(IEventBus bus) {
		bus.publish(getAddress(), this, tag(), headers());
	}
	
	@Override
	public final void send(IEventBus bus, IHandler<IMessage> replyHandler) {
		bus.send(getAddress(), this, tag(), headers(), replyHandler);
	}

	protected Map<String, String> headers() {
		return Collections.emptyMap();
	}
	
	/**
	 * Returns the tag of this {@link Event}. Never null.
	 * 
	 * @return 
	 */
	protected String tag() {
		return IMessage.TAG_EVENT;
	}

	/**
	 * Returns the address as the destination of this {@link Event}.
	 * 
	 * @return
	 */
	protected abstract String getAddress();

}
