package org.atlasapi.feeds.youview;

import org.atlasapi.media.entity.Content;


// TODO extend Predicate<Content>?
public interface ContentPermit {

    /**
     * For a given piece of {@link Content}, returns true if
     * the content is allowed, and false otherwise.
     * @param content
     * @return boolean showing whether content is allowed
     */
    boolean isPermitted(Content content);
    /**
     * reset the permit, so that any previous state is cleared
     */
    void reset();
}
