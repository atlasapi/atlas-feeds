/* Copyright 2009 British Broadcasting Corporation
 
Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.uriplay.rdf;

import java.util.Map;

/**
 * Implementors of this interface should provide the ability
 * to look up a resource locally, or to look the same resource
 * up in a number of locations, including possibly remote sites.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public interface LocalRemoteResourceLookup {

	Object getResourceLocally(String uri, Map<String, Object> params) throws Exception;

	Object getResource(String uri, Map<String, Object> params) throws Exception;
	
}
