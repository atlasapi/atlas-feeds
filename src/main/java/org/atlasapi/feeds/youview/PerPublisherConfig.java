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

    public static final Map<Publisher, String> TO_APP_ID_MAP = ImmutableMap.of(
            Publisher.BBC_NITRO, Configurer.get("YOUVIEW_UPLOAD_NITRO_EQUIVAPPID").get(), //not in use. Equiv not running for nitro.
            Publisher.AMAZON_UNBOX, Configurer.get("YOUVIEW_UPLOAD_UNBOX_EQUIVAPPID").get()
    );
}
