package org.atlasapi.feeds.interlinking;

public class InterlinkEpisode extends InterlinkContent {

	public InterlinkEpisode(String id, Integer index) {
		super(id, index);
	}
	
	public InterlinkEpisode withTitle(String title) {
		this.title = title;
		return this;
	}
}
