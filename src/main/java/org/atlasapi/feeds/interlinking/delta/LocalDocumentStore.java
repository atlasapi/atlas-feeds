package org.atlasapi.feeds.interlinking.delta;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Serializer;

import com.google.common.base.Charsets;
import com.metabroadcast.common.base.Maybe;

public class LocalDocumentStore implements InterlinkingDocumentStore {

    private final File container;

    public LocalDocumentStore(File container) {
        this.container = checkNotNull(container);
    }

    @Override
    public void storeDocument(String title, Document file) {
        try {
            File child = new File(container, title);
            child.createNewFile();
            String charset = Charsets.UTF_8.name();
            Serializer serializer = new Serializer(new FileOutputStream(child), charset);
            serializer.setIndent(2);
            serializer.write(file);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Maybe<Document> getDocument(String title) {
        try {
            return Maybe.just(new Builder().build(new FileInputStream(new File(container,title))));
        } catch (Exception e) {
            return Maybe.nothing();
        }
    }

}
