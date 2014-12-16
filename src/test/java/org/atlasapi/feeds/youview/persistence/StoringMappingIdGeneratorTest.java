package org.atlasapi.feeds.youview.persistence;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
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

import com.google.common.base.Optional;

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
    public void testShouldWriteWhenNoMappingExists() {
        when(store.getValueFor(anyString())).thenReturn(Optional.<String>absent());

        String versionCrid = idGenerator.generateVersionCrid(new Item(), version());

        assertEquals(VERSION_CRID, versionCrid);
        verify(store, times(1)).storeMapping(VERSION_URI, VERSION_CRID);
    }

    @Test
    public void testShouldNotWriteWhenMappingAlreadyExists() {
        when(store.getValueFor(anyString())).thenReturn(Optional.of(VERSION_CRID));

        String versionCrid = idGenerator.generateVersionCrid(new Item(), version());

        assertEquals(VERSION_CRID, versionCrid);
        verify(store, never()).storeMapping(anyString(), anyString());
    }

    @Test
    public void testShouldWriteWhenMappingWithDifferentValueExists() {
        String oldVersionCrid = "oldVersionCrid";
        when(store.getValueFor(anyString())).thenReturn(Optional.of(oldVersionCrid));

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