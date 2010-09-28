package org.atlasapi.feeds.xml;

import nu.xom.Element;

public final class XMLNamespace {
	
	private final String uri;
	private final String prefix;

	public XMLNamespace(String prefix,String uri) {
		this.prefix = prefix;
		this.uri = uri;
	}
	
	public void addDeclarationTo(Element elem) {
		elem.addNamespaceDeclaration(getPrefix(), getUri());
	}

	public String getPrefix() {
		return prefix;
	}

	public String getUri() {
		return uri;
	}
}
