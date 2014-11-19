package org.atlasapi.feeds.youview.nitro;

import static org.junit.Assert.assertEquals;

import java.util.Set;

import org.junit.Test;

import com.google.common.collect.ImmutableSet;


public class NitroServiceMappingTest {
    
    private final NitroServiceMapping mapping = new NitroServiceMapping("nitro_service_mapping.csv");

    // relies on file data, but does confirm that file is being read and processed correctly
    @Test
    public void testFetchingMappingForBbcOneSouthWest() {
        Iterable<String> youViewSIds = mapping.youviewServiceIdFor("bbc_one_south_west");
        
        Set<String> expected = ImmutableSet.of(
                "bbc_one_south_west_233a_1042",
                "bbc_one_south_west_2_2872"
        );
        assertEquals(expected, ImmutableSet.copyOf(youViewSIds));
    }

}
