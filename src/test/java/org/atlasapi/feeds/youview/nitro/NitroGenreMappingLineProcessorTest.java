package org.atlasapi.feeds.youview.nitro;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;


public class NitroGenreMappingLineProcessorTest {

    private static final String GENRE_PREFIX = "prefix/";
    private NitroGenreMappingLineProcessor lineProcessor;
    
    @Before
    public void setup() {
        lineProcessor = new NitroGenreMappingLineProcessor(GENRE_PREFIX);
    }
    
    @Test
    public void testReadsALineOfHeadersBeforeAnythingElse() throws IOException {
        boolean processed = lineProcessor.processLine("A Line");
        
        assertTrue("Anything should be accepted for the first line", processed);
        
        Map<BbcGenreTree, Set<String>> result = lineProcessor.getResult();
        
        assertTrue("No lines should be processed", result.entrySet().isEmpty());
    }
    
    @Test(expected=RuntimeException.class)
    public void testIgnoresLinesWithLessThanEightCommaSeparatedElements() throws IOException {
        readHeaders();
        
        lineProcessor.processLine("SingleElement");
    }
    
    @Test
    public void testCorrectlyParsesSingleBbcGenreAndNoYouViewGenres() throws IOException {
        readHeaders();
        
        lineProcessor.processLine("100002,,,\"C00009\",,,,");
        
        Entry<BbcGenreTree, Set<String>> entry = Iterables.getOnlyElement(lineProcessor.getResult().entrySet());

        BbcGenreTree expected = BbcGenreTree.builder(GENRE_PREFIX + "100002").build();
        
        assertEquals(expected, entry.getKey());
        assertTrue("No YouView genres expected", entry.getValue().isEmpty());
    }
    
    @Test
    public void testCorrectlyParsesTwoBbcGenresAndNoYouViewGenres() throws IOException {
        readHeaders();
        
        lineProcessor.processLine("100002,\"200008\",,\"C00010\",,,,");
        
        Entry<BbcGenreTree, Set<String>> entry = Iterables.getOnlyElement(lineProcessor.getResult().entrySet());

        BbcGenreTree expected = BbcGenreTree.builder(GENRE_PREFIX + "100002")
                .withSecondLevelGenre(GENRE_PREFIX + "200008")
                .build();
        
        assertEquals(expected, entry.getKey());
        assertTrue("No YouView genres expected", entry.getValue().isEmpty());
    }
    
    @Test
    public void testCorrectlyParsesSingleBbcGenreAndSingleYouViewGenre() throws IOException {
        readHeaders();
        
        lineProcessor.processLine("100001,,,\"C00001\",\"urn:tva:metadata:cs:IntendedAudienceCS:2010:4.2.1\",,,");
        
        Entry<BbcGenreTree, Set<String>> entry = Iterables.getOnlyElement(lineProcessor.getResult().entrySet());

        BbcGenreTree expected = BbcGenreTree.builder(GENRE_PREFIX + "100001")
                .build();
        
        assertEquals(expected, entry.getKey());
        assertEquals(ImmutableSet.of("urn:tva:metadata:cs:IntendedAudienceCS:2010:4.2.1"), entry.getValue());
    }
    
    @Test
    public void testCorrectlyParsesTwoBbcGenresAndSingleYouViewGenre() throws IOException {
        readHeaders();
        
        lineProcessor.processLine("100003,\"200015\",,\"C00018\",\"urn:tva:metadata:cs:ContentCS:2010:3.4.6.1\",,,");
        
        Entry<BbcGenreTree, Set<String>> entry = Iterables.getOnlyElement(lineProcessor.getResult().entrySet());

        BbcGenreTree expected = BbcGenreTree.builder(GENRE_PREFIX + "100003")
                .withSecondLevelGenre(GENRE_PREFIX + "200015")
                .build();
        
        assertEquals(expected, entry.getKey());
        assertEquals(ImmutableSet.of("urn:tva:metadata:cs:ContentCS:2010:3.4.6.1"), entry.getValue());
    }
    
    @Test
    public void testCorrectlyParsesThreeBbcGenresAndSingleYouViewGenre() throws IOException {
        readHeaders();
        
        lineProcessor.processLine("100006,\"200072\",\"300044\",\"C00168\",\"urn:tva:metadata:cs:ContentCS:2010:3.6.9.1\",,,");
        
        Entry<BbcGenreTree, Set<String>> entry = Iterables.getOnlyElement(lineProcessor.getResult().entrySet());

        BbcGenreTree expected = BbcGenreTree.builder(GENRE_PREFIX + "100006")
                .withSecondLevelGenre(GENRE_PREFIX + "200072")
                .withThirdLevelGenre(GENRE_PREFIX + "300044")
                .build();
        
        assertEquals(expected, entry.getKey());
        assertEquals(ImmutableSet.of("urn:tva:metadata:cs:ContentCS:2010:3.6.9.1"), entry.getValue());
    }

