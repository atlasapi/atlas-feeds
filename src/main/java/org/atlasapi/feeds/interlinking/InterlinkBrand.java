package org.atlasapi.feeds.interlinking;

public class InterlinkBrand {

	private final String id;
	private String title;

	public InterlinkBrand(String id) {
		this.id = id;
	}

	public String title() {
		return title;
	}
	
	public String id() {
		return id;
	}
	
	public InterlinkBrand withTitle(String title) {
		this.title = title;
		return this;
	}
}
