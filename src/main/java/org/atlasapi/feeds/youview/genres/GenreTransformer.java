package org.atlasapi.feeds.youview.genres;


public class GenreTransformer {

    private GenreTransformer() {
        // private constructor for factory class
    }

    public static String toAtlasGenre(String genrePrefix, String genre) { 
        String lowerCase = genre.toLowerCase();
        return genrePrefix + lowerCase.replace('/', '-').replace(" ", "");
    }
    
    public static String toAtlasSubGenre(String genrePrefix, String genre) {
        String lowerCase = genre.toLowerCase();
        return genrePrefix + lowerCase.replace('/', '-').replace(" ", "").replace(">", "/");
    }
}
