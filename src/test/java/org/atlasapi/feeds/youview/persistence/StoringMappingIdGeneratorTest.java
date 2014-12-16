package org.atlasapi.feeds.youview.persistence;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class StoringMappingIdGeneratorTest {

    private static final String VERSION_CRID = "versionCrid";
    private static final String VERSION_URI = "versionUri";

    @Mock
    private IdGenerator delegate;
    @Mock
    private MongoIdMappingStore store;

    private StoringMappingIdGenerator idGenerator;

    @Before
    public void setup() {
        reset(delegate, store);

        when(delegate.generateVersionCrid(any(Item.class), any(Version.class)))
                .thenReturn(VERSION_CRID);

        idGenerator = new StoringMappingIdGenerator(store, delegate);
    }

    @Test
    public void testShouldWriteMappingWhenVersionCridIsGenerated() {
        String versionCrid = idGenerator.generateVersionCrid(new Item(), version());

        assertEquals(VERSION_CRID, versionCrid);
        verify(store, times(1)).storeMapping(VERSION_URI, VERSION_CRID);
    }

    private Version version() {
        Version version = new Version();
        version.setCanonicalUri(VERSION_URI);

        return version;
    }



}