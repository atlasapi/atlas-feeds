package org.atlasapi.feeds.youview.nitro;

import java.math.BigInteger;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import org.atlasapi.feeds.tvanytime.CreditsItemGenerator;
import org.atlasapi.media.entity.Actor;
import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.CrewMember;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Person;
import org.atlasapi.persistence.content.PeopleResolver;

import com.google.common.base.Optional;
import com.google.common.base.Strings;

import tva.metadata._2010.CreditsItemType;
import tva.metadata._2010.CreditsListType;
import tva.mpeg7._2008.NameComponentType;
import tva.mpeg7._2008.PersonNameType;

public class NitroCreditsItemGenerator implements CreditsItemGenerator {

    private static final String FAMILY_NAME_TYPE = "FamilyName";
    private static final String GIVEN_NAME_TYPE = "GivenName";
    
    private final PeopleResolver peopleResolver;

    public NitroCreditsItemGenerator(PeopleResolver peopleResolver) {
        this.peopleResolver = peopleResolver;
    }

    @Override
    public CreditsListType generate(Content content) {
        List<CrewMember> crewMembers = content.people();
        CreditsListType creditsListType = new CreditsListType();

        int index = 0;
        for (CrewMember crewMember : crewMembers) {
            Optional<Person> person = peopleResolver.person(crewMember.getCanonicalUri());
            if (person.isPresent() 
                    && (!Strings.isNullOrEmpty(person.get().getFamilyName()) 
                        || !Strings.isNullOrEmpty(person.get().getGivenName()))) {
                creditsListType.getCreditsItem().add(createCreditsItem(crewMember, person, index));
                index++;
            }
        }

        return creditsListType;
    }

    private CreditsItemType createCreditsItem(CrewMember crewMember,
            Optional<Person> optionalPerson, int index) {
        CreditsItemType credits = new CreditsItemType();
        credits.setRole(crewMember.role().requireTvaUri());
        credits.setIndex(BigInteger.valueOf(index));
        setCharacterNameIfPresent(credits, crewMember);

        if (optionalPerson.isPresent()) {
            Person person = optionalPerson.get();
            PersonNameType personName = new PersonNameType();

            if (person.getGivenName() == null) {
                setName(person.getFamilyName(), GIVEN_NAME_TYPE, personName);
            } else {
                setName(person.getGivenName(), GIVEN_NAME_TYPE, personName);
                setName(person.getFamilyName(), FAMILY_NAME_TYPE, personName);
            }
            
            JAXBElement<PersonNameType> personNameElem = new JAXBElement<PersonNameType>(new QName(
                    "urn:tva:metadata:2010",
                    "PersonName"), PersonNameType.class, personName);
            credits.getPersonNameOrPersonNameIDRefOrOrganizationName().add(personNameElem);
        }

        return credits;
    }

    private void setCharacterNameIfPresent(CreditsItemType credits, CrewMember crewMember) {
        if (crewMember instanceof Actor) {
            Actor actor = (Actor) crewMember;
            if (!Strings.isNullOrEmpty(actor.character())) {
                credits.getCharacter().add(generateCharacterName(actor.character()));
            }
        }
    }

    private PersonNameType generateCharacterName(String character) {
        PersonNameType personName = new PersonNameType();

        NameComponentType nameComponent = new NameComponentType();
        nameComponent.setValue(character);

        JAXBElement<NameComponentType> characterName = new JAXBElement<>(new QName(
                "urn:tva:mpeg7:2008",
                "GivenName"), NameComponentType.class, nameComponent);

        personName.getGivenNameOrLinkingNameOrFamilyName().add(characterName);

        return personName;
    }

    private void setName(String name, String nameType, PersonNameType personName) {
        if (name != null) {
            NameComponentType nameComponent = new NameComponentType();
            nameComponent.setValue(name);
            JAXBElement<NameComponentType> nameElem = new JAXBElement<>(new QName(
                    "urn:tva:mpeg7:2008",
                    nameType), NameComponentType.class, nameComponent);
            personName.getGivenNameOrLinkingNameOrFamilyName().add(nameElem);
        }
    }

}
