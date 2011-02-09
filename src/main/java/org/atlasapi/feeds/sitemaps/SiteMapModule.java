package org.atlasapi.feeds.sitemaps;

import org.atlasapi.application.query.ApplicationConfigurationFetcher;
import org.atlasapi.persistence.content.mongo.MongoDbBackedContentStore;
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
	private @Autowired MongoDbBackedContentStore mongoStore;
	private @Value("${local.host.name}") String localHostName;
	private @Autowired ApplicationConfigurationFetcher configFetcher;

    public @Bean ApplicationConfigurationIncludingQueryBuilder sitemapQueryBuilder() {
        return new ApplicationConfigurationIncludingQueryBuilder(new QueryStringBackedQueryBuilder().withIgnoreParams("format", "host"), configFetcher);
    }
	
	public @Bean SiteMapController siteMapController() {
		return new SiteMapController(queryExecutor, sitemapQueryBuilder(), localHostName);
	}
	
	public @Bean SiteMapExperimentalController siteMapExperimentalController() {
		return new SiteMapExperimentalController(mongoStore, sitemapQueryBuilder(), localHostName);
	}
}
