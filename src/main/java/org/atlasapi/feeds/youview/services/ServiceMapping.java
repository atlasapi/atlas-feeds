package org.atlasapi.feeds.youview.services;

import com.google.common.base.Optional;


public interface ServiceMapping {

    Iterable<String> youviewServiceIdFor(String bbcServiceId);
}
