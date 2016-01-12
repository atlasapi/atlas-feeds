package org.atlasapi.feeds.youview.nitro;

import java.util.regex.Pattern;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Episode;


public class NitroEpisodeNumberPrefixAddingContentTitleGenerator implements ContentTitleGenerator {
    
    private static final int START_OF_STRING = 0;
    private static Pattern DATE_PATTERN = Pattern.compile("^\\d{2}\\/\\d{2}\\/\\d{4}");
    private static Pattern EPISODE_NUMBER_PATTERN = Pattern.compile("^(Episode|Episodes|Pennod|Pennodau|Part|Week) [\\d]+(.*)?");
    
    public String titleFor(Content content) {
        if (!(content instanceof Episode) 
                || content.getTitle() == null) {
            return content.getTitle();
        }
        
        Episode episode = (Episode) content;
        String episodeTitle = episode.getTitle();
        
        if (episodeTitle == null 
                || episode.getEpisodeNumber() == null
                || DATE_PATTERN.matcher(episodeTitle).find(START_OF_STRING)
                || EPISODE_NUMBER_PATTERN.matcher(episodeTitle).find(START_OF_STRING)) {
            return episodeTitle;
        } else {
            return String.format("%d. %s", episode.getEpisodeNumber(), episodeTitle);
        }
    }
    
}
