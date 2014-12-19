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
import org.joda.time.Duration;
import org.joda.time.Interval;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;

public class SevenDayWindowAvailableContentResolver implements YouViewContentResolver {

    private static final Duration WINDOW_DURATION = Duration.standardDays(7);
    
    private final YouViewContentResolver delegate;
    
    public SevenDayWindowAvailableContentResolver(YouViewContentResolver delegate) {
        this.delegate = checkNotNull(delegate);
    }

    @Override
    public Iterator<Content> updatedSince(DateTime dateTime) {
        return fetchAvailableUpdatedContent(delegate.updatedSince(dateTime));
    }

    @Override
    public Iterator<Content> allContent() {
        return fetchAvailableUpdatedContent(delegate.allContent());
    }
    
    private Iterator<Content> fetchAvailableUpdatedContent(Iterator<Content> content) {
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
    
    private static Predicate<Version> isVersionAvailable() {
        return new Predicate<Version>() {
            @Override
            public boolean apply(Version input) {
                return Iterables.any(input.getManifestedAs(), isEncodingAvailable());
            }
        };
    }

    private static Predicate<Encoding> isEncodingAvailable() {
        return new Predicate<Encoding>() {
            @Override
            public boolean apply(Encoding input) {
                return Iterables.any(input.getAvailableAt(), isLocationAvailable());
            }
        };
    }

    private static Predicate<Location> isLocationAvailable() {
        return new Predicate<Location>() {
            @Override
            public boolean apply(Location input) {
                Policy policy = input.getPolicy();
                if (policy == null || policy.getAvailabilityStart() == null) { 
                    return false;
                }
                if (!Platform.YOUVIEW_IPLAYER.equals(policy.getPlatform())) {
                    return false;
                }
                
                Interval sevenDayWindow = new Interval(policy.getAvailabilityStart(), policy.getAvailabilityStart().plus(WINDOW_DURATION));
                return sevenDayWindow.containsNow();
            }
        };
    }
}
