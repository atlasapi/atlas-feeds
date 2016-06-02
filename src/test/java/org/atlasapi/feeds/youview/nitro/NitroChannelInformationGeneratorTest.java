package org.atlasapi.feeds.youview.nitro;

import org.atlasapi.feeds.tvanytime.ChannelElementGenerator;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Image;
import org.atlasapi.media.entity.Publisher;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.youview.refdata.schemas._2011_07_06.ExtendedServiceInformationType;
import org.junit.Test;
import tva.metadata._2010.RelatedMaterialType;
import tva.metadata._2010.ServiceInformationType;
import tva.metadata._2010.SynopsisType;
import tva.metadata.extended._2010.ExtendedRelatedMaterialType;
import tva.metadata.extended._2010.StillImageContentAttributesType;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

public class NitroChannelInformationGeneratorTest {

    private final ChannelElementGenerator generator = new NitroChannelInformationGenerator();

    @Test
    public void testServiceInformationIsGeneratedFromChannel() {
        Channel channel = createChannel();

        ExtendedServiceInformationType generated = (ExtendedServiceInformationType) generator.generate(channel);

        assertEquals(channel.getTitle(), generated.getName().get(0).getValue());
        assertEquals("BBC", generated.getOwner().get(0));
        assertEquals("dvb://233a..10c0", generated.getServiceURL());

        assertEquals(channel.getTitle(), generated.getServiceDescription().get(0).getValue());
        assertEquals("urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3", generated.getServiceGenre().get(0).getHref());
        assertEquals("main", generated.getServiceGenre().get(0).getType());
        assertEquals("other", generated.getServiceGenre().get(1).getType());
        assertEquals(
                "http://refdata.youview.com/mpeg7cs/YouViewServiceTypeCS/2010-10-25#linear_service-broadcast_channel",
                generated.getServiceGenre().get(1).getHref()
        );
        assertEquals(
                "http://refdata.youview.com/mpeg7cs/YouViewContentProviderCS/2010-09-22#GBR-bbc",
                generated.getServiceGenre().get(2).getHref()
        );

        ExtendedRelatedMaterialType relatedMaterial =
                (ExtendedRelatedMaterialType) generated.getRelatedMaterial().get(0);
        assertEquals(
                "urn:tva:metadata:cs:HowRelatedCS:2010:19",
                relatedMaterial.getHowRelated().getHref()
        );
        assertEquals("urn:mpeg:mpeg7:cs:FileFormatCS2001:1", relatedMaterial.getFormat().getHref());

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

        assertEquals(generated.getOtherIdentifier().get(0).getAuthority(), "applicationPublisher.youview.com");
        assertEquals(generated.getOtherIdentifier().get(0).getValue(), "uk.co.bbc");
    }

    @Test
    public void testServiceInformationIsGeneratedWithInteractiveFromChannel() {
        Channel channel = createChannel();
        channel.setInteractive(true);

        ExtendedServiceInformationType generated = (ExtendedServiceInformationType) generator.generate(channel);
        RelatedMaterialType interactiveRelatedMaterial = generated.getRelatedMaterial().get(2);
        assertThat(interactiveRelatedMaterial.getHowRelated().getHref(), is("urn:tva:metadata:cs:HowRelatedCS:2010:10.5"));
        assertThat(interactiveRelatedMaterial.getFormat().getHref(), is("http://refdata.youview.com/mpeg7cs/YouViewIdentifierTypeCS/2014-09-25#groupId.application.linearEnhancement"));
        assertThat(interactiveRelatedMaterial.getMediaLocator().getMediaUri(), is("crid://bbc.co.uk/iplayer/flash_player/1"));
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
        image.setWidth(1000);
        return Channel.builder()
                .withBroadcaster(Publisher.BBC)
                .withUri("http://nitro.bbc.co.uk/services/channel_233a_10c0")
                .withAliases(ImmutableSet.of(new Alias("bbc:service:locator", "dvb://233a..10c0")))
                .withImage(image)
                .withTitle("channel")
                .build();
    }
}