package org.atlasapi.feeds.youview.payload;

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
    
    public TVAnytimeStringConverter() throws JAXBException {
        this.context = JAXBContext.newInstance("tva.metadata._2010");
    }

    @Override
    public String convert(JAXBElement<TVAMainType> input) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Marshaller marshaller = context.createMarshaller();
            marshaller.marshal(input, baos);
            /* TODO FIXME This output hack was put it to support weirdness from YouView */
            return baos.toString(Charsets.UTF_8.name()).replace(" xsi:nil=\"true\"", "");
        } catch (JAXBException | UnsupportedEncodingException e) {
            throw new TVAnytimeConverterException(e);
        }
    }
}
