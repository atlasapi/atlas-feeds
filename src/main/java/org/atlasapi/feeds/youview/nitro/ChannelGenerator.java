package org.atlasapi.feeds.youview.nitro;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Image;
import org.atlasapi.media.entity.Publisher;

import org.atlasapi.resizer.HttpResizerClient;
import org.atlasapi.resizer.ImageSize;
import org.atlasapi.resizer.ResizerClient;
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

import javax.annotation.Nullable;

public abstract class ChannelGenerator {

    private final static String MAIN_GENRE_TYPE = "main";
    private final static String OTHER_GENRE_TYPE = "other";
    private final static String MAIN_GENRE_HREF = "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3";
    private final static String IMAGE_INTENDED_USE_MAIN = "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#role-primary";
    private final static String HOW_RELATED = "urn:tva:metadata:cs:HowRelatedCS:2010:19";
    private final static String FORMAT = "urn:mpeg:mpeg7:cs:FileFormatCS2001:1";
    private final static String INTERACTIVE_FORMAT = "http://refdata.youview.com/mpeg7cs/YouViewIdentifierTypeCS/2014-09-25#groupId.application.linearEnhancement";
    private final static String INTERACTIVE_MEDIA_LOCATOR_URI = "crid://bbc.co.uk/iplayer/flash_player/1";
    private final static String INTERACTIVE_HOW_RELATED = "urn:tva:metadata:cs:HowRelatedCS:2010:10.5";
    private static final String BBC_IMAGE_TYPE = "bbc:imageType";
    private static final String OVERRIDE = "override";

    protected final static String IMAGE_INTENDED_USE_1 = "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-ident";
    protected final static String IMAGE_INTENDED_USE_2 = "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-dog";

    public static final String RESIZER_FORMAT_STRING = "http://users-images-atlas.metabroadcast.com/?source=%s&profile=monocrop&resize=%dx%d";
    public static final Alias IMAGE_USE_1_ALIAS = new Alias("bbc:imageType", "ident");
    public static final Alias IMAGE_USE_1_NITRO_ALIAS = new Alias("bbc:nitro:type", "ident");
    public static final Alias IMAGE_USE_2_ALIAS = new Alias("bbc:imageType", "dog");

    protected final HttpResizerClient resizerClient = new HttpResizerClient(new NetHttpTransport());

    abstract void setRelatedMaterial(Channel channel, ServiceInformationType serviceInformationType);

    protected Optional<Image> getBbcImageByAlias(Channel channel, final Alias alias1, final Alias alias2) {
        Optional<Image> image = FluentIterable.from(channel.getImages())
                .firstMatch(image1 -> image1.getAliases() != null &&
                        (image1.getAliases().contains(alias1) || image1.getAliases().contains(alias2)));

        return image;
    }

    protected void setRelatedMaterialForInteractive(ServiceInformationType serviceInformationType) {
        ExtendedRelatedMaterialType relatedMaterial = new ExtendedRelatedMaterialType();

        ControlledTermType howRelated = new ControlledTermType();
        howRelated.setHref(INTERACTIVE_HOW_RELATED);
        relatedMaterial.setHowRelated(howRelated);

        ControlledTermType format = new ControlledTermType();
        format.setHref(INTERACTIVE_FORMAT);
        relatedMaterial.setFormat(format);

        setMediaLocatorForInteractive(relatedMaterial);

        serviceInformationType.getRelatedMaterial().add(relatedMaterial);
    }

    protected void setContentProperties(Image image, ExtendedRelatedMaterialType relatedMaterialType,
                                        String imageIntendedUse) {
        ContentPropertiesType contentProperties = new ContentPropertiesType();
        StillImageContentAttributesType contentAttributes = new StillImageContentAttributesType();
        contentAttributes.setHeight(
                image.getHeight() != null && image.getHeight() != 0 ?
                        image.getHeight() : null
        );
        contentAttributes.setWidth(
                image.getWidth() != null && image.getWidth() != 0 ?
                        image.getWidth() : null
        );
        ControlledTermType mainIntendedUse = new ControlledTermType();
        mainIntendedUse.setHref(IMAGE_INTENDED_USE_MAIN);
        contentAttributes.getIntendedUse().add(mainIntendedUse);
        ControlledTermType intendedUse = new ControlledTermType();
        intendedUse.setHref(imageIntendedUse);
        contentAttributes.getIntendedUse().add(intendedUse);
        contentProperties.getContentAttributes().add(contentAttributes);
        relatedMaterialType.setContentProperties(contentProperties);
    }


