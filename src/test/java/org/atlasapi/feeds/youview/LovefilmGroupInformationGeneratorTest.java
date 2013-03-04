package org.atlasapi.feeds.youview;

import static org.hamcrest.Matchers.isOneOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.atlasapi.feeds.tvanytime.GroupInformationGenerator;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
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
import tva.metadata.extended._2010.ExtendedRelatedMaterialType;
import tva.metadata.extended._2010.StillImageContentAttributesType;
import tva.mpeg7._2008.ExtendedLanguageType;
import tva.mpeg7._2008.NameComponentType;
import tva.mpeg7._2008.PersonNameType;
import tva.mpeg7._2008.TitleType;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.intl.Countries;

public class LovefilmGroupInformationGeneratorTest {
    
    private static final GroupInformationGenerator generator = new LovefilmGroupInformationGenerator();

    // TODO update with roles once these are confirmed
    
    @Test
    public void testFilmOnDemandGeneration() {
        GroupInformationType groupInfo = generator.generate(createFilm());

        assertEquals("crid://lovefilm.com/product/177221", groupInfo.getGroupId());
        assertEquals("http://lovefilm.com", groupInfo.getServiceIDRef());
        ProgramGroupTypeType groupType = (ProgramGroupTypeType) groupInfo.getGroupType();
        assertEquals("programConcept", groupType.getValue());
        
        BasicContentDescriptionType desc = groupInfo.getBasicDescription();
        
        TitleType title = Iterables.getOnlyElement(desc.getTitle());
        assertEquals("Dr. Strangelove", title.getValue());
        assertEquals("main", Iterables.getOnlyElement(title.getType()));
        
        SynopsisType synopsis = Iterables.getOnlyElement(desc.getSynopsis());
        assertEquals("The film is set at the height of the tensions between Russia and the United States", synopsis.getValue());
        assertEquals(SynopsisLengthType.SHORT, synopsis.getLength());
        
        // TODO full list of genres won't be possible until genre mapping is known
        GenreType first = Iterables.get(desc.getGenre(), 0);
        GenreType second = Iterables.get(desc.getGenre(), 1);
        
        assertThat(first.getType(), isOneOf("main", "other"));
        assertThat(second.getType(), isOneOf("main", "other"));
        assertTrue(!first.getType().equals(second.getType()));
        assertThat(first.getHref(), isOneOf(
            "urn:tva:metadata:cs:OriginationCS:2005:5.7",
            "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3"
        ));
        assertThat(second.getHref(), isOneOf(
                "urn:tva:metadata:cs:OriginationCS:2005:5.7",
                "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3"
            ));
        assertTrue(!first.getHref().equals(second.getHref()));

        ExtendedLanguageType language = Iterables.getOnlyElement(desc.getLanguage());
        assertEquals("original", language.getType());
        assertEquals("en", language.getValue());

        CreditsItemType firstCredit = Iterables.get(desc.getCreditsList().getCreditsItem(), 0);
        CreditsItemType secondCredit = Iterables.get(desc.getCreditsList().getCreditsItem(), 1);
        
        PersonNameType firstPerson = (PersonNameType) Iterables.getOnlyElement(firstCredit.getPersonNameOrPersonNameIDRefOrOrganizationName()).getValue();
        PersonNameType secondPerson = (PersonNameType) Iterables.getOnlyElement(secondCredit.getPersonNameOrPersonNameIDRefOrOrganizationName()).getValue();
        
        NameComponentType firstName = (NameComponentType) Iterables.getOnlyElement(firstPerson.getGivenNameOrLinkingNameOrFamilyName()).getValue();
        NameComponentType secondName = (NameComponentType) Iterables.getOnlyElement(secondPerson.getGivenNameOrLinkingNameOrFamilyName()).getValue();
        
        assertThat(firstName.getValue(), isOneOf(
            "George C. Scott",
            "Stanley Kubrick"
        ));
        assertThat(secondName.getValue(), isOneOf(
            "George C. Scott",
            "Stanley Kubrick"
        ));

        ExtendedRelatedMaterialType relatedMaterial = (ExtendedRelatedMaterialType) Iterables.getOnlyElement(desc.getRelatedMaterial());

        assertEquals("urn:tva:metadata:cs:HowRelatedCS:2010:19", relatedMaterial.getHowRelated().getHref());
        assertEquals("urn:mpeg:mpeg7:cs:FileFormatCS:2001:1", relatedMaterial.getFormat().getHref());

        assertEquals(
            "http://www.lovefilm.com/lovefilm/images/products/heroshots/1/177221-large.jpg", 
            relatedMaterial.getMediaLocator().getMediaUri()
        );

        StillImageContentAttributesType imageProperties = (StillImageContentAttributesType)
                Iterables.getOnlyElement(relatedMaterial.getContentProperties().getContentAttributes());
        
        assertEquals(Integer.valueOf(640), imageProperties.getWidth());
        assertEquals(Integer.valueOf(360), imageProperties.getHeight());
        
        ControlledTermType firstUse = Iterables.get(imageProperties.getIntendedUse(), 0);
        ControlledTermType secondUse = Iterables.get(imageProperties.getIntendedUse(), 1);
        
        assertThat(firstUse.getHref(), isOneOf(
            "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#role-primary",
            "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-production_still"
        ));
        assertThat(secondUse.getHref(), isOneOf(
            "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#role-primary",
            "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-production_still"
        ));
        assertTrue(!firstUse.equals(secondUse));
    }

