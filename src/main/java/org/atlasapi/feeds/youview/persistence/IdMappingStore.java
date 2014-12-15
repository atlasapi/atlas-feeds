package org.atlasapi.feeds.youview.persistence;

import com.google.common.base.Optional;

public interface IdMappingStore {

    void storeMapping(String key, String value);
    Optional<String> getValueFor(String key);

}
