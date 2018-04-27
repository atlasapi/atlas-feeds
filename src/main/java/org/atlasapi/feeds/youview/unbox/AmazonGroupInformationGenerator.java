package org.atlasapi.feeds.youview.unbox;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.atlasapi.feeds.tvanytime.GroupInformationGenerator;
import org.atlasapi.feeds.youview.genres.GenreMapping;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Image;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Specialization;

import com.metabroadcast.common.text.Truncator;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import tva.mpeg7._2008.UniqueIDType;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.YouViewGeneratorUtils.getAmazonAsin;

public class AmazonGroupInformationGenerator implements GroupInformationGenerator {

    private static final Logger log = LoggerFactory.getLogger(AmazonGroupInformationGenerator.class);

    public static final String GROUP_INFO_SERVICE_ID = "http://amazon.com/services/content_owning/primevideo";
//    private static final int DEFAULT_IMAGE_HEIGHT = 320;
//    private static final int DEFAULT_IMAGE_WIDTH = 240;
    
    private static final String YOUVIEW_CREDIT_ROLE_UNKNOWN = "urn:mpeg:mpeg7:cs:RoleCS:2001:UNKNOWN";
    private static final String YOUVIEW_IMAGE_FORMAT = "urn:mpeg:mpeg7:cs:FileFormatCS:2001:1";
    private static final String YOUVIEW_IMAGE_HOW_RELATED = "urn:tva:metadata:cs:HowRelatedCS:2010:19";
    private static final String YOUVIEW_IMAGE_ATTRIBUTE_PRIMARY_ROLE = "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#role-primary";
    private static final String GROUP_TYPE_PROGRAMCONCEPT = "programConcept";
    private static final String GROUP_TYPE_SERIES = "series";
    private static final String GROUP_TYPE_SHOW = "show";
    private static final String LANGUAGE_TYPE_ORIGINAL = "original";
    public static final String LANGUAGE_UNDEFINED = "und";
    private static final String GENRE_TYPE_MAIN = "main";
    private static final String GENRE_TYPE_OTHER = "other";
    private static final String TITLE_TYPE_MAIN = "main";
    private static final String TITLE_TYPE_SECONDARY = "secondary";
    private static final String LOVEFILM_MEDIATYPE_GENRE_VIDEO = "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3";

    public static final String OTHER_IDENTIFIER_AUTHORITY_BRAND = "show.asin.amazon.com";
    public static final String OTHER_IDENTIFIER_AUTHORITY_SERIES = "season.asin.amazon.com";
    public static final String OTHER_IDENTIFIER_AUTHORITY_EPISODE = "episode.asin.amazon.com";
    public static final String OTHER_IDENTIFIER_AUTHORITY_CONTENT_TYPE = "content-type.amazon.com";
    private static final String CONTENT_TYPE_MOVIE = "movie";
    private static final String CONTENT_TYPE_EPISODE = "episode";

    private static final String MBST_BASE_IMAGE_URL = "https://users-images-atlas.metabroadcast.com/?profile=sixteen-nine-blur&source=";
    private static final int MBST_IMAGE_WIDTH = 1024;
    private static final int MBST_IMAGE_HEIGHT = 576;
    
    private static final Map<Specialization, String> YOUVIEW_SPECIALIZATION_GENRE_MAPPING = ImmutableMap.<Specialization, String>builder()
        .put(Specialization.FILM, "urn:tva:metadata:cs:OriginationCS:2005:5.7")
        .put(Specialization.TV, "urn:tva:metadata:cs:OriginationCS:2005:5.8")
        .build();

    // That is relevant to YV not amazon, so it should not be here.
    // But anyway for efficiency lets make it available to other classes.
    public static final Map<SynopsisLengthType, Truncator> YOUVIEW_SYNOPSIS_LENGTH_MAP =
            ImmutableMap.<SynopsisLengthType, Truncator>builder()
                    .put(SynopsisLengthType.SHORT, new Truncator()
                            .omitTrailingPunctuationWhenTruncated()
                            .onlyTruncateAtAWordBoundary()
                            .withOmissionMarker("...")
                            .withMaxLength(90))
                    .put(SynopsisLengthType.MEDIUM, new Truncator()
                            .omitTrailingPunctuationWhenTruncated()
                            .onlyTruncateAtAWordBoundary()
                            .withOmissionMarker("...")
                            .withMaxLength(210))
                    .put(SynopsisLengthType.LONG, new Truncator()
                            .omitTrailingPunctuationWhenTruncated()
                            .onlyTruncateAtAWordBoundary()
                            .withOmissionMarker("...")
                            .withMaxLength(1200))
                    .build();

