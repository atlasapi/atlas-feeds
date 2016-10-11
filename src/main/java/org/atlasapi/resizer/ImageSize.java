package org.atlasapi.resizer;

public class ImageSize {

    private final Integer width;
    private final Integer height;

    public ImageSize(Integer height, Integer width) {
        this.height = height;
        this.width = width;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }
}
