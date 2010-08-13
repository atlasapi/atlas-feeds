package org.atlasapi.feeds.interlinking;

import org.joda.time.DateTime;

public abstract class InterlinkBase {

	protected final String id;
	protected String title;
	protected Integer index;
	protected DateTime lastUpdated;
	protected Operation operation;
	
	public enum Operation {
		STORE,
		DELETE;
		
		public String toString() {
			return name().toLowerCase();
		};
	}
	
	public InterlinkBase(String id, Operation operation) {
		this.id = id;
		this.operation = operation;
	}
	
	public String title() {
		return title;
	}
	
	public String id() {
		return id;
	}
	
	public DateTime lastUpdated() {
	    return lastUpdated;
	}
	
	public Operation operation() {
		return operation;
	}
	
	@Override
	public int hashCode() {
		return id.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj == null || !getClass().equals(obj.getClass())) {
			return false;
		}
		return id.equals(((InterlinkBase) obj).id);
	}
}
