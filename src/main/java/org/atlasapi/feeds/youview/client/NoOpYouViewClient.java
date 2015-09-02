package org.atlasapi.feeds.youview.client;

import static com.google.common.base.Preconditions.checkNotNull;
import static javax.servlet.http.HttpServletResponse.SC_ACCEPTED;

import org.atlasapi.feeds.tasks.Payload;

import com.metabroadcast.common.time.Clock;

/**
 * Minimal zero-interaction YouView client to test the rest of the 
 * tasks structure without interacting with a remote YouView environment
 * 
 * @author Oliver Hall (oli@metabroadcast.com)
 *
 */
public class NoOpYouViewClient implements YouViewClient {
    
    private final Clock clock;

    public NoOpYouViewClient(Clock clock) {
        this.clock = checkNotNull(clock);
    }

    @Override
    public YouViewResult delete(String elementId) {
        return YouViewResult.success("txnId", clock.now(), SC_ACCEPTED);
    }

    @Override
    public YouViewResult upload(Payload payload) {
        return YouViewResult.success("txnId", clock.now(), SC_ACCEPTED);
    }

    // This requires a fake status report to be returned to parse success properly
    // but this suffice for a basic implementation 
    @Override
    public YouViewResult checkRemoteStatus(String transactionId) {
        return YouViewResult.success("", clock.now(), SC_ACCEPTED);
    }

}
