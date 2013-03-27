package org.atlasapi.feeds.youview.output;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.XMLGregorianCalendar;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class AvailabilityAdapter extends XmlAdapter<String, XMLGregorianCalendar>{

    private static final DateTimeFormatter DATETIME_FORMAT = ISODateTimeFormat.dateTimeNoMillis();
    private final DatatypeFactory dataTypeFactory;
    
    public AvailabilityAdapter() throws DatatypeConfigurationException {
        this.dataTypeFactory = DatatypeFactory.newInstance();
    }
    
    @Override
    public String marshal(XMLGregorianCalendar xmlCal) throws Exception {
        DateTime date = new DateTime(xmlCal.toGregorianCalendar().getTimeInMillis()).withZone(DateTimeZone.UTC);
        return DATETIME_FORMAT.print(date);
    }

    @Override
    public XMLGregorianCalendar unmarshal(String string) throws Exception {
        return dataTypeFactory.newXMLGregorianCalendar(string);
    }

}
