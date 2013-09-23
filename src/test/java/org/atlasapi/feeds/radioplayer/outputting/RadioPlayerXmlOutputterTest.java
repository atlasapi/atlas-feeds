package org.atlasapi.feeds.radioplayer.outputting;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class RadioPlayerXmlOutputterTest {

    private final RadioPlayerXMLOutputter programInfoOutputter = new RadioPlayerProgrammeInformationOutputter(); 
    
    @Test
    public void testConversionOfNitroUriToCrid() {
        String uri = "http://nitro.bbc.co.uk/programmes/p01fxcbj";
        
        String crid = programInfoOutputter.createCridFromUri(uri);
        assertEquals("crid://www.bbc.co.uk/programmes/p01fxcbj", crid);
    }

    @Test
    public void testConversionOfBbcUriToCrid() {
        String uri = "http://www.bbc.co.uk/programmes/p01fxcbj";
        
        String crid = programInfoOutputter.createCridFromUri(uri);
        assertEquals("crid://www.bbc.co.uk/programmes/p01fxcbj", crid);
    }
}
