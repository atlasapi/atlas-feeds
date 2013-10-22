package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.atlasapi.media.entity.Publisher;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.metabroadcast.common.http.SimpleHttpClient;


public class YouViewPerPublisherFactory {
    
    private final Map<Publisher, PublisherConfiguration> configurations;
    private final Map<Publisher, IdParser> idParsers;
    private final Map<Publisher, GenreMapping> genreMaps;
    private final Map<Publisher, SimpleHttpClient> httpClients;
    
    public static Builder builder() {
        return new Builder();
    }
    
    private YouViewPerPublisherFactory(Map<Publisher, PublisherConfiguration> configurations, Map<Publisher, IdParser> idParsers,
            Map<Publisher, GenreMapping> genreMaps, Map<Publisher, SimpleHttpClient> httpClients) {
                this.configurations = ImmutableMap.copyOf(checkNotNull(configurations));
                this.idParsers = ImmutableMap.copyOf(checkNotNull(idParsers));
                this.genreMaps = ImmutableMap.copyOf(checkNotNull(genreMaps));
                this.httpClients = ImmutableMap.copyOf(checkNotNull(httpClients));
    }

    public PublisherConfiguration getConfiguration(Publisher publisher) {
        PublisherConfiguration configuration = configurations.get(publisher);
        if (configuration == null) {
            throw new InvalidPublisherException(publisher);
        }
        return configuration;
    }
    
    public IdParser getIdParser(Publisher publisher) {
        IdParser idParser = idParsers.get(publisher);
        if (idParser == null) {
            throw new InvalidPublisherException(publisher);
        }
        return idParser;
    }
    
    public GenreMapping getGenreMapping(Publisher publisher) {
        GenreMapping genreMap = genreMaps.get(publisher);
        if (genreMap == null) {
            throw new InvalidPublisherException(publisher);
        }
        return genreMap;
    }
    
    public SimpleHttpClient getHttpClient(Publisher publisher) {
        SimpleHttpClient httpClient = httpClients.get(publisher);
        if (httpClient == null) {
            throw new InvalidPublisherException(publisher);
        }
        return httpClient;
    }
    
    public static class Builder {
        
        private Map<Publisher, PublisherConfiguration> configurations = Maps.newHashMap();
        private Map<Publisher, IdParser> idParsers = Maps.newHashMap();
        private Map<Publisher, GenreMapping> genreMaps = Maps.newHashMap();
        private Map<Publisher, SimpleHttpClient> httpClients = Maps.newHashMap();
        
        private Builder() {}
        
        public YouViewPerPublisherFactory build() {
            return new YouViewPerPublisherFactory(configurations, idParsers, genreMaps, httpClients);
        }
        
        public Builder withPublisher(Publisher publisher, PublisherConfiguration config, IdParser idParser, 
                GenreMapping genreMap, SimpleHttpClient httpClient) {
            configurations.put(checkNotNull(publisher), checkNotNull(config));
            idParsers.put(publisher, checkNotNull(idParser));
            genreMaps.put(publisher, checkNotNull(genreMap));
            httpClients.put(publisher, checkNotNull(httpClient));
            return this;
        }
    }
}
