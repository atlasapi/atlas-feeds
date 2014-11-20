package org.atlasapi.feeds.youview.ids;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.YouViewGeneratorUtils.getAsin;

import org.atlasapi.feeds.youview.InvalidPublisherException;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Optional;


public abstract class PublisherIdUtilities implements PublisherIdUtility {

    // TODO this is a little hokey
    public static PublisherIdUtility idUtilFor(Publisher publisher, String baseUri) {
        if (Publisher.LOVEFILM.equals(publisher)) {
            return new LoveFilmIdentifiers(baseUri);
        }
        if (Publisher.AMAZON_UNBOX.equals(publisher)) {
            return new UnboxIdentifiers(baseUri);
        }
        if (Publisher.BBC_NITRO.equals(publisher)) {
            return new NitroIdentifiers(baseUri);
        }
        throw new InvalidPublisherException(publisher);
    }
    
    public static final class LoveFilmIdentifiers implements PublisherIdUtility {

        private static final String LOVEFILM_GROUP_INFO_SERVICE_ID = "http://lovefilm.com/ContentOwning";
        private static final String LOVEFILM_IMI_PREFIX = "imi:lovefilm.com/";
        private static final String LOVEFILM_ONDEMAND_SERVICE_ID = "http://lovefilm.com/OnDemand";
        private static final String LOVEFILM_PRODUCT_CRID_PREFIX = "crid://lovefilm.com/product/";
        private static final String LOVEFILM_DEEP_LINKING_ID = "deep_linking_id.lovefilm.com";
        
        private final String baseUri;
        
        public LoveFilmIdentifiers(String baseUri) {
            this.baseUri = checkNotNull(baseUri);
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

        @Override
        public Optional<String> getOtherIdentifier(Item item) {
            return Optional.of(getAsin(item));
        }
    }

    public static final class NitroIdentifiers implements PublisherIdUtility {

        private static final String NITRO_GROUP_INFO_SERVICE_ID = "http://nitro.bbc.co.uk/ContentOwning";
        private static final String NITRO_PRODUCT_CRID_PREFIX = "crid://nitro.bbc.co.uk/product/";
        private static final String NITRO_IMI_PREFIX = "imi:nitro.bbc.co.uk/";
        private static final String NITRO_DEEP_LINKING_ID = "deep_linking_id.nitro.bbc.co.uk";
        private static final String NITRO_ONDEMAND_SERVICE_ID = "http://nitro.bbc.co.uk/OnDemand";
        
        
        private final String baseUri;
        
        public NitroIdentifiers(String baseUri) {
            this.baseUri = checkNotNull(baseUri);
        }
        
        @Override
        public String getGroupInformationServiceId() {
            return NITRO_GROUP_INFO_SERVICE_ID;
        }

        @Override
        public String getOnDemandServiceId() {
            return NITRO_ONDEMAND_SERVICE_ID;
        }

        @Override
        public String getDeepLinkingAuthorityId() {
            return NITRO_DEEP_LINKING_ID;
        }

        @Override
        public String getCridPrefix() {
            return NITRO_PRODUCT_CRID_PREFIX;
        }

        @Override
        public String getImiPrefix() {
            return NITRO_IMI_PREFIX;
        }

        @Override
        public String getYouViewBaseUrl() {
            return baseUri;
        }

        @Override
        public Optional<String> getOtherIdentifier(Item item) {
            return Optional.absent();
        }
    }

    public static final class UnboxIdentifiers implements PublisherIdUtility {

        private static final String UNBOX_GROUP_INFO_SERVICE_ID = "http://unbox.amazon.co.uk/ContentOwning";
        private static final String UNBOX_PRODUCT_CRID_PREFIX = "crid://unbox.amazon.co.uk/product/";
        private static final String UNBOX_IMI_PREFIX = "imi:unbox.amazon.co.uk/";
        private static final String UNBOX_DEEP_LINKING_ID = "deep_linking_id.unbox.amazon.co.uk";
        private static final String UNBOX_ONDEMAND_SERVICE_ID = "http://unbox.amazon.co.uk/OnDemand";
        
        
        private final String baseUri;
        
        public UnboxIdentifiers(String baseUri) {
            this.baseUri = checkNotNull(baseUri);
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

        @Override
        public Optional<String> getOtherIdentifier(Item item) {
            return Optional.of(getAsin(item));
        }
    }
}
