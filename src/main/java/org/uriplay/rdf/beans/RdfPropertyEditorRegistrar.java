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

package org.uriplay.rdf.beans;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.joda.time.DateTime;
import org.springframework.beans.PropertyEditorRegistrar;
import org.springframework.beans.PropertyEditorRegistry;
import org.uriplay.beans.bind.CustomDateEditor;
import org.uriplay.beans.bind.DateTimePropertyEditor;
import org.uriplay.beans.bind.ResourceEditor;
import org.uriplay.feeds.naming.ResourceMapping;

public class RdfPropertyEditorRegistrar implements PropertyEditorRegistrar {

	private ResourceMapping resourceMapping;
	
	private List<Class<?>> beanTypes;
	
    public static final String FULL_DATE_FMT = "yyyy-MM-dd'T'HH:mm:ssZ";
    public static final String Z_DATE_FMT = "yyyy-MM-dd'T'HH:mm:ss'Z'";
    public static final String SHORT_DATE_FMT = "yyyy-MM-dd'T'HH:mmZ";
    public static final String SHORT_Z_DATE_FMT = "yyyy-MM-dd'T'HH:mm'Z'";
    public static final String UTC_SECONDS_FMT = "yyyy-MM-dd'T'HH:mm:ss";
    public static final String UTC_MINUTES_FMT = "yyyy-MM-dd'T'HH:mm";

    public RdfPropertyEditorRegistrar(ResourceMapping resourceMapping, List<Class<?>> beanTypes) {
		this.resourceMapping = resourceMapping;
		this.beanTypes = beanTypes;
    }
    
    public void registerCustomEditors(PropertyEditorRegistry registry) {
        registerDateEditors(registry);
        registerResourceEditors(registry);
    }

	private void registerResourceEditors(PropertyEditorRegistry registry) {
		for (Class<?> beanType : beanTypes) {
	        registry.registerCustomEditor(
	                beanType, 
	                new ResourceEditor(resourceMapping));			
		}
	}

	private void registerDateEditors(PropertyEditorRegistry registry) {
		TimeZone utc = TimeZone.getTimeZone("UTC");
        SimpleDateFormat format = null;
        List<DateFormat> formats = new ArrayList<DateFormat>();

        format = new SimpleDateFormat(FULL_DATE_FMT);
        format.setTimeZone(utc);
        format.setLenient(false);
        formats.add(format);

        format = new SimpleDateFormat(Z_DATE_FMT);
        format.setTimeZone(utc);
        format.setLenient(false);
        formats.add(format);

        format = new SimpleDateFormat(SHORT_DATE_FMT);
        format.setTimeZone(utc);
        format.setLenient(false);
        formats.add(format);

        format = new SimpleDateFormat(SHORT_Z_DATE_FMT);
        format.setTimeZone(utc);
        format.setLenient(false);
        formats.add(format);

        format = new SimpleDateFormat(UTC_SECONDS_FMT);
        format.setTimeZone(utc);
        format.setLenient(false);
        formats.add(format);

        format = new SimpleDateFormat(UTC_MINUTES_FMT);
        format.setTimeZone(utc);
        format.setLenient(false);
        formats.add(format);

        registry.registerCustomEditor(
                Date.class, 
                new CustomDateEditor(formats, false));
        
        registry.registerCustomEditor(DateTime.class, new DateTimePropertyEditor());
	}
}
