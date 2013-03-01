package org.atlasapi.feeds.youview;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.atlasapi.feeds.tvanytime.GroupInformationGenerator;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Specialization;

import tva.metadata._2010.BaseProgramGroupTypeType;
import tva.metadata._2010.BasicContentDescriptionType;
import tva.metadata._2010.ControlledTermType;
import tva.metadata._2010.CreditsItemType;
import tva.metadata._2010.CreditsListType;
import tva.metadata._2010.GenreType;
import tva.metadata._2010.GroupInformationType;
import tva.metadata._2010.MemberOfType;
import tva.metadata._2010.ProgramGroupTypeType;
import tva.metadata._2010.RelatedMaterialType;
import tva.metadata._2010.SynopsisLengthType;
import tva.metadata._2010.SynopsisType;
import tva.metadata.extended._2010.ContentPropertiesType;
import tva.metadata.extended._2010.ExtendedRelatedMaterialType;
import tva.metadata.extended._2010.StillImageContentAttributesType;
import tva.mpeg7._2008.ExtendedLanguageType;
import tva.mpeg7._2008.MediaLocatorType;
import tva.mpeg7._2008.NameComponentType;
import tva.mpeg7._2008.PersonNameType;
import tva.mpeg7._2008.TitleType;

import com.google.inject.internal.ImmutableMap;
import com.google.inject.internal.Lists;

public class LovefilmGroupInformationGenerator implements GroupInformationGenerator {

    private static final String YOUVIEW_CREDIT_ROLE = "urn:tva:metadata:cs:TVARoleCS:2010:V106";
    private static final String YOUVIEW_IMAGE_FORMAT = "urn:mpeg:mpeg7:cs:FileFormatCS:2001:1";
    private static final String YOUVIEW_IMAGE_HOW_RELATED = "urn:tva:metadata:cs:HowRelatedCS:2010:19";
    private static final String YOUVIEW_IMAGE_ATTRIBUTE_PROD_STILL = "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-production_still";
    private static final String YOUVIEW_IMAGE_ATTRIBUTE_PRIMARY_ROLE = "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#role-primary";
    private static final int IMAGE_HEIGHT = 360;
    private static final int IMAGE_WIDTH = 640;
    private static final String GROUP_TYPE_PROGRAMCONCEPT = "programConcept";
    private static final String GROUP_TYPE_SERIES = "series";
    private static final String GROUP_TYPE_SHOW = "show";
    private static final String LANGUAGE_TYPE_ORIGINAL = "original";
    private static final String GENRE_TYPE_MAIN = "main";
    private static final String GENRE_TYPE_OTHER = "other";
    private static final String LOVEFILM_URL = "http://lovefilm.com";
    private static final String LOVEFILM_PRODUCT_CRID_PREFIX = "crid://lovefilm.com/product/";
    private static final String TITLE_TYPE_MAIN = "main";
    private static final String LOVEFILM_URI_PATTERN = "http:\\/\\/lovefilm\\.com\\/[a-z]*\\/";
    private static final String LOVEFILM_MEDIATYPE_GENRE_VIDEO = "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3";
    private static final Map<Specialization, String> YOUVIEW_SPECIALIZATION_GENRE_MAPPING = ImmutableMap.<Specialization, String>builder()
        .put(Specialization.FILM, "urn:tva:metadata:cs:OriginationCS:2005:5.7")
        .put(Specialization.TV, "urn:tva:metadata:cs:OriginationCS:2005:5.8")
        .build();
    
    private static final Map<String, String> YOUVIEW_ATLAS_GENRE_MAPPING = ImmutableMap.<String, String>builder()
        .build();   
    
    @Override
    public GroupInformationType generate(Film film) {
        GroupInformationType groupInfo = generateWithCommonFields(film);
        
        groupInfo.setGroupType(generateGroupType(GROUP_TYPE_PROGRAMCONCEPT));
        groupInfo.setServiceIDRef(LOVEFILM_URL);
        
        return groupInfo;
    }
    
    // TODO cope with top-level series?
    