    @Test
    public void testCorrectlyParsesTwoBbcGenresAndSingleYouViewGenreWithDifferentFormat() throws IOException {
        readHeaders();

        lineProcessor.processLine("100010,\"200096\",,\"C00109\",\"http://refdata.youview.com/mpeg7cs/YouViewEventCS/2012-02-06#10.1.2\",,,");

        Entry<BbcGenreTree, Set<String>> entry = Iterables.getOnlyElement(lineProcessor.getResult().entrySet());

        BbcGenreTree expected = BbcGenreTree.builder(GENRE_PREFIX + "100010")
                .withSecondLevelGenre(GENRE_PREFIX + "200096")
                .build();

        assertEquals(expected, entry.getKey());
        assertEquals(ImmutableSet.of("http://refdata.youview.com/mpeg7cs/YouViewEventCS/2012-02-06#10.1.2"), entry.getValue());
    }

    @Test
    public void testCorrectlyParsesThreeBbcGenresAndSingleYouViewGenreInDifferentFormat() throws IOException {
        readHeaders();

        lineProcessor.processLine("100010,\"200100\",\"300005\",\"C00125\",\"http://refdata.youview.com/mpeg7cs/YouViewEventCS/2012-02-06#10.1.7\",,,");
        
        Entry<BbcGenreTree, Set<String>> entry = Iterables.getOnlyElement(lineProcessor.getResult().entrySet());

        BbcGenreTree expected = BbcGenreTree.builder(GENRE_PREFIX + "100010")
                .withSecondLevelGenre(GENRE_PREFIX + "200100")
                .withThirdLevelGenre(GENRE_PREFIX + "300005")
                .build();
        
        assertEquals(expected, entry.getKey());
        assertEquals(ImmutableSet.of("http://refdata.youview.com/mpeg7cs/YouViewEventCS/2012-02-06#10.1.7"), entry.getValue());
    }

    @Test
    public void testCorrectlyParsesTwoBbcGenresAndTwoYouViewGenres() throws IOException {
        readHeaders();

        lineProcessor.processLine("100010,\"200101\",,\"C00115\",\"urn:tva:metadata:cs:ContentCS:2010:3.2.3.3\",\"urn:tva:metadata:cs:ContentCS:2010:3.2.3.23\",,");

        Entry<BbcGenreTree, Set<String>> entry = Iterables.getOnlyElement(lineProcessor.getResult().entrySet());

        BbcGenreTree expectedBbcGenres = BbcGenreTree.builder(GENRE_PREFIX + "100010")
                .withSecondLevelGenre(GENRE_PREFIX + "200101")
                .build();
        ImmutableSet<String> expectedYvGenres = ImmutableSet.of(
                "urn:tva:metadata:cs:ContentCS:2010:3.2.3.3",
                "urn:tva:metadata:cs:ContentCS:2010:3.2.3.23"
        );

        assertEquals(expectedBbcGenres, entry.getKey());
        assertEquals(expectedYvGenres, entry.getValue());
    }

    @Test
    public void testCorrectlyParsesThreeBbcGenresAndTwoYouViewGenres() throws IOException {
        readHeaders();

        lineProcessor.processLine("100006,\"200072\",\"300046\",\"C00170\",\"urn:tva:metadata:cs:ContentCS:2010:3.6.9.2\",\"urn:tva:metadata:cs:ContentCS:2010:3.6.9.3\",,");

        Entry<BbcGenreTree, Set<String>> entry = Iterables.getOnlyElement(lineProcessor.getResult().entrySet());

        BbcGenreTree expectedBbcGenres = BbcGenreTree.builder(GENRE_PREFIX + "100006")
                .withSecondLevelGenre(GENRE_PREFIX + "200072")
                .withThirdLevelGenre(GENRE_PREFIX + "300046")
                .build();
        ImmutableSet<String> expectedYvGenres = ImmutableSet.of(
                "urn:tva:metadata:cs:ContentCS:2010:3.6.9.2",
                "urn:tva:metadata:cs:ContentCS:2010:3.6.9.3"
        );
        
        assertEquals(expectedBbcGenres, entry.getKey());
        assertEquals(expectedYvGenres, entry.getValue());
    }

    private void readHeaders() throws IOException {
        lineProcessor.processLine("");
    }
}
