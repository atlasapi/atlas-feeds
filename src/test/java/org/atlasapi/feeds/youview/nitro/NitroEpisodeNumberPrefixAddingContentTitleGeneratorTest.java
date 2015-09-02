package org.atlasapi.feeds.youview.nitro;

import static org.junit.Assert.assertEquals;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.testing.BrandTestDataBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NitroEpisodeNumberPrefixAddingContentTitleGeneratorTest {

    private static final String BRAND_URI = "http://example.org/brands/1";
    private static final String EPISODE_TITLE = "Episode";

    private final NitroEpisodeNumberPrefixAddingContentTitleGenerator generator = new NitroEpisodeNumberPrefixAddingContentTitleGenerator();
    
    @Test
    public void testEpisodeWithEpisodeNumberIsNotMutated() {
        Brand brand = createBrand("Brand 1");
        Episode episode = createEpisode(brand, null);
        String episodeTitle = "Episode 1";
        episode.setTitle(episodeTitle);
        
        assertEquals(episodeTitle, generator.titleFor(episode));
    }
    
    @Test
    public void testEpisodeWithoutEpisodeNumberIsMutated() {
        Brand brand = createBrand("Brand");
        Episode episode = createEpisode(brand, null);
        String episodeTitle = "Badgers";
        episode.setTitle(episodeTitle);
        
        assertEquals("1. Badgers", generator.titleFor(episode));
    }
    
    @Test
    public void testEpisodeWithDateIsNotMutated() {
        Brand brand = createBrand("Brand");
        Episode episode = createEpisode(brand, null);
        String episodeTitle = "01/02/2014";
        episode.setTitle(episodeTitle);
        
        assertEquals("01/02/2014", generator.titleFor(episode));
    }
    
    @Test
    public void testNullEpisodeNumbersAreNotMutated() {
        Brand brand = createBrand("Brand");
        Episode episode = createEpisode(brand, null);
        episode.setEpisodeNumber(null);
        String episodeTitle = "Foo";
        episode.setTitle(episodeTitle);
        
        assertEquals(episodeTitle, generator.titleFor(episode));
    }
    
    @Test
    public void testEpisodePrefixedWithPennodIsNotMutated() {
        Brand brand = createBrand("Brand");
        Episode episode = createEpisode(brand, null);
        episode.setEpisodeNumber(1);
        String episodeTitle = "Pennod 33";
        episode.setTitle(episodeTitle);
        
        assertEquals(episodeTitle, generator.titleFor(episode));
    }
    
    @Test
    public void testEpisodePrefixedWithEpisodesIsNotMutated() {
        Brand brand = createBrand("Brand");
        Episode episode = createEpisode(brand, null);
        episode.setEpisodeNumber(1);
        String episodeTitle = "Episodes 1 and 2";
        episode.setTitle(episodeTitle);
        
        assertEquals(episodeTitle, generator.titleFor(episode));
    }
    
    @Test
    public void testNonEpisodeTypesAreNotMutated() {
        Item item = new Item();
        item.setTitle(EPISODE_TITLE);
        assertEquals(EPISODE_TITLE, generator.titleFor(item));
    }
    
    
    
    private Episode createEpisode(Brand brand, Series series) {
        Episode episode = new Episode();
        if (brand != null) {
            episode.setContainer(brand);
        }
        if (series != null) {
            episode.setSeries(series);
        }
        episode.setEpisodeNumber(1);
        episode.setTitle(EPISODE_TITLE);
        return episode;
    }

    private Brand createBrand(String title) {
        Brand brand = BrandTestDataBuilder
                .brand()
                .withTitle(title)
                .withCanonicalUri(BRAND_URI)
                .build();

        return brand;
    }
    
}
