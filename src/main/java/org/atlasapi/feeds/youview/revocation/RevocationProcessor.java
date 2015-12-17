package org.atlasapi.feeds.youview.revocation;

import org.atlasapi.feeds.tasks.Task;
import org.atlasapi.media.entity.Content;

import com.google.common.collect.ImmutableList;

public interface RevocationProcessor {

    ImmutableList<Task> revoke(Content content);
    ImmutableList<Task> unrevoke(Content content);
}
