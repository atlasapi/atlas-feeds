package org.atlasapi.query.content.parser;

import javax.servlet.http.HttpServletRequest;

import org.atlasapi.application.query.ApiKeyNotFoundException;
import org.atlasapi.application.query.ApplicationConfigurationFetcher;
import org.atlasapi.application.query.InvalidIpForApiKeyException;
import org.atlasapi.application.query.RevokedApiKeyException;
import org.atlasapi.application.v3.ApplicationConfiguration;
import org.atlasapi.content.criteria.AtomicQuery;
import org.atlasapi.content.criteria.ContentQuery;

import com.metabroadcast.common.query.Selection;

public class ApplicationConfigurationIncludingQueryBuilder {
	
	private final QueryStringBackedQueryBuilder queryBuilder;
	private final ApplicationConfigurationFetcher configFetcher;

	public ApplicationConfigurationIncludingQueryBuilder(QueryStringBackedQueryBuilder queryBuilder, ApplicationConfigurationFetcher appFetcher) {
		this.queryBuilder = queryBuilder;
		this.queryBuilder.withIgnoreParams("apiKey").withIgnoreParams("uri","id","event_ids");
		this.configFetcher = appFetcher;
	}

	public ContentQuery build(HttpServletRequest request) throws ApiKeyNotFoundException, RevokedApiKeyException, InvalidIpForApiKeyException {
		ContentQuery query = queryBuilder.build(request);
		ApplicationConfiguration config = configFetcher.configurationFor(request).valueOrNull();
		if (config != null) {
			query = query.copyWithApplicationConfiguration(config);			
		}
		return query;
	}
	
	public ContentQuery build(HttpServletRequest request, Iterable<AtomicQuery> operands, Selection selection) throws ApiKeyNotFoundException, RevokedApiKeyException, InvalidIpForApiKeyException {
		ContentQuery query = new ContentQuery(operands, selection);
		ApplicationConfiguration config = configFetcher.configurationFor(request).valueOrNull();
		if (config != null) {
			query = query.copyWithApplicationConfiguration(config);			
		}
		return query;
	}
}
