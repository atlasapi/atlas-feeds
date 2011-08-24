package org.atlasapi.feeds.interlinking.delta;

import nu.xom.Document;

import com.metabroadcast.common.base.Maybe;

public interface InterlinkingDocumentStore {

    void storeDocument(String title, Document file);

    Maybe<Document> getDocument(String title);

}