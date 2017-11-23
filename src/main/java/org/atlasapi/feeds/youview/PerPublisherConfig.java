package org.atlasapi.feeds.youview;

import java.util.Map;

import org.atlasapi.media.entity.Publisher;
import org.atlasapi.reporting.telescope.FeedsReporterNames;

import com.metabroadcast.common.properties.Configurer;

import com.google.common.collect.ImmutableMap;

/**
 * All per publisher maps where moved in one place to reduce the mess.
 */
public class PerPublisherConfig {

    private PerPublisherConfig() {
    }

    //More maps are around but cant be moved. The ones found are noted below.
    //1. ContentHierarchyExpanderFactory


    public static final Map<Publisher, FeedsReporterNames> TO_TELESCOPE_REPORTER_NAME = ImmutableMap.of(
            Publisher.BBC_NITRO, FeedsReporterNames.YOU_VIEW_AUTOMATIC_UPLOADER_NITRO,
            Publisher.AMAZON_UNBOX, FeedsReporterNames.YOU_VIEW_AUTOMATIC_UPLOADER_AMAZON
    );

    public static final Map<Publisher, String> TO_API_KEY_MAP = ImmutableMap.of(
            Publisher.BBC_NITRO, Configurer.get("youview.upload.nitro.equivapikey").get(),
            Publisher.AMAZON_UNBOX, Configurer.get("youview.upload.unbox.equivapikey").get()
    );

    public static final Map<Publisher, String> TO_APP_ID_MAP = ImmutableMap.of(
            Publisher.BBC_NITRO, "nitroAppId", //not in use. Equiv not running for nitro.
            Publisher.AMAZON_UNBOX, "jd9"
    );
}
