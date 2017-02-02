package org.atlasapi.feeds.sitemaps;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.atlasapi.application.query.ApplicationFetcher;
import org.atlasapi.application.v3.DefaultApplication;
import org.atlasapi.feeds.sitemaps.channel4.C4SiteMapUriGenerator;
import org.atlasapi.feeds.sitemaps.channel4.HttpFetchingC4FlashPlayerVersionSupplier;
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

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.http.SimpleHttpClientBuilder;

@Configuration
public class SiteMapModule {

    private static final String ATLAS_USER_AGENT = "Mozilla/5.0 (compatible; atlas/3.0; +http://atlas.metabroadcast.com)";
    
	private @Autowired @Qualifier("queryExecutor") KnownTypeQueryExecutor queryExecutor;
	private @Autowired ContentLister contentLister;
	private @Value("${local.host.name}") String localHostName;
	private @Autowired ApplicationFetcher configFetcher;
	
	private @Value("${sitemaps.c4.brightcove.publisherId}") String c4BrightcovePublisherId;
	private @Value("${sitemaps.c4.brightcove.playerId}")    String c4BrightcovePlayerId;
	private @Value("${sitemaps.c4.flashplayerversion.uri}")     String c4FlashPlayerVersionUri;	
	private @Value("${sitemaps.c4.flashplayerversion.cacheInMinutes}") String cacheInMinutes; 
	private @Value("${service.web.id}")                     Long webServiceId;

    public @Bean ApplicationConfigurationIncludingQueryBuilder sitemapQueryBuilder() {
        return new ApplicationConfigurationIncludingQueryBuilder(
                new QueryStringBackedQueryBuilder(DefaultApplication.createDefault()).withIgnoreParams("format", "host"),
                configFetcher);
    }
	
	public @Bean SiteMapController siteMapController() {
		return new SiteMapController(queryExecutor, sitemapQueryBuilder(), 
		        contentLister, siteMapUriGenerators(), localHostName, webServiceId);
	}
	
	public @Bean Supplier<String> flashPlayerVersionSupplier() {
	    return Suppliers.memoizeWithExpiration(
	            new HttpFetchingC4FlashPlayerVersionSupplier(
	                    new SimpleHttpClientBuilder()
	                            .withUserAgent(ATLAS_USER_AGENT)
	                            .build(), 
	                    c4FlashPlayerVersionUri
	            ),
	            30, TimeUnit.MINUTES);
	}
	
	private Map<Publisher, SiteMapUriGenerator> siteMapUriGenerators() {
	    return ImmutableMap.<Publisher, SiteMapUriGenerator>of(Publisher.C4_PMLSD, 
	            new C4SiteMapUriGenerator(c4BrightcovePublisherId, c4BrightcovePlayerId, flashPlayerVersionSupplier()));
	}
}
