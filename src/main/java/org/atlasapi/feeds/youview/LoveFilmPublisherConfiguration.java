package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;


public class LoveFilmPublisherConfiguration implements PublisherConfiguration {

    
    private static final int IMAGE_HEIGHT = 360;
    private static final int IMAGE_WIDTH = 640;
    private static final String LOVEFILM_GROUP_INFO_SERVICE_ID = "http://lovefilm.com/ContentOwning";
    private static final String LOVEFILM_IMI_PREFIX = "imi:lovefilm.com/";
    private static final String LOVEFILM_ONDEMAND_SERVICE_ID = "http://lovefilm.com/OnDemand";
    private static final String LOVEFILM_PRODUCT_CRID_PREFIX = "crid://lovefilm.com/product/";
    private static final String LOVEFILM_DEEP_LINKING_ID = "deep_linking_id.lovefilm.com";
    
    private final String baseUri;
    
    public LoveFilmPublisherConfiguration(String baseUri) {
        this.baseUri = checkNotNull(baseUri);
    }
    
    @Override
    public int getDefaultImageHeight() {
        return IMAGE_HEIGHT;
    }

    @Override
    public int getDefaultImageWidth() {
        return IMAGE_WIDTH;
    }

    @Override
    public String getGroupInformationServiceId() {
        return LOVEFILM_GROUP_INFO_SERVICE_ID;
    }

    @Override
    public String getOnDemandServiceId() {
        return LOVEFILM_ONDEMAND_SERVICE_ID;
    }

    @Override
    public String getDeepLinkingAuthorityId() {
        return LOVEFILM_DEEP_LINKING_ID;
    }

    @Override
    public String getCridPrefix() {
        return LOVEFILM_PRODUCT_CRID_PREFIX;
    }

    @Override
    public String getImiPrefix() {
        return LOVEFILM_IMI_PREFIX;
    }

    @Override
    public String getYouViewBaseUrl() {
        return baseUri;
    }
}
