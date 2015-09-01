package org.atlasapi.feeds.youview.payload;


public interface Converter<I, O> {

    O convert(I input);
}