    private static final List<String> TITLE_PREFIXES = ImmutableList.of("The ", "the ", "A ", "a ", "An ", "an ");
    
    private static final Function<String, GenreType> TO_GENRE = new Function<String, GenreType>() {
        @Override
        public GenreType apply(String input) {
            GenreType genre = new GenreType();
            genre.setType(GENRE_TYPE_MAIN);
            genre.setHref(input);
            return genre;
        }
    };
    private static final String SEASON = "Season ";

    private final IdGenerator idGenerator;
    private final GenreMapping genreMapping;
    //    Pattern episodePattern1 = Pattern.compile("(?i)ep[\\.-]*[isode]*[ -]*[\\d]+") //Ep1, Episode 1 etc.
//    Pattern episodePattern2 = Pattern.compile("(?i)[s|e]+[\\d]+[ \\.-\\/]*[s|e]+[\\d]+") //S05E01 etc
//    Pattern strayDashes = Pattern.compile("^[\\p{Pd} ]+|[\\p{Pd} ]+$") //all dashes

    public AmazonGroupInformationGenerator(IdGenerator idGenerator, GenreMapping genreMapping) {
        this.idGenerator = checkNotNull(idGenerator);
        this.genreMapping = checkNotNull(genreMapping);
    }
    
    @Override
    public GroupInformationType generate(Film film) {
        GroupInformationType groupInfo = generateWithCommonFields(film, null);
        createTitle(film, groupInfo.getBasicDescription());

        groupInfo.setGroupType(generateGroupType(GROUP_TYPE_PROGRAMCONCEPT));
        groupInfo.setServiceIDRef(GROUP_INFO_SERVICE_ID);
        groupInfo.getOtherIdentifier().add(generateOtherIdentifier(OTHER_IDENTIFIER_AUTHORITY_EPISODE, getAmazonAsin(film)));
        groupInfo.getOtherIdentifier().add(generateOtherIdentifier(
                OTHER_IDENTIFIER_AUTHORITY_CONTENT_TYPE,
                CONTENT_TYPE_MOVIE
        ));
        
        return groupInfo;
    }
    
    @Override
    public GroupInformationType generate(Item item, Optional<Series> series, Optional<Brand> brand) {
        GroupInformationType groupInfo = generateWithCommonFields(item, null);
        if (item instanceof Episode) {
            createTitle((Episode) item, groupInfo.getBasicDescription());
        } else {
            createTitle(item, groupInfo.getBasicDescription());
        }

        groupInfo.setGroupType(generateGroupType(GROUP_TYPE_PROGRAMCONCEPT));
        if (series.isPresent()) {
            MemberOfType memberOf = new MemberOfType();
            memberOf.setCrid(idGenerator.generateContentCrid(series.get()));
            if (item instanceof Episode) {
                Episode episode = (Episode) item;
                //index 0 not valid for YV
                if (episode.getEpisodeNumber() != null && episode.getEpisodeNumber() !=0 ) {
                    memberOf.setIndex(Long.valueOf(episode.getEpisodeNumber()));
                }
            }
            groupInfo.getMemberOf().add(memberOf);
        } else if (brand.isPresent()) {
            MemberOfType memberOf = new MemberOfType();
            memberOf.setCrid(idGenerator.generateContentCrid(brand.get()));
            if (item instanceof Episode) {
                Episode episode = (Episode) item;
                //index 0 not valid for YV
                if (episode.getEpisodeNumber() != null && episode.getEpisodeNumber() !=0) {
                    memberOf.setIndex(Long.valueOf(episode.getEpisodeNumber()));
                }
            }
            groupInfo.getMemberOf().add(memberOf);
        }
        groupInfo.getOtherIdentifier().add(generateOtherIdentifier(OTHER_IDENTIFIER_AUTHORITY_EPISODE, getAmazonAsin(item)));
        groupInfo.getOtherIdentifier().add(generateOtherIdentifier(
                OTHER_IDENTIFIER_AUTHORITY_CONTENT_TYPE,
                CONTENT_TYPE_EPISODE //films are handled elsewhere, if they land here is a bug.
        ));
        
        return groupInfo;  
    }
    
