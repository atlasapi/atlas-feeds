package org.atlasapi.feeds.youview.nitro;

import static com.google.common.base.Preconditions.checkNotNull;
import static org.atlasapi.feeds.youview.nitro.NitroUtils.getLanguageCodeFor;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.metabroadcast.common.intl.Countries;
import org.atlasapi.feeds.tvanytime.CreditsItemGenerator;
import org.atlasapi.feeds.tvanytime.GroupInformationGenerator;
import org.atlasapi.feeds.youview.ServiceIdResolver;
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
import org.atlasapi.media.entity.ReleaseDate;
import org.atlasapi.media.entity.ReleaseDate.ReleaseType;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Specialization;

import org.joda.time.LocalDate;
import scala.collection.parallel.ParIterableLike;
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
import tva.mpeg7._2008.TextualType;
import tva.mpeg7._2008.TitleType;
import tva.mpeg7._2008.UniqueIDType;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.metabroadcast.common.text.Truncator;

public class NitroGroupInformationGenerator implements GroupInformationGenerator {

    private static final String MASTERBRAND_PREFIX = "http://nitro.bbc.co.uk/masterbrands/";
    private static final String YOUVIEW_CREDIT_ROLE = "urn:mpeg:mpeg7:cs:RoleCS:2001:UNKNOWN";
    private static final String YOUVIEW_IMAGE_FORMAT = "urn:mpeg:mpeg7:cs:FileFormatCS:2001:1";
    private static final String YOUVIEW_IMAGE_HOW_RELATED = "urn:tva:metadata:cs:HowRelatedCS:2010:19";
    private static final String YOUVIEW_IMAGE_ATTRIBUTE_PRIMARY_ROLE = "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#role-primary";
    private static final String GROUP_TYPE_PROGRAMCONCEPT = "programConcept";
    private static final String GROUP_TYPE_SERIES = "series";
    private static final String GROUP_TYPE_SHOW = "show";
    private static final String LANGUAGE_TYPE_ORIGINAL = "original";
    private static final String LANGUAGE = "en";
    private static final String GENRE_TYPE_MAIN = "main";
    private static final String GENRE_TYPE_OTHER = "other";
    private static final String TITLE_TYPE_MAIN = "main";
    private static final String TITLE_TYPE_SECONDARY = "secondary";
    private static final String CHILDREN_GENRE = "urn:tva:metadata:cs:IntendedAudienceCS:2010:4.2.1";
    private static final String OTHER_IDENTIFIER_AUTHORITY = "epid.bbc.co.uk";
    private static final String DATE_FORMAT = "yyyyMMdd";
    private static final Pattern NITRO_URI_PATTERN = Pattern.compile("^http://nitro.bbc.co.uk/programmes/([a-zA-Z0-9]+)$");

    private static final Map<MediaType, String> YOUVIEW_MEDIATYPE_GENRE_MAPPING = ImmutableMap.<MediaType, String>builder()
            .put(MediaType.VIDEO, "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3")
            .put(MediaType.AUDIO, "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.1")
            .build();
    
    private static final Map<Specialization, String> YOUVIEW_SPECIALIZATION_GENRE_MAPPING = ImmutableMap.<Specialization, String>builder()
            .put(Specialization.RADIO, "urn:tva:metadata:cs:OriginationCS:2005:5.9")
            .put(Specialization.FILM, "urn:tva:metadata:cs:OriginationCS:2005:5.7")
            .put(Specialization.TV, "urn:tva:metadata:cs:OriginationCS:2005:5.8")
            .build();
    
    private static final Map<SynopsisLengthType, Integer> YOUVIEW_SYNOPSIS_LENGTH = ImmutableMap.<SynopsisLengthType, Integer>builder()
            .put(SynopsisLengthType.SHORT, 90)
            .put(SynopsisLengthType.MEDIUM, 210)
            .put(SynopsisLengthType.LONG, 1200)
            .build();
    
    private static final List<String> TITLE_PREFIXES = ImmutableList.of("The ", "the ", "A ", "a ", "An ", "an ");
    
