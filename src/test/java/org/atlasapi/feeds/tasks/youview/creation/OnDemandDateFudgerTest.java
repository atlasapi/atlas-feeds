package org.atlasapi.feeds.tasks.youview.creation;

import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Version;

import com.metabroadcast.common.time.Clock;

import com.google.common.collect.ImmutableList;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OnDemandDateFudgerTest {

    @Mock private Clock clock;

    @Mock private Item item;
    @Mock private Version version;
    @Mock private Encoding encoding;
    @Mock private Location location;
    @Mock private Policy policy;

    private ItemOnDemandHierarchy hierarchy;
    private OnDemandDateFudger fudger;
    private DateTime now;

    @Before
    public void setUp() throws Exception {
        now = DateTime.now();

        when(location.getPolicy()).thenReturn(policy);
        when(clock.now()).thenReturn(now);

        hierarchy = new ItemOnDemandHierarchy(
                item,
                version,
                encoding,
                ImmutableList.of(location));

        fudger = OnDemandDateFudger.create(clock);
    }

    @Test
    public void availabilityInFutureGetsLeftAlone() throws Exception {
        when(policy.getAvailabilityStart()).thenReturn(now.plusHours(1));
        when(policy.getAvailabilityEnd()).thenReturn(now.plusHours(11));

        fudger.fudgeStartDates(hierarchy);

        verify(policy, never()).copy();
        verify(policy, never()).setAvailabilityStart(any(DateTime.class));
    }

    @Test
    public void availabilityInProgressGetsStartDateFudged() throws Exception {
        when(policy.getAvailabilityStart()).thenReturn(now.minusHours(1));
        when(policy.getAvailabilityEnd()).thenReturn(now.plusHours(1));

        Location locationCopy = mock(Location.class);
        Policy policyCopy = mock(Policy.class);

        when(location.copy()).thenReturn(locationCopy);
        when(policy.copy()).thenReturn(policyCopy);

        when(location.copy()).thenReturn(locationCopy);

        ItemOnDemandHierarchy result = fudger.fudgeStartDates(hierarchy);

        assertTrue(result.item() == item);
        assertTrue(result.version() == version);
        assertTrue(result.encoding() == encoding);
        assertTrue(result.locations().get(0) == locationCopy);

        verify(locationCopy, times(1)).setPolicy(policyCopy);
        verify(policyCopy, times(1)).setAvailabilityStart(now.minusHours(6));
    }

    @Test
    public void availabilityInPastGetsLeftAlone() throws Exception {
        when(policy.getAvailabilityStart()).thenReturn(now.minusHours(21));
        when(policy.getAvailabilityEnd()).thenReturn(now.minusHours(1));

        fudger.fudgeStartDates(hierarchy);

        verify(policy, never()).copy();
        verify(policy, never()).setAvailabilityStart(any(DateTime.class));
    }

    @Test
    public void handlesNullEndDates() throws Exception {
        when(policy.getAvailabilityStart()).thenReturn(now.minusHours(1));
        when(policy.getAvailabilityEnd()).thenReturn(null);

        Location locationCopy = mock(Location.class);
        Policy policyCopy = mock(Policy.class);

        when(location.copy()).thenReturn(locationCopy);
        when(policy.copy()).thenReturn(policyCopy);

        when(location.copy()).thenReturn(locationCopy);

        ItemOnDemandHierarchy result = fudger.fudgeStartDates(hierarchy);

        assertTrue(result.item() == item);
        assertTrue(result.version() == version);
        assertTrue(result.encoding() == encoding);
        assertTrue(result.locations().get(0) == locationCopy);

        verify(locationCopy, times(1)).setPolicy(policyCopy);
        verify(policyCopy, times(1)).setAvailabilityStart(now.minusHours(6));
    }
}
