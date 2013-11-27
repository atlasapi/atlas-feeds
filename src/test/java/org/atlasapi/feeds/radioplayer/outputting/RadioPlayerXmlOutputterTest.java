package org.atlasapi.feeds.radioplayer.outputting;

import static org.junit.Assert.assertEquals;
import nu.xom.Element;

import org.atlasapi.media.entity.Item;
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

    @Test
    public void testImageOutputConvertsDynamiteImages() {
        Item item = new Item();
        item.setImage("http://ichef.bbci.co.uk/programmeimages/episode/b03c4rs8_640_360.jpg");
        
        Element imageElem = programInfoOutputter.createImageDescriptionElem(item);
        
        assertEquals("http://ichef.bbci.co.uk/programmeimages/episode/b03c4rs8_86_48.jpg", imageElem.getAttribute("url").getValue());
        assertEquals("image/jpeg", imageElem.getAttribute("mimeValue").getValue());
        assertEquals("86", imageElem.getAttribute("width").getValue());
        assertEquals("48", imageElem.getAttribute("height").getValue());
    }

    @Test
    public void testImageOutputConvertsNitroImages() {
        Item item = new Item();
        item.setImage("http://ichef.bbci.co.uk/images/ic/1024x576/legacy/episode/p01fxcbg.jpg");
        
        Element imageElem = programInfoOutputter.createImageDescriptionElem(item);
        
        assertEquals("http://ichef.bbci.co.uk/images/ic/86x48/legacy/episode/p01fxcbg.jpg", imageElem.getAttribute("url").getValue());
        assertEquals("image/jpeg", imageElem.getAttribute("mimeValue").getValue());
        assertEquals("86", imageElem.getAttribute("width").getValue());
        assertEquals("48", imageElem.getAttribute("height").getValue());
    }
}
