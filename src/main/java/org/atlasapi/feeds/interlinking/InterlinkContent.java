package org.atlasapi.feeds.interlinking;

public abstract class InterlinkContent extends InterlinkBase {

	private final Integer index;
	protected String summary;
	protected String description;
	protected String thumbnail;

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
	
	public String description() {
		return description;
	}
	
	public String thumbnail() {
	    return thumbnail;
	}
}
