package org.atlasapi.feeds.youview;

import org.atlasapi.feeds.tvanytime.TvAnytimeGenerator;
import org.atlasapi.feeds.youview.persistence.YouViewLastUpdatedStore;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;

import com.metabroadcast.common.security.UsernameAndPassword;

public class YouViewDeltaUploader extends YouViewUploader {

    public YouViewDeltaUploader(String youViewUrl, LastUpdatedContentFinder contentFinder, TvAnytimeGenerator generator, UsernameAndPassword credentials, YouViewLastUpdatedStore store) {
        super(youViewUrl, contentFinder, generator, credentials, store, false);
    }

}
