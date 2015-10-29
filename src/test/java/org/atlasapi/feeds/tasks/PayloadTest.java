package org.atlasapi.feeds.tasks;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.joda.time.DateTime;
import org.junit.Test;

public class PayloadTest {

    @Test
    public void testHashShouldBeDifferent() throws Exception {
        Payload payloadA = new Payload("payloadA", DateTime.now().minusHours(1));
        Payload payloadB = new Payload("payloadB", DateTime.now());

        assertThat(payloadA.hash(), not(is(payloadB.hash())));
        assertThat(payloadA.hasChanged(payloadB.hash()), is(true));
    }

    @Test
    public void testHashShouldBeTheSame() throws Exception {
        Payload payloadA = new Payload("payload", DateTime.now().minusHours(1));
        Payload payloadB = new Payload("payload", DateTime.now());

        assertThat(payloadA.hash(), is(payloadB.hash()));
        assertThat(payloadA.hasChanged(payloadB.hash()), is(false));
    }
}