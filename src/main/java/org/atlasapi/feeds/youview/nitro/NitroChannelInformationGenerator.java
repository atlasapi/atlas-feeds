package org.atlasapi.feeds.youview.nitro;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.atlasapi.feeds.tvanytime.ChannelElementGenerator;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Image;
import org.atlasapi.media.entity.Publisher;

import com.youview.refdata.schemas._2011_07_06.ExtendedServiceInformationType;
import com.youview.refdata.schemas._2011_07_06.ExtendedTargetingInformationType;
import com.youview.refdata.schemas._2011_07_06.TargetPlaceType;
import org.apache.commons.lang.StringUtils;
import tva.metadata._2010.ServiceInformationNameLengthType;
import tva.metadata._2010.ServiceInformationNameType;
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
    private static final String BBC_SERVICE_SHORT_NAME = "bbc:service:name:short";

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
        serviceInformationType.setServiceURL(getDvbLocator(channel).orElse(null));

        setNameAndOwner(channel, serviceInformationType);

        setDescriptions(channel, serviceInformationType);
        setGenres(channel, serviceInformationType, OTHER_GENRE_HREF_1, OTHER_GENRE_HREF_2);

        setRelatedMaterial(channel, serviceInformationType);

        setOtherIdentifier(channel, serviceInformationType);
        setTargetingInformation(channel, serviceInformationType);

        setShortDescription(channel, serviceInformationType);
        setShortName(channel, serviceInformationType);

        return serviceInformationType;
    }

    private void setShortName(
            Channel channel,
            ExtendedServiceInformationType serviceInformationType
    ) {
        ServiceInformationNameType name = new ServiceInformationNameType();
        name.setLength(ServiceInformationNameLengthType.SHORT);

        Optional<Alias> shortNameAlias = channel.getAliases()
                .stream()
                .filter(alias -> alias.getNamespace().equals(BBC_SERVICE_SHORT_NAME))
                .findFirst();

        shortNameAlias.ifPresent(alias -> name.setValue(alias.getValue()));

        serviceInformationType.getName().add(name);
    }

    private static Optional<String> getDvbLocator(Channel channel) {
        return channel.getAliases()
                .stream()
                .filter(alias -> BBC_SERVICE_LOCATOR.equals(alias.getNamespace()))
                .findFirst().flatMap(alias -> Optional.of(alias.getValue()));
    }

    private void setTargetingInformation(Channel channel, ExtendedServiceInformationType serviceInformationType) {
        ExtendedTargetingInformationType targetingInfo = new ExtendedTargetingInformationType();
        for (String target : channel.getTargetRegions()) {
            targetingInfo.getTargetPlace().add(createTargetPlaceType(target));
        }
        serviceInformationType.setTargetingInformation(targetingInfo);
    }

    private TargetPlaceType createTargetPlaceType(String target) {
        TargetPlaceType targetPlace = new TargetPlaceType();
        targetPlace.setHref(target);
        targetPlace.setExclusive(true);
        return targetPlace;
    }

    private void setShortDescription(
            Channel channel,
            ServiceInformationType generated
    ) {
        SynopsisType shortDescription = new SynopsisType();
        shortDescription.setLength(SynopsisLengthType.SHORT);

        Optional<Alias> shortNameAlias = channel.getAliases()
                .stream()
                .filter(alias -> alias.getNamespace().equals(BBC_SERVICE_SHORT_NAME))
                .findFirst();

        shortNameAlias.ifPresent(alias -> shortDescription.setValue(alias.getValue()));

        generated.getServiceDescription().add(shortDescription);
    }

    @Override
    void setRelatedMaterial(Channel channel, ServiceInformationType svcInfoType) {
        /* Services use the ident image (which is pulled from the masterbrand at ingest) as both
            ident and dog */
        Optional<Image> maybeIdentImage = getBbcImageByAlias(
                channel,
                IMAGE_USE_1_ALIAS,
                IMAGE_USE_1_NITRO_ALIAS
        );
        if (maybeIdentImage.isPresent()) {
            Image identImage = maybeIdentImage.get();
            if (!isOverrideImage(identImage)) {
                identImage = resizeImage(identImage);
            }
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
}