    @Override
    public GroupInformationType generate(Series series, Optional<Brand> brand, @Nullable Item firstChild) {
        GroupInformationType groupInfo = generateWithCommonFields(series, firstChild);
        createTitle(series, groupInfo.getBasicDescription());
        
        groupInfo.setGroupType(generateGroupType(GROUP_TYPE_SERIES));
        groupInfo.setOrdered(true);

        if (brand.isPresent()) {
            MemberOfType memberOf = new MemberOfType();
            memberOf.setCrid(idGenerator.generateContentCrid(brand.get()));
            //index 0 not valid for YV
            if (series.getSeriesNumber() != null && series.getSeriesNumber() != 0) {
                memberOf.setIndex(Long.valueOf(series.getSeriesNumber()));
            }
            groupInfo.getMemberOf().add(memberOf);
        } else {
            groupInfo.setServiceIDRef(GROUP_INFO_SERVICE_ID);
        }

        groupInfo.getOtherIdentifier().add(generateOtherIdentifier(OTHER_IDENTIFIER_AUTHORITY_SERIES, getAmazonAsin(series)));
        
        return groupInfo;
    }
    
    @Override
    public GroupInformationType generate(Brand brand, @Nullable Item item) {
        GroupInformationType groupInfo = generateWithCommonFields(brand, item);
        createTitle(brand, groupInfo.getBasicDescription());

        groupInfo.setGroupType(generateGroupType(GROUP_TYPE_SHOW));
        groupInfo.setOrdered(true);
        groupInfo.setServiceIDRef(GROUP_INFO_SERVICE_ID);

        groupInfo.getOtherIdentifier().add(generateOtherIdentifier(OTHER_IDENTIFIER_AUTHORITY_BRAND, getAmazonAsin(brand)));
        
        return groupInfo;
    }
    
    private GroupInformationType generateWithCommonFields(Content content, @Nullable Item item) {
        GroupInformationType groupInfo = new GroupInformationType();
        groupInfo.setGroupId(idGenerator.generateContentCrid(content));
        groupInfo.setBasicDescription(generateBasicDescription(content, item));
        
        return groupInfo;
    }

    private BaseProgramGroupTypeType generateGroupType(String groupType) {
        ProgramGroupTypeType type = new ProgramGroupTypeType();
        type.setValue(groupType);
        return type;
    }

    private BasicContentDescriptionType generateBasicDescription(Content content, @Nullable Item item) {
        BasicContentDescriptionType basicDescription = new BasicContentDescriptionType();

        basicDescription.getSynopsis().addAll(generateSynopses(content));
        basicDescription.getGenre().addAll(generateGenres(content));
        basicDescription.getGenre().add(generateGenreFromSpecialization(content));
        basicDescription.getGenre().add(generateGenreFromMediaType(content));
        basicDescription.getLanguage().addAll(generateLanguage(content));
        basicDescription.setCreditsList(generateCreditsList(content));
        Optional<RelatedMaterialType> relatedMaterial;
        if (content instanceof Series) {
            relatedMaterial = generateRelatedMaterial(item);
        } else if (content instanceof Brand) {
            relatedMaterial = generateRelatedMaterial(item);
        } else {
            relatedMaterial = generateRelatedMaterial(content);
        }
        if (relatedMaterial.isPresent()) {
            basicDescription.getRelatedMaterial().add(relatedMaterial.get());
        }
        
        return basicDescription;
    }

