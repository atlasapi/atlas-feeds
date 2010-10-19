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

package org.atlasapi.beans;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.atlasapi.content.rdf.annotations.RdfClass;
import org.springframework.beans.PropertyAccessorUtils;
import org.springframework.core.GenericCollectionTypeResolver;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

public class BeanIntrospector {

	private static final Set<String> RDF_IGNORE_ATTRIBUTES = ImmutableSet.of("isActivelyPublished", "series");
	
    public static Map<String, PropertyDescriptor> getPropertyDescriptors(Class<?> beanType) throws IntrospectionException {
        Map<String, PropertyDescriptor> result = Maps.newHashMap();

        BeanIntrospector.getPropertiesInternal(beanType, "", result);

        return result;
    }

    public static Map<String, PropertyDescriptor> getRelations(Class<?> beanType) throws IntrospectionException {
        Map<String, PropertyDescriptor> result = Maps.newHashMap();
        Map<String, PropertyDescriptor> properties = BeanIntrospector.getPropertyDescriptors(beanType);

        for (Map.Entry<String, PropertyDescriptor> propertyEntry : properties.entrySet()) {
            Class<?> propertyType = propertyEntry.getValue().getPropertyType();

            if (Collection.class.isAssignableFrom(propertyType)) {
                Method readMethod = propertyEntry.getValue().getReadMethod();
                propertyType = GenericCollectionTypeResolver.getCollectionReturnType(readMethod);
            }

            if (propertyType != null && isEntity(propertyType)) {
                result.put(propertyEntry.getKey(), propertyEntry.getValue());
            }
        }

        return result;
    }

    public static boolean isEntity(Class<?> type) {
        if (type == null) {
            return false;
        }
        return type.getAnnotation(RdfClass.class) != null;
    }

    public static boolean isCollection(PropertyDescriptor property) {
        Class<?> type = property.getPropertyType();

        return type != null && Collection.class.isAssignableFrom(type);
    }

    public static boolean isRelation(PropertyDescriptor property) {
        Class<?> type = property.getPropertyType();

        if (type != null) {
            if (Collection.class.isAssignableFrom(type)) {
                Method readMethod = property.getReadMethod();
                Class<?> elementType = GenericCollectionTypeResolver.getCollectionReturnType(readMethod);

                return isEntity(elementType);
            } else {
                return isEntity(type);
            }
        } else {
            return false;
        }
    }

    public static PropertyDescriptor getPropertyDescriptor(Class<?> beanType, String propertyPath) throws Exception {
        BeanInfo beanInfo = Introspector.getBeanInfo(beanType);

        String current = null;
        String remainder = null;
        int separator = PropertyAccessorUtils.getFirstNestedPropertySeparatorIndex(propertyPath);

        // Handle nested properties recursively.
        if (separator > -1) {
            current = propertyPath.substring(0, separator);
            remainder = propertyPath.substring(separator + 1);
        } else {
            current = propertyPath;
        }

        PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) {
            Class<?> propertyType = pd.getPropertyType();

            // We ignore properties without a known type; they cannot be used
            // to define RDF metadata.
            if (propertyType != null && pd.getName().equals(current)) {
                if (remainder == null) {
                    return pd;
                } else {
                    return getPropertyDescriptor(propertyType, remainder);
                }
            }
        }

        return null;
    }

    private static void getPropertiesInternal(Class<?> entityType, String prefix, Map<String, PropertyDescriptor> result) throws IntrospectionException {
        BeanInfo beanInfo = Introspector.getBeanInfo(entityType);

        PropertyDescriptor[] pds = beanInfo.getPropertyDescriptors();
        for (PropertyDescriptor pd : pds) {
        	if (RDF_IGNORE_ATTRIBUTES.contains(pd.getName())) {
        		continue;
        	}
            result.put(prefix + pd.getName(), pd);
        }
    }

}
