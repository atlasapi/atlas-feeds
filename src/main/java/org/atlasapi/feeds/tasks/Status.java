package org.atlasapi.feeds.tasks;


public enum Status {

    NEW,
    PENDING,
    ACCEPTED,
    REJECTED,
    POLLING,
    QUARANTINED,
    VALIDATING,
    COMMITTING,
    COMMITTED,
    PUBLISHING,
    PUBLISHED,
    FAILED,
    UNKNOWN
    ;
}