    @Test
    public void testEpisodeOnDemandGeneration() {
        GroupInformationType groupInfo = generator.generate(createEpisode());

        assertEquals("crid://lovefilm.com/product/180014", groupInfo.getGroupId());
        
        BaseMemberOfType memberOf = Iterables.getOnlyElement(groupInfo.getMemberOf());
        assertEquals("crid://lovefilm.com/product/179534", memberOf.getCrid());
        assertEquals(Long.valueOf(5), memberOf.getIndex());
        
        ProgramGroupTypeType groupType = (ProgramGroupTypeType) groupInfo.getGroupType();
        assertEquals("programConcept", groupType.getValue());
        
        BasicContentDescriptionType desc = groupInfo.getBasicDescription();
        
        TitleType title = Iterables.getOnlyElement(desc.getTitle());
        assertEquals("Episode 1", title.getValue());
        assertEquals("main", Iterables.getOnlyElement(title.getType()));
        
        SynopsisType synopsis = Iterables.getOnlyElement(desc.getSynopsis());
        assertEquals("Some episode description", synopsis.getValue());
        assertEquals(SynopsisLengthType.SHORT, synopsis.getLength());
        
        // TODO full list of genres won't be possible until genre mapping is known
        GenreType first = Iterables.get(desc.getGenre(), 0);
        GenreType second = Iterables.get(desc.getGenre(), 1);
        
        assertThat(first.getType(), isOneOf("main", "other"));
        assertThat(second.getType(), isOneOf("main", "other"));
        assertTrue(!first.getType().equals(second.getType()));
        assertThat(first.getHref(), isOneOf(
            "urn:tva:metadata:cs:OriginationCS:2005:5.8",
            "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3"
        ));
        assertThat(second.getHref(), isOneOf(
            "urn:tva:metadata:cs:OriginationCS:2005:5.8",
            "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3"
        ));
        assertTrue(!first.getHref().equals(second.getHref()));

        ExtendedLanguageType language = Iterables.getOnlyElement(desc.getLanguage());
        assertEquals("original", language.getType());
        assertEquals("en", language.getValue());

        CreditsItemType firstCredit = Iterables.get(desc.getCreditsList().getCreditsItem(), 0);
        CreditsItemType secondCredit = Iterables.get(desc.getCreditsList().getCreditsItem(), 1);
        
        PersonNameType firstPerson = (PersonNameType) Iterables.getOnlyElement(firstCredit.getPersonNameOrPersonNameIDRefOrOrganizationName()).getValue();
        PersonNameType secondPerson = (PersonNameType) Iterables.getOnlyElement(secondCredit.getPersonNameOrPersonNameIDRefOrOrganizationName()).getValue();
        
        NameComponentType firstName = (NameComponentType) Iterables.getOnlyElement(firstPerson.getGivenNameOrLinkingNameOrFamilyName()).getValue();
        NameComponentType secondName = (NameComponentType) Iterables.getOnlyElement(secondPerson.getGivenNameOrLinkingNameOrFamilyName()).getValue();
        
        assertThat(firstName.getValue(), isOneOf(
            "Robson Green",
            "Mark Benton"
        ));
        assertThat(secondName.getValue(), isOneOf(
            "Robson Green",
            "Mark Benton"
        ));

        ExtendedRelatedMaterialType relatedMaterial = (ExtendedRelatedMaterialType) Iterables.getOnlyElement(desc.getRelatedMaterial());

        assertEquals("urn:tva:metadata:cs:HowRelatedCS:2010:19", relatedMaterial.getHowRelated().getHref());
        assertEquals("urn:mpeg:mpeg7:cs:FileFormatCS:2001:1", relatedMaterial.getFormat().getHref());

        assertEquals(
            "http://www.lovefilm.com/lovefilm/images/products/heroshots/0/137640-large.jpg", 
            relatedMaterial.getMediaLocator().getMediaUri()
        );

        StillImageContentAttributesType imageProperties = (StillImageContentAttributesType)
                Iterables.getOnlyElement(relatedMaterial.getContentProperties().getContentAttributes());
        
        assertEquals(Integer.valueOf(640), imageProperties.getWidth());
        assertEquals(Integer.valueOf(360), imageProperties.getHeight());
        
        ControlledTermType firstUse = Iterables.get(imageProperties.getIntendedUse(), 0);
        ControlledTermType secondUse = Iterables.get(imageProperties.getIntendedUse(), 1);
        
        assertThat(firstUse.getHref(), isOneOf(
            "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#role-primary",
            "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-production_still"
        ));
        assertThat(secondUse.getHref(), isOneOf(
            "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#role-primary",
            "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-production_still"
        ));
        assertTrue(!firstUse.equals(secondUse));
    }
    
