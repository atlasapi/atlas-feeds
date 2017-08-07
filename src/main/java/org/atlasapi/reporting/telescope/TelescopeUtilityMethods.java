package org.atlasapi.reporting.telescope;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import telescope_client_shaded.com.metabroadcast.columbus.telescope.api.Environment;
import telescope_client_shaded.com.metabroadcast.columbus.telescope.api.Process;

import java.util.Set;

public class TelescopeUtilityMethods {

    private static final Logger log = LoggerFactory.getLogger(TelescopeUtilityMethods.class);

    //create and return a telescope.api.Process.
    protected static Process getProcess(TelescopeReporter name) {
        Environment environment;
        try {
            environment = Environment.valueOf(TelescopeConfiguration.ENVIRONMENT);
        } catch (IllegalArgumentException e) {
            //add stage as the default environment, which is better than crashing
            environment = Environment.STAGE;
            log.warn(
                    "Could not find a telescope environment with the given name, name={}. Falling back to STAGE.",
                    TelescopeConfiguration.ENVIRONMENT,
                    e
            );
        }

        return Process.create(name.getReporterKey(), name.getReporterName(), environment);
    }
}
