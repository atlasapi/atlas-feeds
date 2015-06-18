package org.atlasapi.feeds.youview.nitro;

import java.util.regex.Pattern;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Episode;


public class NitroEpisodeNumberPrefixAddingContentTitleGenerator implements ContentTitleGenerator {
    
    private static Pattern DATE_PATTERN = Pattern.compile("^\\d{2}\\/\\d{2}\\/\\d{4}$");
    private static Pattern EPISODE_NUMBER_PATTERN = Pattern.compile("^(Episode|Pennod|Part|Week) [\\d+]$");
    
    public String titleFor(Content content) {
        if (!(content instanceof Episode) 
                || content.getTitle() == null) {
            return content.getTitle();
        }
        
        Episode episode = (Episode) content;
        String episodeTitle = episode.getTitle();
        
        if (episodeTitle == null 
                || episode.getEpisodeNumber() == null
                || DATE_PATTERN.matcher(episodeTitle).matches()
                || EPISODE_NUMBER_PATTERN.matcher(episodeTitle).matches()) {
            return episodeTitle;
        } else {
            return String.format("%d. %s", episode.getEpisodeNumber(), episodeTitle);
        }
    }
    
}
