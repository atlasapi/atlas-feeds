package org.atlasapi.feeds.youview.payload;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import tva.metadata._2010.TVAMainType;

import com.google.common.base.Charsets;


public class TVAnytimeStringConverter implements Converter<JAXBElement<TVAMainType>, String> {
    
    private final JAXBContext context;
    
    public TVAnytimeStringConverter(JAXBContext context) {
        this.context = checkNotNull(context);
    }

    @Override
    public String convert(JAXBElement<TVAMainType> input) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(input, baos);
            return baos.toString(Charsets.UTF_8.name());
        } catch (JAXBException | UnsupportedEncodingException e) {
            throw new TVAnytimeConverterException(e);
        }
    }
}
