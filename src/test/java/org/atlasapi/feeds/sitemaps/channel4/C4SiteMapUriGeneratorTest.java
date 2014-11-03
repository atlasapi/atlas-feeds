package org.atlasapi.feeds.sitemaps.channel4;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.atlasapi.feeds.sitemaps.channel4.C4SiteMapUriGenerator;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Clip;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.junit.Test;

import com.google.common.base.Supplier;


public class C4SiteMapUriGeneratorTest {

    private static final String BRIGHTCOVE_PUBLISHER_ID = "12345";
    private static final String BRIGHTCOVE_PLAYER_ID = "33333";
    private static final String FLASH_PLAYER_VERSION = "1.23";
    
    private final C4SiteMapUriGenerator c4SitemapOutputter 
        = new C4SiteMapUriGenerator(BRIGHTCOVE_PUBLISHER_ID, BRIGHTCOVE_PLAYER_ID, new Supplier<String>() {
            
            @Override
            public String get() {
                return FLASH_PLAYER_VERSION;
            }
        });
    
    @Test
    public void testVideoUriForClip() {
        final String embedId = "98765";
        
        Clip clip = new Clip();
        Location location = new Location();
        location.setEmbedId(embedId);
        
        assertThat(c4SitemapOutputter.videoUriForClip(clip, location).get(), 
                is("http://c.brightcove.com/services/viewer/federated_f9/?isVid=1&domain=embed&playerID=" 
                             + BRIGHTCOVE_PLAYER_ID + "&publisherID=" + BRIGHTCOVE_PUBLISHER_ID + "&videoID=" + embedId));
    }
    
    @Test
    public void testPlayerPageUriForClip() {
        Brand brand = new Brand();
        brand.setCanonicalUri("http://pmlsc.channel4.com/pmlsd/a-brand");
        Item item = new Item();
        item.setContainer(brand);
        Clip clip = new Clip();
        Location location = new Location();
        clip.setTitle("This is a title; it's long");
        
        assertThat(c4SitemapOutputter.playerPageUriForClip(item, clip, location).get(), 
                is("http://www.channel4.com/programmes/a-brand/videos/all/this-is-a-title-its-long"));
    }
    
    @Test
    public void testPlayerPageUriForClipWithDashes() {
        Brand brand = new Brand();
        brand.setCanonicalUri("http://pmlsc.channel4.com/pmlsd/a-brand");
        Item item = new Item();
        item.setContainer(brand);
        Clip clip = new Clip();
        Location location = new Location();
        clip.setTitle("S1-Ep3: Characters");
        
        assertThat(c4SitemapOutputter.playerPageUriForClip(item, clip, location).get(), 
                is("http://www.channel4.com/programmes/a-brand/videos/all/s1-ep3-characters"));
    }
    
    @Test
    public void testPlayerPageUriForContent() {
        Item item = new Item();
        
        final String locationUri = "http://www.example.org/on-demand-location";
        Location location = new Location();
        location.setUri(locationUri);
        assertThat(c4SitemapOutputter.playerPageUriForContent(item, location).get(), 
                is(locationUri));
    }
    
    @Test
    public void testVideoUriForContent() {
        Item item = new Item();
        
        Location location = new Location();
        location.setUri("http://www.example.org/on-demand-location/4od#11111");
        assertThat(c4SitemapOutputter.videoUriForContent(item, location).get(),
                is("http://www.channel4.com/static/programmes-bips-flash/" 
                        + FLASH_PLAYER_VERSION 
                        + "/4odplayer_bips.swf?preSelectAsset=11111"));
    }
    
}
