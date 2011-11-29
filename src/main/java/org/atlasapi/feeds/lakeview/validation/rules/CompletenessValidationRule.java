package org.atlasapi.feeds.lakeview.validation.rules;

import static org.atlasapi.persistence.content.ContentCategory.CHILD_ITEM;
import static org.atlasapi.persistence.content.listing.ContentListingCriteria.defaultCriteria;

import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBElement;

import org.atlasapi.feeds.lakeview.validation.FeedItemStore;
import org.atlasapi.feeds.lakeview.validation.rules.ValidationResult.ValidationResultType;
import org.atlasapi.generated.ElementTVEpisode;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy.Platform;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.listing.ContentLister;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterators;
import com.google.inject.internal.Lists;

/**
 * Validation rule to check all on-demands that we expect to be present, are.
 * 
 * @author tom
 * 
 */
public class CompletenessValidationRule implements LakeviewFeedValidationRule {

	private ContentLister contentLister;
	private int tolerance;
	private static Pattern ONDEMAND_ID = Pattern.compile(".*#(\\d+)");

	public CompletenessValidationRule(ContentLister contentLister, int tolerance) {
		this.contentLister = contentLister;
		this.tolerance = tolerance;
	}

	@Override
	public ValidationResult validate(FeedItemStore feedItemStore) {

		// TODO: Films

		List<String> expectedOnDemands = getExpectedOnDemandsFromDatabase();
		List<String> errors = Lists.newArrayList();

		// Do a two-way comparison between feed items and expected items
		for (String expectedOnDemand : expectedOnDemands) {
			boolean foundItemInFeed = false;
			for (ElementTVEpisode feedEpisode : feedItemStore.getEpisodes()
					.values()) {
				if (getApplicationSpecificData(feedEpisode).equals(
						expectedOnDemand)) {
					foundItemInFeed = true;
				}
			}
			if (!foundItemInFeed) {
				errors.add("Valid ondemand in db, not in feed: " + expectedOnDemand);
			}
		}

		for (ElementTVEpisode feedEpisode : feedItemStore.getEpisodes()
				.values()) {

			if (!expectedOnDemands
					.contains(getApplicationSpecificData(feedEpisode))) {
				errors.add("Item found in feed, not expected" + feedEpisode.getItemId());
			}
		}

		// We expect a small number of differences because the data in the feed
		// will be slightly stale
		if (errors.size() < tolerance) {
			return new ValidationResult(getRuleName(), ValidationResultType.SUCCESS,
					String.format("%d items in feed, %d items in database", feedItemStore.getEpisodes().size(), expectedOnDemands.size()));
		} else {
			return new ValidationResult(getRuleName(), ValidationResultType.FAILURE,
					String.format("%d items in feed, %d items in database. Tolerance is %d", feedItemStore.getEpisodes().size(), expectedOnDemands.size(), tolerance));
		}
	}

	private List<String> getExpectedOnDemandsFromDatabase() {
		Iterator<Episode> listContent = Iterators.filter(
				contentLister.listContent(defaultCriteria()
						.forPublisher(Publisher.C4).forContent(CHILD_ITEM)
						.build()), Episode.class);

		Builder<String> expectedOnDemands = ImmutableList.builder();

		while (listContent.hasNext()) {
			Episode episode = listContent.next();
			String onDemandId = getFirstAvailableOnDemandId(episode);

			if (onDemandId != null) {
				expectedOnDemands.add(onDemandId);
			}
		}

		return expectedOnDemands.build();
	}

	private String getFirstAvailableOnDemandId(Episode episode) {
		for (Version version : episode.getVersions()) {
			for (Encoding encoding : version.getManifestedAs()) {
				for (Location location : encoding.getAvailableAt()) {
					if (location.getAvailable()
                            && location.getPolicy() != null 
                            && Platform.XBOX.equals(location.getPolicy().getPlatform())
                            && location.getPolicy().getAvailabilityStart().isBeforeNow() 
                            && location.getPolicy().getAvailabilityEnd().isAfterNow()) {
						Matcher matcher = ONDEMAND_ID
								.matcher(location.getUri());
						if (matcher.matches()) {
							return matcher.group();
						}
					}
				}
			}
		}
		return null;
	}

	public static String getApplicationSpecificData(ElementTVEpisode episode) {
		for (JAXBElement<?> restElement : episode.getRest()) {
			if (restElement.getName().getLocalPart()
					.equals("ApplicationSpecificData"))
				return (String) restElement.getValue();
		}
		throw new RuntimeException("Element ApplicationSpecificData not found");
	}

	@Override
	public String getRuleName() {
		return "Completeness check";
	}
}
