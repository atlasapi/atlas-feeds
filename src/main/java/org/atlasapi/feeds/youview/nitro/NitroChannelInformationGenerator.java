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

public class NitroChannelInformationGenerator implements ChannelElementGenerator {

    private final static String SERVICE_ID_PREFIX = "http://nitro.bbc.co.uk/services/";
    private final static  String MISSING = "missing";
    private final static String MAIN_GENRE_TYPE = "main";
    private final static String OTHER_GENRE_TYPE = "other";
    private final static String MAIN_GENRE_HREF = "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3";
    private final static String OTHER_GENRE_HREF_1 = "http://refdata.youview.com/mpeg7cs/YouViewServiceTypeCS/2010-10-25#linear_service-broadcast_channel";
    private final static String OTHER_GENRE_HREF_2 = "http://refdata.youview.com/mpeg7cs/YouViewContentProviderCS/2010-09-22#GBR-bbc";
    private final static String IMAGE_INTENDED_USE_MAIN = "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#role-primary";
    private final static String IMAGE_INTENDED_USE_1 = "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-ident";
    private final static String IMAGE_INTENDED_USE_2 = "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-dog";
    private final static String HOW_RELATED = "urn:tva:metadata:cs:HowRelatedCS:2010:19";
    private final static String FORMAT = "urn:mpeg:mpeg7:cs:FileFormatCS2001:1";
    private final static String AUTHORITY = "applicationPublisher.youview.com";

    @Override
    public ServiceInformationType generate(Channel channel) {
        ExtendedServiceInformationType serviceInformationType = new ExtendedServiceInformationType();
        serviceInformationType.setServiceId(SERVICE_ID_PREFIX + MISSING);
        serviceInformationType.setServiceURL(MISSING);
        setNameAndOwner(channel, serviceInformationType);
        setDescriptions(channel, serviceInformationType);
        setGenres(serviceInformationType);

        setRelatedMaterial(channel, serviceInformationType, IMAGE_INTENDED_USE_1);
        setRelatedMaterial(channel, serviceInformationType, IMAGE_INTENDED_USE_2);

        setOtherIdentifier(channel, serviceInformationType);
        setTargetingInformation(serviceInformationType);
        return serviceInformationType;
    }

    private void setTargetingInformation(ExtendedServiceInformationType serviceInformationType) {
        ExtendedTargetingInformationType targetingInfo = new ExtendedTargetingInformationType();
        TargetPlaceType targetPlace = new TargetPlaceType();
        targetPlace.setHref(MISSING);
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
        ServiceInformationType generated = generate(channel);
        setShortDescriptionFromParent(parentChannel, generated);
        return generated;
    }

    private void setShortDescriptionFromParent(Channel parentChannel,
            ServiceInformationType generated) {
        SynopsisType shortDescription = new SynopsisType();
        shortDescription.setLength(SynopsisLengthType.SHORT);
        shortDescription.setValue(parentChannel.getTitle());
        generated.getServiceDescription().add(shortDescription);
    }

    private void setRelatedMaterial(Channel channel,
            ServiceInformationType serviceInformationType, String imageIntendedUse) {
        Image image = Iterables.getFirst(channel.getImages(), null);
        ExtendedRelatedMaterialType relatedMaterial = new ExtendedRelatedMaterialType();
        if (image != null) {
            ControlledTermType howRelated = new ControlledTermType();
            howRelated.setHref(HOW_RELATED);
            relatedMaterial.setHowRelated(howRelated);
            ControlledTermType format = new ControlledTermType();
            format.setHref(FORMAT);
            relatedMaterial.setFormat(format);
            setMediaLocator(image, relatedMaterial);
            setPromotionalText(channel, relatedMaterial);
            setContentProperties(image, relatedMaterial, imageIntendedUse);
            serviceInformationType.getRelatedMaterial().add(relatedMaterial);
        }
    }

    private void setContentProperties(Image image, ExtendedRelatedMaterialType relatedMaterialType,
            String imageIntendedUse) {
        ContentPropertiesType contentProperties = new ContentPropertiesType();
        StillImageContentAttributesType contentAttributes = new StillImageContentAttributesType();
        contentAttributes.setHeight(image.getHeight());
        contentAttributes.setWidth(image.getWidth());
        ControlledTermType mainIntendedUse = new ControlledTermType();
        mainIntendedUse.setHref(IMAGE_INTENDED_USE_MAIN);
        contentAttributes.getIntendedUse().add(mainIntendedUse);
        ControlledTermType intendedUse = new ControlledTermType();
        intendedUse.setHref(imageIntendedUse);
        contentAttributes.getIntendedUse().add(intendedUse);
        contentProperties.getContentAttributes().add(contentAttributes);
        relatedMaterialType.setContentProperties(contentProperties);
    }

    private void setPromotionalText(Channel channel, ExtendedRelatedMaterialType relatedMaterial) {
        TextualType promotionalText = new TextualType();
        promotionalText.setValue(channel.getTitle());
        relatedMaterial.getPromotionalText().add(promotionalText);
    }

    private void setMediaLocator(Image image, ExtendedRelatedMaterialType relatedMaterial) {
        MediaLocatorType mediaLocator = new MediaLocatorType();
        mediaLocator.setMediaUri(image.getCanonicalUri());
        relatedMaterial.setMediaLocator(mediaLocator);
    }

    private void setDescriptions(Channel channel, ServiceInformationType serviceInformationType) {
        SynopsisType longDescription = new SynopsisType();
        longDescription.setLength(SynopsisLengthType.LONG);
        longDescription.setValue(channel.getTitle());
        serviceInformationType.getServiceDescription().add(longDescription);
    }

    private void setNameAndOwner(Channel channel, ServiceInformationType serviceInformationType) {
        ServiceInformationNameType name = new ServiceInformationNameType();
        name.setValue(channel.getTitle());
        Publisher broadcaster = channel.getBroadcaster();
        if (broadcaster != null) {
            serviceInformationType.getOwner().add(broadcaster.title());
        }
        serviceInformationType.getName().add(name);
    }

    private void setGenres(ServiceInformationType serviceInformationType) {
        GenreType mainGenre = new GenreType();
        mainGenre.setType(MAIN_GENRE_TYPE);
        mainGenre.setHref(MAIN_GENRE_HREF);
        serviceInformationType.getServiceGenre().add(mainGenre);
        GenreType otherGengre1 = new GenreType();
        otherGengre1.setType(OTHER_GENRE_TYPE);
        otherGengre1.setHref(OTHER_GENRE_HREF_1);
        serviceInformationType.getServiceGenre().add(otherGengre1);
        GenreType otherGengre2 = new GenreType();
        otherGengre2.setType(OTHER_GENRE_TYPE);
        otherGengre2.setHref(OTHER_GENRE_HREF_2);
        serviceInformationType.getServiceGenre().add(otherGengre2);
    }
}
