package org.atlasapi.feeds.youview.amazon;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.metabroadcast.common.intl.Countries;
import org.atlasapi.feeds.tvanytime.GroupInformationGenerator;
import org.atlasapi.feeds.youview.NameComponentTypeEquivalence;
import org.atlasapi.feeds.youview.SynopsisTypeEquivalence;
import org.atlasapi.feeds.youview.genres.GenreMapping;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Alias;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Image;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.ParentRef;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Series;
import org.atlasapi.media.entity.Specialization;
import org.atlasapi.media.entity.Version;
import org.joda.time.Duration;
import org.junit.Test;
import tva.metadata._2010.BaseMemberOfType;
import tva.metadata._2010.BasicContentDescriptionType;
import tva.metadata._2010.ControlledTermType;
import tva.metadata._2010.CreditsItemType;
import tva.metadata._2010.GenreType;
import tva.metadata._2010.GroupInformationType;
import tva.metadata._2010.ProgramGroupTypeType;
import tva.metadata._2010.SynopsisLengthType;
import tva.metadata._2010.SynopsisType;
import tva.metadata.extended._2010.ContentPropertiesType;
import tva.metadata.extended._2010.ExtendedRelatedMaterialType;
import tva.metadata.extended._2010.StillImageContentAttributesType;
import tva.mpeg7._2008.ExtendedLanguageType;
import tva.mpeg7._2008.NameComponentType;
import tva.mpeg7._2008.PersonNameType;
import tva.mpeg7._2008.TitleType;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import static org.atlasapi.feeds.youview.amazon.AmazonGroupInformationGenerator.GROUP_INFO_SERVICE_ID;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class AmazonGroupInformationGeneratorTest extends org.atlasapi.TestsWithConfiguration {

    private static final Function<GenreType, String> TO_HREF = new Function<GenreType, String>() {
        @Override
        public String apply(GenreType input) {
            return input.getHref();
        }
    };

    private SynopsisTypeEquivalence SYNOPSIS_EQUIVALENCE = new SynopsisTypeEquivalence();
    private NameComponentTypeEquivalence NAME_EQUIVALENCE = new NameComponentTypeEquivalence();
    private IdGenerator idGenerator = new AmazonIdGenerator();
    private GenreMapping genreMapping = new AmazonGenreMapping();

    private final GroupInformationGenerator generator = new AmazonGroupInformationGenerator(idGenerator, genreMapping);

    @Test
    public void testRelatedMaterialNotGeneratedIfNullOrEmptyImageString() {
        //Films have default images injected if no image is present.
        Film film = createFilm();
        film.setImage(null);
        BasicContentDescriptionType filmDesc = generator.generate(film).getBasicDescription();
        assertTrue(!filmDesc.getRelatedMaterial().isEmpty());

        film.setImage("");
        filmDesc = generator.generate(film).getBasicDescription();
        assertTrue(!filmDesc.getRelatedMaterial().isEmpty());

        //The rest should have no image sent
        Episode ep = createEpisode();
        ep.setImage("");
        BasicContentDescriptionType epDesc =
                generator.generate(ep, Optional.absent(), Optional.absent())
                .getBasicDescription();
        assertTrue(epDesc.getRelatedMaterial().isEmpty());
    }


    @Test
    public void testServiceIdRefCreatedForTopLevelSeries() {
        Series series = createSeries();
        series.setParentRef(null);

        Episode child = createEpisode();
        child.setParentRef(ParentRef.parentRefFrom(series));
        child.setSeriesRef(null);
        child.setSeriesNumber(null);

        GroupInformationType groupInfo = generator.generate(series, Optional.<Brand>absent(), child);

        assertEquals(GROUP_INFO_SERVICE_ID, groupInfo.getServiceIDRef());
    }

    // TODO: uncomment tests after fixing genres
    /**
     * This tests both the conversion from amazon genre uris to the youview
     * genres, as well as de-duplication of the resulting genre collection
     */
    @Test
    public void testFilmGenreGeneration() {

//        Film film = createFilm();
//        film.setGenres(ImmutableSet.of(
//            "http://unbox.amazon.co.uk/genres/comedy",
//            "http://unbox.amazon.co.uk/genres/comedy/family",
//            "http://unbox.amazon.co.uk/genres/comedy/general"
//            ));
//
//        GroupInformationType groupInfo = generator.generate(film);
//
//        BasicContentDescriptionType desc = groupInfo.getBasicDescription();
//
//        List<String> genreHrefs = Lists.newArrayList(Iterables.transform(desc.getGenre(), TO_HREF));
//
//        List<String> expectedGenres = Lists.newArrayList(
//            "urn:tva:metadata:cs:OriginationCS:2005:5.7",
//            "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3",
//            "urn:tva:metadata:cs:ContentCS:2010:3.5.7",
//            "urn:tva:metadata:cs:IntendedAudienceCS:2010:4.9.9"
//            );
//
//        Collections.sort(expectedGenres);
//        Collections.sort(genreHrefs);
//
//        assertEquals(expectedGenres, genreHrefs);
    }

    @Test
    public void testTVGenreGeneration() {

//        Episode episode = createEpisode();
//        episode.setGenres(ImmutableSet.of(
//            "http://unbox.amazon.co.uk/genres/comedy",
//            "http://unbox.amazon.co.uk/genres/comedy/family"
//            ));
//
//        GroupInformationType groupInfo = generator.generate(episode, Optional.of(createSeries()), Optional.of(createBrand()));
//
//        BasicContentDescriptionType desc = groupInfo.getBasicDescription();
//
//        List<String> genreHrefs = Lists.newArrayList(Iterables.transform(desc.getGenre(), TO_HREF));
//
//        List<String> expectedGenres = Lists.newArrayList(
//            "urn:tva:metadata:cs:OriginationCS:2005:5.8",
//            "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3",
//            "urn:tva:metadata:cs:ContentCS:2010:3.5.7",
//            "urn:tva:metadata:cs:IntendedAudienceCS:2010:4.9.9"
//            );
//
//        Collections.sort(expectedGenres);
//        Collections.sort(genreHrefs);
//
//        assertEquals(expectedGenres, genreHrefs);
    }

    @Test
    public void testSynopsisGeneration() {
        Film film = createFilm();
        film.setDescription(
            "Some lengthy episode description, that manages to go well over the medium description cut-off and " +
            "thus shows the differences between short, medium and long descriptions, particularly regarding the " +
            "appending or not of ellipses."
            );

        GroupInformationType groupInfo = generator.generate(film);
        BasicContentDescriptionType desc = groupInfo.getBasicDescription();

        SynopsisType shortDesc = new SynopsisType();
        shortDesc.setLength(SynopsisLengthType.SHORT);
        shortDesc.setValue("Some lengthy episode description, that manages to go well over the medium description...");

        SynopsisType mediumDesc = new SynopsisType();
        mediumDesc.setLength(SynopsisLengthType.MEDIUM);
        mediumDesc.setValue(
            "Some lengthy episode description, that manages to go well over the medium description cut-off" +
            " and thus shows the differences between short, medium and long descriptions, particularly regarding the appending..."
            );

        SynopsisType longDesc = new SynopsisType();
        longDesc.setLength(SynopsisLengthType.LONG);
        longDesc.setValue(
            "Some lengthy episode description, that manages to go well over the medium description cut-off and " +
            "thus shows the differences between short, medium and long descriptions, particularly regarding the appending or " +
            "not of ellipses."
            );

        assertTrue(SYNOPSIS_EQUIVALENCE.pairwise().equivalent(
            ImmutableSet.of(shortDesc, mediumDesc, longDesc),
            desc.getSynopsis()
            ));
    }

    @Test
    public void testImageGeneration() {
        Film film = createFilm();
        film.setImage("https://m.media-amazon.com/images/S/aiv-image/jp/84e17b33-8910-491a-b8a1-1e81cbecce65-f193b98f-3d70-458c-b44e-c7d685779341_RGB_SD._SX320_SY240_.jpg");

        GroupInformationType groupInfo = generator.generate(film);

        BasicContentDescriptionType desc = groupInfo.getBasicDescription();
        ExtendedRelatedMaterialType relatedMaterial = (ExtendedRelatedMaterialType) Iterables.getOnlyElement(desc.getRelatedMaterial());

        assertEquals("urn:tva:metadata:cs:HowRelatedCS:2010:19", relatedMaterial.getHowRelated().getHref());
        assertEquals("urn:mpeg:mpeg7:cs:FileFormatCS:2001:1", relatedMaterial.getFormat().getHref());

        StillImageContentAttributesType imageProperties = (StillImageContentAttributesType)
                Iterables.getOnlyElement(relatedMaterial.getContentProperties().getContentAttributes());

        assertEquals(Integer.valueOf(1024), imageProperties.getWidth());
        assertEquals(Integer.valueOf(576), imageProperties.getHeight());

        ControlledTermType usage = Iterables.getOnlyElement(imageProperties.getIntendedUse());

        assertEquals("http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#role-primary", usage.getHref());
    }

    @Test
    public void testPersonGeneration() {
        Film film = createFilm();

        CrewMember georgeScott = new CrewMember();
        georgeScott.withName("George C. Scott");
        CrewMember stanley = new CrewMember();
        stanley.withName("Stanley Kubrick");
        film.setPeople(ImmutableList.of(georgeScott, stanley));

        GroupInformationType groupInfo = generator.generate(film);

        BasicContentDescriptionType desc = groupInfo.getBasicDescription();

        List<CreditsItemType> creditsList = desc.getCreditsList().getCreditsItem();
        for (CreditsItemType credit : creditsList) {
            assertEquals("urn:mpeg:mpeg7:cs:RoleCS:2001:UNKNOWN", credit.getRole());
        }

        Iterable<NameComponentType> people = Iterables.transform(
            creditsList,
            new Function<CreditsItemType, NameComponentType>() {
                @Override
                public NameComponentType apply(CreditsItemType input) {
                    PersonNameType firstPerson = (PersonNameType) Iterables.getOnlyElement(input.getPersonNameOrPersonNameIDRefOrOrganizationName()).getValue();
                    return (NameComponentType) Iterables.getOnlyElement(firstPerson.getGivenNameOrLinkingNameOrFamilyName()).getValue();
                }
            });

        NameComponentType scott = new NameComponentType();
        scott.setValue("George C. Scott");

        NameComponentType kubrick = new NameComponentType();
        kubrick.setValue("Stanley Kubrick");

        assertTrue(NAME_EQUIVALENCE.pairwise().equivalent(
                ImmutableSet.of(scott, kubrick),
                people
                ));
    }

    @Test
    public void testFilmGroupInformationGeneration() {
        GroupInformationType groupInfo = generator.generate(createFilm());

        assertEquals("crid://stage-metabroadcast.com/v3.amazon.co.uk:content:zzz", groupInfo.getGroupId());
        assertEquals(GROUP_INFO_SERVICE_ID, groupInfo.getServiceIDRef());
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
        GroupInformationType groupInfo = generator.generate(createEpisode(), Optional.of(createSeries()), Optional.of(createBrand()));

        assertEquals("crid://stage-metabroadcast.com/v3.amazon.co.uk:content:ssssh", groupInfo.getGroupId());

        BaseMemberOfType memberOf = Iterables.getOnlyElement(groupInfo.getMemberOf());
        assertEquals("crid://stage-metabroadcast.com/v3.amazon.co.uk:content:qq", memberOf.getCrid());
        assertEquals(Long.valueOf(1), memberOf.getIndex());

        ProgramGroupTypeType groupType = (ProgramGroupTypeType) groupInfo.getGroupType();
        assertEquals("programConcept", groupType.getValue());

        BasicContentDescriptionType desc = groupInfo.getBasicDescription();

        TitleType title = Iterables.getOnlyElement(desc.getTitle());
        assertEquals("Episode 1 - Drug Lords", title.getValue());
        assertEquals("main", Iterables.getOnlyElement(title.getType()));

        ExtendedLanguageType language = Iterables.getOnlyElement(desc.getLanguage());
        assertEquals("original", language.getType());
        assertEquals("en", language.getValue());
    }

    @Test
    public void testLanguageFallbackToUndefined() { //YV schema requires at least one lang.
        Episode episode = createEpisode();
        episode.setLanguages(ImmutableList.of()); //content with no language.
        GroupInformationType groupInfo = generator.generate(episode, Optional.of(createSeries()), Optional.of(createBrand()));
        BasicContentDescriptionType desc = groupInfo.getBasicDescription();
        ExtendedLanguageType language = Iterables.getOnlyElement(desc.getLanguage());
        assertEquals(AmazonGroupInformationGenerator.LANGUAGE_UNDEFINED, language.getValue());
    }

    @Test
    public void testSeriesGroupInformationGeneration() throws UnsupportedEncodingException {
        Series series = createSeries();

        GroupInformationType groupInfo = generator.generate(series, Optional.of(createBrand()), null);

        assertEquals("crid://stage-metabroadcast.com/v3.amazon.co.uk:content:qq", groupInfo.getGroupId());
        assertTrue(groupInfo.isOrdered());

        BaseMemberOfType memberOf = Iterables.getOnlyElement(groupInfo.getMemberOf());
        assertEquals("crid://stage-metabroadcast.com/v3.amazon.co.uk:content:wtf", memberOf.getCrid());
        assertEquals(Long.valueOf(2), memberOf.getIndex());

        ProgramGroupTypeType groupType = (ProgramGroupTypeType) groupInfo.getGroupType();
        assertEquals("series", groupType.getValue());

        BasicContentDescriptionType desc = groupInfo.getBasicDescription();

        TitleType title = Iterables.getOnlyElement(desc.getTitle());
        assertEquals("Season 2", title.getValue()); //Synthesized series title
        assertEquals("main", Iterables.getOnlyElement(title.getType()));

        ExtendedLanguageType language = Iterables.getOnlyElement(desc.getLanguage());
        assertEquals("original", language.getType());
        assertEquals("en", language.getValue());

        ExtendedRelatedMaterialType relatedMaterial = (ExtendedRelatedMaterialType) Iterables.getOnlyElement(desc.getRelatedMaterial());

        assertEquals(
                "https://m.media-amazon.com/images/S/pv-target-images/e7dd06959c77de9c53a629aaf2121c29cc096bd28065673db9feff9cc0283610.jpg",
                relatedMaterial.getMediaLocator().getMediaUri()
        );
    }

    @Test
    public void testSeriesGroupInformationGenerationFromEpisode() {
        Episode episode = createEpisode();
        episode.setId(1L);
        episode.setImage("episode image");

        Series series = createSeries();
        series.setImage(null);

        GroupInformationType groupInfo = generator.generate(series, Optional.of(createBrand()), episode);

        assertEquals("crid://stage-metabroadcast.com/v3.amazon.co.uk:content:qq", groupInfo.getGroupId());
        assertTrue(groupInfo.isOrdered());

        BaseMemberOfType memberOf = Iterables.getOnlyElement(groupInfo.getMemberOf());
        assertEquals("crid://stage-metabroadcast.com/v3.amazon.co.uk:content:wtf", memberOf.getCrid());
        assertEquals(Long.valueOf(2), memberOf.getIndex());

        ProgramGroupTypeType groupType = (ProgramGroupTypeType) groupInfo.getGroupType();
        assertEquals("series", groupType.getValue());

        BasicContentDescriptionType desc = groupInfo.getBasicDescription();

        TitleType title = Iterables.getOnlyElement(desc.getTitle());
        assertEquals("Season 2", title.getValue()); //Synthesized series title
        assertEquals("main", Iterables.getOnlyElement(title.getType()));

        ExtendedLanguageType language = Iterables.getOnlyElement(desc.getLanguage());
        assertEquals("original", language.getType());
        assertEquals("en", language.getValue());
    }

    @Test
    public void testBrandGroupInformationGeneration() {
        Episode episode = createEpisode();
        episode.setId(1L);
        episode.setImage("episode image");

        GroupInformationType groupInfo = generator.generate(createBrand(), episode);

        assertEquals("crid://stage-metabroadcast.com/v3.amazon.co.uk:content:wtf", groupInfo.getGroupId());
        assertEquals(GROUP_INFO_SERVICE_ID, groupInfo.getServiceIDRef());
        assertTrue(groupInfo.isOrdered());

        ProgramGroupTypeType groupType = (ProgramGroupTypeType) groupInfo.getGroupType();
        assertEquals("show", groupType.getValue());

        BasicContentDescriptionType desc = groupInfo.getBasicDescription();

        TitleType title = Iterables.getOnlyElement(desc.getTitle());
        assertEquals("Northern Lights", title.getValue());
        assertEquals("main", Iterables.getOnlyElement(title.getType()));

        ExtendedRelatedMaterialType relatedMaterial = (ExtendedRelatedMaterialType) Iterables.getOnlyElement(desc.getRelatedMaterial());

        assertEquals(
            "https://m.media-amazon.com/images/S/pv-target-images/d37f924d0def0d675336f503d5c1b24f58fa67b17a6f43ccb5797e00dae9f105.jpg",
            relatedMaterial.getMediaLocator().getMediaUri()
        );
    }

    @Test
    public void testSecondaryTitleGeneration() {
        Film film = createFilm();
        film.setId(1L);
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
        film.setId(1L);
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
    public void testImageDimensionsDefaultIfNoneProvided() {
        Film film = createFilm();
        film.setId(1L);
        Image image = new Image("someImageUri");
        image.setHeight(246);
        image.setWidth(572);
        film.setImages(ImmutableSet.of(image));

        GroupInformationType groupInfo = generator.generate(film);

        ExtendedRelatedMaterialType relatedMaterial = (ExtendedRelatedMaterialType) Iterables.getOnlyElement(groupInfo.getBasicDescription().getRelatedMaterial());
        ContentPropertiesType contentProperties = relatedMaterial.getContentProperties();
        StillImageContentAttributesType attributes = (StillImageContentAttributesType) Iterables.getOnlyElement(contentProperties.getContentAttributes());
        assertThat(attributes.getHeight(), is(equalTo(576))); // dimensions change to those of mbst images.
        assertThat(attributes.getWidth(), is(equalTo(1024)));
    }

    @Test
    public void testAmazonDefaultImageDimensions() {
        Film film = createFilm();
        film.setId(1L);
        film.setPublisher(Publisher.AMAZON_V3);
        film.setImages(ImmutableSet.<Image>of());

        GroupInformationType groupInfo = generator.generate(film);

        ExtendedRelatedMaterialType relatedMaterial = (ExtendedRelatedMaterialType) Iterables.getOnlyElement(groupInfo.getBasicDescription().getRelatedMaterial());
        ContentPropertiesType contentProperties = relatedMaterial.getContentProperties();
        StillImageContentAttributesType attributes = (StillImageContentAttributesType) Iterables.getOnlyElement(contentProperties.getContentAttributes());
        assertThat(attributes.getHeight(), is(equalTo(576))); // default dimensions are those of mbst images.
        assertThat(attributes.getWidth(), is(equalTo(1024)));
    }

    @Test
    public void testAmazonConstants() {
        Film film = createFilm();
        film.setId(1L);
        film.setPublisher(Publisher.AMAZON_V3);

        GroupInformationType groupInfo = generator.generate(film);
        assertEquals("crid://stage-metabroadcast.com/v3.amazon.co.uk:content:c", groupInfo.getGroupId());
        assertEquals(GROUP_INFO_SERVICE_ID, groupInfo.getServiceIDRef());
    }


    private Brand createBrand() {
        Brand brand = new Brand();

        brand.setId(12045L);
        brand.setCanonicalUri("http://v3.amazon.co.uk/amzn1.dv.gti.3ab3ab21-611d-757e-aeaa-44c007c9233d:GB");
        brand.setCurie("lf:e-184930");
        brand.setTitle("Northern Lights");
        brand.setDescription("Some brand description");
        brand.setImage("https://m.media-amazon.com/images/S/pv-target-images/d37f924d0def0d675336f503d5c1b24f58fa67b17a6f43ccb5797e00dae9f105.jpg");
        brand.setPublisher(Publisher.AMAZON_V3);
        brand.setCertificates(ImmutableList.of(new Certificate("15", Countries.GB)));
        brand.setYear(2006);
        brand.setLanguages(ImmutableList.of("en"));
        brand.setMediaType(MediaType.VIDEO);
        brand.setSpecialization(Specialization.TV);
        brand.addAlias(new Alias("amazon:asin:3", "brandAsin"));

        CrewMember robson = new CrewMember();
        robson.withName("Robson Green");
        CrewMember mark = new CrewMember();
        mark.withName("Mark Benton");
        brand.setPeople(ImmutableList.of(robson, mark));

        return brand;
    }

    private Series createSeries() {
        Series series = new Series();

        series.setId(308L);
        series.setCanonicalUri("http://v3.amazon.co.uk/amzn1.dv.gti.02b75779-552b-5388-2615-8f61eb7c2063:GB");
        series.setCurie("lf:e-179534");
        series.setTitle("Series 2");
        series.setDescription("Some series description");
        series.setImage("https://m.media-amazon.com/images/S/pv-target-images/e7dd06959c77de9c53a629aaf2121c29cc096bd28065673db9feff9cc0283610.jpg");
        series.setPublisher(Publisher.AMAZON_V3);
        series.setCertificates(ImmutableList.of(new Certificate("15", Countries.GB)));
        series.setYear(2006);
        series.setLanguages(ImmutableList.of("en"));
        series.setMediaType(MediaType.VIDEO);
        series.setSpecialization(Specialization.TV);
        series.addAlias(new Alias("amazon:asin:3", "seriesAsin"));

        CrewMember robson = new CrewMember();
        robson.withName("Robson Green");
        CrewMember mark = new CrewMember();
        mark.withName("Mark Benton");
        series.setPeople(ImmutableList.of(robson, mark));

        ParentRef brandRef = new ParentRef("http://v3.amazon.co.uk/amzn1.dv.gti.3ab3ab21-611d-757e-aeaa-44c007c9233d:GB");
        series.setParentRef(brandRef);
        series.withSeriesNumber(2);

        return series;
    }

    private Episode createEpisode() {
        Episode episode = new Episode();

        episode.setId(7174445L);
        episode.setCanonicalUri("http://v3.amazon.co.uk/amzn1.dv.gti.2eb76463-7bba-4ef6-f38d-a1e59a40dc4e:GB");
        episode.setCurie("lf:e-180014");
        episode.setTitle("Episode 1 - Drug Lords");
        episode.setDescription("some episode description");
        episode.setImage("some episode image");
        episode.setPublisher(Publisher.AMAZON_V3);
        episode.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        episode.setCertificates(ImmutableList.of(new Certificate("15", Countries.GB)));
        episode.setYear(2006);
        episode.setLanguages(ImmutableList.of("en"));
        episode.setMediaType(MediaType.VIDEO);
        episode.setSpecialization(Specialization.TV);
        episode.addAlias(new Alias("amazon:asin:3", "episodeAsin"));

        Version version = new Version();
        version.setDuration(Duration.standardMinutes(45));
        episode.addVersion(version);

        ParentRef seriesRef = new ParentRef("http://v3.amazon.co.uk/amzn1.dv.gti.02b75779-552b-5388-2615-8f61eb7c2063:GB");
        episode.setSeriesRef(seriesRef);

        episode.setEpisodeNumber(1);
        episode.setSeriesNumber(2);

        return episode;
    }

    private Film createFilm() {
        Film film = new Film();
        film.setId(14383L);
        film.setCanonicalUri("http://v3.amazon.co.uk/amzn1.dv.gti.e2b8b2a4-94c7-a622-aa95-1af60936b0cd:GB");
        film.setCurie("lf:f-177221");
        film.setTitle("Dr. Strangelove");
        film.setDescription("Some film description");
        film.setImage("image");
        film.setPublisher(Publisher.AMAZON_V3);
        film.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        film.setCertificates(ImmutableList.of(new Certificate("PG", Countries.GB)));
        film.setYear(1963);
        film.setLanguages(ImmutableList.of("en"));
        film.setMediaType(MediaType.VIDEO);
        film.setSpecialization(Specialization.FILM);
        film.addAlias(new Alias("amazon:asin:3", "filmAsin"));

        Version version = new Version();
        version.setDuration(Duration.standardMinutes(90));
        film.addVersion(version);

        return film;
    }
}
