package org.atlasapi.reporting.telescope;

import com.metabroadcast.columbus.telescope.client.TelescopeReporterName;

/**
 * Add more elements as needed if more uploaders need to report to telescope.
 */
public enum AtlasFeedsReporters implements TelescopeReporterName {
    // naming the key with dot namespacing allows for grouping permissions when it comes to
    // viewing events, so be mindful of your names.
    // Other names might exist in different projects so make some effort to avoid collisions.
    YOU_VIEW_ASYNC_UPLOADER("youview.uploader", "YouView Async Uploader"),
    YOU_VIEW_SCHEDULE_UPLOADER("youview.schedule-uploader", "YouView Schedule Uploader"),
    YOU_VIEW_CONTENT_PROCESSOR("youview.content-processor", "YouView Content Processor"),
    YOU_VIEW_BBC_MULTI_UPLOADER("youview.multi-uploader", "YouView BBC Multi-uploader"),
    YOU_VIEW_REVOKER("youview.revoker", "YouView Revoker"),
    YOU_VIEW_UNREVOKER("youview.unrevoker", "YouView Unrevoker"),
    YOU_VIEW_XML_UPLOADER("youview.xml-uploader", "YouView XML Uploader"),
    YOU_VIEW_XML_DELETER("youview.xml-deleter", "YouView XML deleter");

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

