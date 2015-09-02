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
    
    // TODO it may be worth adding subsets of these as public static methods, such as
    // non-terminal states, or un-uploaded states.
}