    @Test
    public void testSeriesOnDemandGeneration() {
        GroupInformationType groupInfo = generator.generate(createSeries());

        assertEquals("crid://lovefilm.com/product/179534", groupInfo.getGroupId());
        assertTrue(groupInfo.isOrdered());
        
        BaseMemberOfType memberOf = Iterables.getOnlyElement(groupInfo.getMemberOf());
        assertEquals("crid://lovefilm.com/product/184930", memberOf.getCrid());
        assertEquals(Long.valueOf(2), memberOf.getIndex());
        
        ProgramGroupTypeType groupType = (ProgramGroupTypeType) groupInfo.getGroupType();
        assertEquals("series", groupType.getValue());
        
        BasicContentDescriptionType desc = groupInfo.getBasicDescription();
        
        TitleType title = Iterables.getOnlyElement(desc.getTitle());
        assertEquals("Series 2", title.getValue());
        assertEquals("main", Iterables.getOnlyElement(title.getType()));
        
        SynopsisType synopsis = Iterables.getOnlyElement(desc.getSynopsis());
        assertEquals("Some series description", synopsis.getValue());
        assertEquals(SynopsisLengthType.SHORT, synopsis.getLength());
        
        // TODO full list of genres won't be possible until genre mapping is known
        GenreType first = Iterables.get(desc.getGenre(), 0);
        GenreType second = Iterables.get(desc.getGenre(), 1);
        
        assertThat(first.getType(), isOneOf("main", "other"));
        assertThat(second.getType(), isOneOf("main", "other"));
        assertTrue(!first.getType().equals(second.getType()));
        assertThat(first.getHref(), isOneOf(
            "urn:tva:metadata:cs:OriginationCS:2005:5.8",
            "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3"
        ));
        assertThat(second.getHref(), isOneOf(
            "urn:tva:metadata:cs:OriginationCS:2005:5.8",
            "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3"
        ));
        assertTrue(!first.getHref().equals(second.getHref()));

        ExtendedLanguageType language = Iterables.getOnlyElement(desc.getLanguage());
        assertEquals("original", language.getType());
        assertEquals("en", language.getValue());

        CreditsItemType firstCredit = Iterables.get(desc.getCreditsList().getCreditsItem(), 0);
        CreditsItemType secondCredit = Iterables.get(desc.getCreditsList().getCreditsItem(), 1);
        
        PersonNameType firstPerson = (PersonNameType) Iterables.getOnlyElement(firstCredit.getPersonNameOrPersonNameIDRefOrOrganizationName()).getValue();
        PersonNameType secondPerson = (PersonNameType) Iterables.getOnlyElement(secondCredit.getPersonNameOrPersonNameIDRefOrOrganizationName()).getValue();
        
        NameComponentType firstName = (NameComponentType) Iterables.getOnlyElement(firstPerson.getGivenNameOrLinkingNameOrFamilyName()).getValue();
        NameComponentType secondName = (NameComponentType) Iterables.getOnlyElement(secondPerson.getGivenNameOrLinkingNameOrFamilyName()).getValue();
        
        assertThat(firstName.getValue(), isOneOf(
            "Robson Green",
            "Mark Benton"
        ));
        assertThat(secondName.getValue(), isOneOf(
            "Robson Green",
            "Mark Benton"
        ));

        ExtendedRelatedMaterialType relatedMaterial = (ExtendedRelatedMaterialType) Iterables.getOnlyElement(desc.getRelatedMaterial());

        assertEquals("urn:tva:metadata:cs:HowRelatedCS:2010:19", relatedMaterial.getHowRelated().getHref());
        assertEquals("urn:mpeg:mpeg7:cs:FileFormatCS:2001:1", relatedMaterial.getFormat().getHref());

        assertEquals(
            "http://www.lovefilm.com/lovefilm/images/products/heroshots/0/179534-large.jpg", 
            relatedMaterial.getMediaLocator().getMediaUri()
        );

        StillImageContentAttributesType imageProperties = (StillImageContentAttributesType)
                Iterables.getOnlyElement(relatedMaterial.getContentProperties().getContentAttributes());
        
        assertEquals(Integer.valueOf(640), imageProperties.getWidth());
        assertEquals(Integer.valueOf(360), imageProperties.getHeight());
        
        ControlledTermType firstUse = Iterables.get(imageProperties.getIntendedUse(), 0);
        ControlledTermType secondUse = Iterables.get(imageProperties.getIntendedUse(), 1);
        
        assertThat(firstUse.getHref(), isOneOf(
            "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#role-primary",
            "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-production_still"
        ));
        assertThat(secondUse.getHref(), isOneOf(
            "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#role-primary",
            "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-production_still"
        ));
        assertTrue(!firstUse.equals(secondUse));
    }
    