    private static final Function<String, List<GenreType>> TO_GENRE = new Function<String, List<GenreType>>() {
        @Override
        public List<GenreType> apply(String input) {
            ImmutableList.Builder<GenreType> genres = new ImmutableList.Builder<>();

            GenreType genre = new GenreType();
            genre.setHref(input);
            genres.add(genre);

            if (input.equals(CHILDREN_GENRE)) {
                GenreType childrenGenre = new GenreType();
                childrenGenre.setType(GENRE_TYPE_MAIN);
                childrenGenre.setHref(input);
                genres.add(childrenGenre);
            }

            return genres.build();
        }
    };
    private static final Integer DEFAULT_IMAGE_WIDTH = 1024;
    private static final Integer DEFAULT_IMAGE_HEIGHT = 576;

    private final IdGenerator idGenerator;
    private final GenreMapping genreMapping;
    private final ServiceIdResolver sIdResolver;
    private final CreditsItemGenerator creditsGenerator;
    private final ContentTitleGenerator titleGenerator;
    
    private Truncator truncator = new Truncator()
            .omitTrailingPunctuationWhenTruncated()
            .onlyTruncateAtAWordBoundary()
            .withOmissionMarker("...");
    
    public NitroGroupInformationGenerator(IdGenerator idGenerator, GenreMapping genreMapping,
            ServiceIdResolver sIdResolver, CreditsItemGenerator creditsGenerator,
            ContentTitleGenerator titleGenerator) {
        this.idGenerator = checkNotNull(idGenerator);
        this.genreMapping = checkNotNull(genreMapping);
        this.sIdResolver = checkNotNull(sIdResolver);
        this.creditsGenerator = checkNotNull(creditsGenerator);
        this.titleGenerator = checkNotNull(titleGenerator);
    }
    
    @Override
    public GroupInformationType generate(Film film) {
        GroupInformationType groupInfo = generateWithCommonFields(film);
        
        groupInfo.setGroupType(generateGroupType(GROUP_TYPE_PROGRAMCONCEPT));
        groupInfo.setServiceIDRef(createMasterBrandLink(film));

        return groupInfo;
    }

    private UniqueIDType createContentPidOtherIdentifier(Content content) {
        Matcher matcher = NITRO_URI_PATTERN.matcher(content.getCanonicalUri());

        if (!matcher.matches()) {
            throw new RuntimeException("Uri not compliant to Nitro format: " + content.getCanonicalUri());
        }

        String pid = matcher.group(1);
        UniqueIDType idType = new UniqueIDType();
        idType.setAuthority(OTHER_IDENTIFIER_AUTHORITY);
        idType.setValue(pid);

        return idType;
    }

    @Override
    public GroupInformationType generate(Item item, Optional<Series> series, Optional<Brand> brand) {
        GroupInformationType groupInfo = generateWithCommonFields(item);
        
        groupInfo.setGroupType(generateGroupType(GROUP_TYPE_PROGRAMCONCEPT));

        if (item instanceof Episode) {
            groupInfo.getOtherIdentifier().add(createContentPidOtherIdentifier(item));
        }

        if (series.isPresent()) {
            MemberOfType memberOf = new MemberOfType();
            memberOf.setCrid(idGenerator.generateContentCrid(series.get()));
            if (item instanceof Episode) {
                setEpisodeIndex(memberOf, item);
            }
            groupInfo.getMemberOf().add(memberOf);
        } else if (brand.isPresent()) {
            MemberOfType memberOf = new MemberOfType();
            memberOf.setCrid(idGenerator.generateContentCrid(brand.get()));
            if (item instanceof Episode) {
                setEpisodeIndex(memberOf, item);
            }
            groupInfo.getMemberOf().add(memberOf);
        }
        
        groupInfo.setServiceIDRef(createMasterBrandLink(item));
        
        return groupInfo;  
    }
    
    @Override
    public GroupInformationType generate(Series series, Optional<Brand> brand, Item firstChild) {
        GroupInformationType groupInfo = generateWithCommonFields(series);
        
        groupInfo.setGroupType(generateGroupType(GROUP_TYPE_SERIES));
        groupInfo.setOrdered(true);

        if (brand.isPresent()) {
            MemberOfType memberOf = new MemberOfType();
            memberOf.setCrid(idGenerator.generateContentCrid(brand.get()));
            if (series.getSeriesNumber() != null) {
                memberOf.setIndex(Long.valueOf(series.getSeriesNumber()));
            }
            groupInfo.getMemberOf().add(memberOf);
        } else {
            groupInfo.setServiceIDRef(createMasterBrandLink(series));
        }
        
        return groupInfo;
    }
    
