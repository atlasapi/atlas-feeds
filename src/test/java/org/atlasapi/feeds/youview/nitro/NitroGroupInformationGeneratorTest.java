package org.atlasapi.feeds.youview.nitro;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBElement;

import org.atlasapi.feeds.tvanytime.GroupInformationGenerator;
import org.atlasapi.feeds.youview.NameComponentTypeEquivalence;
import org.atlasapi.feeds.youview.SynopsisTypeEquivalence;
import org.atlasapi.feeds.youview.genres.GenreMapping;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Person;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.PeopleResolver;
import org.hamcrest.Matchers;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import tva.metadata._2010.BaseMemberOfType;
import tva.metadata._2010.BasicContentDescriptionType;
import tva.metadata._2010.CreditsItemType;
import tva.metadata._2010.GenreType;
import tva.metadata._2010.GroupInformationType;
import tva.metadata._2010.ProgramGroupTypeType;
import tva.metadata._2010.RelatedMaterialType;
import tva.metadata._2010.SynopsisLengthType;
import tva.metadata._2010.SynopsisType;
import tva.metadata.extended._2010.ExtendedRelatedMaterialType;
import tva.mpeg7._2008.ExtendedLanguageType;
import tva.mpeg7._2008.NameComponentType;
import tva.mpeg7._2008.PersonNameType;
import tva.mpeg7._2008.TextualType;
import tva.mpeg7._2008.TitleType;
import tva.mpeg7._2008.UniqueIDType;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.common.hash.Hashing;
import com.metabroadcast.common.intl.Countries;

public class NitroGroupInformationGeneratorTest {
    
    private static final String BBC_SERVICE_PREFIX = "http://bbc.co.uk/services/";
    private static final String MASTER_BRAND = "master_brand";

    private static final String GEORGE_SCOTT_URI = "http://george.scott";
    private static final String KUBRICK_URI = "http://stanley.kubrick";

    private static final Function<JAXBElement<?>, NameComponentType> TO_NAME_COMPONENT_TYPE = new Function<JAXBElement<?>, NameComponentType>() {
        @Override
        public NameComponentType apply(JAXBElement<?> input) {
            return (NameComponentType) input.getValue();
        }
    };

    private static final Function<GenreType, String> TO_HREF = new Function<GenreType, String>() {
        @Override
        public String apply(GenreType input) {
            return input.getHref();
        }
    };
    
    private SynopsisTypeEquivalence SYNOPSIS_EQUIVALENCE = new SynopsisTypeEquivalence();
    private NameComponentTypeEquivalence NAME_EQUIVALENCE = new NameComponentTypeEquivalence();
    private BbcServiceIdResolver bbcServiceIdResolver = Mockito.mock(BbcServiceIdResolver.class);
    private IdGenerator idGenerator = new NitroIdGenerator(Hashing.md5());
    private GenreMapping genreMapping = new NitroGenreMapping();
    private PeopleResolver peopleResolver = Mockito.mock(PeopleResolver.class);
    private NitroCreditsItemGenerator creditsGenerator = new NitroCreditsItemGenerator(peopleResolver);
    private ContentTitleGenerator titleGenerator = Mockito.mock(ContentTitleGenerator.class);
    
    private final GroupInformationGenerator generator = new NitroGroupInformationGenerator(idGenerator, genreMapping, bbcServiceIdResolver,
            creditsGenerator, titleGenerator);
    
