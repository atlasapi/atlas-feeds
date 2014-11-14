package org.atlasapi.feeds.youview.nitro;

import static org.junit.Assert.*;

import java.util.Set;

import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Publisher;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;


public class NitroGenreMappingTest {

    private final NitroGenreMapping mapping = new NitroGenreMapping("nitro_genre_mapping.csv");
    
    @Test
    public void testContentWithNoGenresProducesNoYVGenres() {
        Film film = createContentWithGenres(ImmutableSet.<String>of());
        
        assertTrue("No genres should be output if input item has no genres", mapping.youViewGenresFor(film).isEmpty());
    }

    @Test
    public void testContentWithAGenreMappedToNothingOutputsNoYVGenres() {
        Film film = createContentWithGenres(ImmutableSet.<String>of("http://nitro.bbc.co.uk/genres/100002"));
        
        assertTrue("No genres should be output if input item genre has no output mapping", mapping.youViewGenresFor(film).isEmpty());
    }

    @Test
    public void testContentWithASingleGenreMapsToAppropriateYVGenre() {
        Film film = createContentWithGenres(ImmutableSet.<String>of("http://nitro.bbc.co.uk/genres/100001"));
        
        Set<String> youViewGenres = mapping.youViewGenresFor(film);
        assertEquals(ImmutableSet.of("urn:tva:metadata:cs:IntendedAudienceCS:2010:4.2.1"), youViewGenres);
    }

    @Test
    public void testContentWithASingleGenreTreeMapsToAppropriateYVGenres() {
        Film film = createContentWithGenres(ImmutableSet.<String>of(
                "http://nitro.bbc.co.uk/genres/100001", 
                "http://nitro.bbc.co.uk/genres/200001"
        ));
        
        Set<String> youViewGenres = mapping.youViewGenresFor(film);
        ImmutableSet<String> expected = ImmutableSet.of(
                "urn:tva:metadata:cs:IntendedAudienceCS:2010:4.2.1",
                "urn:tva:metadata:cs:ContentCS:2010:3.1.3.12"
        );
        
        assertEquals(expected, youViewGenres);
    }
    
    @Test
    public void testContentWithSeveralSetsMapsToAppropriateYVGenres() {
        Film film = createContentWithGenres(ImmutableSet.<String>of(
                "http://nitro.bbc.co.uk/genres/100001", 
                "http://nitro.bbc.co.uk/genres/200001",
                "http://nitro.bbc.co.uk/genres/200002"
        ));
        
        Set<String> youViewGenres = mapping.youViewGenresFor(film);
        ImmutableSet<String> expected = ImmutableSet.of(
                "urn:tva:metadata:cs:IntendedAudienceCS:2010:4.2.1",
                "urn:tva:metadata:cs:ContentCS:2010:3.1.3.12",
                "urn:tva:metadata:cs:ContentCS:2010:3.5"
        );
        
        assertEquals(expected, youViewGenres);
    }
    
    private Film createContentWithGenres(ImmutableSet<String> genres) {
        Film film = new Film("uri", "curie", Publisher.METABROADCAST);
        film.setGenres(genres);
        return film;
    }
}