    @Override
    public GroupInformationType generate(Brand brand, @Nullable Item item) {
        GroupInformationType groupInfo = generateWithCommonFields(brand);
        
        groupInfo.setGroupType(generateGroupType(GROUP_TYPE_SHOW));
        groupInfo.setOrdered(true);
        groupInfo.setServiceIDRef(createMasterBrandLink(brand));
        
        return groupInfo;
    }
    
    private String createMasterBrandLink(Content content) {
        return MASTERBRAND_PREFIX + sIdResolver.resolveMasterBrandId(content).get();
    }

    private GroupInformationType generateWithCommonFields(Content content) {
        GroupInformationType groupInfo = new GroupInformationType();
        
        groupInfo.setLang(LANGUAGE);
        groupInfo.setGroupId(idGenerator.generateContentCrid(content));
        groupInfo.setBasicDescription(generateBasicDescription(content));
        
        return groupInfo;
    }

    private BaseProgramGroupTypeType generateGroupType(String groupType) {
        ProgramGroupTypeType type = new ProgramGroupTypeType();
        type.setValue(groupType);
        return type;
    }

    private BasicContentDescriptionType generateBasicDescription(Content content) {
        BasicContentDescriptionType basicDescription = new BasicContentDescriptionType();
        
        if (content.getTitle() != null) {
            
            String title = titleGenerator.titleFor(content);
            basicDescription.getTitle().add(generateTitle(TITLE_TYPE_MAIN, title));
            
            String secondaryTitle = generateAlternateTitle(title);
            if (!title.equals(secondaryTitle)) {
                basicDescription.getTitle().add(generateTitle(TITLE_TYPE_SECONDARY, secondaryTitle));
            }            
        }
        
        basicDescription.getSynopsis().addAll(generateSynopses(content));
        basicDescription.getGenre().addAll(generateGenres(content));
        
        Optional<GenreType> specializationGenre = generateGenreFromSpecialization(content);
        if (specializationGenre.isPresent()) {
            basicDescription.getGenre().add(specializationGenre.get());
        }
        
        basicDescription.getGenre().add(generateGenreFromMediaType(content));

        String language = getLanguageCodeFor(content);
        basicDescription.getLanguage().addAll(generateLanguage(language));

        Optional<RelatedMaterialType> relatedMaterial = Optional.absent();
        relatedMaterial = generateRelatedMaterial(content);
        
        if (relatedMaterial.isPresent()) {
            basicDescription.getRelatedMaterial().add(relatedMaterial.get());
        }

        basicDescription.setCreditsList(creditsGenerator.generate(content));
        
        return basicDescription;
    }

    private String generateAlternateTitle(String title) {
        for (String prefix : TITLE_PREFIXES) {
            if (title.startsWith(prefix)) {
                return title.replaceFirst(prefix, "").concat(", " + prefix).trim();
            }    
        }
        return title;
    }

    private Optional<RelatedMaterialType> generateRelatedMaterial(Content content) {
        if (Strings.isNullOrEmpty(content.getImage())) {
            return Optional.absent();
        }
        ExtendedRelatedMaterialType relatedMaterial = new ExtendedRelatedMaterialType();
        
        relatedMaterial.setHowRelated(generateHowRelated());
        relatedMaterial.setFormat(generateFormat());
        relatedMaterial.setMediaLocator(generateMediaLocator(content));
        relatedMaterial.setContentProperties(generateContentProperties(content));

        if (content instanceof Episode) {
            TextualType textualType = new TextualType();
            textualType.setValue(titleGenerator.titleFor(content));
            relatedMaterial.getPromotionalText().add(textualType);
        }
        
        return Optional.<RelatedMaterialType>of(relatedMaterial);
    }

    private ContentPropertiesType generateContentProperties(Content content) {
        ContentPropertiesType contentProperties = new ContentPropertiesType();
        StillImageContentAttributesType attributes = new StillImageContentAttributesType();
        
        if (content.getImages() == null || content.getImages().isEmpty()) {
            attributes.setWidth(DEFAULT_IMAGE_WIDTH);
            attributes.setHeight(DEFAULT_IMAGE_HEIGHT);
        } else {
            Image image = Iterables.getFirst(content.getImages(), null);
            attributes.setWidth(image.getWidth());
            attributes.setHeight(image.getHeight());
        }
        if (attributes.getWidth() == null) {
            attributes.setWidth(DEFAULT_IMAGE_WIDTH);
        }
        if (attributes.getHeight() == null) {
            attributes.setHeight(DEFAULT_IMAGE_HEIGHT);
        }
        
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
        mediaLocator.setMediaUri(content.getImage());
        return mediaLocator;
    }

