package org.atlasapi.feeds.interlinking.delta;

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

import com.metabroadcast.common.base.Maybe;

public class InterlinkingDeltaUpdater {

    public static final String BUCKET_NAME = "bbc-interlinking";
    public static final String FOLDER_NAME = "feeds/bbc-interlinking";

    private static final DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

    private final LastUpdatedContentFinder contentFinder;
    private final InterlinkFeedOutputter outputter;
    private final PlaylistToInterlinkFeed adapter;

    public InterlinkingDeltaUpdater(LastUpdatedContentFinder contentFinder, InterlinkFeedOutputter outputter, PlaylistToInterlinkFeed adapter) {
        this.contentFinder = contentFinder;
        this.outputter = outputter;
        this.adapter = adapter;
    }

    public Document updateFeed(Maybe<Document> existingFeedElement, DateTime until) {
        return updateFeed(existingFeedElement, getLastUpdated(existingFeedElement.requireValue()), until);
    }
    
    public Document updateFeed(Maybe<Document> existingFeedElement, DateTime from, DateTime to) {
        Iterator<Content> newContent = contentFinder.updatedSince(Publisher.C4, from);
        InterlinkFeed interlinkFeed = adapter.fromContent(InterlinkController.FEED_ID + getDateString(from), Publisher.C4, from, to, newContent);

        Document document;
        if (existingFeedElement.hasValue()) {
            document = existingFeedElement.requireValue();
        } else {
            document = new Document(outputter.createFeed(interlinkFeed, from));
        }
        
        outputter.updateLastUpdated(interlinkFeed.entries(), getLastUpdated(document), document);
        outputter.outputFeedToElements(interlinkFeed.entries(), false, document.getRootElement());

        return document;
    }

    private DateTime getLastUpdated(Document document) {
        return new DateTime(InterlinkFeedOutputter.DATE_TIME_FORMAT.parseDateTime(document.getRootElement().getFirstChildElement("updated", InterlinkFeedOutputter.NS_ATOM.getUri()).getValue()));
    }
    
    private String getDateString(DateTime date) {
        return dateFormat.format(date.toDate());
    }
}
