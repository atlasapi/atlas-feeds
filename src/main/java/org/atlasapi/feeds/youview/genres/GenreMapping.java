package org.atlasapi.feeds.youview.genres;

import java.util.Set;

import org.atlasapi.media.entity.Content;


public interface GenreMapping {

    Set<String> youViewGenresFor(Content content);
}
