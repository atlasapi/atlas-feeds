package org.atlasapi.feeds.lakeview.validation.rules;

import javax.xml.bind.JAXBElement;

import org.atlasapi.feeds.lakeview.validation.FeedItemStore;
import org.atlasapi.feeds.lakeview.validation.rules.ValidationResult.ValidationResultType;
import org.atlasapi.generated.ElementTVEpisode;
import org.joda.time.DateTime;

import com.metabroadcast.common.time.Clock;

/**
 * Validation rule to confirm we have new items published in the last day.
 * 
 * @author tom
 *
 */
public class UpToDateValidationRule implements LakeviewFeedValidationRule {

	private int minNewItemsInLastDay;
	private Clock clock;

	public UpToDateValidationRule(int minNewItemsInLastDay, Clock clock) {
		this.minNewItemsInLastDay = minNewItemsInLastDay;
		this.clock = clock;
	}

	@Override
	public ValidationResult validate(
			FeedItemStore feedItemStore) {

		DateTime aDayAgo = clock.now().minusDays(1);
		int publishedInLastDay = 0;
		for (ElementTVEpisode episode : feedItemStore.getEpisodes().values()) {
			DateTime originalPublicationDate = getOriginalPublicationDate(episode);
			if (originalPublicationDate.isAfter(aDayAgo)
					&& originalPublicationDate.isBeforeNow()) {
				publishedInLastDay++;
			}
		}
		if (publishedInLastDay < minNewItemsInLastDay) {
			return new ValidationResult(getRuleName(), ValidationResultType.FAILURE, String.format(
							"Not enough new items found in last day. Found %d, threshold is %d",
							publishedInLastDay, minNewItemsInLastDay));
		}
		else {
			return new ValidationResult(getRuleName(), ValidationResultType.SUCCESS, String.format("%d items published in last day", publishedInLastDay));
		}
	}
	
	@Override
	public String getRuleName() {
		return "Up-to-date check";
	}

	public static DateTime getOriginalPublicationDate(ElementTVEpisode episode) {
		for (JAXBElement<?> restElement : episode.getRest()) {
			if (restElement.getName().getLocalPart()
					.equals("OriginalPublicationDate")) {
				return new DateTime((String) restElement.getValue());
			}
		}
		throw new RuntimeException("Element OriginalPublicationDate not found");
	}
}
