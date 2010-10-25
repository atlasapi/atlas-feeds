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
import java.util.List;

import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Playlist;
import org.atlasapi.media.util.ChildFinder;

import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

/**
 * {@link Projector} that takes a graph of beans (Playlists and Items) and removes
 * all of the intermediate levels, returning all of the leaves connected as direct
 * children of the root.
 * 
 * e.g. if we had an OPML feed of RSS feeds of podcasts, this transformation would
 * result in all of the podcasts being directly inside the OPML feed, without the
 * intermediate RSS feed level.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class FlatteningProjector implements Projector {

	public <T> Collection<T> applyTo(Collection<T> beanGraph) {
		
		Iterable<T> roots = rootsOf(beanGraph);
		checkPreconditionsOn(roots);
		
		return flatten(beanGraph, roots);
	}

	@SuppressWarnings("unchecked")
	private <T> Collection<T> flatten(Collection<T> beans, Iterable<T> roots) {
		
		T root = Iterables.getOnlyElement(roots);
		List<T> flattened = Lists.newArrayList();
		flattened.add(root);
		
		if (root instanceof Playlist) {
			Playlist rootList = (Playlist) root;
			
			Iterable<Item> leaves = Iterables.filter(beans, Item.class);
			for (Item leafItem : leaves) {
				rootList.addItem(leafItem);
				flattened.add((T) leafItem);
			}
		}
		
		return flattened;
	}

	private <T> Iterable<T> rootsOf(Collection<T> beans) {
		return Iterables.filter(beans, Predicates.not(new ChildFinder(beans)));
	}

	private void checkPreconditionsOn(Iterable<?> roots) {
		if (Iterables.size(roots) == 0) {
			throw new ProjectionException("No root collection found in object graph");
		}

		if (Iterables.size(roots) > 1) {
			throw new ProjectionException("Multiple root elements in object graph");
		}
		
		if (!(Iterables.getOnlyElement(roots) instanceof Playlist)) {
			throw new ProjectionException("No root collection found in object graph");
		}
	}

}
