package org.atlasapi.feeds.youview.output;

import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import javax.xml.datatype.Duration;

public class DurationAdapter extends XmlAdapter<String, Duration> {

    private final DatatypeFactory dataTypeFactory;

    public DurationAdapter() throws DatatypeConfigurationException {
        this.dataTypeFactory = DatatypeFactory.newInstance();
    }
    
    @Override
    public String marshal(Duration duration) throws Exception {
        return String.format("P%dY%dM%dDT%dH%dM%dS", 
                duration.getYears(), 
                duration.getMonths(), 
                duration.getDays(),
                duration.getHours(),
                duration.getMinutes(),
                duration.getSeconds());
    }

    @Override
    public Duration unmarshal(String string) throws Exception {
        return dataTypeFactory.newDuration(string);
    }

}
