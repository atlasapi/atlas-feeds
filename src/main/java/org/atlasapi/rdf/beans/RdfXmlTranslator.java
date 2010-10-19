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

package org.atlasapi.rdf.beans;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;

import org.atlasapi.beans.AtlasErrorSummary;

import com.hp.hpl.jena.ontology.OntModel;

/**
 * Translates a {@link Representation} into a Jena model and writes out
 * the model as RDF/XML to a given output stream.
 * 
 * The default output format is RDF/XML
 *
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class RdfXmlTranslator extends JenaRdfTranslator {

	private static final String RDF_XML = "RDF/XML";

	private static final String XML_VERSION = "<?xml version=\"1.0\"?>";
	
	private String outputFormat;
	
	public RdfXmlTranslator(TypeMap typeMap) {
		this(typeMap, RDF_XML);
	}
	
	public RdfXmlTranslator(TypeMap typeMap, String outputFormat) {
		super(typeMap);
		this.outputFormat = outputFormat;
	}

	@Override
	protected void writeOut(OntModel rdf, OutputStream stream) {
		try {
			if (RDF_XML.equals(outputFormat)) {
				stream.write((XML_VERSION + "\n").getBytes());
			}
		} catch (IOException ioe) {
			throw new RuntimeException(ioe);
		}
	
		Writer writer;
		try {
			writer = new OutputStreamWriter(stream, "UTF-8");
		} catch (UnsupportedEncodingException uee) {
			throw new RuntimeException(uee);
		}
		rdf.write(writer, outputFormat);
	}

	@Override
	public void writeError(AtlasErrorSummary exception, OutputStream oStream) {
		//no-op
	}
}
