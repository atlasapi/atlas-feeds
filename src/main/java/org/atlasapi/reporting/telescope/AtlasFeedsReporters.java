package org.atlasapi.reporting.telescope;

import com.metabroadcast.columbus.telescope.client.TelescopeReporterName;

/**
 * Add more elements as needed if more uploaders need to report to telescope.
 */
public enum AtlasFeedsReporters implements TelescopeReporterName {
    // naming the key with dot namespacing allows for grouping permissions when it comes to
    // viewing events, so be mindful of your names.
    // Other names might exist in different projects so make some effort to avoid collisions.
    YOU_VIEW_MANUAL_UPLOADER("youview.manual-uploader", "YouView Manual Uploader"),
    YOU_VIEW_MANUAL_SCHEDULE_UPLOADER("youview.manual-schedule-uploader", "YouView Manual Schedule Uploader"),
    YOU_VIEW_AUTOMATIC_UPLOADER("youview.automatic-uploader", "YouView Automatic Uploader"),
    YOU_VIEW_AUTOMATIC_SCHEDULE_UPLOADER("youview.automatic-schedule-uploader", "YouView Automatic Schedule Uploader"),

    YOU_VIEW_REVOKER("youview.revoker", "YouView Revoker"),
    YOU_VIEW_UNREVOKER("youview.unrevoker", "YouView Unrevoker")
    ;

    String reporterKey;
    String reporterName;

    AtlasFeedsReporters(String reporterKey, String reporterName) {
        this.reporterKey = reporterKey;
        this.reporterName = reporterName;
    }

    public String getReporterKey() {
        return reporterKey;
    }

    public String getReporterName() {
        return reporterName;
    }
}

