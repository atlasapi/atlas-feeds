package org.atlasapi.reporting.telescope;

import com.metabroadcast.common.properties.Configurer;

public class TelescopeConfiguration {

    // "columbus-telescope.stage.svc.cluster.local";
    public static final String TELESCOPE_HOST = Configurer.get("telescope.host").get();
    public static final String ENVIRONMENT = Configurer.get("telescope.environment").get();

}
