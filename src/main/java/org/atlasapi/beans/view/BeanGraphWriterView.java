/* Copyright 2009 British Broadcasting Corporation
   Copyright 2009 Meta Broadcast Ltd
 
Licensed under the Apache License, Version 2.0 (the "License"); you
may not use this file except in compliance with the License. You may
obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
implied. See the License for the specific language governing
permissions and limitations under the License. */

package org.atlasapi.beans.view;

import java.util.Collection;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.atlasapi.beans.AtlasErrorSummary;
import org.atlasapi.beans.BeanGraphWriter;
import org.atlasapi.persistence.servlet.RequestNs;
import org.springframework.web.servlet.View;

import com.metabroadcast.common.media.MimeType;

public class BeanGraphWriterView implements View {

	private final BeanGraphWriter writer;

	private final MimeType contentType;
	
	public String getContentType() {
		return contentType.toString();
	}

	public BeanGraphWriterView(MimeType contentType, BeanGraphWriter writer) {
		this.contentType = contentType;
		this.writer = writer;
	}

	@SuppressWarnings("unchecked")
	public void render(Map model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		response.setContentType(getContentType());
		response.setCharacterEncoding("utf-8");
		
		if (model != null) {
			if (model.containsKey(RequestNs.GRAPH)) {
				Collection<Object> beans = beanGraph(model);
				writer.writeTo(beans, response.getOutputStream());
			} else {
				AtlasErrorSummary exception = extractException(model);
				response.setStatus(exception.statusCode());
				writer.writeError(exception, response.getOutputStream());
			}
		} else {
			response.sendError(HttpServletResponse.SC_NOT_FOUND);
		}
	}

	@SuppressWarnings("unchecked")
	protected Collection<Object> beanGraph(Map<String, Object> model) {
		return (Collection) model.get(RequestNs.GRAPH);
	}
	
	protected AtlasErrorSummary extractException(Map<String, Object> model) {
		return (AtlasErrorSummary) model.get(RequestNs.ERROR);
	}
}
