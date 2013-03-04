package org.atlasapi.feeds;


public final class UpdateProgress implements Reducible<UpdateProgress>{
    
    public static final UpdateProgress START = new UpdateProgress(0,0);
    public static final UpdateProgress SUCCESS = new UpdateProgress(1, 0);
    public static final UpdateProgress FAILURE = new UpdateProgress(0, 1);
    
    private final int processed;
    private final int failures;
    

    public UpdateProgress(int processed, int failures) {
        this.processed = processed;
        this.failures = failures;
    }
    
    @Override
    public UpdateProgress reduce(UpdateProgress other) {
        if(other == null || START.equals(other)) {
            return this;
        }
        return new UpdateProgress(processed + other.processed, failures + other.failures);
    }
    

    public int getFailures() {
        return failures;
    }

    public int getProcessed() {
        return processed;
    }
    
    public boolean hasFailures() {
        return failures > 0;
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof UpdateProgress) {
            UpdateProgress other = (UpdateProgress) that;
            return processed == other.processed && failures == other.failures;
        }
        return false;
    }
    
    public int hashCode() {
        //Same as Objects.hashCode(processed, failures)
        return 961 + ((processed << 5) - processed) + failures;
    }
    
    @Override
    public String toString() {
        return toString("Progress: %d processed, %s failures");
    }
    
    public String toString(String pattern) {
        return String.format(pattern, processed, failures);
    }
    
    
}
