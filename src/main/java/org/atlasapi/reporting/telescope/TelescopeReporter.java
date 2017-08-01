package org.atlasapi.reporting.telescope;

/**
 * We implement this interface with Enums. Unluckily Enums cannot be abstract and are final, so
 * we'll have to reimplement the getters of the interface every time. If anyone finds good reason to
 * do so, use classes instead.
 */
public interface TelescopeReporter {

    public String getReporterKey();

    public String getReporterName();
}
