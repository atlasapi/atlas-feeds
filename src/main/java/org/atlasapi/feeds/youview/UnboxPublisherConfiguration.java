package org.atlasapi.feeds.youview;

import static com.google.common.base.Preconditions.checkNotNull;


public class UnboxPublisherConfiguration implements PublisherConfiguration {

    private static final int IMAGE_HEIGHT = 320;
    private static final int IMAGE_WIDTH = 240;
    private static final String UNBOX_GROUP_INFO_SERVICE_ID = "http://unbox.amazon.co.uk/ContentOwning";
    private static final String UNBOX_PRODUCT_CRID_PREFIX = "crid://unbox.amazon.co.uk/product/";
    private static final String UNBOX_IMI_PREFIX = "imi:unbox.amazon.co.uk/";
    private static final String UNBOX_DEEP_LINKING_ID = "deep_linking_id.unbox.amazon.co.uk";
    private static final String UNBOX_ONDEMAND_SERVICE_ID = "http://unbox.amazon.co.uk/OnDemand";
    
    
    private final String baseUri;
    
    public UnboxPublisherConfiguration(String baseUri) {
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
        return UNBOX_GROUP_INFO_SERVICE_ID;
    }

    @Override
    public String getOnDemandServiceId() {
        return UNBOX_ONDEMAND_SERVICE_ID;
    }

    @Override
    public String getDeepLinkingAuthorityId() {
        return UNBOX_DEEP_LINKING_ID;
    }

    @Override
    public String getCridPrefix() {
        return UNBOX_PRODUCT_CRID_PREFIX;
    }

    @Override
    public String getImiPrefix() {
        return UNBOX_IMI_PREFIX;
    }

    @Override
    public String getYouViewBaseUrl() {
        return baseUri;
    }
}
