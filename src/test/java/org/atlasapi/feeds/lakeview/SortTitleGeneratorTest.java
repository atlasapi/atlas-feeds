package org.atlasapi.feeds.lakeview;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;


public class SortTitleGeneratorTest {

    private final Set<String> prefixes = ImmutableSet.of("A", "An", "The");
    
    private final SortTitleGenerator sortTitleGenerator = new SortTitleGenerator(prefixes);
    
    @Test
    public void testNullSafe() {
        assertThat(sortTitleGenerator.createSortTitle(null), is((String)null));
    }
    
    @Test
    public void testDoesNotChangeTitleWithoutPrefix() {
        String titleThatShouldntChange = "American History X";
        assertThat(sortTitleGenerator.createSortTitle(titleThatShouldntChange), is(titleThatShouldntChange));
    }
    
    @Test
    public void testMovesPrefixToSuffix() {
        assertThat(sortTitleGenerator.createSortTitle("A Tale Of Two Cities"), is("Tale Of Two Cities, A"));
    }
}
