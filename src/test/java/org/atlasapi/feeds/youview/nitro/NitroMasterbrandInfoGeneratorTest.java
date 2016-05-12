package org.atlasapi.feeds.youview.nitro;

import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Image;
import org.atlasapi.media.entity.Publisher;

import com.google.common.collect.Iterables;
import com.youview.refdata.schemas._2011_07_06.ExtendedServiceInformationType;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import tva.metadata.extended._2010.ExtendedRelatedMaterialType;
import tva.metadata.extended._2010.StillImageContentAttributesType;

import static org.junit.Assert.assertEquals;

@RunWith(MockitoJUnitRunner.class)
public class NitroMasterbrandInfoGeneratorTest {

    private NitroMasterbrandInfoGenerator generator = new NitroMasterbrandInfoGenerator();

    @Test
    public void testCorrectGenerationOfMasterbrand() {
        Channel channel = createChannel();

        ExtendedServiceInformationType generated = (ExtendedServiceInformationType) generator.generate(channel);

        assertEquals(generated.getName().get(0).getValue(), channel.getTitle());
        assertEquals(generated.getOwner().get(0), "BBC");
        assertEquals(generated.getServiceDescription().get(0).getValue(), channel.getTitle());
        assertEquals(generated.getServiceGenre().get(0).getHref(), "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3");
        assertEquals(generated.getServiceGenre().get(0).getType(), "main");
        assertEquals(generated.getServiceGenre().get(1).getType(), "other");
        assertEquals(generated.getServiceGenre().get(1).getHref(), "http://refdata.youview.com/mpeg7cs/YouViewServiceTypeCS/2010-10-25#owning_service");
        assertEquals(generated.getServiceGenre().get(2).getHref(), "http://refdata.youview.com/mpeg7cs/YouViewContentProviderCS/2010-09-22#GBR-bbc");
        ExtendedRelatedMaterialType relatedMaterial = (ExtendedRelatedMaterialType) generated.getRelatedMaterial().get(0);
        assertEquals(relatedMaterial.getHowRelated().getHref(), "urn:tva:metadata:cs:HowRelatedCS:2010:19");
        assertEquals(relatedMaterial.getFormat().getHref(), "urn:mpeg:mpeg7:cs:FileFormatCS2001:1");
        Image image = Iterables.getOnlyElement(channel.getImages());
        assertEquals(relatedMaterial.getMediaLocator().getMediaUri(),  image.getCanonicalUri());
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
        Image image2 = Iterables.getOnlyElement(channel.getImages());
        assertEquals(relatedMaterial2.getMediaLocator().getMediaUri(),  image.getCanonicalUri());
        assertEquals(relatedMaterial2.getPromotionalText().get(0).getValue(), channel.getTitle());
        StillImageContentAttributesType contentAttributesType2 = (StillImageContentAttributesType) relatedMaterial2.getContentProperties()
                .getContentAttributes()
                .get(0);
        assertEquals(contentAttributesType2.getHeight(), image2.getHeight());
        assertEquals(contentAttributesType2.getWidth(), image2.getWidth());
        assertEquals(contentAttributesType2.getIntendedUse().get(0).getHref(), "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#role-primary");
        assertEquals(contentAttributesType2.getIntendedUse().get(1).getHref(), "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-dog");
    }

    private Channel createChannel() {
        Image image = new Image("imageuri");
        image.setHeight(1000);
        image.setWidth(1000);
        return Channel.builder()
                .withBroadcaster(Publisher.BBC)
                .withUri("canonical")
                .withImage(image)
                .withTitle("channel")
                .build();
    }
}