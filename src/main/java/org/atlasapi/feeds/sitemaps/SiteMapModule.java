package org.atlasapi.feeds.sitemaps;

import org.atlasapi.application.ApplicationSourcesFetcher;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.query.content.parser.ApplicationConfigurationIncludingQueryBuilder;
import org.atlasapi.query.content.parser.QueryStringBackedQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SiteMapModule {

	private @Autowired @Qualifier("queryExecutor") KnownTypeQueryExecutor queryExecutor;
	private @Autowired ContentLister contentLister;
	private @Value("${local.host.name}") String localHostName;
	private @Autowired ApplicationSourcesFetcher configFetcher;

    public @Bean ApplicationConfigurationIncludingQueryBuilder sitemapQueryBuilder() {
        return new ApplicationConfigurationIncludingQueryBuilder(new QueryStringBackedQueryBuilder().withIgnoreParams("format", "host"), configFetcher);
    }
	
	public @Bean SiteMapController siteMapController() {
		return new SiteMapController(queryExecutor, sitemapQueryBuilder(), contentLister, localHostName);
	}
}
