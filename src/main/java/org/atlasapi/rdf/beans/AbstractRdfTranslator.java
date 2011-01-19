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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.atlasapi.beans.BeanGraphWriter;
import org.atlasapi.beans.BeanIntrospector;
import org.atlasapi.content.rdf.annotations.RdfProperty;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.vocabulary.RDF;
import org.atlasapi.rdf.RdfIntrospector;
import org.springframework.beans.BeanWrapperImpl;

public abstract class AbstractRdfTranslator<ModelType, ResourceType, PropertyType, ReferenceType, LiteralType> implements  BeanGraphWriter {

	protected TypeMap typeMap = null;
	
	protected Map<String, String> ontologyMap = new HashMap<String, String>();
	
	protected Map<String, String> nsPrefixes = new HashMap<String, String>();

	public TypeMap getTypeMap() {
		return typeMap;
	}

	public void setTypeMap(TypeMap typeMap) {
		this.typeMap = typeMap;
	}

	public Map<String, String> getNsPrefixMap() {
	    return this.nsPrefixes;
	}

	public void setNsPrefixes(Properties nsPrefixes) {
	    for (Map.Entry<Object, Object> nsPrefix : nsPrefixes.entrySet()) {
	        this.nsPrefixes.put(
	            (String) nsPrefix.getKey(), 
	            (String) nsPrefix.getValue());
	    }
	}

	public void setOntologies(Properties ontologies) {
	    for (Map.Entry<Object, Object> ontology : ontologies.entrySet()) {
	        this.ontologyMap.put(
	            (String) ontology.getKey(), 
	            (String) ontology.getValue());
	    }
	}

	public void addBean(
	        ModelType model, 
	        List<Object> workList,
            Map<Object, ReferenceType> references, 
            Object bean) {
        ResourceType resource = null;
        ReferenceType reference = null;
        
        reference = createReference(model, uriOf(bean), references, bean);
        resource = createResource(model, bean, reference);
        addPropertyValues(model, workList, references, bean, resource);
    }

	protected abstract ReferenceType createTypedReference(ModelType model, String uri);

	protected abstract ResourceType createTypedResource(ModelType model, String uri);

	protected abstract ResourceType createTypedResource(ModelType model, ReferenceType ref);

	protected abstract LiteralType createTypedLiteral(ModelType model, Object value);

	protected abstract PropertyType createTypedProperty(ModelType model, ResourceType resource, String propertyNs, String propertyUri);

	protected abstract void createLiteralStatement(ModelType model, ResourceType subject, PropertyType predicate, LiteralType literal);

	protected abstract void createReferenceStatement(ModelType model, ResourceType subject, PropertyType predicate, ReferenceType object);

    protected ResourceType createResource(ModelType model, Object bean, ReferenceType ref) {
	    ResourceType resource = createTypedResource(model, ref);
	    PropertyType typeProperty = createTypedProperty(model, resource, RDF.NS,  RDF.TYPE);
	    Set<String> beanTypeUris = typeMap.rdfTypes(bean.getClass());
	    
	    for (String beanTypeUri : beanTypeUris) {
	        ReferenceType typeRef = createTypedReference(model, beanTypeUri);
	        createReferenceStatement(model, resource, typeProperty, typeRef);
	    }
	    
	    return resource;
    }
		
	protected ReferenceType createReference(
            ModelType model, 
            String uri,
            Map<Object, ReferenceType> references, 
            Object bean) {
        ReferenceType ref = null;

        if (bean != null && references.containsKey(bean)) {
            ref = references.get(bean);
        } else {
            ref = createTypedReference(model, uri);
            
            if (bean != null) {
                references.put(bean, ref);
            }
        }
                
        return ref;
    }
    
    protected void addPropertyValues(
            ModelType model,
            List<Object> workList,
            Map<Object, ReferenceType> references,
            Object bean, 
            ResourceType resource) {
        Map<String, PropertyDescriptor> properties;
        String defaultNs;
        BeanWrapperImpl wrapper = new BeanWrapperImpl(bean);

        try {
        	defaultNs = RdfIntrospector.defaultNamespace(bean.getClass());
            properties = 
                BeanIntrospector.getPropertyDescriptors(bean.getClass());
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }

        for (Map.Entry<String, PropertyDescriptor> property : properties.entrySet()) {
            Object value = wrapper.getPropertyValue(property.getKey());

            if (value != null 
            	&& (!(value instanceof Collection<?>) || ((Collection<?>) value).size() > 0)) {
            	PropertyDescriptor pd = property.getValue();
            	RdfProperty rdfProperty = RdfIntrospector.getProperty(pd);

				if (rdfProperty != null) {
					boolean isEntityRelation = BeanIntrospector.isRelation(pd);
					String propertyUri = rdfProperty.uri().length() > 0 ? rdfProperty.uri() : property.getKey();
					String propertyNs = rdfProperty.namespace().length() > 0 ? rdfProperty.namespace() : defaultNs;
					PropertyType typedProperty = createTypedProperty(model, resource, propertyNs, propertyUri);

					addPropertyValue(model, resource, value, typedProperty, isEntityRelation, rdfProperty.relation(), workList, references);
				}
            }
        }
    }

    protected void addPropertyValue(
            ModelType model, 
            ResourceType resource, 
            Object value, 
            PropertyType rdfProperty,
            boolean isEntityRelation,
            boolean isRdfRelation, 
            List<Object> workList,
            Map<Object, ReferenceType> references) {
        if (value instanceof Collection<?>) {
            addCollectionPropertyValue(
                    model,
                    resource,
                    (Collection<?>) value, 
                    rdfProperty,
                    isEntityRelation,
                    isRdfRelation,
                    workList, 
                    references);
        } else if (isEntityRelation) {
            ReferenceType valueRef = createReference(model, uriOf(value), references, value);
            createReferenceStatement(model, resource, rdfProperty, valueRef);
                
            if (!workList.contains(value)) {
                workList.add(value);
            }
        } else if (isRdfRelation) {
            ReferenceType valueRef = createReference(model, value.toString(), references, value);
            createReferenceStatement(model, resource, rdfProperty, valueRef);                
        } else {
            LiteralType valueLit = createTypedLiteral(model, value.toString());
            createLiteralStatement(model, resource, rdfProperty, valueLit);                
        }
    }
    
	private String uriOf(Object bean) {
		return (bean instanceof Identified) ? ((Identified) bean).getCanonicalUri() : null;
	}

    protected void addCollectionPropertyValue(
            ModelType model, 
            ResourceType resource, 
            Collection<?> c, 
            PropertyType property,
            boolean isEntityRelation, 
            boolean isRdfRelation, 
            List<Object> workList,
            Map<Object, ReferenceType> references) {
        for (Object value : c) {
        	if (value != null) {
        		addPropertyValue(
        				model,
        				resource,
        				value, 
        				property, 
        				isEntityRelation,
        				isRdfRelation,
        				workList, 
        				references);
        	}
        } 
    }

}
