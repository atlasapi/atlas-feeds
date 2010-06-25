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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.uriplay.beans.BeanIntrospector;
import org.uriplay.content.rdf.annotations.RdfClass;
import org.uriplay.content.rdf.annotations.RdfId;
import org.uriplay.content.rdf.annotations.RdfProperty;
import org.uriplay.feeds.Defect;

import com.google.common.collect.Maps;

/** 
 * Inspects compile time bean properties of a class for RDF metadata.
 */
public class RdfIntrospector {

    protected static final Log logger = 
        LogFactory.getLog(RdfIntrospector.class);

    public static List<String> rdfTypes(Class<?> entityType) throws Exception {
        List<String> result = new ArrayList<String>();
        RdfClass rdfClass = (RdfClass) entityType.getAnnotation(RdfClass.class);

        if (rdfClass != null) {
            result.add(getRdfClassUri(entityType, rdfClass));
        }

        Class<?> superclass = entityType.getSuperclass();
        if (superclass != null) {
            List<String> superRdfTypes = rdfTypes(superclass);
            superRdfTypes.removeAll(result);
            result.addAll(superRdfTypes);
        }

        return result;
    }

    public static String defaultNamespace(Class<?> entityType) {
        RdfClass rdfClass = (RdfClass) entityType.getAnnotation(RdfClass.class);

        if (rdfClass != null) {
            return rdfClass.namespace();
        }
        else {
            return null;
        }
    }

    /**
     * Currently a property is considered a relation if it is a persistence 
     * relation.  This may need to be updated, but is convenient for now.
     */
    public static RdfProperty getProperty(PropertyDescriptor property) {
        Method readMethod = property.getReadMethod();

        return readMethod.getAnnotation(RdfProperty.class);
    }

    public static String rdfProperty(
            PropertyDescriptor property, 
            String defaultNS) {
    	Method readMethod = property.getReadMethod();
        String propertyName = null;

        RdfProperty rdfProperty = 
            (RdfProperty) readMethod.getAnnotation(RdfProperty.class);

        if (rdfProperty != null) {
            if (rdfProperty.uri() != null && rdfProperty.uri().length() > 0) {
                propertyName = rdfProperty.uri();
            }
            else {
                propertyName = property.getName();
            }

            if (rdfProperty.namespace() != null
                && rdfProperty.namespace().length() > 0) {
                return rdfProperty.namespace() + propertyName;
            }
            else {
                return defaultNS + propertyName;
            }
        }

        return null;
    }

    public static String getRdfIdProperty(Class<?> entityType) throws Exception {
    	Map<String, PropertyDescriptor> metadata = BeanIntrospector.getPropertyDescriptors(entityType);

        for (Map.Entry<String, PropertyDescriptor> entry 
                : metadata.entrySet()) {
            PropertyDescriptor pd = entry.getValue();
            Method readMethod = pd.getReadMethod();
            RdfId ann = readMethod.getAnnotation(RdfId.class);
            if (ann != null) {
            	return entry.getKey();
            }
        }
        
        return null;
    }
    
    public static boolean containsPropertySet(String[] sets, String set) {
        for (String candidate : sets) {
            if (candidate != null && candidate.equals(set)) {
                return true;
            }
        }

        return false;
    }

    private static String getRdfClassUri(Class<?> entityType, RdfClass rdfClass) {
        StringBuffer uri = new StringBuffer();
        
        if (rdfClass.namespace() != null) {
            uri.append(rdfClass.namespace());
        }

        if (rdfClass.uri() != null && rdfClass.uri().length() > 0) {
            uri.append(rdfClass.uri());
        }
        else {
            uri.append(entityType.getSimpleName());
        }

        return uri.toString();
    }

    public static Map<String, PropertyDescriptor> getRdfPropertyDescriptors(Class<?> type) {
        Map<String, PropertyDescriptor> result = Maps.newHashMap();
        String defaultNs = defaultNamespace(type);
        Map<String, PropertyDescriptor> metadata;
        
        try {
            metadata = BeanIntrospector.getPropertyDescriptors(type);
        } catch (IntrospectionException e) {
            throw new Defect(e);
        }

        for (String key : metadata.keySet()) {
            PropertyDescriptor pd = metadata.get(key);
            String uri = rdfProperty(pd, defaultNs);

            if (uri != null) {
            	result.put(uri, pd);
            }
        }
        
        return result;
    }
}
