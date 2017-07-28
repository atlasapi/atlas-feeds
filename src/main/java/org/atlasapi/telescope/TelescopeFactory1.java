package org.atlasapi.telescope;

import com.metabroadcast.columbus.telescope.api.Environment;
import com.metabroadcast.columbus.telescope.api.Process;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Creates proxies to telescopeClients that can be used for reporting to telescope.
 * <p>
 * If you need to extend this class to accommodate more Processes (i.e. add more owl ingesters),
 * extend the {@link ReporterName1} enum accordingly.
 */
public class TelescopeFactory1 {

    public static final String TELESCOPE_HOST = "columbus-telescope.stage.svc.cluster.local";//Configurer.get("telescope.host").get();
    public static final String ENVIRONMENT ="STAGE";//Configurer.get("telescope.environment").get();
    private static final Logger log = LoggerFactory.getLogger(TelescopeFactory1.class);

    /**
     * This factory will always give you a telescope (never null). If there are initialization
     * errors the telescope you will get might be unable to report.
     */
    public static TelescopeProxy1 make(ReporterName1 reporterName) {
        Process process = getProcess(reporterName);
        TelescopeProxy1 telescopeProxy = new TelescopeProxy1(process);

        return telescopeProxy;
    }

    //create and return a telescope.api.Process.
    private static Process getProcess(ReporterName1 name) {
        Environment environment;
        try {
            environment = Environment.valueOf(ENVIRONMENT);
        } catch (IllegalArgumentException e) {
            //add stage as the default environment, which is better than crashing
            environment = Environment.STAGE;
            log.error(
                    "Could not find a telescope environment with the given name, name={}. Falling back to STAGE.",
                    ENVIRONMENT,
                    e
            );
        }

        return Process.create(name.getReporterKey(), name.getReporterName(), environment);
    }

    /**
     * Holds the pairs of Ingester Keys-Names used by atlas to report to telescope.
     */
    public enum ReporterName1 {
        YOU_VIEW_ASYNC_UPLOADER("you-view-async-uploader","YouView Async Uploader"),
        YOU_VIEW_SCHEDULE_UPLOADER("you-view-schedule-uploader","YouView Schedule Uploader"),
        YOU_VIEW_CONTENT_PROCESSOR("you-view-content-processor","YouView Content Processor"),
        YOU_VIEW_BBC_MULTI_UPLOADER("you-view-multi-uploader","YouView BBC Multi-uploader"),
        YOU_VIEW_REVOKER("you-view-revoker","YouView Revoker"),
        YOU_VIEW_UNREVOKER("you-view-unrevoker", "YouView Unrevoker"),
        YOU_VIEW_XML_UPLOADER("you-view-xml-uploader", "YouView XML Uploader"),
        YOU_VIEW_XML_DELETER("you-view-xml-delete","YouView XML deleter")
        ;

        String reporterKey;
        String reporterName;

        ReporterName1(String reporterKey, String reporterName) {
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

}