    private List<ExtendedLanguageType> generateLanguage(String language) {
        List<ExtendedLanguageType> languages = Lists.newArrayList();
        ExtendedLanguageType languageType = new ExtendedLanguageType();
        languageType.setType(LANGUAGE_TYPE_ORIGINAL);
        languageType.setValue(language);
        languages.add(languageType);
        return languages;
    }

    private List<GenreType> generateGenres(Content content) {
        return FluentIterable.from(genreMapping.youViewGenresFor(content))
                .transformAndConcat(TO_GENRE)
                .toList();
    }

    private Optional<GenreType> generateGenreFromSpecialization(Content content) {
        Specialization specialization = content.getSpecialization();
        if (specialization == null) {
            return Optional.absent();
        }
        GenreType genre = new GenreType();
        genre.setType(GENRE_TYPE_MAIN);
        genre.setHref(YOUVIEW_SPECIALIZATION_GENRE_MAPPING.get(specialization));
        return Optional.of(genre);
    }

    private GenreType generateGenreFromMediaType(Content content) {
       MediaType mediaType = content.getMediaType();
       if (mediaType == null) {
           throw new RuntimeException("invalid media type " + mediaType + " on item " + content.getCanonicalUri());
       }
       String genreHref = YOUVIEW_MEDIATYPE_GENRE_MAPPING.get(mediaType);
       if (genreHref == null) {
           throw new RuntimeException("invalid media type " + mediaType + " on item " + content.getCanonicalUri());
       }
       
       GenreType genre = new GenreType();
       genre.setType(GENRE_TYPE_OTHER);
       genre.setHref(genreHref);
       return genre;
    }

    private List<SynopsisType> generateSynopses(Content content) {
        List<SynopsisType> synopses = Lists.newArrayList();

        synopses.add(createSynopsis(SynopsisLengthType.SHORT, shortDescriptionOrFallback(content)));
        synopses.add(createSynopsis(SynopsisLengthType.MEDIUM, content.getMediumDescription()));
        synopses.add(createSynopsis(SynopsisLengthType.LONG, content.getLongDescription()));
        
        return synopses;
    }

    /**
     * Short description is mandatory, so if it's not present, we'll
     * fall back to another description length, which may subsequently
     * be truncated
     * 
     */
    private String shortDescriptionOrFallback(Content content) {
        if (content.getShortDescription() != null) {
            return content.getShortDescription();
        }
        if (content.getMediumDescription() != null) {
            return content.getMediumDescription();
        }
        return content.getLongDescription();
    }
    
    private SynopsisType createSynopsis(SynopsisLengthType synopsisType, String description) {
        Integer length = YOUVIEW_SYNOPSIS_LENGTH.get(synopsisType);
        if (length == null) {
            throw new RuntimeException("No length mapping found for YouView Synopsis length " + synopsisType.name());
        }
        
        truncator = truncator.withMaxLength(length);
        SynopsisType synopsis = new SynopsisType();
        
        synopsis.setLength(synopsisType);
        synopsis.setValue(truncator.truncatePossibleNull(description));
        
        return synopsis;
    }

    private TitleType generateTitle(String titleType, String contentTitle) {
        TitleType title = new TitleType();
        
        title.getType().add(titleType);
        title.setLang(LANGUAGE);
        title.setValue(contentTitle);
        
        return title;
    }

    private void setEpisodeIndex(MemberOfType memberOf, Item item) {
        Episode episode = (Episode) item;
        Integer episodeNumber = episode.getEpisodeNumber();
        if (episodeNumber != null) {
            memberOf.setIndex(Long.valueOf(episodeNumber));
        } else if (!item.getReleaseDates().isEmpty()) {
            for (ReleaseDate releaseDate : item.getReleaseDates()) {
                if (releaseDate.type().equals(ReleaseType.FIRST_BROADCAST)) {
                    int index = generateIndexFromReleaseDate(releaseDate);
                    memberOf.setIndex(Long.valueOf(index));
                }
            }
        }
    }

    private int generateIndexFromReleaseDate(ReleaseDate releaseDate) {
        String date = releaseDate.date().toString(DATE_FORMAT);
        return Integer.parseInt(date);
    }
}