    @Override
    public GroupInformationType generate(Episode episode) {
        GroupInformationType groupInfo = generateWithCommonFields(episode);
        
        groupInfo.setGroupType(generateGroupType(GROUP_TYPE_PROGRAMCONCEPT));
        
        MemberOfType memberOf = new MemberOfType();
        memberOf.setCrid(LOVEFILM_PRODUCT_CRID_PREFIX + getId(episode.getSeriesRef().getUri()));
        memberOf.setIndex(Long.valueOf(episode.getEpisodeNumber()));
        groupInfo.getMemberOf().add(memberOf);
        
        return groupInfo;  
    }
    
    @Override
    public GroupInformationType generate(Series series) {
        GroupInformationType groupInfo = generateWithCommonFields(series);
        
        groupInfo.setGroupType(generateGroupType(GROUP_TYPE_SERIES));
        groupInfo.setOrdered(true);
        MemberOfType memberOf = new MemberOfType();
        memberOf.setCrid(LOVEFILM_PRODUCT_CRID_PREFIX + getId(series.getParent().getUri()));
        memberOf.setIndex(Long.valueOf(series.getSeriesNumber()));
        groupInfo.getMemberOf().add(memberOf);
        
        return groupInfo;
    }
    
    @Override
    public GroupInformationType generate(Brand brand) {
        GroupInformationType groupInfo = generateWithCommonFields(brand);
        
        groupInfo.setGroupType(generateGroupType(GROUP_TYPE_SHOW));
        groupInfo.setOrdered(true);
        groupInfo.setServiceIDRef(LOVEFILM_URL);
        
        return groupInfo;
    }
    
    GroupInformationType generateWithCommonFields(Content content) {
        GroupInformationType groupInfo = new GroupInformationType();
        
        groupInfo.setGroupId(LOVEFILM_PRODUCT_CRID_PREFIX + getId(content.getCanonicalUri()));
        // this needs tweaking depending on type
        groupInfo.setBasicDescription(generateBasicDescription(content));
        
        return groupInfo;
    }
    
    // TODO extract into common utils class
    private String getId(String uri) {
        return uri.replaceAll(LOVEFILM_URI_PATTERN, "");
    }

    private BaseProgramGroupTypeType generateGroupType(String groupType) {
        ProgramGroupTypeType type = new ProgramGroupTypeType();
        type.setValue(groupType);
        return type;
    }

    private BasicContentDescriptionType generateBasicDescription(Content content) {
        BasicContentDescriptionType basicDescription = new BasicContentDescriptionType();
        
        basicDescription.getTitle().add(generateTitle(content));
        basicDescription.getSynopsis().add(generateSynopsis(content));
        basicDescription.getGenre().addAll(generateGenres(content));
        basicDescription.getGenre().add(generateGenreFromSpecialization(content));
        basicDescription.getGenre().add(generateGenreFromMediaType(content));
        basicDescription.getLanguage().addAll(generateLanguage(content));
        basicDescription.setCreditsList(generateCreditsList(content));
        basicDescription.getRelatedMaterial().add(generateRelatedMaterial(content));
        
        return basicDescription;
    }

    private RelatedMaterialType generateRelatedMaterial(Content content) {
        ExtendedRelatedMaterialType relatedMaterial = new ExtendedRelatedMaterialType();
        
        relatedMaterial.setHowRelated(generateHowRelated());
        relatedMaterial.setFormat(generateFormat());
        relatedMaterial.setMediaLocator(generateMediaLocator(content));
        relatedMaterial.setContentProperties(generateContentProperties());
        
        return relatedMaterial;
    }

    private ContentPropertiesType generateContentProperties() {
        ContentPropertiesType contentProperties = new ContentPropertiesType();
        StillImageContentAttributesType attributes = new StillImageContentAttributesType();
        
        attributes.setWidth(IMAGE_WIDTH);
        attributes.setHeight(IMAGE_HEIGHT);
        
        ControlledTermType primaryRole = new ControlledTermType();
        primaryRole.setHref(YOUVIEW_IMAGE_ATTRIBUTE_PRIMARY_ROLE);
        attributes.getIntendedUse().add(primaryRole);
        
        ControlledTermType productionStill = new ControlledTermType();
        productionStill.setHref(YOUVIEW_IMAGE_ATTRIBUTE_PROD_STILL);
        attributes.getIntendedUse().add(productionStill);
        
        contentProperties.getContentAttributes().add(attributes);
        return contentProperties;
    }

