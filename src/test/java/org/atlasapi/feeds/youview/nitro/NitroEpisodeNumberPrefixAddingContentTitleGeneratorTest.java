package org.atlasapi.feeds.youview.nitro;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.testing.BrandTestDataBuilder;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.ResolvedContent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class NitroEpisodeNumberPrefixAddingContentTitleGeneratorTest {

    private static final String BRAND_URI = "http://example.org/brands/1";
    private static final String SERIES_URI = "http://example.org/series/1";

    private static final String EPISODE_TITLE = "Episode";
    private static final String EPISODE_TITLE_WITH_EPISODE_NUMBER_PREFIX = "1. Episode";

    private final ContentResolver contentResolver = mock(ContentResolver.class);
    private final NitroEpisodeNumberPrefixAddingContentTitleGenerator generator = new NitroEpisodeNumberPrefixAddingContentTitleGenerator(contentResolver);
    
    @Test
    public void testNonEpisodeTypesAreNotMutated() {
        Item item = new Item();
        item.setTitle(EPISODE_TITLE);
        assertEquals(EPISODE_TITLE, generator.titleFor(item));
    }
    
    @Test
    public void testEpisodeWithBrandContainingNumberIsNotMutated() {
        Brand brand = createBrandAndSetUpResolverToResolve("Brand 1");
        Episode episode = createEpisode(brand, null);
        
        assertEquals(EPISODE_TITLE, generator.titleFor(episode));
    }
    
    @Test
    public void testEpisodeWithBrandNotContainingSeparateNumberSuffixIsMutated() {
        Brand brand = createBrandAndSetUpResolverToResolve("Brand1");
        Episode episode = createEpisode(brand, null);
        assertEquals(EPISODE_TITLE_WITH_EPISODE_NUMBER_PREFIX, generator.titleFor(episode));
    }
    
    @Test
    public void testEpisodeWithBrandNotContainingNumberIsMutated() {
        Brand brand = createBrandAndSetUpResolverToResolve("Brand");
        Episode episode = createEpisode(brand, null);
        assertEquals(EPISODE_TITLE_WITH_EPISODE_NUMBER_PREFIX, generator.titleFor(episode));
    }
    
    @Test
    public void testEpisodeWithSeriesNotContainingNumberIsMutated() {
        Series series = createSeriesAndSetUpResolverToResolve("Series", null);
        Episode episode = createEpisode(null, series);
        assertEquals(EPISODE_TITLE_WITH_EPISODE_NUMBER_PREFIX, generator.titleFor(episode));
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

    private Brand createBrandAndSetUpResolverToResolve(String title) {
        Brand brand = BrandTestDataBuilder
                .brand()
                .withTitle(title)
                .withCanonicalUri(BRAND_URI)
                .build();

        mockResolverShouldResolve(brand);
        return brand;
    }
    
    private Series createSeriesAndSetUpResolverToResolve(String title, Brand brand) {
        Series series = new Series();
        series.setCanonicalUri(SERIES_URI);
        if (brand != null) {
            series.setParent(brand);
        }
        series.setTitle(title);
        mockResolverShouldResolve(series);
        
        return series;
    }

    
    private void mockResolverShouldResolve(Content content) {
        when(contentResolver.findByCanonicalUris(Matchers.argThat(containsInAnyOrder(content.getCanonicalUri()))))
            .thenReturn(ResolvedContent.builder().put(content.getCanonicalUri(), content).build());

    }
}
