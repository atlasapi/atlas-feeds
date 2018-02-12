package org.atlasapi.feeds.youview;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

import org.atlasapi.media.entity.Content;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Version;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AmazonContentConsolidator {

    private static final Logger log = LoggerFactory.getLogger(AmazonContentConsolidator.class);

    /**
     * This method will take all the versions of an item, and all the locations inside those
     * versions, and put everything under a single version, with locations that are grouped under
     * the same quality.
     * <p>
     * This is a destructive merge. If versions were legitimately different, bad luck. This was
     * chosen because we don't dedup version on ingest, so they should have different titles and
     * would be different items.
     */
    public static Content consolidate(Content content) {
        Content copy = (Content) content.copy();
        Set<Version> versions = copy.getVersions();
        // If there is none or one version, nothing to consolidate.
        if (versions.size() <= 1) {
            return copy;
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

        copy.setVersions(new HashSet<Version>(Arrays.asList(winnerVersion)));
        return copy;
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
     * Quality is based on the HorizontalSize (because it's what we use to determine the YV
     * quality).
     */
    private static Encoding getSameQualityEncoding(Version version, Encoding enc) {
        for (Encoding tmpEnc : version.getManifestedAs()) {
            if (Objects.equals(tmpEnc.getVideoHorizontalSize(), enc.getVideoHorizontalSize())) {
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
