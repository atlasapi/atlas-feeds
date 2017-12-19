package org.atlasapi;

import org.junit.BeforeClass;

/**
 * This allows us to run tests that rely on configuration by recreating the required configuration
 * parameters. There is a need for this because Configuration is available in the main atlas
 * project, but not here. (this runs as part of owl during runtime so that's fine).
 */
public abstract class TestsWithConfiguration {

    @BeforeClass
    public static void recreateConfig() {
        System.setProperty("MBST_PLATFORM", "stage");
    }
}
