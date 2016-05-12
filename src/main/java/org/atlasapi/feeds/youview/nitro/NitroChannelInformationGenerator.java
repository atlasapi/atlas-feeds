package org.atlasapi.feeds.youview.nitro;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.atlasapi.feeds.tvanytime.ChannelElementGenerator;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Image;
import org.atlasapi.media.entity.Publisher;

import com.google.common.collect.Iterables;
import com.youview.refdata.schemas._2011_07_06.ExtendedServiceInformationType;
import com.youview.refdata.schemas._2011_07_06.ExtendedTargetingInformationType;
import com.youview.refdata.schemas._2011_07_06.TargetPlaceType;
import org.apache.commons.lang.StringUtils;
import tva.metadata._2010.ControlledTermType;
import tva.metadata._2010.GenreType;
import tva.metadata._2010.ServiceInformationNameType;
import tva.metadata._2010.ServiceInformationType;
import tva.metadata._2010.SynopsisLengthType;
import tva.metadata._2010.SynopsisType;
import tva.metadata.extended._2010.ContentPropertiesType;
import tva.metadata.extended._2010.ExtendedRelatedMaterialType;
import tva.metadata.extended._2010.StillImageContentAttributesType;
import tva.mpeg7._2008.MediaLocatorType;
import tva.mpeg7._2008.TextualType;
import tva.mpeg7._2008.UniqueIDType;

public class NitroChannelInformationGenerator extends ChannelGenerator implements ChannelElementGenerator {

    private final static String SERVICE_ID_PREFIX = "http://nitro.bbc.co.uk/services/";
    private final static String OTHER_GENRE_HREF_1 = "http://refdata.youview.com/mpeg7cs/YouViewServiceTypeCS/2010-10-25#linear_service-broadcast_channel";
    private final static String OTHER_GENRE_HREF_2 = "http://refdata.youview.com/mpeg7cs/YouViewContentProviderCS/2010-09-22#GBR-bbc";
    private final static String IMAGE_INTENDED_USE_1 = "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-ident";
    private final static String IMAGE_INTENDED_USE_2 = "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-dog";
    private final static String AUTHORITY = "applicationPublisher.youview.com";

    private void setTargetingInformation(ExtendedServiceInformationType serviceInformationType) {
        ExtendedTargetingInformationType targetingInfo = new ExtendedTargetingInformationType();
        TargetPlaceType targetPlace = new TargetPlaceType();
        targetPlace.setHref("http://refdata.youview.com/mpeg7cs/YouViewTargetRegionCS/2010-10-26#GBR");
        targetPlace.setExclusive(true);
        targetingInfo.getTargetPlace().add(targetPlace);
        serviceInformationType.setTargetingInformation(targetingInfo);
    }

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
    public ServiceInformationType generate(Channel channel, Channel parentChannel) {
        ExtendedServiceInformationType serviceInformationType = new ExtendedServiceInformationType();
        serviceInformationType.setServiceId(SERVICE_ID_PREFIX + "bbc_three_233a_10c0");
        serviceInformationType.setServiceURL("dvb://233a..10c0");
        setNameAndOwner(channel, serviceInformationType);
        setDescriptions(channel, serviceInformationType);
        setGenres(serviceInformationType, OTHER_GENRE_HREF_1, OTHER_GENRE_HREF_2);

        setRelatedMaterial(channel, serviceInformationType, IMAGE_INTENDED_USE_1);
        setRelatedMaterial(channel, serviceInformationType, IMAGE_INTENDED_USE_2);

        setOtherIdentifier(channel, serviceInformationType);
        setTargetingInformation(serviceInformationType);
        setShortDescriptionFromParent(parentChannel, serviceInformationType);
        return serviceInformationType;
    }

    private void setShortDescriptionFromParent(Channel parentChannel,
            ServiceInformationType generated) {
        SynopsisType shortDescription = new SynopsisType();
        shortDescription.setLength(SynopsisLengthType.SHORT);
        shortDescription.setValue(parentChannel.getTitle());
        generated.getServiceDescription().add(shortDescription);
    }



}
