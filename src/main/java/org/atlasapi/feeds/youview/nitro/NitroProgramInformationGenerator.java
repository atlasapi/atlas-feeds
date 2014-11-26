package org.atlasapi.feeds.youview.nitro;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.datatype.Duration;

import org.atlasapi.feeds.tvanytime.TvAnytimeElementFactory;
import org.atlasapi.feeds.youview.AbstractProgramInformationGenerator;
import org.atlasapi.feeds.youview.ids.IdGenerator;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Certificate;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Version;

import tva.metadata._2010.BasicContentDescriptionType;
import tva.metadata._2010.ProgramInformationType;
import tva.metadata._2010.TVAParentalGuidanceType;
import tva.metadata._2010.TVATimeType;
import tva.metadata.extended._2010.ExtendedContentDescriptionType;
import tva.metadata.extended._2010.TargetingInformationType;
import tva.mpeg7._2008.ControlledTermUseType;
import tva.mpeg7._2008.UniqueIDType;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.intl.Country;

public final class NitroProgramInformationGenerator extends AbstractProgramInformationGenerator {

    // TODO all this certificate code will likely change
    private static final String YOUVIEW_DEFAULT_CERTIFICATE = "http://refdata.youview.com/mpeg7cs/YouViewContentRatingCS/2010-11-25#unrated";
    private static final String BBC_VERSION_PID_AUTHORITY = "vpid.bbc.co.uk";
    private static final Pattern BBC_VERSION_PID_URI_PATTERN = Pattern.compile("http://nitro.bbc.co.uk/programmes/(.*)");
    
    // TODO fix all certificate code
    private static final Predicate<Certificate> FILTER_CERT_FOR_GB = new Predicate<Certificate>() {
        @Override
        public boolean apply(Certificate input) {
            return input.country().equals(Countries.GB);
        }
    };
    
    private static final Function<Certificate, String> CERTIFICATE_TO_CLASSIFICATION = new Function<Certificate, String>() {
        @Override
        public String apply(Certificate input) {
            String href = YOUVIEW_CERTIFICATE_MAPPING.get(input.classification());
            if (href == null) {
                href = YOUVIEW_DEFAULT_CERTIFICATE;
            }
            return href;
        }
    };
    
    // TODO are there other settings than this? E, NR, TBA etc
    private static final Map<String, String> YOUVIEW_CERTIFICATE_MAPPING = ImmutableMap.<String, String>builder()
            .put("U", "http://bbfc.org.uk/BBFCRatingCS/2002#U")
            .put("PG", "http://bbfc.org.uk/BBFCRatingCS/2002#PG")
            .put("12", "http://bbfc.org.uk/BBFCRatingCS/2002#12")
            .put("15", "http://bbfc.org.uk/BBFCRatingCS/2002#15")
            .put("18", "http://bbfc.org.uk/BBFCRatingCS/2002#18")
            .build();

    private static final Integer DEFAULT_DURATION = 30 * 60;
    
    private final TvAnytimeElementFactory elementFactory = TvAnytimeElementFactory.INSTANCE;

    /**
     * NB DatatypeFactory is required for creation of javax Durations
     * This DatatypeFactory class may not be threadsafe
     */
    public NitroProgramInformationGenerator(IdGenerator idGenerator) {
        super(idGenerator);
    }

    @Override
    public final ProgramInformationType generate(String versionCrid, Item item, Version version) {
        ProgramInformationType progInfo = new ProgramInformationType();

        progInfo.setProgramId(versionCrid);
        progInfo.setBasicDescription(generateBasicDescription(item, version));
        progInfo.setDerivedFrom(generateDerivedFromElem(item));

        Optional<UniqueIDType> bbcVersionPid = versionPidOtherId(item);
        if (bbcVersionPid.isPresent()) {
            progInfo.getOtherIdentifier().add(bbcVersionPid.get());
        }
        
        return progInfo;
    }
    
    private Optional<UniqueIDType> versionPidOtherId(Item item) {
        Version version = Iterables.getFirst(item.getVersions(), null);
        if (version == null) {
            return Optional.absent();
        }
        Matcher matcher = BBC_VERSION_PID_URI_PATTERN.matcher(version.getCanonicalUri());
        if (!matcher.find()) {
            return Optional.absent();
        }
        UniqueIDType id = new UniqueIDType();
        id.setAuthority(BBC_VERSION_PID_AUTHORITY);
        id.setValue(matcher.group());
        return Optional.of(id);
    }

    private BasicContentDescriptionType generateBasicDescription(Item item, Version version) {
        ExtendedContentDescriptionType basicDescription = new ExtendedContentDescriptionType();

        basicDescription.setParentalGuidance(generateParentalGuidance(item));
        Optional<TVATimeType> prodDate = generateProductionDate(item);
        if (prodDate.isPresent()) {
            basicDescription.setProductionDate(prodDate.get());
        }
        basicDescription.getProductionLocation().addAll(generateProductLocations(item));
        basicDescription.setDuration(generateDuration(version));
        basicDescription.getTargetingInformationOrTargetingInformationRef().add(new TargetingInformationType());

        return basicDescription;
    }

    private List<String> generateProductLocations(Item item) {
        return ImmutableList.copyOf(Iterables.transform(item.getCountriesOfOrigin(), new Function<Country, String>() {
            @Override
            public String apply(Country input) {
                return input.code().toLowerCase();
            }
        }));
    }

    private TVAParentalGuidanceType generateParentalGuidance(Item item) {
        TVAParentalGuidanceType parentalGuidance = new TVAParentalGuidanceType();

        String certificate = Iterables.getFirst(
            Iterables.transform(
                Iterables.filter(item.getCertificates(), FILTER_CERT_FOR_GB), 
                CERTIFICATE_TO_CLASSIFICATION
            ), 
            YOUVIEW_DEFAULT_CERTIFICATE);

        ControlledTermUseType useType = new ControlledTermUseType();
        useType.setHref(certificate);
        parentalGuidance.setParentalRating(useType);
        return parentalGuidance;
    }

    private Duration generateDuration(Version version) {
        Integer durationInSecs = version.getDuration();
        if (durationInSecs == null) {
            durationInSecs = durationFromFirstBroadcast(version);
        } 
        return elementFactory.durationFrom(org.joda.time.Duration.standardSeconds(durationInSecs));
    }

    // TODO this is a workaround until versions are ingested correctly from BBC Nitro
    private Integer durationFromFirstBroadcast(Version version) {
        Broadcast broadcast = Iterables.getFirst(version.getBroadcasts(), null);
        if (broadcast == null) {
            // this needs to go away
            return DEFAULT_DURATION;
//            throw new RuntimeException("no broadcasts on version " + version.getCanonicalUri());
        }
        return broadcast.getBroadcastDuration();
    }

    private Optional<TVATimeType> generateProductionDate(Item item) {
        if (item.getYear() != null) {
            TVATimeType productionDate = new TVATimeType();
            productionDate.setTimePoint(item.getYear().toString());
            return Optional.of(productionDate);
        }
        return Optional.absent();
    }
}
