package org.atlasapi.feeds.tvanytime;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.xml.datatype.XMLGregorianCalendar;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Duration;
import org.junit.Test;

import tva.metadata._2010.FlagType;

import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.TimeMachine;


public class TVAnytimeElementFactoryTest {
    
    private DateTime time = new DateTime(2012, 1, 1, 0, 0, 0, 0).withZone(DateTimeZone.UTC);
    private Clock clock = new TimeMachine(time);
    private final TvAnytimeElementFactory elementFactory = TvAnytimeElementFactory.INSTANCE;
    
    @Test
    public void testGenerationOfTrueFlagType() {
        FlagType trueFlag = elementFactory.flag(true);

        assertTrue("flag should have value of true", trueFlag.isValue());
    }

    @Test
    public void testGenerationOfFalseFlagType() {
        FlagType falseFlag = elementFactory.flag(false);

        assertFalse("flag should have value of false", falseFlag.isValue());
    }

    @Test
    public void testGenerationOfGregorianCalendarFromDateTime() {
        XMLGregorianCalendar calendar = elementFactory.gregorianCalendar(clock.now());
        assertEquals("2012-01-01T00:00:00Z", calendar.toString());
    }

    @Test
    public void testGenerationJavaxDurationFromJodaDuration() {
        Duration duration = Duration.millis(1000l);
        javax.xml.datatype.Duration generated = elementFactory.durationFrom(duration);
        assertEquals("P0DT0H0M1.000S", generated.toString());
    }

}
