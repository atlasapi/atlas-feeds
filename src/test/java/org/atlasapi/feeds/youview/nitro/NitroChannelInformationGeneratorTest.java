package org.atlasapi.feeds.youview.nitro;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import org.atlasapi.feeds.tvanytime.ChannelElementGenerator;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Image;
import org.atlasapi.media.entity.Publisher;

import com.google.common.collect.Iterables;
import com.youview.refdata.schemas._2011_07_06.ExtendedServiceInformationType;
import com.youview.refdata.schemas._2011_07_06.ExtendedTargetingInformationType;
import org.junit.Test;
import tva.metadata._2010.ServiceInformationType;
import tva.metadata._2010.SynopsisType;
import tva.metadata.extended._2010.ExtendedRelatedMaterialType;
import tva.metadata.extended._2010.StillImageContentAttributesType;

import javax.annotation.Nullable;

import static org.junit.Assert.assertEquals;

public class NitroChannelInformationGeneratorTest {

    private final ChannelElementGenerator generator = new NitroChannelInformationGenerator();

    @Test
    public void testServiceInformationIsGeneratedFromChannel() {
        Channel channel = createChannel();

        ExtendedServiceInformationType generated = (ExtendedServiceInformationType) generator.generate(channel);

        assertEquals(generated.getName().get(0).getValue(), channel.getTitle());
        assertEquals(generated.getOwner().get(0), "BBC");
        assertEquals(generated.getServiceURL(), "dvb://233a..10c0");
        assertEquals(generated.getServiceDescription().get(0).getValue(), channel.getTitle());
        assertEquals(generated.getServiceGenre().get(0).getHref(), "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3");
        assertEquals(generated.getServiceGenre().get(0).getType(), "main");
        assertEquals(generated.getServiceGenre().get(1).getType(), "other");
        assertEquals(generated.getServiceGenre().get(1).getHref(), "http://refdata.youview.com/mpeg7cs/YouViewServiceTypeCS/2010-10-25#linear_service-broadcast_channel");
        assertEquals(generated.getServiceGenre().get(2).getHref(), "http://refdata.youview.com/mpeg7cs/YouViewContentProviderCS/2010-09-22#GBR-bbc");
        ExtendedRelatedMaterialType relatedMaterial = (ExtendedRelatedMaterialType) generated.getRelatedMaterial().get(0);
        assertEquals(relatedMaterial.getHowRelated().getHref(), "urn:tva:metadata:cs:HowRelatedCS:2010:19");
        assertEquals(relatedMaterial.getFormat().getHref(), "urn:mpeg:mpeg7:cs:FileFormatCS2001:1");
        Image image = Iterables.getFirst(Iterables.filter(channel.getImages(), new Predicate<Image>() {
            @Override
            public boolean apply(@Nullable Image image) {
                return image.getCanonicalUri().startsWith("imageuri");
            }
        }), null);
        assertEquals(
                relatedMaterial.getMediaLocator().getMediaUri(),
                "http://users-images-atlas.metabroadcast.com/?source=http://imageuri&profile=monocrop&resize=512x1000"
        );
        assertEquals(relatedMaterial.getPromotionalText().get(0).getValue(), channel.getTitle());
        StillImageContentAttributesType contentAttributesType = (StillImageContentAttributesType) relatedMaterial.getContentProperties()
                .getContentAttributes()
                .get(0);
        assertEquals(contentAttributesType.getHeight(), image.getHeight());
        assertEquals(contentAttributesType.getWidth(), image.getWidth());
        assertEquals(contentAttributesType.getIntendedUse().get(0).getHref(), "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#role-primary");
        assertEquals(contentAttributesType.getIntendedUse().get(1).getHref(), "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-ident");

        ExtendedRelatedMaterialType relatedMaterial2 = (ExtendedRelatedMaterialType) generated.getRelatedMaterial().get(1);
        assertEquals(relatedMaterial2.getHowRelated().getHref(), "urn:tva:metadata:cs:HowRelatedCS:2010:19");
        assertEquals(relatedMaterial2.getFormat().getHref(), "urn:mpeg:mpeg7:cs:FileFormatCS2001:1");
        Image image2 = Iterables.getFirst(Iterables.filter(channel.getImages(), new Predicate<Image>() {
            @Override
            public boolean apply(@Nullable Image image) {
                return image.getCanonicalUri().startsWith("http://www.bbc.co.uk");
            }
        }), null);
        assertEquals(relatedMaterial2.getMediaLocator().getMediaUri(),  "http://users-images-atlas.metabroadcast.com/?source=http://www.bbc.co.uk/iplayer/images/youview/bbc_iplayer.png&profile=monocrop&resize=1024x169");
        assertEquals(relatedMaterial2.getPromotionalText().get(0).getValue(), channel.getTitle());
        StillImageContentAttributesType contentAttributesType2 = (StillImageContentAttributesType) relatedMaterial2.getContentProperties()
                .getContentAttributes()
                .get(0);
        assertEquals(contentAttributesType2.getHeight(), image2.getHeight());
        assertEquals(contentAttributesType2.getWidth(), image2.getWidth());
        assertEquals(contentAttributesType2.getIntendedUse().get(0).getHref(), "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#role-primary");
        assertEquals(contentAttributesType2.getIntendedUse().get(1).getHref(), "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-dog");

        assertEquals(generated.getOtherIdentifier().get(0).getAuthority(), "applicationPublisher.youview.com");
        assertEquals(generated.getOtherIdentifier().get(0).getValue(), "uk.co.bbc");
    }

    @Test
    public void testShortDescriptionIsGenerated() {
        Channel channel = createChannel();
        channel.addAlias(new Alias("bbc:service:name:short", "BBC"));

        ServiceInformationType generated = generator.generate(channel);

        SynopsisType synopsisType = generated.getServiceDescription().get(1);
        assertEquals(synopsisType.getValue(), "BBC");
    }

    private Channel createChannel() {
        Image image = new Image("imageuri");
        image.setHeight(1000);
        image.setWidth(512);
        Image image2 = new Image("http://www.bbc.co.uk/iplayer/images/youview/bbc_iplayer.png");
        image2.setHeight(169);
        image2.setWidth(1024);
        image2.setAliases(
                ImmutableSet.of(
                        new Alias("bbc:imageType", "dog")
                )
        );
        return Channel.builder()
                .withBroadcaster(Publisher.BBC)
                .withUri("dvb://233a..10c0")
                .withImage(image)
                .withImage(image2)
                .withTitle("channel")
                .build();
    }

    private Channel createParentChannel() {
        return Channel.builder()
                .withTitle("BBC")
                .build();
    }

}