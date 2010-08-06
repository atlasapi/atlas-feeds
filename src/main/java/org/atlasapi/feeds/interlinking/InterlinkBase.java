package org.atlasapi.feeds.interlinking;

public abstract class InterlinkBase {

	protected final String id;
	protected String title;
	protected Integer index;

	
	public InterlinkBase(String id) {
		this.id = id;
	}
	
	public String title() {
		return title;
	}
	
	public String id() {
		return id;
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
