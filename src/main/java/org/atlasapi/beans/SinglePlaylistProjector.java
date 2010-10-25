/*  Copyright 2009 Meta Broadcast Ltd

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
 * {@link Projector} that checks that there is a single {@link Playlist} in
 * the bean graph, passes is through if so, and throws an exception if not.
 * 
 * @author John Ayres (john@metabroadcast.com)
 */
public class SinglePlaylistProjector implements Projector {

	public <T> Collection<T> applyTo(Collection<T> beans) {
	
		if (beans == null) {
			throw new ProjectionException("Not a single playlist - nothing found");
		}
		
		Set<Object> playlists = Sets.newHashSet();
		
		for (Object bean : beans) {
			if (bean instanceof Item) {
				throw new ProjectionException("Not a single item - playlist found");
			}
			
			if (bean instanceof Playlist) {
				playlists.add(bean);
			}
		}
		
		if (playlists.size() > 1) {
			throw new ProjectionException("Not a single playlist - multiple found");
		}
		
		return beans;
	}
}
