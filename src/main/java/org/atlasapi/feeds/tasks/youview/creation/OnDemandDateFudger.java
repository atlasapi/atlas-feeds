package org.atlasapi.feeds.tasks.youview.creation;

import org.atlasapi.feeds.youview.hierarchy.ItemOnDemandHierarchy;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;

import com.metabroadcast.common.time.Clock;

import org.joda.time.DateTime;

import static com.google.common.base.Preconditions.checkNotNull;

/** This exists because
 * 1. The BBC "sometimes" completely nuke availabilites
 * 2. We decided to handle that case in R4 and send DELETEs for those to YV
 * 3. In R3 we decided to decrease the amount of "entity unchanged" transactions generated
 *    via payload hashes
 * 4. This then means that BBC flap an availability, we send both the payload and the matching
 *    DELETE, and then nothing once BBC republish it, because we have both hashes now
 *
 * This class is used in the JAXB generator to artificially fudge the start dates of Locations if
 * the start date is in the past, thus changing the payload, busthing the hash and resulting in the
 * OnDemand being uploaded. What we do is hard.
 */
public class OnDemandDateFudger {

    private final Clock clock;

    private OnDemandDateFudger(Clock clock) {
        this.clock = checkNotNull(clock);
    }

    public static OnDemandDateFudger create(Clock clock) {
        return new OnDemandDateFudger(clock);
    }

    public ItemOnDemandHierarchy fudgeStartDates(ItemOnDemandHierarchy onDemandHierarchy) {
        Location resultLocation = onDemandHierarchy.location();

        DateTime now = clock.now();

        Policy policy = resultLocation.getPolicy();

        boolean availableNow = false;
        if (policy != null) {
            boolean startsBefore = policy.getAvailabilityStart().isBefore(now);
            boolean endsAfter = policy.getAvailabilityEnd() == null
                    || policy.getAvailabilityEnd().isAfter(now);

            availableNow = startsBefore && endsAfter;
        }

        if (availableNow) {
            Policy resultPolicy = policy.copy();
            resultPolicy.setAvailabilityStart(now.minusHours(6));

            resultLocation = resultLocation.copy();
            resultLocation.setPolicy(resultPolicy);
        }

        return new ItemOnDemandHierarchy(
                onDemandHierarchy.item(),
                onDemandHierarchy.version(),
                onDemandHierarchy.encoding(),
                resultLocation
        );
    }
}