package org.atlasapi.feeds.youview;

import org.atlasapi.feeds.youview.persistence.IdMappingStore;
import org.atlasapi.feeds.youview.persistence.MongoIdMappingStore;
import org.atlasapi.feeds.youview.www.NitroYouViewResolutionController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.metabroadcast.common.persistence.mongo.DatabasedMongo;

@Configuration
public class NitroYouViewResolutionModule {

    @Autowired 
    private DatabasedMongo mongo;
    
    @Bean
    public IdMappingStore idMappingStore() {
        return new MongoIdMappingStore(mongo);
    }
    
    @Bean
    public NitroYouViewResolutionController nitroYouViewResolutionController() {
        return new NitroYouViewResolutionController(idMappingStore());
    }
    
}
