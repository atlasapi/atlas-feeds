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

package org.uriplay.feeds;

/**
 * Indicates a compile time error or configuration error within the client code of the JHerd library.
 * 
 * @author Lee Denison (lee@metabroadcast.com)
 * @author Rob Chatley
 */
@SuppressWarnings("serial")
public class Defect extends RuntimeException {

	public Defect() {
		super();
	}

	public Defect(String message, Throwable cause) {
		super(message, cause);
	}

	public Defect(String message) {
		super(message);
	}

	public Defect(Throwable cause) {
		super(cause);
	}
	
}
