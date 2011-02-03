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
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.beans.AtlasErrorSummary;
import org.atlasapi.beans.NaiveTypeMap;

import com.google.common.collect.ImmutableMap;
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

	private static final Map<String, String> PREFIXES = ImmutableMap.<String, String>builder()
		.put("http://uriplay.org/elements/", "play")
        .put("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf")
        .put("http://www.w3.org/2000/01/rdf-schema#", "rdfs")
        .put("http://purl.org/dc/elements/1.1/", "dc")
        .put("http://purl.org/ontology/po/", "po")
        .put("http://xmlns.com/foaf/0.1/", "foaf")
        .put("http://rdfs.org/sioc/ns#", "sioc")
        .put("http://purl.org/dc/terms/", "dcterms")
    .build();
	
	private String outputFormat;
	
	public RdfXmlTranslator() {
		this(new NaiveTypeMap());
		setNsPrefixes(PREFIXES);
	}
	
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
	public void writeError(HttpServletRequest request, HttpServletResponse response, AtlasErrorSummary exception) {
		//no-op
	}
}
