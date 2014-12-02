package org.atlasapi.feeds.tvanytime;

import java.text.SimpleDateFormat;

import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.joda.time.DateTime;
import org.joda.time.Duration;

import tva.metadata._2010.FlagType;

import com.google.common.base.Throwables;


public enum TvAnytimeElementFactory {
    
    INSTANCE;
    
    private static final SimpleDateFormat SIMPLE_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private static final DatatypeFactory datatypeFactory;
    
    static {
        try {
            datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw Throwables.propagate(e);
        }
    }

    public static FlagType flag(boolean value) {
        FlagType flag = new FlagType();
        flag.setValue(value);
        return flag;
    }
    
    public static XMLGregorianCalendar gregorianCalendar(DateTime dateTime) {
        String format = SIMPLE_DATE_FORMAT.format(dateTime.toDate());
        return datatypeFactory.newXMLGregorianCalendar(format);
    }
    
    public static javax.xml.datatype.Duration durationFrom(Duration duration) {
        return datatypeFactory.newDurationDayTime(duration.getMillis());
    }
}
