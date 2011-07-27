package org.atlasapi.feeds.interlinking;

import java.util.List;

import org.joda.time.DateTime;

import com.google.common.collect.Lists;

public final class InterlinkFeed {

	private final String id;
	private String title;
	private String subtitle;
	private InterlinkFeedAuthor author;
	
	private List<InterlinkBase> entries = Lists.newArrayList();
	
	public InterlinkFeed(String id) {
		this.id = id;
	}
	
	public InterlinkFeed withTitle(String title) {
		this.title = title;
		return this;
	}
	
	public InterlinkFeed withSubtitle(String subtitle) {
		this.subtitle = subtitle;
		return this;
	}
	
	public InterlinkFeed withAuthor(InterlinkFeedAuthor author) {
		this.author = author;
		return this;
	}
	
	public InterlinkFeed addEntry(InterlinkBase entry) {
		entries.add(entry);
		return this;
	}
	
	public List<InterlinkBase> entries() {
		return entries;
	}
	
	public InterlinkFeedAuthor author() {
		return author;
	}
	
	public String subtitle() {
		return subtitle;
	}
	
	public String title() {
		return title;
	}
	
	public String id() {
		return id;
	}
	
	public final static class InterlinkFeedAuthor {
		
		private final String partner;
		private final String supplier;

		public InterlinkFeedAuthor(String partner, String supplier) {
			this.partner = partner;
			this.supplier = supplier;
		}

		public String partner() {
			return partner;
		}
		
		public String supplier() {
			return supplier;
		}
	}
}
