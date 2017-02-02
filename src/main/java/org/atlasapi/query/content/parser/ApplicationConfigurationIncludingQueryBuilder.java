package org.atlasapi.query.content.parser;

import javax.servlet.http.HttpServletRequest;

import com.metabroadcast.applications.client.model.internal.Application;
import org.atlasapi.application.query.ApplicationFetcher;
import org.atlasapi.application.query.InvalidApiKeyException;
import org.atlasapi.content.criteria.AtomicQuery;
import org.atlasapi.content.criteria.ContentQuery;

import com.metabroadcast.common.query.Selection;

public class ApplicationConfigurationIncludingQueryBuilder {
	
	private final QueryStringBackedQueryBuilder queryBuilder;
	private final ApplicationFetcher configFetcher;

	public ApplicationConfigurationIncludingQueryBuilder(QueryStringBackedQueryBuilder queryBuilder, ApplicationFetcher appFetcher) {
		this.queryBuilder = queryBuilder;
		this.queryBuilder.withIgnoreParams("apiKey").withIgnoreParams("uri","id","event_ids");
		this.configFetcher = appFetcher;
	}

	public ContentQuery build(HttpServletRequest request) throws InvalidApiKeyException {
		ContentQuery query = queryBuilder.build(request);
		Application config = configFetcher.applicationFor(request).orElse(null);
		if (config != null) {
			query = query.copyWithApplication(config);
		}
		return query;
	}
	
	public ContentQuery build(
			HttpServletRequest request,
			Iterable<AtomicQuery> operands,
			Selection selection
	) throws InvalidApiKeyException {
		ContentQuery query = new ContentQuery(operands, selection);
		Application config = configFetcher.applicationFor(request).orElse(null);
		if (config != null) {
			query = query.copyWithApplication(config);
		}
		return query;
	}
}
