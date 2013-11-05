package org.atlasapi.query.content.parser;

import javax.servlet.http.HttpServletRequest;

import org.atlasapi.application.ApplicationSources;
import org.atlasapi.application.auth.ApplicationSourcesFetcher;
import org.atlasapi.application.auth.InvalidApiKeyException;
import org.atlasapi.content.criteria.AtomicQuery;
import org.atlasapi.content.criteria.ContentQuery;

import com.google.common.base.Optional;
import com.metabroadcast.common.query.Selection;

public class ApplicationConfigurationIncludingQueryBuilder {
	
	private final QueryStringBackedQueryBuilder queryBuilder;
	private final ApplicationSourcesFetcher configFetcher;

	public ApplicationConfigurationIncludingQueryBuilder(QueryStringBackedQueryBuilder queryBuilder, ApplicationSourcesFetcher appFetcher) {
		this.queryBuilder = queryBuilder;
		this.queryBuilder.withIgnoreParams("apiKey").withIgnoreParams("uri","id");
		this.configFetcher = appFetcher;
	}

	public ContentQuery build(HttpServletRequest request) throws InvalidApiKeyException {
		ContentQuery query = queryBuilder.build(request);
		Optional<ApplicationSources> sources = configFetcher.sourcesFor(request);
		if (sources.isPresent()) {
			query = query.copyWithApplicationSources(sources.get());			
		}
		return query;
	}
	
	public ContentQuery build(HttpServletRequest request, Iterable<AtomicQuery> operands, Selection selection) throws InvalidApiKeyException {
		ContentQuery query = new ContentQuery(operands, selection);
		Optional<ApplicationSources> sources = configFetcher.sourcesFor(request);
		if (sources.isPresent()) {
			query = query.copyWithApplicationSources(sources.get());			
		}
		return query;
	}
}