    protected void setPromotionalText(Channel channel, ExtendedRelatedMaterialType relatedMaterial) {
        TextualType promotionalText = new TextualType();
        promotionalText.setValue(channel.getTitle());
        relatedMaterial.getPromotionalText().add(promotionalText);
    }

    protected void setMediaLocator(Image image, ExtendedRelatedMaterialType relatedMaterial) {
        MediaLocatorType mediaLocator = new MediaLocatorType();
        String imgUrl = image.getCanonicalUri();
        mediaLocator.setMediaUri(imgUrl);
        relatedMaterial.setMediaLocator(mediaLocator);
    }

    protected boolean isOverrideImage(Image image) {
        for (Alias alias : image.getAliases()) {
            if (BBC_IMAGE_TYPE.equals(alias.getNamespace()) &&
                    OVERRIDE.equals(alias.getValue())) {
                return true;
            }
        }
        return false;
    }

    protected void setMediaLocatorForInteractive(ExtendedRelatedMaterialType relatedMaterial) {
        MediaLocatorType mediaLocator = new MediaLocatorType();
        mediaLocator.setMediaUri(INTERACTIVE_MEDIA_LOCATOR_URI);
        relatedMaterial.setMediaLocator(mediaLocator);
    }

    protected void setDescriptions(Channel channel, ServiceInformationType serviceInformationType) {
        SynopsisType longDescription = new SynopsisType();
        longDescription.setLength(SynopsisLengthType.LONG);
        longDescription.setValue(channel.getTitle());
        serviceInformationType.getServiceDescription().add(longDescription);
    }

    protected void setNameAndOwner(Channel channel, ServiceInformationType serviceInformationType) {
        ServiceInformationNameType name = new ServiceInformationNameType();
        name.setValue(channel.getTitle());
        Publisher broadcaster = channel.getBroadcaster();
        if (broadcaster != null) {
            serviceInformationType.getOwner().add(broadcaster.title());
        }
        serviceInformationType.getName().add(name);
    }

    protected void setGenres(ServiceInformationType serviceInformationType, String otherGenreHref1,
                             String otherGenreHref2) {
        GenreType mainGenre = new GenreType();
        mainGenre.setType(MAIN_GENRE_TYPE);
        mainGenre.setHref(MAIN_GENRE_HREF);
        serviceInformationType.getServiceGenre().add(mainGenre);
        GenreType otherGengre1 = new GenreType();
        otherGengre1.setType(OTHER_GENRE_TYPE);
        otherGengre1.setHref(otherGenreHref1);
        serviceInformationType.getServiceGenre().add(otherGengre1);
        GenreType otherGengre2 = new GenreType();
        otherGengre2.setType(OTHER_GENRE_TYPE);
        otherGengre2.setHref(otherGenreHref2);
        serviceInformationType.getServiceGenre().add(otherGengre2);
    }

    protected ExtendedRelatedMaterialType createRelatedMaterial(
            Channel channel,
            String imageIntendedUse,
            Image image
    ) {
        ExtendedRelatedMaterialType relatedMaterial = new ExtendedRelatedMaterialType();
        ControlledTermType howRelated = new ControlledTermType();
        howRelated.setHref(HOW_RELATED);
        relatedMaterial.setHowRelated(howRelated);
        ControlledTermType format = new ControlledTermType();
        format.setHref(FORMAT);
        relatedMaterial.setFormat(format);
        setMediaLocator(image, relatedMaterial);
        setPromotionalText(channel, relatedMaterial);
        setContentProperties(image, relatedMaterial, imageIntendedUse);
        return relatedMaterial;
    }

    protected Image resizeImage(Image image) {
        if (!image.getCanonicalUri().startsWith("http")) {
            image.setCanonicalUri(String.format("http://%s", image.getCanonicalUri()));
        }
        image.setCanonicalUri(
                String.format(
                        RESIZER_FORMAT_STRING,
                        image.getCanonicalUri(),
                        image.getWidth(),
                        image.getHeight()
                )
        );
        ImageSize imageDimensions = resizerClient.getImageDimensions(image.getCanonicalUri());
        image.setWidth(imageDimensions.getWidth());
        image.setHeight(imageDimensions.getHeight());
        return image;
    }
}
