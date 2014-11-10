package org.atlasapi.feeds.youview;

import java.util.Set;

import com.google.common.collect.ImmutableSet;


public class UploadPublisherConfigurations {

    
    private final Set<UploadPublisherConfiguration> configs;

    public UploadPublisherConfigurations(Iterable<UploadPublisherConfiguration> configs) {
        this.configs = ImmutableSet.copyOf(configs);
    }
    
    public Set<UploadPublisherConfiguration> getConfigs() {
        return configs;
    }
}
