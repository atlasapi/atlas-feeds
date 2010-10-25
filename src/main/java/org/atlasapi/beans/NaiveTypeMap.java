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

import java.util.Collections;
import java.util.Set;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Playlist;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Version;
import org.atlasapi.rdf.beans.TypeMap;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;

/**
 * Very simple hardcoded typemap.
 * TODO: replace this with something based on annotations on entity classes.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class NaiveTypeMap implements TypeMap {

	private BiMap<Class<?>, Set<String>> map;
	
	public NaiveTypeMap() {
		map = HashBiMap.create();
		map.put(Encoding.class, Collections.singleton("http://uriplay.org/elements/Encoding"));
		map.put(Location.class, Collections.singleton("http://uriplay.org/elements/Location"));
		map.put(Episode.class, Collections.singleton("http://purl.org/ontology/po/Episode"));
		map.put(Version.class, Collections.singleton("http://purl.org/ontology/po/Version"));
		map.put(Broadcast.class, Collections.singleton("http://purl.org/ontology/po/Broadcast"));
		map.put(Brand.class, Collections.singleton("http://purl.org/ontology/po/Brand"));
		map.put(Item.class, Collections.singleton("http://uriplay.org/elements/Item"));
		map.put(Playlist.class, Collections.singleton("http://uriplay.org/elements/List"));
		map.put(Policy.class, Collections.singleton("http://uriplay.org/elements/Policy"));
	}
		
	public Set<String> rdfTypes(Class<?> beanType) {
		return map.get(beanType);
	}

	public Class<?> beanType(Set<String> types) {
		return map.inverse().get(types);
	}

}
