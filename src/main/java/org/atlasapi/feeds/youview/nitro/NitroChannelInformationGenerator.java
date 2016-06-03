package org.atlasapi.feeds.youview.nitro;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.atlasapi.feeds.tvanytime.ChannelElementGenerator;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Image;
import org.atlasapi.media.entity.Publisher;

import com.google.common.base.Optional;
import com.youview.refdata.schemas._2011_07_06.ExtendedServiceInformationType;
import com.youview.refdata.schemas._2011_07_06.ExtendedTargetingInformationType;
import com.youview.refdata.schemas._2011_07_06.TargetPlaceType;
import org.apache.commons.lang.StringUtils;
import tva.metadata._2010.ControlledTermType;
import tva.metadata._2010.ServiceInformationType;
import tva.metadata._2010.SynopsisLengthType;
import tva.metadata._2010.SynopsisType;
import tva.metadata.extended._2010.ExtendedRelatedMaterialType;
import tva.mpeg7._2008.UniqueIDType;

public class NitroChannelInformationGenerator extends ChannelGenerator implements ChannelElementGenerator {

    private final static String OTHER_GENRE_HREF_1 = "http://refdata.youview.com/mpeg7cs/YouViewServiceTypeCS/2010-10-25#linear_service-broadcast_channel";
    private final static String OTHER_GENRE_HREF_2 = "http://refdata.youview.com/mpeg7cs/YouViewContentProviderCS/2010-09-22#GBR-bbc";

    private final static String AUTHORITY = "applicationPublisher.youview.com";
    private static final String BBC_SERVICE_LOCATOR = "bbc:service:locator";


    private void setOtherIdentifier(Channel channel,
            ExtendedServiceInformationType serviceInformationType) {
        Publisher broadcaster = channel.getBroadcaster();
        if (broadcaster != null) {
            UniqueIDType uniqueIDType = new UniqueIDType();
            uniqueIDType.setAuthority(AUTHORITY);
            uniqueIDType.setValue(invertBroadcaster(broadcaster.key()));
            serviceInformationType.getOtherIdentifier().add(uniqueIDType);
        }
    }

    private String invertBroadcaster(String key) {
        List<String> keyParts = Arrays.asList(key.split("\\."));
        Collections.reverse(keyParts);
        return StringUtils.join(keyParts, '.');
    }

    @Override
    public ServiceInformationType generate(Channel channel) {
        ExtendedServiceInformationType serviceInformationType = new ExtendedServiceInformationType();
        serviceInformationType.setServiceId(channel.getCanonicalUri());
        serviceInformationType.setServiceURL(getDvbLocator(channel).orNull());
        setNameAndOwner(channel, serviceInformationType);
        setDescriptions(channel, serviceInformationType);
        setGenres(serviceInformationType, OTHER_GENRE_HREF_1, OTHER_GENRE_HREF_2);

        setRelatedMaterial(channel, serviceInformationType);

        setOtherIdentifier(channel, serviceInformationType);
        setTargetingInformation(channel, serviceInformationType);
        setShortDescription(channel, serviceInformationType);
        return serviceInformationType;
    }


    private static Optional<String> getDvbLocator(Channel channel) {
        for (Alias alias : channel.getAliases()) {
            if (BBC_SERVICE_LOCATOR.equals(alias.getNamespace())) {
                return Optional.of(alias.getValue());
            }
        }
        return Optional.absent();
    }

    private void setTargetingInformation(Channel channel, ExtendedServiceInformationType serviceInformationType) {
        ExtendedTargetingInformationType targetingInfo = new ExtendedTargetingInformationType();
        TargetPlaceType targetPlace = new TargetPlaceType();
        for (String targets : channel.getTargetRegions()) {
            targetPlace.setHref("http://refdata.youview.com/mpeg7cs/YouViewTargetRegionCS/" + targets);
        }
        targetPlace.setExclusive(true);
        targetingInfo.getTargetPlace().add(targetPlace);
        serviceInformationType.setTargetingInformation(targetingInfo);
    }

    private void setShortDescription(Channel channel,
            ServiceInformationType generated) {
        SynopsisType shortDescription = new SynopsisType();
        shortDescription.setLength(SynopsisLengthType.SHORT);
        for (Alias alias : channel.getAliases()) {
            if (alias.getNamespace().equals("bbc:service:name:short")) {
                shortDescription.setValue(alias.getValue());
            }
        }
        generated.getServiceDescription().add(shortDescription);
    }

    @Override
    void setRelatedMaterial(Channel channel, ServiceInformationType svcInfoType) {
        /* Services use the ident image (which is pulled from the masterbrand at ingest) as both
            ident and dog */
        Image identImage = getBbcImageByAlias(channel, IMAGE_USE_1_ALIAS);
        ExtendedRelatedMaterialType identMaterial = createRelatedMaterial(
                channel, IMAGE_INTENDED_USE_1, identImage
        );
        ExtendedRelatedMaterialType dogMaterial = createRelatedMaterial(
                channel, IMAGE_INTENDED_USE_2, identImage
        );
        svcInfoType.getRelatedMaterial().add(identMaterial);
        svcInfoType.getRelatedMaterial().add(dogMaterial);
    }
}
