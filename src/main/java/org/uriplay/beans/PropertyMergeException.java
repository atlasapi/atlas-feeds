package org.uriplay.beans;

public class PropertyMergeException extends Exception {

	private static final long serialVersionUID = 1L;
	
	private final String propertyName;
	
	public PropertyMergeException(String propertyName, String message) {
		super(message);
		this.propertyName = propertyName;
	}
	
	public String getPropertyName() {
		return propertyName;
	}
	
}
