package org.atlasapi.reporting.telescope;

/**
 * Add more elements as needed if more uploaders need to report to telescope.
 */
public enum AtlasFeedsReporters implements TelescopeReporter {
    YOU_VIEW_ASYNC_UPLOADER("you-view-async-uploader", "YouView Async Uploader"),
    YOU_VIEW_SCHEDULE_UPLOADER("you-view-schedule-uploader", "YouView Schedule Uploader"),
    YOU_VIEW_CONTENT_PROCESSOR("you-view-content-processor", "YouView Content Processor"),
    YOU_VIEW_BBC_MULTI_UPLOADER("you-view-multi-uploader", "YouView BBC Multi-uploader"),
    YOU_VIEW_REVOKER("you-view-revoker", "YouView Revoker"),
    YOU_VIEW_UNREVOKER("you-view-unrevoker", "YouView Unrevoker"),
    YOU_VIEW_XML_UPLOADER("you-view-xml-uploader", "YouView XML Uploader"),
    YOU_VIEW_XML_DELETER("you-view-xml-delete", "YouView XML deleter");

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