    private void createTitle(Series series, BasicContentDescriptionType basicDescription) {
        // YV Has requested specifically the title to be synthesised, because they don't want
        // the textual titles that are occasionally provided.
        // https://jira-ngyv.youview.co.uk/browse/ECOTEST-282
        if(series.getSeriesNumber() != null ) {
            String sythesizedTitle = SEASON + series.getSeriesNumber();
            basicDescription.getTitle().add(generateTitle(TITLE_TYPE_MAIN, sythesizedTitle));
        }
        else { //fallback
            createTitle((Content) series, basicDescription);
        }
    }

    private void createTitle(Episode episode, BasicContentDescriptionType basicDescription) {
            createTitle((Content) episode, basicDescription);
    }

    private void createTitle(Content content, BasicContentDescriptionType basicDescription) {
        if (content.getTitle() != null) {
            basicDescription.getTitle().add(generateTitle(TITLE_TYPE_MAIN, content.getTitle()));

            String secondaryTitle = generateAlternateTitle(content.getTitle());
            if (!content.getTitle().equals(secondaryTitle)) {
                basicDescription.getTitle().add(generateTitle(TITLE_TYPE_SECONDARY, secondaryTitle));
            }
        }
    }

    private String generateAlternateTitle(String title) {
        for (String prefix : TITLE_PREFIXES) {
            if (title.startsWith(prefix)) {
                return title.replaceFirst(prefix, "").concat(", " + prefix).trim();
            }    
        }
        return title;
    }

    private Optional<RelatedMaterialType> generateRelatedMaterial(@Nullable Content content) {
        if (content == null || Strings.isNullOrEmpty(content.getImage())) {
            return Optional.absent();
        }
        ExtendedRelatedMaterialType relatedMaterial = new ExtendedRelatedMaterialType();
        
        relatedMaterial.setHowRelated(generateHowRelated());
        relatedMaterial.setFormat(generateFormat());
        relatedMaterial.setMediaLocator(generateMediaLocator(content));
        relatedMaterial.setContentProperties(generateContentProperties(content));
        
        return Optional.<RelatedMaterialType>of(relatedMaterial);
    }
    private UniqueIDType generateOtherIdentifier(String authority, String asin) {
        UniqueIDType id = new UniqueIDType();
        id.setAuthority(authority);
        id.setValue(asin);
        return id;
    }
    private ContentPropertiesType generateContentProperties(Content content) {
        ContentPropertiesType contentProperties = new ContentPropertiesType();
        StillImageContentAttributesType attributes = new StillImageContentAttributesType();

        //Metabroadcast now supplies the images, so the format is fixed.
        attributes.setWidth(MBST_IMAGE_WIDTH);
        attributes.setHeight(MBST_IMAGE_HEIGHT);
//        if (content.getImages() == null || content.getImages().isEmpty()) {
//            attributes.setWidth(DEFAULT_IMAGE_WIDTH);
//            attributes.setHeight(DEFAULT_IMAGE_HEIGHT);
//        } else {
//            Image image = Iterables.getFirst(content.getImages(), null);
//            attributes.setWidth(image.getWidth());
//            attributes.setHeight(image.getHeight());
//        }

        ControlledTermType primaryRole = new ControlledTermType();
        primaryRole.setHref(YOUVIEW_IMAGE_ATTRIBUTE_PRIMARY_ROLE);
        attributes.getIntendedUse().add(primaryRole);

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
        mediaLocator.setMediaUri(getMetabroadcastImageUrl(content.getImage()));
        return mediaLocator;
    }

