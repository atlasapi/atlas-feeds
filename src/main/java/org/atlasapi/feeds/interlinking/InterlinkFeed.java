package org.atlasapi.feeds.interlinking;

import java.util.Set;

import org.joda.time.DateTime;

import com.google.common.collect.Sets;

public final class InterlinkFeed {

	private final String id;
	private String title;
	private String subtitle;
	private DateTime updated; 
	private InterlinkFeedAuthor author;
	
	private Set<InterlinkBrand> brands = Sets.newHashSet();
	
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
	
	public InterlinkFeed withUpdatedAt(DateTime dateTime) {
		updated = dateTime;
		return this;
	}
	
	public InterlinkFeed withAuthor(InterlinkFeedAuthor author) {
		this.author = author;
		return this;
	}
	
	public InterlinkFeed addBrand(InterlinkBrand brand) {
		brands.add(brand);
		return this;
	}
	
	public Set<InterlinkBrand> brands() {
		return brands;
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
	
	public DateTime updated() {
		return updated;
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