    @Test
    public void testBrandOnDemandGeneration() {
        GroupInformationType groupInfo = generator.generate(createBrand());

        assertEquals("crid://lovefilm.com/product/184930", groupInfo.getGroupId());
        assertEquals("http://lovefilm.com", groupInfo.getServiceIDRef());
        assertTrue(groupInfo.isOrdered());
                
        ProgramGroupTypeType groupType = (ProgramGroupTypeType) groupInfo.getGroupType();
        assertEquals("show", groupType.getValue());
        
        BasicContentDescriptionType desc = groupInfo.getBasicDescription();
        
        TitleType title = Iterables.getOnlyElement(desc.getTitle());
        assertEquals("Northern Lights", title.getValue());
        assertEquals("main", Iterables.getOnlyElement(title.getType()));
        
        SynopsisType synopsis = Iterables.getOnlyElement(desc.getSynopsis());
        assertEquals("Some brand description", synopsis.getValue());
        assertEquals(SynopsisLengthType.SHORT, synopsis.getLength());
        
        // TODO full list of genres won't be possible until genre mapping is known
        GenreType first = Iterables.get(desc.getGenre(), 0);
        GenreType second = Iterables.get(desc.getGenre(), 1);
        
        assertThat(first.getType(), isOneOf("main", "other"));
        assertThat(second.getType(), isOneOf("main", "other"));
        assertTrue(!first.getType().equals(second.getType()));
        assertThat(first.getHref(), isOneOf(
            "urn:tva:metadata:cs:OriginationCS:2005:5.8",
            "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3"
        ));
        assertThat(second.getHref(), isOneOf(
            "urn:tva:metadata:cs:OriginationCS:2005:5.8",
            "urn:tva:metadata:cs:MediaTypeCS:2005:7.1.3"
        ));
        assertTrue(!first.getHref().equals(second.getHref()));

        ExtendedLanguageType language = Iterables.getOnlyElement(desc.getLanguage());
        assertEquals("original", language.getType());
        assertEquals("en", language.getValue());

        CreditsItemType firstCredit = Iterables.get(desc.getCreditsList().getCreditsItem(), 0);
        CreditsItemType secondCredit = Iterables.get(desc.getCreditsList().getCreditsItem(), 1);
        
        PersonNameType firstPerson = (PersonNameType) Iterables.getOnlyElement(firstCredit.getPersonNameOrPersonNameIDRefOrOrganizationName()).getValue();
        PersonNameType secondPerson = (PersonNameType) Iterables.getOnlyElement(secondCredit.getPersonNameOrPersonNameIDRefOrOrganizationName()).getValue();
        
        NameComponentType firstName = (NameComponentType) Iterables.getOnlyElement(firstPerson.getGivenNameOrLinkingNameOrFamilyName()).getValue();
        NameComponentType secondName = (NameComponentType) Iterables.getOnlyElement(secondPerson.getGivenNameOrLinkingNameOrFamilyName()).getValue();
        
        assertThat(firstName.getValue(), isOneOf(
            "Robson Green",
            "Mark Benton"
        ));
        assertThat(secondName.getValue(), isOneOf(
            "Robson Green",
            "Mark Benton"
        ));

        ExtendedRelatedMaterialType relatedMaterial = (ExtendedRelatedMaterialType) Iterables.getOnlyElement(desc.getRelatedMaterial());

        assertEquals("urn:tva:metadata:cs:HowRelatedCS:2010:19", relatedMaterial.getHowRelated().getHref());
        assertEquals("urn:mpeg:mpeg7:cs:FileFormatCS:2001:1", relatedMaterial.getFormat().getHref());

        assertEquals(
            "http://www.lovefilm.com/lovefilm/images/products/heroshots/0/184930-large.jpg", 
            relatedMaterial.getMediaLocator().getMediaUri()
        );

        StillImageContentAttributesType imageProperties = (StillImageContentAttributesType)
                Iterables.getOnlyElement(relatedMaterial.getContentProperties().getContentAttributes());
        
        assertEquals(Integer.valueOf(640), imageProperties.getWidth());
        assertEquals(Integer.valueOf(360), imageProperties.getHeight());
        
        ControlledTermType firstUse = Iterables.get(imageProperties.getIntendedUse(), 0);
        ControlledTermType secondUse = Iterables.get(imageProperties.getIntendedUse(), 1);
        
        assertThat(firstUse.getHref(), isOneOf(
            "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#role-primary",
            "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-production_still"
        ));
        assertThat(secondUse.getHref(), isOneOf(
            "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#role-primary",
            "http://refdata.youview.com/mpeg7cs/YouViewImageUsageCS/2010-09-23#source-production_still"
        ));
        assertTrue(!firstUse.equals(secondUse));
    }
    
