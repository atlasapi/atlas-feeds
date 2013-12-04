package org.atlasapi.feeds.radioplayer.outputting;

import java.util.List;
import java.util.Set;

public interface RadioPlayerGenreMap {

    public Set<List<String>> map(Set<String> sourceGenres);

}