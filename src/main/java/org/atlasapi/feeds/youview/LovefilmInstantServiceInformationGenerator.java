package org.atlasapi.feeds.youview;

import java.util.List;

import org.atlasapi.feeds.tvanytime.ServiceInformationGenerator;

import com.google.common.collect.ImmutableList;
import com.youview.refdata.schemas._2011_07_06.ExtendedServiceInformationType;
import com.youview.refdata.schemas._2011_07_06.ExtendedTargetingInformationType;
import com.youview.refdata.schemas._2011_07_06.TargetPlaceType;

import tva.metadata._2010.ControlledTermType;
import tva.metadata._2010.GenreType;
import tva.metadata._2010.RelatedMaterialType;
import tva.metadata._2010.ServiceInformationNameType;
import tva.metadata._2010.ServiceInformationType;
import tva.metadata._2010.SynopsisType;
import tva.metadata.extended._2010.ContentAttributesType;
import tva.metadata.extended._2010.ContentPropertiesType;
import tva.metadata.extended._2010.ExtendedRelatedMaterialType;
import tva.metadata.extended._2010.StillImageContentAttributesType;
import tva.metadata.extended._2010.TargetingInformationType;
import tva.mpeg7._2008.MediaLocatorType;
import tva.mpeg7._2008.TextualType;

public class LovefilmInstantServiceInformationGenerator implements ServiceInformationGenerator {
    
    private static final String GENRE_TYPE_OTHER = "other";
    private static final String GENRE_TYPE_MAIN = "main";

    @Override
    public ServiceInformationType generate() {
        ExtendedServiceInformationType serviceInfo = new ExtendedServiceInformationType();
        
        serviceInfo.setServiceId("http://lovefilm.com/OnDemand");
        serviceInfo.getName().add(generateName());
        serviceInfo.getOwner().add("LOVEFiLM UK Limited");
        serviceInfo.setServiceURL("crid://www.lovefilm.com/application/1");
        serviceInfo.getServiceDescription().add(generateServiceDescription());
        serviceInfo.setLang("en");
        serviceInfo.getServiceGenre().addAll(generateGenres());
        serviceInfo.setTargetingInformation(generateTargetingInfo());
        serviceInfo.getRelatedMaterial().add(generateRelatedMaterial());

        
        return serviceInfo;
    }

    private RelatedMaterialType generateRelatedMaterial() {
        ExtendedRelatedMaterialType relatedMaterial = new ExtendedRelatedMaterialType();
        
        ControlledTermType howRelated = new ControlledTermType();
        howRelated.setHref("urn:tva:metadata:cs:HowRelatedCS:2010:19");
        relatedMaterial.setHowRelated(howRelated);
        
        MediaLocatorType mediaLocator = new MediaLocatorType();
        mediaLocator.setMediaUri("http://g-ec2.images-amazon.com/images/G/01/AIV/youview/hires._V388117660_.png");
        relatedMaterial.setMediaLocator(mediaLocator);
        
        TextualType promoText = new TextualType();
        promoText.setValue("LOVEFiLM Instant");
        relatedMaterial.getPromotionalText().add(promoText);
        
        ContentPropertiesType contentProps = new ContentPropertiesType();
        contentProps.getContentAttributes().add(generateImageAttrs());
        relatedMaterial.setContentProperties(contentProps);
        
        return relatedMaterial;
    }

    private ContentAttributesType generateImageAttrs() {
        StillImageContentAttributesType imageAttrs = new StillImageContentAttributesType();
        imageAttrs.setWidth(1024);
        imageAttrs.setHeight(576);
        ControlledTermType intendedUse = new ControlledTermType();
        intendedUse.setHref("http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-ident");
        imageAttrs.getIntendedUse().add(intendedUse );
        return imageAttrs;
    }

    private TargetingInformationType generateTargetingInfo() {
        ExtendedTargetingInformationType targetingInfo = new ExtendedTargetingInformationType();
        TargetPlaceType targetPlace = new TargetPlaceType();
        targetPlace.setExclusive(true);
        targetPlace.setHref("http://refdata.youview.com/mpeg7cs/YouViewTargetRegionCS/2010-10-26#GBR");
        targetingInfo.getTargetPlace().add(targetPlace);
        return targetingInfo;
    }

    private List<GenreType> generateGenres() {
        GenreType film = new GenreType();
        
        film.setType(GENRE_TYPE_MAIN);
        film.setHref("urn:tva:metadata:cs:OriginationCS:2005:5.7");
        GenreType tv = new GenreType();
        tv.setType(GENRE_TYPE_MAIN);
        tv.setHref("urn:tva:metadata:cs:OriginationCS:2005:5.8");
        GenreType av = new GenreType();
        av.setType(GENRE_TYPE_OTHER);
        av.setHref("urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3");
        GenreType onDemandService = new GenreType();
        onDemandService.setType(GENRE_TYPE_OTHER);
        onDemandService.setHref("http://refdata.youview.com/mpeg7cs/YouViewServiceTypeCS/2010-10-25#on_demand_service ");
        GenreType lovefilm = new GenreType();
        lovefilm.setType(GENRE_TYPE_OTHER);
        lovefilm.setHref("http://refdata.youview.com/mpeg7cs/YouViewContentProviderCS/2010-09-22#GBR-lovefilm");
        
        return ImmutableList.of(film, tv, av, onDemandService, lovefilm);
    }

    private SynopsisType generateServiceDescription() {
        SynopsisType synopsis = new SynopsisType();
        synopsis.setValue("LOVEFiLM, an Amazon Company");
        return synopsis;
    }

    private ServiceInformationNameType generateName() {
        ServiceInformationNameType name = new ServiceInformationNameType();
        name.setValue("LOVEFiLM Instant");
        return name;
    }
}
