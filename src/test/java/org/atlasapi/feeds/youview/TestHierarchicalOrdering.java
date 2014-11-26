package org.atlasapi.feeds.youview;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.atlasapi.feeds.youview.upload.UploadTask;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;
import org.junit.Test;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;


public class TestHierarchicalOrdering {

    @Test
    public void testHierarchicalContentOrdering() {
        Item item = new Item();
        Series series = new Series();
        Brand brand = new Brand();
        
        for (List<Content> permutation : Collections2.permutations(ImmutableList.of(item, series, brand))) {
            List<Content> sorted = UploadTask.orderContentForDeletion(permutation);
            assertEquals(brand, Iterables.get(sorted, 0));
            assertEquals(series, Iterables.get(sorted, 1));
            assertEquals(item, Iterables.get(sorted, 2));
        }
    }
}
