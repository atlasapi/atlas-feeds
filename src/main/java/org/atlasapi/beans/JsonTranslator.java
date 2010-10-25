/* Copyright 2009 Meta Broadcast Ltd

Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.atlasapi.beans;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBException;

import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.base.Charsets;
import com.google.common.collect.Iterables;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

/**
 * Outputs simple URIplay model in plain XML format using JAXB.
 * 
 * @author Robert Chatley (robert@metabroadcast.com)
 */
public class JsonTranslator implements BeanGraphWriter {

	public static final String CALLBACK = "callback";

	@Autowired
	private HttpServletRequest request;

	private Gson gson;

	public JsonTranslator() throws JAXBException {
		gson = new GsonBuilder()
					.disableHtmlEscaping()
					.setDateFormat(DateFormat.LONG)
					.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
					.registerTypeAdapter(AtlasErrorSummary.class, new AtlasExceptionJsonSerializer())
					.create();
	}

	public void writeTo(Collection<Object> graph, OutputStream stream) {

		String callback = callback(request);

		OutputStreamWriter writer = new OutputStreamWriter(stream, Charsets.UTF_8);

		try {
			if (callback != null) {
				writer.write(callback + "(");
			}
			writer.write(gson.toJson(Iterables.getOnlyElement(graph)));

			if (callback != null) {
				writer.write(");");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				writer.flush();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private String callback(HttpServletRequest request) {
		if (request == null) {
			return null;
		}
		String callback = request.getParameter(CALLBACK);
		if (callback == null || callback.length() < 1) {
			return null;
		}

		try {
			return URLEncoder.encode(callback, "UTF-8");
		} catch (UnsupportedEncodingException e) {
			return null;
		}
	}

	@Override
	public void writeError(AtlasErrorSummary exception, OutputStream stream) {
		String callback = callback(request);

		OutputStreamWriter writer = new OutputStreamWriter(stream, Charsets.UTF_8);

		try {
			if (callback != null) {
				writer.write(callback + "(");
			}
			writer.write(gson.toJson(exception));

			if (callback != null) {
				writer.write(");");
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				writer.flush();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
	}

	private class AtlasExceptionJsonSerializer implements JsonSerializer<AtlasErrorSummary> {

		@Override
		public JsonElement serialize(AtlasErrorSummary src, Type typeOfSrc, JsonSerializationContext context) {
			JsonObject serialized = new JsonObject();
			JsonObject error = new JsonObject();
			error.addProperty("message", src.message());
			error.addProperty("error_code", src.errorCode());
			error.addProperty("error_id", src.id());
			serialized.add("error", error);
			return serialized;// ;
		}

	}
}