    private ControlledTermType generateFormat() {
        ControlledTermType controlledTerm = new ControlledTermType();
        controlledTerm.setHref(YOUVIEW_IMAGE_FORMAT);
        return controlledTerm;
    }

    private ControlledTermType generateHowRelated() {
        ControlledTermType controlledTerm = new ControlledTermType();
        controlledTerm.setHref(YOUVIEW_IMAGE_HOW_RELATED);
        return controlledTerm;
    }

    private MediaLocatorType generateMediaLocator(Content content) {
        MediaLocatorType mediaLocator = new MediaLocatorType();
        mediaLocator.setMediaUri(content.getImage());
        return mediaLocator;
    }

    private CreditsListType generateCreditsList(Content content) {
       CreditsListType creditsList = new CreditsListType();
       
       for (CrewMember person : content.people()) {
           CreditsItemType credit = new CreditsItemType();
           // TODO TBC that this is correct role 
           credit.setRole(YOUVIEW_CREDIT_ROLE);
           
           PersonNameType personName = new PersonNameType();
           
           NameComponentType nameComponent = new NameComponentType();
           nameComponent.setValue(person.name());
           
           JAXBElement<NameComponentType> nameElem = new JAXBElement<NameComponentType>(new QName("urn:tva:mpeg7:2008", "GivenName"), NameComponentType.class, nameComponent);
           personName.getGivenNameOrLinkingNameOrFamilyName().add(nameElem);
           
           JAXBElement<PersonNameType> personNameElem = new JAXBElement<PersonNameType>(new QName("urn:tva:metadata:2010", "PersonName"), PersonNameType.class, personName);
           credit.getPersonNameOrPersonNameIDRefOrOrganizationName().add(personNameElem);
           
           creditsList.getCreditsItem().add(credit);
       }
       
       return creditsList;
    }

    private List<ExtendedLanguageType> generateLanguage(Content content) {
        List<ExtendedLanguageType> languages = Lists.newArrayList();
        for (String languageStr : content.getLanguages()) {
            ExtendedLanguageType language = new ExtendedLanguageType();
            language.setType(LANGUAGE_TYPE_ORIGINAL);
            language.setValue(languageStr);
            languages.add(language);
        }
        return languages;
    }

    private Collection<GenreType> generateGenres(Content content) {
        List<GenreType> genres = Lists.newArrayList();
        // TODO uncomment once genre mapping is present
//        for (String genreStr : content.getGenres()) {
//            GenreType genre = new GenreType();
//            genre.setType(GENRE_TYPE_MAIN);
//            genre.setHref(YOUVIEW_ATLAS_GENRE_MAPPING.get(genreStr));
//            genres.add(genre);
//        }
        return genres;
    }

    private GenreType generateGenreFromSpecialization(Content content) {
        GenreType genre = new GenreType();
        genre.setType(GENRE_TYPE_MAIN);
        genre.setHref(YOUVIEW_SPECIALIZATION_GENRE_MAPPING.get(content.getSpecialization()));
        return genre;
    }

    private GenreType generateGenreFromMediaType(Content content) {
       GenreType genre = new GenreType();
       genre.setType(GENRE_TYPE_OTHER);
       if (content.getMediaType().equals(MediaType.VIDEO)) {
           genre.setHref(LOVEFILM_MEDIATYPE_GENRE_VIDEO);
       } else {
           throw new RuntimeException("invalid media type " + content.getMediaType() + " on item " + content.getCanonicalUri());
       }
       return genre;
    }

    private SynopsisType generateSynopsis(Content content) {
        SynopsisType synopsis = new SynopsisType();
        synopsis.setLength(SynopsisLengthType.SHORT);
        synopsis.setValue(content.getDescription());
        return synopsis;
    }

    private TitleType generateTitle(Content content) {
        TitleType title = new TitleType();
        title.getType().add(TITLE_TYPE_MAIN);
        title.setValue(content.getTitle());
        return title;
    }
}
