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

package org.uriplay.beans;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.springframework.beans.MutablePropertyValues;

import com.google.common.collect.Sets;

/**
 * Representation of the unvalidated properties of a resource.
 *
 * @author Robert Chatley (robert@metabroadcast.com)
 * @author Lee Denison (lee@metabroadcast.com)
 */
public class Representation {
	
    // List of document ids that are anonymous
	private List<String> anonymous = new ArrayList<String>();
	
	// Map of document id to resource entity type
    private Map<String, Class<?>> types = new HashMap<String, Class<?>>();

    // Map of document id to entity objects which represents the
    // resource
    private Map<String, Object> resources = new HashMap<String, Object>();

    private Map<String, Set<String>> uris = new HashMap<String, Set<String>>();

    // Map of document id to property values generated from the input 
    // document
    private Map<String, MutablePropertyValues> values = new HashMap<String, MutablePropertyValues>();

	public Map<String, Class<?>> getTypes() {
		return types;
	}

	public void setTypes(Map<String, Class<?>> types) {
		this.types = types;
	}

    public Map<String, Object> getResources() {
		return resources;
	}

	public void setResources(Map<String, Object> resources) {
		this.resources = resources;
	}

	public Map<String, Set<String>> getUris() {
        return uris;
    }
	
	private Map<String, MutablePropertyValues> getValues() {
		return values;
	}

	public MutablePropertyValues getValues(String docId) {
		return getValues().get(docId);
	}
	
	public void setValues(Map<String, MutablePropertyValues> values) {
		this.values = values;
	}

	public void setAnonymous(List<String> anonymous) {
		this.anonymous = anonymous;
	}

	public List<String> getAnonymous() {
		return anonymous;
	}
	
	public void addUri(String docId) {
		if (uris.get(docId) == null) {
			uris.put(docId, Sets.newHashSet(docId));
		}
	}

	public void addAnonymous(String docId) {
		anonymous.add(docId);
	}

	@SuppressWarnings("unchecked")
	public void addAliasFor(String docId, String uri) {
		
		if (values.get(docId) != null && values.get(docId).getPropertyValue("aliases") != null) {
			Collection<String> aliases = (Collection<String>) values.get(docId).getPropertyValue("aliases").getValue(); 
			aliases.add(uri);
		} else {
			MutablePropertyValues mpvs = new MutablePropertyValues();
			mpvs.addPropertyValue("aliases", Sets.newHashSet(uri));
			addValues(docId, mpvs);
		}
	}
	
	public void addType(String docId, Class<?> type) {
		types.put(docId, type);
	}

	public void addValues(String docId, MutablePropertyValues mpvs) {
		if (values.containsKey(docId)) {
			values.get(docId).addPropertyValues(mpvs);
		} else {
			values.put(docId, mpvs);
		}
	}

	public boolean isAnonymous(String docId) {
        return anonymous.contains(docId);
	}
	
	public Class<?> getType(String docId) {
		return types.get(docId);
	}

    public Set<String> getUris(String docId) {
        return uris.get(docId);
    }

	public Object getPropertyValue(String uri, String propertyName) {
		if (values.get(uri) != null
			&& values.get(uri).getPropertyValue(propertyName) != null) {
			return values.get(uri).getPropertyValue(propertyName).getValue();			
		} else {
			return null;
		}
	}
	
	@Override
	public String toString() {
	    StringBuilder sb = new StringBuilder();
	    
	    sb.append(this.uris);
	    sb.append(this.types);
	    sb.append(this.values);
	    sb.append(this.anonymous);
	    sb.append(this.resources);
	    
		return sb.toString();
	}

	public void mergeIn(Representation representation) {
		anonymous.addAll(representation.getAnonymous());
		types.putAll(representation.getTypes());
        uris.putAll(representation.getUris());
		values.putAll(representation.getValues());
		resources.putAll(representation.getResources());
	}

    public Object getResource(String docId) {
        return resources.get(docId);
    }

	public Set<String> getUrisForTypesAssignableFrom(Class<?> type) {
		Set<String> uris = Sets.newHashSet();
		for (Entry<String, Class<?>> entry : types.entrySet()) {
			if (type.isAssignableFrom(entry.getValue())) {
				uris.add(entry.getKey());
			}
		}
		return uris;
	}
}
