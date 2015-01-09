package org.atlasapi.feeds.youview.resolution;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Iterator;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Policy.Platform;
import org.atlasapi.media.entity.Version;
import org.joda.time.DateTime;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;


public class PlatformFilteringContentResolver implements YouViewContentResolver {
    
    private final YouViewContentResolver delegate;
    
    public PlatformFilteringContentResolver(YouViewContentResolver delegate) {
        this.delegate = checkNotNull(delegate);
    }

    @Override
    public Iterator<Content> updatedSince(DateTime dateTime) {
        return filterAvailableUpdatedContent(delegate.updatedSince(dateTime));
    }

    @Override
    public Iterator<Content> allContent() {
        return filterAvailableUpdatedContent(delegate.allContent());
    }
    
    private Iterator<Content> filterAvailableUpdatedContent(Iterator<Content> content) {
        return Iterators.filter(
                content, 
                new Predicate<Content>(){
                    @Override
                    public boolean apply(Content input) {
                        if (input instanceof Item) {
                            return Iterables.any(((Item)input).getVersions(), isVersionAvailable());
                        }
                        // non-items have no availability window, so are passed through
                        return true;
                    }
                }
        );
    }
    
    private Predicate<Version> isVersionAvailable() {
        return new Predicate<Version>() {
            @Override
            public boolean apply(Version input) {
                return Iterables.any(input.getManifestedAs(), isEncodingAvailable());
            }
        };
    }

    private Predicate<Encoding> isEncodingAvailable() {
        return new Predicate<Encoding>() {
            @Override
            public boolean apply(Encoding input) {
                return Iterables.any(input.getAvailableAt(), isYouViewPlatform());
            }
        };
    }

    private static Predicate<Location> isYouViewPlatform() {
        return new Predicate<Location>() {
            @Override
            public boolean apply(Location input) {
                Policy policy = input.getPolicy();
                return policy != null && Platform.YOUVIEW_IPLAYER.equals(policy.getPlatform());
            }
        };
    }
}
