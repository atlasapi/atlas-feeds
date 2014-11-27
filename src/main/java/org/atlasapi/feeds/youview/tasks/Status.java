package org.atlasapi.feeds.youview.tasks;


public enum Status {

    NEW,
    UPLOADING,
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
