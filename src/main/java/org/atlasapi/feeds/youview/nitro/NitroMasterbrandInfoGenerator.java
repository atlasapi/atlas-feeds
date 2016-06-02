package org.atlasapi.feeds.youview.nitro;

import org.atlasapi.feeds.tvanytime.MasterbrandElementGenerator;
import org.atlasapi.media.channel.Channel;

import com.youview.refdata.schemas._2011_07_06.ExtendedServiceInformationType;
import tva.metadata._2010.ServiceInformationType;

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
        setRelatedMaterial(channel, serviceInformationType, IMAGE_INTENDED_USE_1);
        setRelatedMaterial(channel, serviceInformationType, IMAGE_INTENDED_USE_2);
        return serviceInformationType;
    }
}
