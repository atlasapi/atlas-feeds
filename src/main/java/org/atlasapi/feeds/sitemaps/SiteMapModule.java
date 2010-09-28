package org.atlasapi.feeds.sitemaps;

import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SiteMapModule {

	private @Autowired @Qualifier("mongoDbQueryExcutorThatFiltersUriQueries") KnownTypeQueryExecutor queryExecutor;

	public @Bean SiteMapController siteMapController() {
		return new SiteMapController(queryExecutor);
	}
}
