package org.atlasapi.feeds.youview.nitro;

import static org.atlasapi.media.entity.CrewMember.Role.PRODUCTION_COMPANY;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.List;

import javax.xml.bind.JAXBElement;

import org.atlasapi.media.entity.Actor;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Person;
import org.atlasapi.persistence.content.PeopleResolver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import tva.metadata._2010.CreditsItemType;
import tva.metadata._2010.CreditsListType;
import tva.mpeg7._2008.NameComponentType;
import tva.mpeg7._2008.PersonNameType;

public class NitroCreditsItemGeneratorTest {

    private final Actor ACTOR = actor("Graham Norton", "Character Name", "12345");
    private final CrewMember ACTOR_WITHOUT_NAME = actor("", "Character Name", "12346");
    private final CrewMember CREW_MEMBER = crewMember("So Television", PRODUCTION_COMPANY, "67890");
    
    private final PeopleResolver peopleResolver = Mockito.mock(PeopleResolver.class);
    private final NitroCreditsItemGenerator generator = new NitroCreditsItemGenerator(peopleResolver);

    @Before
    public void setup() {
        when(peopleResolver.person("12345")).thenReturn(person("Graham", "Norton"));
        when(peopleResolver.person("12346")).thenReturn(person("", ""));
        when(peopleResolver.person("67890")).thenReturn(person(null, "So Television"));
    }

    private Optional<Person> person(String givenName, String familyName) {
        Person person = new Person();
        person.setGivenName(givenName);
        person.setFamilyName(familyName);

        return Optional.of(person);
    }

    @Test
    public void testCreditsListGeneration() {
        List<CreditsItemType> creditsItem = generateCreditsFor(people());
        assertEquals(2, creditsItem.size());

        // Improve credits check
    }
    
    @Test
    public void testCrewWithNoNameAreOmitted() {
        List<CreditsItemType> creditsItem = generateCreditsFor(ImmutableList.of(ACTOR_WITHOUT_NAME));
        assertTrue(creditsItem.isEmpty());
    }
    
    // GivenName is a mandatory field, so if we only have a family name
    // we should use the GivenName field rather than FamilyName, which
    // is optional
    @Test
    @SuppressWarnings("unchecked")
    public void testSingleNameIsAlwaysGivenName() {
        CreditsItemType credit = Iterables.getOnlyElement(generateCreditsFor(ImmutableList.of(CREW_MEMBER)));
        
        JAXBElement<PersonNameType> person = (JAXBElement<PersonNameType>) 
                Iterables.getOnlyElement(credit.getPersonNameOrPersonNameIDRefOrOrganizationName());
        
        JAXBElement<NameComponentType> name = (JAXBElement<NameComponentType>) 
                Iterables.getOnlyElement(person.getValue().getGivenNameOrLinkingNameOrFamilyName());
        
        assertThat(name.getName().getLocalPart(), is("GivenName"));
        
    }
    
    private List<CreditsItemType> generateCreditsFor(List<CrewMember> crew) {
        Item item = new Item();
        item.setPeople(crew);

        CreditsListType creditsList = generator.generate(item);
        return creditsList.getCreditsItem();
    }

    private List<CrewMember> people() {
        return ImmutableList.of(ACTOR, CREW_MEMBER);
    }

    private CrewMember crewMember(String name, CrewMember.Role role, String id) {
        CrewMember crewMember = new CrewMember();
        crewMember.withName(name);
        crewMember.withRole(role);
        crewMember.setCanonicalUri(id);

        return crewMember;
    }

    private Actor actor(String name, String characterName, String id) {
        Actor actor = new Actor();
        actor.withName(name);
        actor.withCharacter(characterName);
        actor.setCanonicalUri(id);

        return actor;
    }

}