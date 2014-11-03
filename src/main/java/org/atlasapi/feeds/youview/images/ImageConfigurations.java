package org.atlasapi.feeds.youview.images;

import org.atlasapi.feeds.youview.InvalidPublisherException;
import org.atlasapi.media.entity.Publisher;


public final class ImageConfigurations {
    
    private ImageConfigurations() {}

 // TODO this is a little hokey
    public static ImageConfiguration imageConfigFor(Publisher publisher) {
        if (Publisher.LOVEFILM.equals(publisher)) {
            return new LoveFilmImageConfiguration();
        }
        if (Publisher.AMAZON_UNBOX.equals(publisher)) {
            return new UnboxImageConfiguration();
        }
        if (Publisher.BBC_NITRO.equals(publisher)) {
            return new NitroImageConfiguration();
        }
        throw new InvalidPublisherException(publisher);
    }
    
    public static final class LoveFilmImageConfiguration implements ImageConfiguration {
        
        private static final int IMAGE_HEIGHT = 360;
        private static final int IMAGE_WIDTH = 640;

        @Override
        public int defaultImageHeight() {
            return IMAGE_HEIGHT;
        }

        @Override
        public int defaultImageWidth() {
            return IMAGE_WIDTH;
        }
    }
    
    public static final class UnboxImageConfiguration implements ImageConfiguration {
        
        private static final int IMAGE_HEIGHT = 320;
        private static final int IMAGE_WIDTH = 240;

        @Override
        public int defaultImageHeight() {
            return IMAGE_HEIGHT;
        }

        @Override
        public int defaultImageWidth() {
            return IMAGE_WIDTH;
        }
    }
    
    public static final class NitroImageConfiguration implements ImageConfiguration {

        private static final int IMAGE_HEIGHT = 320;
        private static final int IMAGE_WIDTH = 240;
        
        @Override
        public int defaultImageHeight() {
            return IMAGE_HEIGHT;
        }

        @Override
        public int defaultImageWidth() {
            return IMAGE_WIDTH;
        }
    }
}
