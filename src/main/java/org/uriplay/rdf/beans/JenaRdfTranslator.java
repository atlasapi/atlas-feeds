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

import static com.hp.hpl.jena.ontology.OntModelSpec.OWL_MEM;

import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.ClassPathResource;
import org.uriplay.beans.BeanIntrospector;
import org.uriplay.beans.DescriptionMode;
import org.uriplay.beans.Representation;
import org.uriplay.feeds.naming.ResourceMapping;
import org.uriplay.media.vocabulary.RDF;
import org.uriplay.rdf.RdfIntrospector;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.hp.hpl.jena.ontology.OntDocumentManager;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.vocabulary.OWL;

public abstract class JenaRdfTranslator extends AbstractRdfTranslator<OntModel, Resource, Property, Resource, String> 
                                                implements InitializingBean {

	private OntDocumentManager documentMgr;

	/**
	 * Define a particular output rendering of the Jena model to a stream.
	 */
	protected abstract void writeOut(OntModel rdf, OutputStream stream);

	public JenaRdfTranslator(TypeMap typeMap, ResourceMapping resourceMap) {
		this.typeMap = typeMap;
		this.resourceMap = resourceMap;
	}

	public void initOntologies() throws BeansException {
		for (Map.Entry<String, String> ontology : ontologyMap.entrySet()) {
			URL ontologyUrl;
			try {
				ontologyUrl = new ClassPathResource(ontology.getValue()).getURL();
			} catch (IOException e) {
				throw new BeanInitializationException("Failed to load ontology file " + ontology.getValue(), e);
			}

			documentMgr.addAltEntry(ontology.getKey(), ontologyUrl.toString());
		}
	}
    
    public void loadOntologies(OntModel model) {
        for (String ontology : ontologyMap.keySet()) {
            documentMgr.loadImport(model, ontology);
        }
    }

    public void loadPrefixes(OntModel model) {
        for (Map.Entry<String, String> nsPrefix : nsPrefixes.entrySet()) {
            model.setNsPrefix(nsPrefix.getValue(), nsPrefix.getKey());
        }
    }
    
	public Representation extractFrom(Reader stream) {
	    return extractFrom(stream, DescriptionMode.OPEN_WORLD);
	}
	
	public Representation extractFrom(Reader stream, DescriptionMode mode) {
		Representation representation = new Representation();
		Model rdf = readRdfDocument(stream);
        ResIterator subjects = rdf.listSubjects();

        while (subjects.hasNext()) {
            Resource resource = subjects.nextResource();
            String docId = createDocId(representation, resource);

            setBeanType(representation, rdf, resource, docId);
            setDocUris(representation, rdf, resource, docId);
            setPropertyValues(representation, rdf, resource, docId, mode);
        }		
		
		return representation;
	}

	@Override
    public void writeTo(Collection<Object> graph, OutputStream stream) {
		OntModel rdf = ModelFactory.createOntologyModel(OWL_MEM);

		loadOntologies(rdf);
	    loadPrefixes(rdf);
		   
		Map<Object, Resource> references = Maps.newHashMap();

		List<Object> workList = Lists.newArrayList(graph);
		for (int i = 0; i < workList.size(); i++) {
		    Object bean = workList.get(i);
		    
			addBean(rdf, workList, references, bean);
		}

		writeOut(rdf, stream);
	}

	public void afterPropertiesSet() throws Exception {
		documentMgr = OntDocumentManager.getInstance();
		documentMgr.setProcessImports(false);
		initOntologies();
	}

    @Override
    protected Property createTypedProperty(OntModel model, Resource resource, String ns, String uri) {
        return model.createProperty(ns + uri);
    }

    @Override
    protected Resource createTypedReference(OntModel model, String uri) {
        return model.createResource(uri);
    }

    @Override
    protected Resource createTypedResource(OntModel model, String uri) {
        return model.createResource(uri);
    }

    @Override
    protected void createReferenceStatement(
            OntModel model, 
            Resource subject,
            Property predicate, 
            Resource object) {
        subject.addProperty(predicate, object);   
    }

    @Override
    protected void createLiteralStatement(
            OntModel model, 
            Resource subject,
            Property predicate, 
            String literal) {
        subject.addProperty(predicate, literal);   
    }

    @Override
    protected Resource createTypedResource(OntModel model, Resource ref) {
        return ref;
    }

    @Override
    protected String createTypedLiteral(OntModel model, Object value) {
        return value.toString();
    }
    
    private Model readRdfDocument(Reader stream) {
        OntModel rdf = ModelFactory.createOntologyModel(OWL_MEM);
        loadOntologies(rdf);

        rdf.read(stream, null);

        return rdf;
    }
    
    private void setBeanType(Representation representation, Model rdf, Resource resource, String docId) {
        Property typeProperty = rdf.createProperty(RDF.NS + RDF.TYPE);
        Set<String> typeUris = Sets.newHashSet();

        StmtIterator stmts = resource.listProperties(typeProperty);
        while (stmts.hasNext()) {
            Resource typeResource = stmts.nextStatement().getResource();
            
            if (typeResource != null && !typeResource.isAnon()) {
                typeUris.add(typeResource.getURI());
            }
        }

        Class<?> beanType = typeMap.beanType(typeUris);

        if (beanType != null) {
            representation.addType(docId, beanType);
        }
    }

    private String createDocId(Representation representation, Resource resource) {
        String docId = null;
        
        if (resource.isAnon()) {
            docId = resource.getId().toString();
            representation.addAnonymous(docId);
        } else {
            docId = resource.getURI();
            representation.addUri(docId);
        }
        
        return docId;
    }

    private void setDocUris(Representation representation, Model rdf, Resource resource, String docId) {
        StmtIterator stmts = resource.listProperties(OWL.sameAs);
        while (stmts.hasNext()) {
            Resource sameAs = stmts.nextStatement().getResource();
            
            if (sameAs != null && !sameAs.isAnon()) {
                // TODO: turn this on when representations support doc URIs
//                representation.addDocUri(sameAs.getURI());
            }
        }
    }

    private void setPropertyValues(Representation representation, Model rdf, Resource resource, String docId, DescriptionMode mode) {
        Map<String, PropertyDescriptor> properties = getPropertyMetadata(representation, docId);

        if (properties.size() > 0) {
            MutablePropertyValues mpvs = new MutablePropertyValues();

            for (Map.Entry<String, PropertyDescriptor> propMetadata : properties.entrySet()) {
                Property propPredicate = rdf.getProperty(propMetadata.getKey());
                boolean isCollection = BeanIntrospector.isCollection(propMetadata.getValue());
                String propName = propMetadata.getValue().getName();

                StmtIterator stmts = resource.listProperties(propPredicate);

                if (stmts.hasNext()) {
                    while (stmts.hasNext()) {
                        Statement stmt = stmts.nextStatement();
                        RDFNode object = stmt.getObject();
                        Object propValue = null;

                        if (object instanceof Resource) {
                            Resource objResource = (Resource) object;

                            if (!objResource.isAnon()) {
                                propValue = objResource.getURI();
                            } else {
                                propValue = objResource.getId().toString();
                            }
                        } else if (object instanceof Literal) {
                            propValue = ((Literal) object).getValue();
                        }

                        addPropertyValue(mpvs, isCollection, propName, propValue);
                    }
                } else if (mode.equals(DescriptionMode.CLOSED_WORLD)) {
                    // In the closed world assumption we state that the
                    // representation
                    // is a complete description of the resources it contains.
                    // Therefore
                    // any property not explicitly described should be treated
                    // as an
                    // explicit null value.

                    // Initialise property to null.
                    if (isCollection) {
                        mpvs.addPropertyValue(propName, new ArrayList<Object>());
                    } else {
                        mpvs.addPropertyValue(propName, null);
                    }
                }

            }

            representation.addValues(docId, mpvs);
        }
    }

    @SuppressWarnings("unchecked")
    private void addPropertyValue(MutablePropertyValues mpvs, boolean isCollection, String name, Object value) {
        if (isCollection) {
            ArrayList<Object> list;
            PropertyValue pv = mpvs.getPropertyValue(name);

            if (pv == null) {
                list = new ArrayList<Object>();
                mpvs.addPropertyValue(name, list);
            } else {
                list = (ArrayList<Object>) pv.getValue();
            }

            list.add(value);    
        } else if (!mpvs.contains(name)) {
            mpvs.addPropertyValue(name, value);
        } else {
            // FIXME: Currently we ignore cardinality errors
            // FIXME because we commonly want to store only
            // FIXME one value where the RDF schema allows
            // FIXME many (eg. dc:title).
            /*
             * errors.add(new RequestError( docId,
             * "invalid.property", predicate.getURI() +
             * ": too many values supplied"));
             */
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, PropertyDescriptor> getPropertyMetadata(Representation representation, String docId) {
        Class<?> type = representation.getType(docId);

        if (type != null) {
            return Collections.unmodifiableMap(RdfIntrospector.getRdfPropertyDescriptors(type));
        } else {
            return (Map<String, PropertyDescriptor>) Collections.EMPTY_MAP;
        }
    }
}
