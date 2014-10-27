package org.atlasapi.feeds.youview;

import static org.junit.Assert.*;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;
import org.junit.Before;
import org.junit.Test;


public class UriBasedContentPermitTest {
    
    private final ContentPermit permit = new UriBasedContentPermit();
    
    @Before
    public void setup() {
        permit.reset();
    }

    @Test
    public void testThatOnlyTheFirstInstanceOfAPieceOfContentIsPermitted() {
        
        Content content = createContentWithUri("contentUri");
        
        assertTrue("The first occurence of a piece of content should be permitted", permit.isPermitted(content));
        assertFalse("Repeated occurrences of a piece of content should not be permitted", permit.isPermitted(content));
    }

    @Test
    public void testThatContentWithDifferingUrisDoNotPreventEachOtherBeingPermitted() {
        
        Content content = createContentWithUri("contentUri");
        Content different = createContentWithUri("differentUri");
        
        assertTrue("The first occurence of a piece of content should be permitted", permit.isPermitted(content));
        assertTrue("One piece of content should not prevent another from being permitted", permit.isPermitted(different));
    }

    @Test
    public void testThatResetPermitsAPreviouslyUnpermittedPieceOfContent() {
        Content content = createContentWithUri("contentUri");
        
        permit.isPermitted(content);
        assertFalse("Repeated occurrences of a piece of content should not be permitted", permit.isPermitted(content));
        
        permit.reset();
        
        assertTrue("A reset should clear any previous occurrences of a piece of content", permit.isPermitted(content));
    }
    
    private Content createContentWithUri(String contentUri) {
        return new Item(contentUri, "curie", Publisher.METABROADCAST);
    }
}
