package org.atlasapi.feeds.sitemaps;

import java.util.Map;

import org.atlasapi.application.query.ApplicationConfigurationFetcher;
import org.atlasapi.feeds.sitemaps.channel4.C4FlashPlayerVersionSupplier;
import org.atlasapi.feeds.sitemaps.channel4.C4SiteMapUriGenerator;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.listing.ContentLister;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.query.content.parser.ApplicationConfigurationIncludingQueryBuilder;
import org.atlasapi.query.content.parser.QueryStringBackedQueryBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.common.collect.ImmutableMap;

@Configuration
public class SiteMapModule {

	private @Autowired @Qualifier("queryExecutor") KnownTypeQueryExecutor queryExecutor;
	private @Autowired ContentLister contentLister;
	private @Value("${local.host.name}") String localHostName;
	private @Autowired ApplicationConfigurationFetcher configFetcher;
	
	private @Value("${sitemaps.c4.brightcove.publisherId}") String c4BrightcovePublisherId;
	private @Value("${sitemaps.c4.brightcove.playerId}")    String c4BrightcovePlayerId;
	private @Value("${sitemaps.c4.flashplayerversion}")     String c4FlashPlayerVersion;
	private @Value("${service.web.id}")                     Long webServiceId;

    public @Bean ApplicationConfigurationIncludingQueryBuilder sitemapQueryBuilder() {
        return new ApplicationConfigurationIncludingQueryBuilder(
                new QueryStringBackedQueryBuilder().withIgnoreParams("format", "host"), 
                configFetcher);
    }
	
	public @Bean SiteMapController siteMapController() {
		return new SiteMapController(queryExecutor, sitemapQueryBuilder(), 
		        contentLister, siteMapUriGenerators(), localHostName, webServiceId);
	}
	
	private Map<Publisher, SiteMapUriGenerator> siteMapUriGenerators() {
	    return ImmutableMap.<Publisher, SiteMapUriGenerator>of(Publisher.C4_PMLSD, 
	            new C4SiteMapUriGenerator(c4BrightcovePublisherId, c4BrightcovePlayerId, new C4FlashPlayerVersionSupplier() {
                    
                    @Override
                    public String get() {
                        return c4FlashPlayerVersion;
                    }
                }));
	}
}
