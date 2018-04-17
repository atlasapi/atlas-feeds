package org.atlasapi.reporting.telescope;

import com.metabroadcast.columbus.telescope.client.TelescopeReporterName;

/**
 * Add more elements as needed if more uploaders need to report to telescope.
 */
public enum FeedsReporterNames implements TelescopeReporterName {
    // naming the key with dot namespacing allows for grouping permissions when it comes to
    // viewing events, so be mindful of your names.
    // Other names might exist in different projects so make some effort to avoid collisions.
    YOU_VIEW_MANUAL_UPLOADER("youview.manual-uploader", "YouView Manual Uploader"),
    YOU_VIEW_MANUAL_SCHEDULE_UPLOADER("youview.manual-schedule-uploader", "YouView Manual Schedule Uploader"),
    YOU_VIEW_AUTOMATIC_UPLOADER("youview.automatic-uploader", "YouView Auto Uploader (All)"),
    YOU_VIEW_AUTOMATIC_UPLOADER_AMAZON("youview.amazon.automatic-uploader", "YouView Auto Uploader (Amazon)"),
    YOU_VIEW_AUTOMATIC_UPLOADER_NITRO("youview.nitro.automatic-uploader", "YouView Auto Uploader (Nitro)"),
    YOU_VIEW_AUTOMATIC_DELETER("youview.automatic-deleter", "YouView Auto Deleter" ),
    YOU_VIEW_AUTOMATIC_SCHEDULE_UPLOADER("youview.automatic-schedule-uploader", "YouView Auto Schedule Uploader"),

    YOU_VIEW_REVOKER("youview.revoker", "YouView Revoker"),
    YOU_VIEW_UNREVOKER("youview.unrevoker", "YouView Unrevoker"),

    RADIO_PLAYER_AUTO_OD_UPLOADER("radioplayer.od.auto-uploader", "RadioPlayer OD Auto Uploader"),
    RADIO_PLAYER_MANUAL_OD_UPLOADER("radioplayer.od.manual-uploader", "RadioPlayer OD Manual Uploader"),
    RADIO_PLAYER_AUTO_PI_UPLOADER("radioplayer.pi.auto-uploader", "RadioPlayer PI Auto Uploader"),
    RADIO_PLAYER_MANUAL_PI_UPLOADER("radioplayer.pi.manual-uploader", "RadioPlayer PI Manual Uploader"),
    ;

    String reporterKey;
    String reporterName;

    FeedsReporterNames(String reporterKey, String reporterName) {
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

