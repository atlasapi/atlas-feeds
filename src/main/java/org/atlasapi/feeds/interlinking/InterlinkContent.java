package org.atlasapi.feeds.interlinking;

public abstract class InterlinkContent extends InterlinkBase {

	private final Integer index;
	protected String summary;

	public InterlinkContent(String id, Integer index) {
		super(id);
		this.index = index;
	}

	public Integer indexWithinParent() {
		return index;
	}
	
	public String summary() {
		return summary;
	}
}
