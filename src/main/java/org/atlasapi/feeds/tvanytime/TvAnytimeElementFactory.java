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
    // TODO can this be instantiated staticly?
    private DatatypeFactory datatypeFactory;

    private TvAnytimeElementFactory() {
        try {
            this.datatypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            Throwables.propagate(e);
        }
    }
    
    public FlagType flag(boolean value) {
        FlagType flag = new FlagType();
        flag.setValue(value);
        return flag;
    }
    
    public XMLGregorianCalendar gregorianCalendar(DateTime dateTime) {
        String format = SIMPLE_DATE_FORMAT.format(dateTime.toDate());
        return datatypeFactory.newXMLGregorianCalendar(format);
    }
    
    public javax.xml.datatype.Duration durationFrom(Duration duration) {
        return datatypeFactory.newDurationDayTime(duration.getMillis());
    }
}
