package org.atlasapi.feeds.interlinking.delta;

import static org.atlasapi.feeds.interlinking.delta.InterlinkingDelta.deltaFor;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Iterator;

import nu.xom.Document;

import org.atlasapi.feeds.interlinking.InterlinkFeed;
import org.atlasapi.feeds.interlinking.PlaylistToInterlinkFeed;
import org.atlasapi.feeds.interlinking.outputting.InterlinkFeedOutputter;
import org.atlasapi.feeds.interlinking.www.InterlinkController;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.content.mongo.LastUpdatedContentFinder;
import org.joda.time.DateTime;

public class InterlinkingDeltaUpdater {

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    private final LastUpdatedContentFinder contentFinder;
    private final InterlinkFeedOutputter outputter;
    private final PlaylistToInterlinkFeed adapter;

    public InterlinkingDeltaUpdater(LastUpdatedContentFinder contentFinder, InterlinkFeedOutputter outputter, PlaylistToInterlinkFeed adapter) {
        this.contentFinder = contentFinder;
        this.outputter = outputter;
        this.adapter = adapter;
    }

    public InterlinkingDelta updateFeed(InterlinkingDelta delta, DateTime until) {
        return updateFeed(delta, delta.lastUpdated(), until);
    }
    
    public InterlinkingDelta updateFeed(InterlinkingDelta delta, DateTime from, DateTime to) {
        Iterator<Content> newContent = contentFinder.updatedSince(Publisher.C4_PMLSD, from);
        InterlinkFeed interlinkFeed = adapter.fromContent(InterlinkController.FEED_ID + getDateString(from), Publisher.C4, from, to, newContent);

        Document document;
        if (delta.exists()) {
            document = delta.document();
        } else {
            document = new Document(outputter.createFeed(interlinkFeed, from));
        }
        
        DateTime lastUpdated = outputter.updateLastUpdated(interlinkFeed.entries(), delta.exists() ? delta.lastUpdated() : from, document);
        outputter.outputFeedToElements(interlinkFeed.entries(), false, document.getRootElement());

        return deltaFor(document, lastUpdated);
    }
    
    private String getDateString(DateTime date) {
        return dateFormat.format(date.toDate());
    }
}