    private Brand createBrand() {
        Brand brand = new Brand();
        
        brand.setCanonicalUri("http://lovefilm.com/episodes/184930");
        brand.setCurie("lf:e-184930");
        brand.setGenres(ImmutableList.of(
            "http://lovefilm.com/genres/comedy", 
            "http://lovefilm.com/genres/television"
        ));
        brand.setTitle("Northern Lights");
        brand.setDescription("Some brand description");
        brand.setImage("http://www.lovefilm.com/lovefilm/images/products/heroshots/0/184930-large.jpg");
        brand.setPublisher(Publisher.LOVEFILM);
        brand.setCertificates(ImmutableList.of(new Certificate("15", Countries.GB)));
        brand.setYear(2006);
        brand.setLanguages(ImmutableList.of("en"));
        brand.setMediaType(MediaType.VIDEO);
        brand.setSpecialization(Specialization.TV);
        
        CrewMember robson = new CrewMember();
        robson.withName("Robson Green");
        CrewMember mark = new CrewMember();
        mark.withName("Mark Benton");
        brand.setPeople(ImmutableList.of(robson, mark));
        
        return brand;
    }
    
    private Series createSeries() {
        Series series = new Series();
        
        series.setCanonicalUri("http://lovefilm.com/episodes/179534");
        series.setCurie("lf:e-179534");
        series.setGenres(ImmutableList.of(
            "http://lovefilm.com/genres/comedy", 
            "http://lovefilm.com/genres/television"
        ));
        series.setTitle("Series 2");
        series.setDescription("Some series description");
        series.setImage("http://www.lovefilm.com/lovefilm/images/products/heroshots/0/179534-large.jpg");
        series.setPublisher(Publisher.LOVEFILM);
        series.setCertificates(ImmutableList.of(new Certificate("15", Countries.GB)));
        series.setYear(2006);
        series.setLanguages(ImmutableList.of("en"));
        series.setMediaType(MediaType.VIDEO);
        series.setSpecialization(Specialization.TV);
        
        CrewMember robson = new CrewMember();
        robson.withName("Robson Green");
        CrewMember mark = new CrewMember();
        mark.withName("Mark Benton");
        series.setPeople(ImmutableList.of(robson, mark));
        
        ParentRef brandRef = new ParentRef("http://lovefilm.com/series/184930");
        series.setParentRef(brandRef);
        series.withSeriesNumber(2);
        
        return series;
    }
    
