package org.atlasapi.feeds.youview.resolution;

import java.util.Iterator;

import org.atlasapi.media.entity.Content;
import org.atlasapi.persistence.content.ResolvedContent;

import org.joda.time.DateTime;


public interface YouViewContentResolver {

    Iterator<Content> updatedSince(DateTime dateTime);

    ResolvedContent findByUris(Iterable<String> uris);
}
