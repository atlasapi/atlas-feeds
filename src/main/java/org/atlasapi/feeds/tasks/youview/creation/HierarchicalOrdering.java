package org.atlasapi.feeds.tasks.youview.creation;

import com.google.common.collect.Ordering;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;

import javax.annotation.Nullable;
import java.util.Comparator;

public final class HierarchicalOrdering extends Ordering<Content> {

    private static final Ordering<Content> URI_COMPARATOR = Ordering.from(
            Comparator.comparing(Content::getCanonicalUri)
    ).nullsLast();

    private static final HierarchicalOrdering INSTANCE = new HierarchicalOrdering();

    public static HierarchicalOrdering create() {
        return INSTANCE;
    }

    private HierarchicalOrdering() {}

    @Override
    public int compare(@Nullable Content left, @Nullable Content right) {
        if (left instanceof Item) {
            if (right instanceof Item) {
                return URI_COMPARATOR.compare(left, right);
            } else {
                return 1;
            }
        } else if (left instanceof Series) {
            if (right instanceof Item) {
                return -1;
            } else if (right instanceof Series) {
                return URI_COMPARATOR.compare(left, right);
            } else {
                return 1;
            }
        } else {
            if (right instanceof Brand) {
                return URI_COMPARATOR.compare(left, right);
            } else {
                return -1;
            }
        }
    }
}