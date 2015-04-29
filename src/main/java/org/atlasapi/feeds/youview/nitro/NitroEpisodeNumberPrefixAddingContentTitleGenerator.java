package org.atlasapi.feeds.youview.nitro;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Set;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Described;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Identified;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.persistence.content.ContentResolver;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;


public class NitroEpisodeNumberPrefixAddingContentTitleGenerator implements ContentTitleGenerator {

    private static final Function<Identified, String> TO_TITLE = new Function<Identified, String>() {

        @Override
        public String apply(Identified input) {
            if (!(input instanceof Described)) {
                return null;
            }
            return ((Described)input).getTitle();
        }
        
    };

    private static final Predicate<CharSequence> ENDS_WITH_NUMBER = Predicates.containsPattern(" [\\d+]$");
    
    private final ContentResolver contentResolver;

    public NitroEpisodeNumberPrefixAddingContentTitleGenerator(ContentResolver contentResolver) {
        this.contentResolver = checkNotNull(contentResolver);
    }
    
    public String titleFor(Content content) {
        if (!(content instanceof Episode) 
                || content.getTitle() == null) {
            return content.getTitle();
        }
        
        Episode episode = (Episode) content;
        boolean anyParentTitleEndsWithNumber = Iterables.any(containerTitlesFor(episode), ENDS_WITH_NUMBER);
        
        if (!anyParentTitleEndsWithNumber
                && episode.getEpisodeNumber() != null) {
            return String.format("%d. %s", episode.getEpisodeNumber(), episode.getTitle());
        } else {
            return episode.getTitle();
        }
    }
    
    private Iterable<String> containerTitlesFor(Episode episode) {
        Set<ParentRef> parents = Sets.newHashSet();
        if (episode.getSeriesRef() != null) {
            parents.add(episode.getSeriesRef());
        }
        if (episode.getContainer() != null) {
            parents.add(episode.getContainer());
        }
        return Iterables.filter(
                Iterables.transform(
                    contentResolver.findByCanonicalUris(Iterables.transform(parents, ParentRef.TO_URI)).getAllResolvedResults(), 
                    TO_TITLE),
                Predicates.notNull());
    }
}
