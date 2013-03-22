package org.atlasapi.feeds.youview;

import java.util.List;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;
import org.junit.Test;

import com.google.common.collect.ImmutableList;

public class TestReverseOrdering {

    @Test
    public void testContentOrdering() {
        Iterable<Content> items = ImmutableList.of(new Brand(), new Series(), new Item());
        List<Content> sortedCopy = YouViewUploader.REVERSE_HIERARCHICAL_ORDER.sortedCopy(items);
        
        assert(sortedCopy.get(0) instanceof Item);
        assert(sortedCopy.get(1) instanceof Series);
        assert(sortedCopy.get(2) instanceof Brand);
    }
}