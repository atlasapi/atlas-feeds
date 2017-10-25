package org.atlasapi.reporting.telescope;

import java.util.Map;

import org.atlasapi.media.entity.Publisher;

import com.metabroadcast.columbus.telescope.client.TelescopeReporterName;

import com.google.common.collect.ImmutableMap;

/**
 * Add more elements as needed if more uploaders need to report to telescope.
 */
public enum FeedsReporterNames implements TelescopeReporterName {
    // naming the key with dot namespacing allows for grouping permissions when it comes to
    // viewing events, so be mindful of your names.
    // Other names might exist in different projects so make some effort to avoid collisions.
    YOU_VIEW_MANUAL_UPLOADER("youview.manual-uploader", "YouView Manual Uploader"),
    YOU_VIEW_MANUAL_SCHEDULE_UPLOADER("youview.manual-schedule-uploader", "YouView Manual Schedule Uploader"),
    YOU_VIEW_AUTOMATIC_UPLOADER("youview.automatic-uploader", "YV Auto Uploader (All)"),
    YOU_VIEW_AUTOMATIC_UPLOADER_UNBOX("youview.automatic-uploader.unbox", "YV Auto Uploader (Amazon)"),
    YOU_VIEW_AUTOMATIC_UPLOADER_NITRO("youview.automatic-uploader.nitro", "YV Auto Uploader (Nitro)"),
    YOU_VIEW_AUTOMATIC_DELETER("youview.automatic-deleter", "Youview Automatic Deleter" ),
    YOU_VIEW_AUTOMATIC_SCHEDULE_UPLOADER("youview.automatic-schedule-uploader", "YouView Automatic Schedule Uploader"),

    YOU_VIEW_REVOKER("youview.revoker", "YouView Revoker"),
    YOU_VIEW_UNREVOKER("youview.unrevoker", "YouView Unrevoker"),
    ;

    String reporterKey;
    String reporterName;

    FeedsReporterNames(String reporterKey, String reporterName) {
        this.reporterKey = reporterKey;
        this.reporterName = reporterName;
    }

    public static final Map<Publisher, FeedsReporterNames> PUBLISHER_MAPPING = ImmutableMap.of(
            Publisher.BBC_NITRO, YOU_VIEW_AUTOMATIC_UPLOADER_NITRO,
            Publisher.AMAZON_UNBOX, YOU_VIEW_AUTOMATIC_UPLOADER_UNBOX
    );

    public String getReporterKey() {
        return reporterKey;
    }

    public String getReporterName() {
        return reporterName;
    }
}

