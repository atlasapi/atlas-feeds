package org.atlasapi.feeds.youview.tasks.creation;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;

import com.google.common.collect.Ordering;

public final class HierarchicalOrdering extends Ordering<Content> {

    @Override
    public int compare(Content left, Content right) {
        if (left instanceof Item) {
            if (right instanceof Item) {
                return 0;
            } else {
                return 1;
            }
        } else if (left instanceof Series) {
            if (right instanceof Item) {
                return -1;
            } else if (right instanceof Series) {
                return 0;
            } else {
                return 1;
            }
        } else {
            if (right instanceof Brand) {
                return 0;
            } else {
                return -1;
            }
        }
    }
}