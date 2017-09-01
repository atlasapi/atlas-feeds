package org.atlasapi.reporting.telescope;

import com.metabroadcast.common.properties.Configurer;

public class TelescopeConfiguration {

    // the actual configuration is drawn from atlas, because this runs as part of atlas.
    public static final String TELESCOPE_HOST = Configurer.get("telescope.host").get();
    public static final String ENVIRONMENT = Configurer.get("telescope.environment").get();

}
