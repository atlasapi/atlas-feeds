/* Copyright 2009 British Broadcasting Corporation
   Copyright 2009 Meta Broadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.atlasapi.beans;

import java.util.Collection;
import java.util.Set;

import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Playlist;

import com.google.common.collect.Sets;

/**
 * {@link Projector} that checks that there is a single {@link Item} in
 * the bean graph, passes is through if so, and throws an exception if not.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class SingleItemProjector implements Projector {

	public <T> Collection<T> applyTo(Collection<T> beans) {
	
		if (beans == null) {
			throw new ProjectionException("Not a single item - nothing found");
		}
		
		Set<Object> items = Sets.newHashSet();
		
		for (Object bean : beans) {
			if (bean instanceof Playlist) {
				throw new ProjectionException("Not a single item - playlist found");
			}
			
			if (bean instanceof Item) {
				items.add(bean);
			}
		}
		
		if (items.size() > 1) {
			throw new ProjectionException("Not a single item - multiple found");
		}
		
		return beans;
	}

}
