package org.uriplay.feeds.naming;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.uriplay.beans.Representation;

public class RepresentationResourceMapping implements ResourceMapping {

	private Representation source;
	
	public RepresentationResourceMapping(Representation source) {
		this.source = source;
	}
	
	public boolean canMatch(String uri) {
		return true;
	}

	public Object getResource(String uri) {
		String docId = null;
		
		for (Map.Entry<String, Set<String>> docUris : source.getUris().entrySet()) {
			if (docUris.getValue().contains(uri)) {
				docId = docUris.getKey();
				break;
			}
		}
		
		if (docId == null) {
			if (source.getAnonymous().contains(uri)) {
				docId = uri;
			}
		}
		
		if (docId != null) {
			return source.getResource(docId);
		} else {
			return null;
		}
	}

	public String getUri(Object bean) {
		for (Map.Entry<String, Object> entry : source.getResources().entrySet()) {
			if (entry.getValue().equals(bean)) {
				return entry.getKey();
			}
		}
		
		return null;
	}

	public Set<String> getUris(Object bean) {
		String docId = getUri(bean);
		
		if (docId != null) {
			return Collections.unmodifiableSet(source.getUris().get(docId));
		} else {
			return Collections.emptySet();
		}
	}

	public boolean isReserved(String uri) {
		return false;
	}

}
