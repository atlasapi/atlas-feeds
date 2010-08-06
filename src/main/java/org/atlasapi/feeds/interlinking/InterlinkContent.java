package org.atlasapi.feeds.interlinking;

public abstract class InterlinkContent extends InterlinkBase {

	private final Integer index;

	public InterlinkContent(String id, Integer index) {
		super(id);
		this.index = index;
	}

	public Integer indexWithinParent() {
		return index;
	}
	
	
}
