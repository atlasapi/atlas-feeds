package org.atlasapi.feeds.lakeview.validation.rules;

import javax.xml.bind.JAXBElement;

import org.atlasapi.feeds.lakeview.validation.FeedItemStore;
import org.atlasapi.feeds.lakeview.validation.rules.ValidationResult.ValidationResultType;
import org.atlasapi.generated.lakeview.ElementTVEpisode;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import com.metabroadcast.common.time.Clock;

public class RecentUpdateToBrandValidationRule implements
		LakeviewFeedValidationRule {

	private final String seriesId;
	private final Clock clock;
	private int maximumNumberOfDaysWithoutNewContent;

	/**
	 * 
	 * @param seriesId	Channel 4 series ID, which is equivalent to an Atlas brand
	 * @param maximumNumberOfDaysWithoutNewContent
	 * @param clock
	 */
	public RecentUpdateToBrandValidationRule(String seriesId,
			int maximumNumberOfDaysWithoutNewContent, Clock clock) {
		this.seriesId = seriesId;
		this.clock = clock;
		this.maximumNumberOfDaysWithoutNewContent = maximumNumberOfDaysWithoutNewContent;
	}

	@Override
	public ValidationResult validate(FeedItemStore feedItemStore) {
		DateTime latestValidUpdate = null;
		
		DateTime oldestAllowedUpdate = clock.now().minusDays(
				maximumNumberOfDaysWithoutNewContent);
	
		for (ElementTVEpisode episode : feedItemStore.getEpisodes().values()) {
			DateTime originalPublicationDate = getOriginalPublicationDate(episode);
			if (getSeriesId(episode).equals(seriesId)
					&& (latestValidUpdate == null || latestValidUpdate
							.isBefore(originalPublicationDate))) {
				latestValidUpdate = originalPublicationDate;
			}
		}
		if (latestValidUpdate != null
				&& latestValidUpdate.isAfter(oldestAllowedUpdate)) {
			return new ValidationResult(getRuleName(),
					ValidationResultType.SUCCESS, "Latest update "
							+ latestValidUpdate.toString(DateTimeFormat.mediumDateTime()));
		} else {
			return new ValidationResult(
					getRuleName(),
					ValidationResultType.FAILURE,
					"Latest update " + latestValidUpdate );
		}
	}

	@Override
	public String getRuleName() {
		return "Recent update to programme in series " + seriesId;
	}

	public static String getSeriesId(ElementTVEpisode episode) {
		for (JAXBElement<?> restElement : episode.getRest()) {
			if (restElement.getName().getLocalPart().equals("SeriesId")) {
				return (String) restElement.getValue();
			}
		}
		throw new RuntimeException("Element seriesId not found");
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
