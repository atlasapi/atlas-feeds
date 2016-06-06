package org.atlasapi.feeds.youview.nitro;

import org.atlasapi.feeds.tvanytime.MasterbrandElementGenerator;
import org.atlasapi.media.channel.Channel;

import com.youview.refdata.schemas._2011_07_06.ExtendedServiceInformationType;
import org.atlasapi.media.entity.Image;
import tva.metadata._2010.ServiceInformationType;
import tva.metadata.extended._2010.ExtendedRelatedMaterialType;

public class NitroMasterbrandInfoGenerator extends ChannelGenerator implements MasterbrandElementGenerator {

    private final static String OTHER_GENRE_HREF_1 = "http://refdata.youview.com/mpeg7cs/YouViewServiceTypeCS/2010-10-25#owning_service";
    private final static String OTHER_GENRE_HREF_2 = "http://refdata.youview.com/mpeg7cs/YouViewContentProviderCS/2010-09-22#GBR-bbc";
    private final static String IMAGE_INTENDED_USE_1 = "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-ident";
    private final static String IMAGE_INTENDED_USE_2 = "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-dog";

    @Override
    public ServiceInformationType generate(Channel channel) {
        ExtendedServiceInformationType serviceInformationType = new ExtendedServiceInformationType();
        serviceInformationType.setServiceId(channel.getCanonicalUri());
        setNameAndOwner(channel, serviceInformationType);
        setDescriptions(channel, serviceInformationType);
        setGenres(serviceInformationType, OTHER_GENRE_HREF_1, OTHER_GENRE_HREF_2);
        setRelatedMaterial(channel, serviceInformationType);
        return serviceInformationType;
    }

    @Override
    void setRelatedMaterial(Channel channel, ServiceInformationType svcInfoType) {
        Image identImage = getBbcImageByAlias(channel, IMAGE_USE_1_ALIAS, IMAGE_USE_1_NITRO_ALIAS);
        Image dogImage = getBbcImageByAlias(channel, IMAGE_USE_2_ALIAS, null);
        ExtendedRelatedMaterialType identMaterial = createRelatedMaterial(
                channel, IMAGE_INTENDED_USE_1, identImage
        );
        ExtendedRelatedMaterialType dogMaterial = createRelatedMaterial(
                channel, IMAGE_INTENDED_USE_2, dogImage
        );
        svcInfoType.getRelatedMaterial().add(identMaterial);
        svcInfoType.getRelatedMaterial().add(dogMaterial);
    }
}