    @Before
    public void setup() {
        when(bbcServiceIdResolver.resolveMasterBrandId(any(Content.class))).thenReturn(Optional.of(MASTER_BRAND));
        when(peopleResolver.person(GEORGE_SCOTT_URI)).thenReturn(Optional.of(createPerson(GEORGE_SCOTT_URI, "George C.", "Scott")));
        when(peopleResolver.person(KUBRICK_URI)).thenReturn(Optional.of(createPerson(KUBRICK_URI, "Stanley", "Kubrick")));
        when(titleGenerator.titleFor(any(Content.class))).thenAnswer(new Answer<String>() {

            @Override
            public String answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                return ((Content)args[0]).getTitle();
            }
        });
    }
    
    @Test
    public void testRelatedMaterialNotGeneratedIfNullOrEmptyImageString() {
        Film film = createFilm();
        film.setImage("");
        
        GroupInformationType groupInfo = generator.generate(film);
        
        BasicContentDescriptionType desc = groupInfo.getBasicDescription();
        
        assertTrue(desc.getRelatedMaterial().isEmpty());
    }
    
    @Test
    public void testGenresOutput() {
        Film film = createFilm();
        film.setGenres(ImmutableSet.of("http://nitro.bbc.co.uk/genres/100003"));
        
        GroupInformationType groupInfo = generator.generate(film);
        
        ImmutableSet<String> outputGenreHrefs = ImmutableSet.copyOf(Iterables.transform(
                groupInfo.getBasicDescription().getGenre(), 
                TO_HREF
        ));
        ImmutableSet<String> expected = ImmutableSet.of(
                "urn:tva:metadata:cs:ContentCS:2010:3.4", // from genre mapping 
                "urn:tva:metadata:cs:OriginationCS:2005:5.7", // content type == film
                "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3"  // mediatype == video 
        );
        
        assertEquals(expected, outputGenreHrefs);
    }
    
    @Test
    public void testDescriptionTrimmedToAppropriateLengthIfTooLong() {
        int shortDescriptionMaxLength = 90;
        String shortDescriptionLongerThanMaxLength = "Here's a really long string that's longer than the short description max length permitted by YouView";
        
        assertTrue("input short description should be longer than the max permitted length", shortDescriptionLongerThanMaxLength.length() > shortDescriptionMaxLength);
        
        Film film = createFilm();
        film.setShortDescription(shortDescriptionLongerThanMaxLength);

        GroupInformationType groupInfo = generator.generate(film);
        
        String outputShortDesc = getShortDescriptionFrom(groupInfo);
        assertTrue("output short description should be equal to max permitted length", outputShortDesc.length() < shortDescriptionMaxLength);
    }
    
    private String getShortDescriptionFrom(GroupInformationType groupInfo) {
        BasicContentDescriptionType basicDescription = groupInfo.getBasicDescription();
        
        List<SynopsisType> synopses = basicDescription.getSynopsis();
        Optional<SynopsisType> shortDescription = Iterables.tryFind(synopses, new Predicate<SynopsisType>() {
            @Override
            public boolean apply(SynopsisType input) {
                return SynopsisLengthType.SHORT.equals(input.getLength());
            }
        });
        
        if (!shortDescription.isPresent()) {
            throw new RuntimeException("unable to find a short description on this group information");
        }
        return shortDescription.get().getValue();
    }

    @Test
    public void testMasterbrandOutputAsServiceIDRef() {
        Film film = createFilm();
        film.setPresentationChannel(BBC_SERVICE_PREFIX + MASTER_BRAND);
        
        GroupInformationType groupInfo = generator.generate(film);

        String serviceIDRef = groupInfo.getServiceIDRef();
        
        // N.B. this has been temporarily changed from 'masterbrands' to 'master_brands'
        assertEquals("http://nitro.bbc.co.uk/masterbrands/" + MASTER_BRAND, serviceIDRef);
    }
    
    @Test
    public void testContentWithNoSpecializationDoesntOutputSpecializationGenre() {
        Film film = createFilm();
        film.setSpecialization(null);
        
        GroupInformationType groupInfo = generator.generate(film);
        Iterable<String> genres = Iterables.transform(groupInfo.getBasicDescription().getGenre(), TO_HREF);
        
        assertFalse("Genres shouldn't contain Film Specialization Genre if no specialization", Iterables.contains(genres, "urn:tva:metadata:cs:OriginationCS:2005:5.7"));
        assertFalse("Genres shouldn't contain TV Specialization Genre if no specialization", Iterables.contains(genres, "urn:tva:metadata:cs:OriginationCS:2005:5.8"));
        assertFalse("Genres shouldn't contain Radio Specialization Genre if no specialization", Iterables.contains(genres, "urn:tva:metadata:cs:OriginationCS:2005:5.9"));
    }
    
    @Test
    public void testContentWithFilmSpecializationOutputsFilmSpecializationGenre() {
        Film film = createFilm();
        film.setSpecialization(Specialization.FILM);
        
        GroupInformationType groupInfo = generator.generate(film);
        Iterable<String> genres = Iterables.transform(groupInfo.getBasicDescription().getGenre(), TO_HREF);
        
        assertFalse("Genres shouldn't contain TV Specialization Genre if specialization is Film", Iterables.contains(genres, "urn:tva:metadata:cs:OriginationCS:2005:5.8"));
        assertFalse("Genres shouldn't contain Radio Specialization Genre if specialization is Film", Iterables.contains(genres, "urn:tva:metadata:cs:OriginationCS:2005:5.9"));
        assertTrue("Genres should contain Film Specialization Genre if specialization is Film", Iterables.contains(genres, "urn:tva:metadata:cs:OriginationCS:2005:5.7"));
    }
    
    @Test
    public void testContentWithTvSpecializationOutputsTvSpecializationGenre() {
        Film film = createFilm();
        film.setSpecialization(Specialization.TV);
        
        GroupInformationType groupInfo = generator.generate(film);
        Iterable<String> genres = Iterables.transform(groupInfo.getBasicDescription().getGenre(), TO_HREF);
        
        assertFalse("Genres shouldn't contain Film Specialization Genre if specialization is TV", Iterables.contains(genres, "urn:tva:metadata:cs:OriginationCS:2005:5.7"));
        assertFalse("Genres shouldn't contain Radio Specialization Genre if specialization is TV", Iterables.contains(genres, "urn:tva:metadata:cs:OriginationCS:2005:5.9"));
        assertTrue("Genres should contain TV Specialization Genre if specialization is TV", Iterables.contains(genres, "urn:tva:metadata:cs:OriginationCS:2005:5.8"));
    }
    
    @Test
    public void testContentWithRadioSpecializationOutputsRadioSpecializationGenre() {
        Film film = createFilm();
        film.setSpecialization(Specialization.RADIO);
        
        GroupInformationType groupInfo = generator.generate(film);
        Iterable<String> genres = Iterables.transform(groupInfo.getBasicDescription().getGenre(), TO_HREF);
        
        assertFalse("Genres shouldn't contain Film Specialization Genre if specialization is Radio", Iterables.contains(genres, "urn:tva:metadata:cs:OriginationCS:2005:5.7"));
        assertFalse("Genres shouldn't contain TV Specialization Genre if specialization is Radio", Iterables.contains(genres, "urn:tva:metadata:cs:OriginationCS:2005:5.8"));
        assertTrue("Genres should contain Radio Specialization Genre if specialization is Radio", Iterables.contains(genres, "urn:tva:metadata:cs:OriginationCS:2005:5.9"));
    }
    
    @Test
    public void testContentWithVideoMediaTypeOutputsVideoMediaTypeGenre() {
        Film film = createFilm();
        film.setMediaType(MediaType.VIDEO);
        
        GroupInformationType groupInfo = generator.generate(film);
        Iterable<String> genres = Iterables.transform(groupInfo.getBasicDescription().getGenre(), TO_HREF);
        
        assertFalse("Genres shouldn't contain Audio MediaType Genre if media type is Video", Iterables.contains(genres, "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.1"));
        assertTrue("Genres should contain Video MediaType Genre if media type is Video", Iterables.contains(genres, "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3"));
    }
    
    @Test
    public void testContentWithAudioMediaTypeOutputsAudioMediaTypeGenre() {
        Film film = createFilm();
        film.setMediaType(MediaType.AUDIO);
        
        GroupInformationType groupInfo = generator.generate(film);
        Iterable<String> genres = Iterables.transform(groupInfo.getBasicDescription().getGenre(), TO_HREF);
        
        assertFalse("Genres shouldn't contain Video MediaType Genre if media type is Audio", Iterables.contains(genres, "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3"));
        assertTrue("Genres should contain Audio MediaType Genre if media type is Audio", Iterables.contains(genres, "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.1"));
    }
    
    @Test(expected = RuntimeException.class)
    public void testContentWithNoMediaTypeThrowsException() {
        Film film = createFilm();
        film.setMediaType(null);
        
        generator.generate(film);
    }
    
    @Test
    public void testSynopsisGeneration() {
        String fullDescription = "full description";
        String shortDescription = "short description";
        String mediumDescription = "medium description";
        String longDescription = "long description";
        
        Film film = createFilm();
        film.setDescription(fullDescription);
        film.setShortDescription(shortDescription);
        film.setMediumDescription(mediumDescription);
        film.setLongDescription(longDescription);
        
        GroupInformationType groupInfo = generator.generate(film);
        BasicContentDescriptionType desc = groupInfo.getBasicDescription();
        
        SynopsisType shortDesc = new SynopsisType();
        shortDesc.setLength(SynopsisLengthType.SHORT);
        shortDesc.setValue(shortDescription);
        
        SynopsisType mediumDesc = new SynopsisType();
        mediumDesc.setLength(SynopsisLengthType.MEDIUM);
        mediumDesc.setValue(mediumDescription);
        
        SynopsisType longDesc = new SynopsisType();
        longDesc.setLength(SynopsisLengthType.LONG);
        longDesc.setValue(longDescription);
        
        assertTrue(SYNOPSIS_EQUIVALENCE.pairwise().equivalent(
            ImmutableSet.of(shortDesc, mediumDesc, longDesc), 
            desc.getSynopsis()
            ));
    }
    
   // TODO test image generation
    // TODO test default image dimensions
    
    @Test
    public void testPersonGeneration() {
        Film film = createFilm();
        
        CrewMember georgeScott = new CrewMember();
        georgeScott.withName("George C. Scott");
        georgeScott.withRole(CrewMember.Role.ACTOR);
        georgeScott.setCanonicalUri(GEORGE_SCOTT_URI);

        CrewMember stanley = new CrewMember();
        stanley.withName("Stanley Kubrick");
        stanley.withRole(CrewMember.Role.ACTOR);
        stanley.setCanonicalUri(KUBRICK_URI);

        film.setPeople(ImmutableList.of(georgeScott, stanley));
        
        GroupInformationType groupInfo = generator.generate(film);
        
        BasicContentDescriptionType desc = groupInfo.getBasicDescription();
        
        List<CreditsItemType> creditsList = desc.getCreditsList().getCreditsItem();

        assertEquals(2, creditsList.size());

        for (CreditsItemType credit : creditsList) {
            assertEquals("urn:mpeg:mpeg7:cs:RoleCS:2001:ACTOR", credit.getRole());
            checkNames(credit);
        }
    }

    private void checkNames(CreditsItemType credit) {
        Set<String> names = getNames(credit);
        Assert.assertThat(names,
                Matchers.either(containsInAnyOrder("George C.", "Scott"))
                        .or(containsInAnyOrder("Stanley", "Kubrick")));
    }

    private Set<String> getNames(CreditsItemType credit) {
        PersonNameType name = (PersonNameType) Iterables.getOnlyElement(credit.getPersonNameOrPersonNameIDRefOrOrganizationName())
                .getValue();

        return FluentIterable.from(name.getGivenNameOrLinkingNameOrFamilyName()).transform(
                new Function<JAXBElement<?>, String>() {
                    @Override
                    public String apply(JAXBElement<?> input) {
                        return ((NameComponentType) input.getValue()).getValue();

                    }
                }).toSet();
    }

    private Person createPerson(String uri, String givenName, String familyName) {
        Person person = new Person();
        person.setCanonicalUri(uri);
        person.setGivenName(givenName);
        person.setFamilyName(familyName);

        return person;
    }
    
    @Test
    public void testFilmGroupInformationGeneration() {
        GroupInformationType groupInfo = generator.generate(createFilm());

        assertEquals("crid://nitro.bbc.co.uk/iplayer/youview/b020tm1g", groupInfo.getGroupId());
        ProgramGroupTypeType groupType = (ProgramGroupTypeType) groupInfo.getGroupType();
        assertEquals("programConcept", groupType.getValue());
        
        BasicContentDescriptionType desc = groupInfo.getBasicDescription();
        
        TitleType title = Iterables.getOnlyElement(desc.getTitle());
        assertEquals("Dr. Strangelove", title.getValue());
        assertEquals("main", Iterables.getOnlyElement(title.getType()));
        
        ExtendedLanguageType language = Iterables.getOnlyElement(desc.getLanguage());
        assertEquals("original", language.getType());
        assertEquals("en", language.getValue());
    }

    @Test
    public void testEpisodeGroupInformationGeneration() {
        Episode episode = createEpisode();
        GroupInformationType groupInfo = generator.generate(episode, Optional.of(createSeries()), Optional.of(createBrand()));

        assertEquals("crid://nitro.bbc.co.uk/iplayer/youview/b03dfd6d", groupInfo.getGroupId());

        UniqueIDType idType = Iterables.getOnlyElement(groupInfo.getOtherIdentifier());
        assertEquals("epid.bbc.co.uk", idType.getAuthority());
        assertEquals("b03dfd6d", idType.getValue());

        BaseMemberOfType memberOf = Iterables.getOnlyElement(groupInfo.getMemberOf());
        assertEquals("crid://nitro.bbc.co.uk/iplayer/youview/b020tm1g", memberOf.getCrid());
        assertEquals(Long.valueOf(5), memberOf.getIndex());
        
        ProgramGroupTypeType groupType = (ProgramGroupTypeType) groupInfo.getGroupType();
        assertEquals("programConcept", groupType.getValue());
        
        BasicContentDescriptionType desc = groupInfo.getBasicDescription();
        RelatedMaterialType relatedMaterial = Iterables.getOnlyElement(desc.getRelatedMaterial());
        TextualType textualType = Iterables.getOnlyElement(relatedMaterial.getPromotionalText());
        assertEquals(episode.getTitle(), textualType.getValue());

        TitleType title = Iterables.getOnlyElement(desc.getTitle());
        assertEquals("Episode 1", title.getValue());
        assertEquals("main", Iterables.getOnlyElement(title.getType()));
        
        ExtendedLanguageType language = Iterables.getOnlyElement(desc.getLanguage());
        assertEquals("original", language.getType());
        assertEquals("en", language.getValue());

    }
    
    @Test
    public void testSeriesGroupInformationGeneration() {
        Series series = createSeries();
        series.setImage("series image");
        
        GroupInformationType groupInfo = generator.generate(series, Optional.of(createBrand()), createEpisode());

        assertEquals("crid://nitro.bbc.co.uk/iplayer/youview/b020tm1g", groupInfo.getGroupId());
        assertTrue(groupInfo.isOrdered());
        
        BaseMemberOfType memberOf = Iterables.getOnlyElement(groupInfo.getMemberOf());
        assertEquals("crid://nitro.bbc.co.uk/iplayer/youview/b007n2qs", memberOf.getCrid());
        assertEquals(Long.valueOf(2), memberOf.getIndex());
        
        ProgramGroupTypeType groupType = (ProgramGroupTypeType) groupInfo.getGroupType();
        assertEquals("series", groupType.getValue());
        
        BasicContentDescriptionType desc = groupInfo.getBasicDescription();
        
        TitleType title = Iterables.getOnlyElement(desc.getTitle());
        assertEquals("Series 2", title.getValue());
        assertEquals("main", Iterables.getOnlyElement(title.getType()));

        ExtendedLanguageType language = Iterables.getOnlyElement(desc.getLanguage());
        assertEquals("original", language.getType());
        assertEquals("en", language.getValue());

        ExtendedRelatedMaterialType relatedMaterial = (ExtendedRelatedMaterialType) Iterables.getOnlyElement(desc.getRelatedMaterial());

        assertEquals(
            "series image", 
            relatedMaterial.getMediaLocator().getMediaUri()
        );
    }
    
    @Test
    public void testBrandGroupInformationGeneration() {
        Brand brand = createBrand();
        brand.setImage("brand image");
        
        GroupInformationType groupInfo = generator.generate(brand, createEpisode());

        assertEquals("crid://nitro.bbc.co.uk/iplayer/youview/b007n2qs", groupInfo.getGroupId());
        assertTrue(groupInfo.isOrdered());
                
        ProgramGroupTypeType groupType = (ProgramGroupTypeType) groupInfo.getGroupType();
        assertEquals("show", groupType.getValue());
        
        BasicContentDescriptionType desc = groupInfo.getBasicDescription();
        
        TitleType title = Iterables.getOnlyElement(desc.getTitle());
        assertEquals("Northern Lights", title.getValue());
        assertEquals("main", Iterables.getOnlyElement(title.getType()));
        
        ExtendedRelatedMaterialType relatedMaterial = (ExtendedRelatedMaterialType) Iterables.getOnlyElement(desc.getRelatedMaterial());

        assertEquals(
            "brand image", 
            relatedMaterial.getMediaLocator().getMediaUri()
        );
    }
    
    @Test
    public void testSecondaryTitleGeneration() {
        Film film = createFilm();
        
        film.setTitle("The film");
        GroupInformationType groupInfo = generator.generate(film);
        
        List<TitleType> titles = groupInfo.getBasicDescription().getTitle();
        assertThat(titles.size(), is(2));
        
        TitleType first = titles.get(0);
        TitleType second = titles.get(1);
        
        if (Iterables.getOnlyElement(first.getType()).equals("main")) {
            assertEquals("The film", first.getValue());
            assertEquals("film, The", second.getValue());
            assertEquals("secondary", Iterables.getOnlyElement(second.getType()));
        } else if (Iterables.getOnlyElement(second.getType()).equals("main")) {
            assertEquals("The film", second.getValue());
            assertEquals("film, The", first.getValue());
            assertEquals("secondary", Iterables.getOnlyElement(first.getType()));
        }
        
        film.setTitle("the film");
        groupInfo = generator.generate(film);
        
        titles = groupInfo.getBasicDescription().getTitle();
        assertThat(titles.size(), is(2));
        
        first = titles.get(0);
        second = titles.get(1);
        
        if (Iterables.getOnlyElement(first.getType()).equals("main")) {
            assertEquals("the film", first.getValue());
            assertEquals("film, the", second.getValue());
            assertEquals("secondary", Iterables.getOnlyElement(second.getType()));
        } else if (Iterables.getOnlyElement(second.getType()).equals("main")) {
            assertEquals("the film", second.getValue());
            assertEquals("film, the", first.getValue());
            assertEquals("secondary", Iterables.getOnlyElement(first.getType()));
        }
        
        film.setTitle("a film");
        groupInfo = generator.generate(film);
        
        titles = groupInfo.getBasicDescription().getTitle();
        assertThat(titles.size(), is(2));
        
        first = titles.get(0);
        second = titles.get(1);
        
        if (Iterables.getOnlyElement(first.getType()).equals("main")) {
            assertEquals("a film", first.getValue());
            assertEquals("film, a", second.getValue());
            assertEquals("secondary", Iterables.getOnlyElement(second.getType()));
        } else if (Iterables.getOnlyElement(second.getType()).equals("main")) {
            assertEquals("a film", second.getValue());
            assertEquals("film, a", first.getValue());
            assertEquals("secondary", Iterables.getOnlyElement(first.getType()));
        }
        
        film.setTitle("An interesting film");
        groupInfo = generator.generate(film);
        
        titles = groupInfo.getBasicDescription().getTitle();
        assertThat(titles.size(), is(2));
        
        first = titles.get(0);
        second = titles.get(1);
        
        if (Iterables.getOnlyElement(first.getType()).equals("main")) {
            assertEquals("An interesting film", first.getValue());
            assertEquals("interesting film, An", second.getValue());
            assertEquals("secondary", Iterables.getOnlyElement(second.getType()));
        } else if (Iterables.getOnlyElement(second.getType()).equals("main")) {
            assertEquals("An interesting film", second.getValue());
            assertEquals("interesting film, An", first.getValue());
            assertEquals("secondary", Iterables.getOnlyElement(first.getType()));
        }
        
        film.setTitle("Some interesting film");
        groupInfo = generator.generate(film);
        
        TitleType title = Iterables.getOnlyElement(groupInfo.getBasicDescription().getTitle());
        
        assertEquals("Some interesting film", title.getValue());
        assertEquals("main", Iterables.getOnlyElement(title.getType()));
    }
    
    @Test
    public void testSecondaryTitleGenerationDoesntReplaceNonFirstWord() {
        Film film = createFilm();
        
        film.setTitle("the film that contains another instance of the");
        GroupInformationType groupInfo = generator.generate(film);
        
        List<TitleType> titles = groupInfo.getBasicDescription().getTitle();
        assertThat(titles.size(), is(2));
        
        TitleType first = titles.get(0);
        TitleType second = titles.get(1);
        
        if (Iterables.getOnlyElement(first.getType()).equals("main")) {
            assertEquals("the film that contains another instance of the", first.getValue());
            assertEquals("film that contains another instance of the, the", second.getValue());
            assertEquals("secondary", Iterables.getOnlyElement(second.getType()));
        } else if (Iterables.getOnlyElement(second.getType()).equals("main")) {
            assertEquals("the film that contains another instance of the", second.getValue());
            assertEquals("film that contains another instance of the, The", first.getValue());
            assertEquals("secondary", Iterables.getOnlyElement(first.getType()));
        }
    }
    
    @Test
    public void testFallbackSynopses() {
        Episode episode = createEpisode();
        episode.setShortDescription("short");
        episode.setMediumDescription("medium");
        episode.setLongDescription("long");
        
        
        assertEquals("short", generateShortSynopsis(episode).getValue());
        
        episode.setShortDescription(null);
        assertEquals("medium", generateShortSynopsis(episode).getValue());
        
        episode.setMediumDescription(null);
        assertEquals("long", generateShortSynopsis(episode).getValue());
    }
    
    private SynopsisType generateShortSynopsis(Item item) {
        GroupInformationType groupInfo = generator.generate(item, 
                Optional.<Series>absent(), Optional.<Brand>absent());
        
        return Iterables.find(groupInfo.getBasicDescription().getSynopsis(), 
                new Predicate<SynopsisType>() {

                    @Override
                    public boolean apply(SynopsisType input) {
                        return SynopsisLengthType.SHORT.equals(input.getLength());
                    }
                });
    }
    
    private SynopsisType getShortSynopsis(Iterable<SynopsisType> synopses) {
        return Iterables.find(synopses, new Predicate<SynopsisType>() {

            @Override
            public boolean apply(SynopsisType input) {
                return SynopsisLengthType.SHORT.equals(input.getLength());
            }
            
        });
    }
    
    

    private Brand createBrand() {
        Brand brand = new Brand();
        
        brand.setCanonicalUri("http://nitro.bbc.co.uk/programmes/b007n2qs");
        brand.setCurie("lf:e-184930");
        brand.setTitle("Northern Lights");
        brand.setDescription("Some brand description");
        brand.setImage("some brand image");
        brand.setPublisher(Publisher.BBC_NITRO);
        brand.setCertificates(ImmutableList.of(new Certificate("15", Countries.GB)));
        brand.setYear(2006);
        brand.setLanguages(ImmutableList.of("en"));
        brand.setMediaType(MediaType.VIDEO);
        brand.setSpecialization(Specialization.TV);
        brand.addAlias(new Alias("gb:amazon:asin", "brandAsin"));
        
        return brand;
    }
    
    private Series createSeries() {
        Series series = new Series();
        
        series.setCanonicalUri("http://nitro.bbc.co.uk/programmes/b020tm1g");
        series.setCurie("lf:e-179534");
        series.setTitle("Series 2");
        series.setDescription("Some series description");
        series.setImage("some series image");
        series.setPublisher(Publisher.BBC_NITRO);
        series.setCertificates(ImmutableList.of(new Certificate("15", Countries.GB)));
        series.setYear(2006);
        series.setLanguages(ImmutableList.of("en"));
        series.setMediaType(MediaType.VIDEO);
        series.setSpecialization(Specialization.TV);
        series.addAlias(new Alias("gb:amazon:asin", "seriesAsin"));
        
        ParentRef brandRef = new ParentRef("http://nitro.bbc.co.uk/programmes/b007n2qs");
        series.setParentRef(brandRef);
        series.withSeriesNumber(2);
        
        return series;
    }
    
    private Episode createEpisode() {
        Episode episode = new Episode();
        
        episode.setCanonicalUri("http://nitro.bbc.co.uk/programmes/b03dfd6d");
        episode.setCurie("lf:e-180014");
        episode.setTitle("Episode 1");
        episode.setDescription("some episode description");
        episode.setImage("some episode image");
        episode.setPublisher(Publisher.BBC_NITRO);
        episode.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        episode.setCertificates(ImmutableList.of(new Certificate("15", Countries.GB)));
        episode.setYear(2006);
        episode.setLanguages(ImmutableList.of("en"));
        episode.setMediaType(MediaType.VIDEO);
        episode.setSpecialization(Specialization.TV);
        episode.addAlias(new Alias("gb:amazon:asin", "episodeAsin"));
        
        Version version = new Version();
        version.setDuration(Duration.standardMinutes(45));
        episode.addVersion(version);
        
        ParentRef seriesRef = new ParentRef("http://nitro.bbc.co.uk/programmes/b020tm1g");
        episode.setSeriesRef(seriesRef);
        
        episode.setEpisodeNumber(5);
        episode.setSeriesNumber(2);
        
        return episode;
    }

    private Film createFilm() {
        Film film = new Film();
        
        film.setCanonicalUri("http://nitro.bbc.co.uk/programmes/b020tm1g");
        film.setCurie("lf:f-177221");
        film.setTitle("Dr. Strangelove");
        film.setDescription("Some film description");
        film.setImage("image");
        film.setPublisher(Publisher.BBC_NITRO);
        film.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        film.setCertificates(ImmutableList.of(new Certificate("PG", Countries.GB)));
        film.setYear(1963);
        film.setLanguages(ImmutableList.of("en"));
        film.setMediaType(MediaType.VIDEO);
        film.setSpecialization(Specialization.FILM);
        film.addAlias(new Alias("gb:amazon:asin", "filmAsin"));
        
        Version version = new Version();
        version.setDuration(Duration.standardMinutes(90));
        film.addVersion(version);
        
        return film;
    }
}
