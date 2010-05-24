package org.uriplay.beans;

public interface UriPropertySource {

	/**
	 * Adds a URI of a resource, or parts thereof, to the property values of the resource with the supplied docId.
	 * 
	 * @param uri a URI for the resource.
	 * @param representation 
	 * @param docId the primary URI for the resource which identifies it within the representation.
	 * @throws PropertyMergeException
	 */
	void merge(Representation representation, String docId) throws PropertyMergeException;

}