    public static String getMetabroadcastImageUrl(String amazonUrl) {
        //Chop amazon's native resizing out of their url.
//        int lastDot = amazonUrl.lastIndexOf('.');
//        int preLastDot = amazonUrl.lastIndexOf('.', lastDot - 1);
//        if (lastDot > 0 || preLastDot > 0) {
//            amazonUrl = amazonUrl.substring(0, preLastDot)
//                        + amazonUrl.substring(lastDot, amazonUrl.length());
//        }
        try {
            return MBST_BASE_IMAGE_URL + URLEncoder.encode(amazonUrl, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            log.error("", e);
        }

        return amazonUrl;
    }

    private CreditsListType generateCreditsList(Content content) {
       CreditsListType creditsList = new CreditsListType();
       int index=1;

        Iterable<CrewMember> people = getOrderedCrewMembers(content);
        for (CrewMember person : people) {
           CreditsItemType credit = new CreditsItemType();
           try {
               credit.setRole(person.role().requireTvaUri());
           } catch( NullPointerException e){
               credit.setRole(YOUVIEW_CREDIT_ROLE_UNKNOWN);
           }
           credit.setIndex(BigInteger.valueOf(index));
           index++;

           NameComponentType nameComponent = new NameComponentType();
           nameComponent.setValue(person.name());
           JAXBElement<NameComponentType> nameElem = new JAXBElement<>(new QName("urn:tva:mpeg7:2008", "GivenName"), NameComponentType.class, nameComponent);

           PersonNameType personName = new PersonNameType();
           personName.getGivenNameOrLinkingNameOrFamilyName().add(nameElem);
           JAXBElement<PersonNameType> personNameElem = new JAXBElement<>(new QName("urn:tva:metadata:2010", "PersonName"), PersonNameType.class, personName);

           credit.getPersonNameOrPersonNameIDRefOrOrganizationName().add(personNameElem);
           creditsList.getCreditsItem().add(credit);
       }
       
       return creditsList;
    }

    private static Iterable<CrewMember> getOrderedCrewMembers(Content content) {
        List<CrewMember> actors = new ArrayList<>();
        List<CrewMember> directors = new ArrayList<>();
        List<CrewMember> other = new ArrayList<>();
        //sort actors first, directors second. atm amazon ingest does not contain anything else.
        for (CrewMember person : content.people()) {
            if (person.role() == CrewMember.Role.ACTOR) {
                actors.add(person);
            } else if (person.role() == CrewMember.Role.DIRECTOR) {
                directors.add(person);
            } else {
                other.add(person);
            }
        }
        return Iterables.concat(actors, directors, other);
    }

    private List<ExtendedLanguageType> generateLanguage(Content content) {
        List<ExtendedLanguageType> languages = Lists.newArrayList();
        for (String lang : content.getLanguages()) {
            ExtendedLanguageType language = new ExtendedLanguageType();
            language.setType(LANGUAGE_TYPE_ORIGINAL);
            language.setValue(lang);
            languages.add(language);
            // This block kinda relies on a single language geing available. At the time of writing
            // amazon does not even provide one. If more are provided YV might error.
        }

        if(languages.isEmpty()){ //if the content had no language, send undefined to YV.
                ExtendedLanguageType language = new ExtendedLanguageType();
                language.setType(LANGUAGE_TYPE_ORIGINAL);
                language.setValue(LANGUAGE_UNDEFINED);
                languages.add(language);
        }
        return languages;
    }

    private List<GenreType> generateGenres(Content content) {
        return FluentIterable.from(genreMapping.youViewGenresFor(content))
                .transform(TO_GENRE)
                .toList();
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
           throw new IllegalArgumentException("Invalid media type " + content.getMediaType()
                                              + " on item " + content.getCanonicalUri()
                                              + ". Cannot generate genre.");
       }
       return genre;
    }

    private List<SynopsisType> generateSynopses(Content content) {
        List<SynopsisType> synopses = Lists.newArrayList();
        for (Entry<SynopsisLengthType, Truncator> entry : YOUVIEW_SYNOPSIS_LENGTH_MAP.entrySet()) {
            String description = content.getDescription();
            if (description == null){
               description = "";
            } else {
                description = description.trim();
            }
            //YV needs a short synopses. Everything should already have one, but bugs are more
            //than humans in this world. Use the title.
            if (description.equals("") && entry.getKey().equals(SynopsisLengthType.SHORT)) {
                description = content.getTitle();
                log.warn(
                        "Amazon content was lacking synopsis. It shouldn't. The title was used instead. uri={}",
                        content.getCanonicalUri());
            }
            if (description != null) {
                SynopsisType synopsis = new SynopsisType();
                synopsis.setLength(entry.getKey());
                synopsis.setValue(entry.getValue().truncate(description));
                synopses.add(synopsis);
            }
        }
        return synopses;
    }

    private TitleType generateTitle(String titleType, String contentTitle) {
        TitleType title = new TitleType();
        title.getType().add(titleType);
        title.setValue(contentTitle);
        return title;
    }
}
