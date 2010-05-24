package org.uriplay.beans;

import java.io.OutputStream;
import java.io.Reader;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.uriplay.rdf.beans.RdfXmlTranslator;

import com.google.common.util.concurrent.SimpleTimeLimiter;
import com.google.common.util.concurrent.TimeLimiter;
import com.google.common.util.concurrent.UncheckedTimeoutException;

public class TimeLimitingRdfXmlTranslator implements BeanGraphWriter, BeanGraphExtractor<Reader> {

	private final Log log = LogFactory.getLog(getClass());
	
	private final RdfXmlTranslator delegate;

	public TimeLimitingRdfXmlTranslator(RdfXmlTranslator delegate) {
		this.delegate = delegate;
	}

	@Override
	public void writeTo(Collection<Object> graph, OutputStream oStream) {
		TimeLimiter limiter = new SimpleTimeLimiter();
		BeanGraphWriter proxy = limiter.newProxy(delegate, BeanGraphWriter.class, 2, TimeUnit.MINUTES);
		try {
			proxy.writeTo(graph, oStream);
		} catch (UncheckedTimeoutException e) {
			log.warn("Failed to produce RDF within time limit, beans follow");
			for (Object object : graph) {
				log.info("Graph contained: " +  object);
			}
			throw e;
		}
	}

	@Override
	public Representation extractFrom(Reader source) {
		return delegate.extractFrom(source);
	}

}
