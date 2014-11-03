package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import org.atlasapi.feeds.youview.genres.GenreMapping;
import org.atlasapi.feeds.youview.ids.IdParser;
import org.atlasapi.feeds.youview.ids.PublisherIdUtility;
import org.atlasapi.feeds.youview.images.ImageConfiguration;
import org.atlasapi.media.entity.Publisher;

import com.google.common.collect.ImmutableMap;
import com.metabroadcast.common.http.SimpleHttpClient;


public class YouViewPerPublisherFactory {
    
    private final Map<Publisher, PublisherIdUtility> configurations;
    private final Map<Publisher, ImageConfiguration> imageConfigurations;
    private final Map<Publisher, IdParser> idParsers;
    private final Map<Publisher, GenreMapping> genreMaps;
    private final Map<Publisher, SimpleHttpClient> httpClients;
    
    public static Builder builder() {
        return new Builder();
    }
    
    private YouViewPerPublisherFactory(Map<Publisher, PublisherIdUtility> configurations, 
            Map<Publisher, ImageConfiguration> imageConfigurations,
            Map<Publisher, IdParser> idParsers, Map<Publisher, GenreMapping> genreMaps, 
            Map<Publisher, SimpleHttpClient> httpClients) {
                this.configurations = ImmutableMap.copyOf(checkNotNull(configurations));
                this.imageConfigurations = ImmutableMap.copyOf(checkNotNull(imageConfigurations));
                this.idParsers = ImmutableMap.copyOf(checkNotNull(idParsers));
                this.genreMaps = ImmutableMap.copyOf(checkNotNull(genreMaps));
                this.httpClients = ImmutableMap.copyOf(checkNotNull(httpClients));
    }

    public PublisherIdUtility getIdUtil(Publisher publisher) {
        PublisherIdUtility idUtil = configurations.get(publisher);
        if (idUtil == null) {
            throw new InvalidPublisherException(publisher);
        }
        return idUtil;
    }
    
    public ImageConfiguration getImageConfig(Publisher publisher) {
        ImageConfiguration configuration = imageConfigurations.get(publisher);
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
        
        private ImmutableMap.Builder<Publisher, PublisherIdUtility> idUtilities = ImmutableMap.<Publisher, PublisherIdUtility>builder();
        private ImmutableMap.Builder<Publisher, ImageConfiguration> imageConfigurations = ImmutableMap.<Publisher, ImageConfiguration>builder();
        private ImmutableMap.Builder<Publisher, IdParser> idParsers = ImmutableMap.<Publisher, IdParser>builder();
        private ImmutableMap.Builder<Publisher, GenreMapping> genreMaps = ImmutableMap.<Publisher, GenreMapping>builder();
        private ImmutableMap.Builder<Publisher, SimpleHttpClient> httpClients = ImmutableMap.<Publisher, SimpleHttpClient>builder();
        
        private Builder() {}
        
        public YouViewPerPublisherFactory build() {
            return new YouViewPerPublisherFactory(idUtilities.build(), imageConfigurations.build(), 
                    idParsers.build(), genreMaps.build(), httpClients.build());
        }
        
        public Builder withPublisher(Publisher publisher, PublisherIdUtility config, ImageConfiguration imageConfig,
                IdParser idParser, GenreMapping genreMap, SimpleHttpClient httpClient) {
            idUtilities.put(checkNotNull(publisher), checkNotNull(config));
            imageConfigurations.put(publisher, checkNotNull(imageConfig));
            idParsers.put(publisher, checkNotNull(idParser));
            genreMaps.put(publisher, checkNotNull(genreMap));
            httpClients.put(publisher, checkNotNull(httpClient));
            return this;
        }
    }
}