    private Episode createEpisode() {
        Episode episode = new Episode();
        
        episode.setCanonicalUri("http://lovefilm.com/episodes/180014");
        episode.setCurie("lf:e-180014");
        episode.setGenres(ImmutableList.of(
            "http://lovefilm.com/genres/comedy", 
            "http://lovefilm.com/genres/television"
        ));
        episode.setTitle("Episode 1");
        episode.setDescription("Some episode description");
        episode.setImage("http://www.lovefilm.com/lovefilm/images/products/heroshots/0/137640-large.jpg");
        episode.setPublisher(Publisher.LOVEFILM);
        episode.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        episode.setCertificates(ImmutableList.of(new Certificate("15", Countries.GB)));
        episode.setYear(2006);
        episode.setLanguages(ImmutableList.of("en"));
        episode.setMediaType(MediaType.VIDEO);
        episode.setSpecialization(Specialization.TV);
        
        CrewMember robson = new CrewMember();
        robson.withName("Robson Green");
        CrewMember mark = new CrewMember();
        mark.withName("Mark Benton");
        episode.setPeople(ImmutableList.of(robson, mark));
        
        Version version = new Version();
        version.setDuration(Duration.standardMinutes(45));
        episode.addVersion(version);
        
        ParentRef seriesRef = new ParentRef("http://lovefilm.com/series/179534");
        episode.setSeriesRef(seriesRef);
        
        episode.setEpisodeNumber(5);
        episode.setSeriesNumber(2);
        
        return episode;
    }

    private Film createFilm() {
        Film film = new Film();
        
        film.setCanonicalUri("http://lovefilm.com/films/177221");
        film.setCurie("lf:f-177221");
        film.setTitle("Dr. Strangelove");
        film.setDescription("The film is set at the height of the tensions between Russia and the United States");
        film.setGenres(ImmutableList.of("http://lovefilm.com/genres/comedy"));
        film.setImage("http://www.lovefilm.com/lovefilm/images/products/heroshots/1/177221-large.jpg");
        film.setPublisher(Publisher.LOVEFILM);
        film.setCountriesOfOrigin(ImmutableSet.of(Countries.GB));
        film.setCertificates(ImmutableList.of(new Certificate("PG", Countries.GB)));
        film.setYear(1963);
        film.setLanguages(ImmutableList.of("en"));
        film.setMediaType(MediaType.VIDEO);
        film.setSpecialization(Specialization.FILM);
        
        CrewMember georgeScott = new CrewMember();
        georgeScott.withName("George C. Scott");
        CrewMember stanley = new CrewMember();
        stanley.withName("Stanley Kubrick");
        film.setPeople(ImmutableList.of(georgeScott, stanley));
        
        Version version = new Version();
        version.setDuration(Duration.standardMinutes(90));
        film.addVersion(version);
        
        return film;
    }

}
