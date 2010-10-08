package org.atlasapi.query.content.parser;

import javax.servlet.http.HttpServletRequest;

import org.atlasapi.application.ApplicationConfiguration;
import org.atlasapi.application.query.ApplicationConfigurationFetcher;
import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.media.entity.Content;

public class ApplicationConfigurationIncludingQueryBuilder {
	
	private final QueryStringBackedQueryBuilder queryBuilder;
	private final ApplicationConfigurationFetcher configFetcher;

	public ApplicationConfigurationIncludingQueryBuilder(QueryStringBackedQueryBuilder queryBuilder, ApplicationConfigurationFetcher appFetcher) {
		this.queryBuilder = queryBuilder;
		this.queryBuilder.withIgnoreParams("apiKey");
		this.configFetcher = appFetcher;
	}

	public ContentQuery build(HttpServletRequest request,  Class<? extends Content> context) {
		ContentQuery query = queryBuilder.build(request, context);
		
		ApplicationConfiguration config = configFetcher.configurationFor(request).valueOrNull();
		
		if (config != null) {
			query = query.copyWithApplicationConfiguration(config);			
		}
		
		return query;
	}
	
}
