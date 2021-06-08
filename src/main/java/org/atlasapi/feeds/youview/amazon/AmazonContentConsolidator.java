package org.atlasapi.feeds.youview.amazon;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class AmazonContentConsolidator {

    private static final Logger log = LoggerFactory.getLogger(AmazonContentConsolidator.class);

    /**
     * This method will take all the versions of an item, and all the locations inside those
     * versions, and put everything under a single version, with locations that are grouped under
     * the same quality.
     * <p>
     * This is a destructive merge. If versions were legitimately different, tough luck. This was
     * chosen because we don't dedup version on ingest, so they should have different titles and
     * would be different items.
     * <p>
     * THIS MUTATES THE ORIGINAL CONTENT. Unluckily content.copy() does not properly copy the
     * manifestedAs().
     */
    public static Content consolidate(Content content) {
        Set<Version> versions = content.getVersions();
        // If there is none or one version, nothing to consolidate.
        if (versions == null || versions.size() <= 1) {
            return content;
        }

        Iterator<Version> iter = versions.iterator();
        Version winnerVersion = iter.next();
        iter.remove();

        // We loop through what is left and put their stuff to the winner
        while (iter.hasNext()) {
            Version version = iter.next();
            Set<Encoding> encodings = version.getManifestedAs();
            for (Encoding encoding : encodings) {
                Encoding sameQualityEncoding = getSameQualityEncoding(winnerVersion, encoding);
                //if we have encodings of the same quality, merge
                if (sameQualityEncoding != null) {
                    mergeEncodings(sameQualityEncoding, encoding);
                    addEncoding(winnerVersion, sameQualityEncoding);
                } else { //otherwise append as it is
                    addEncoding(winnerVersion, encoding);
                }
            }
        }

        content.setVersions(new HashSet<Version>(Arrays.asList(winnerVersion)));
        return content;
    }

    /**
     * Gets the list of availableAt's from the enc2, and adds them to the availableAt's of enc1.
     */
    private static void mergeEncodings(Encoding enc1, Encoding enc2) {
        Set<Location> originalLocations = enc1.getAvailableAt();
        originalLocations.addAll(enc2.getAvailableAt());
        enc1.setAvailableAt(originalLocations);
    }

    /**
     * Looks into the encodings of the Version, to see if an encoding of the same quality exists.
     * The generic quality is good enough for this purpose, but in general this test should match
     * whatever is used in the youview outputter for amazon.
     */
    private static Encoding getSameQualityEncoding(Version version, Encoding enc) {
        for (Encoding tmpEnc : version.getManifestedAs()) {
            if (Objects.equals(tmpEnc.getQuality(), enc.getQuality())) {
                return tmpEnc;
            }
        }
        return null;
    }

    /**
     * Adds the given encoding, to the existing encodings of the version.
     */
    private static void addEncoding(Version version, Encoding enc) {
        Set<Encoding> winnersManifestedAs = version.getManifestedAs();
        winnersManifestedAs.add(enc);
        version.setManifestedAs(winnersManifestedAs);

    }
}
