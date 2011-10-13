package org.atlasapi.feeds.lakeview.validation.rules;

import java.util.List;

import javax.xml.bind.JAXBElement;

import org.atlasapi.feeds.lakeview.validation.FeedItemStore;
import org.atlasapi.feeds.lakeview.validation.rules.ValidationResult.ValidationResultType;
import org.atlasapi.generated.ElementProduct;
import org.atlasapi.generated.ElementTVEpisode;
import org.atlasapi.generated.ElementTVSeason;

import com.google.inject.internal.Lists;
import com.sun.tools.javac.util.Pair;

/**
 * Validation rule to ensure the episode/series/brand hierarchy is consistent
 * 
 * @author tom
 * 
 */
public class HeirarchyValidationRule implements FeedValidationRule {

	private enum Parent {
		SERIES, BRAND
	};

	public HeirarchyValidationRule() {
	}

	@Override
	public ValidationResult validate(FeedItemStore feedItemStore) {
		List<Pair<String, ElementProduct>> errors = Lists.newArrayList();

		validateEpisodes(errors, feedItemStore);
		validateSeries(errors, feedItemStore);

		if(!errors.isEmpty()) {
			return new ValidationResult(getRuleName(), ValidationResultType.FAILURE, errors.toString());
		}
		else {
			return new ValidationResult(getRuleName(), ValidationResultType.SUCCESS);
		}
	}
	
	@Override
	public String getRuleName() {
		return "Consistent heirarchy check";
	}

	// An Atlas Series, which is a season in C4 nomenclature
	private void validateSeries(List<Pair<String, ElementProduct>> errors, FeedItemStore feedItemStore) {
		for (ElementTVSeason season : feedItemStore.getSeries().values()) {
			if (!feedItemStore.getBrands().containsKey(season.getSeriesId())) {
				addError(errors, season, "Brand not present");
			}
		}
	}

	private void validateEpisodes(List<Pair<String, ElementProduct>> errors, FeedItemStore feedItemStore) {
		for (ElementTVEpisode episode : feedItemStore.getEpisodes().values()) {
			if (!feedItemStore.getSeries().containsKey(
					getParent(Parent.SERIES, episode))) {
				addError(errors, episode, "Series not present");
			}

			if (!feedItemStore.getBrands().containsKey(
					getParent(Parent.BRAND, episode))) {
				addError(errors, episode, "Brand not present");
			}
		}
	}

	private void addError(List<Pair<String, ElementProduct>> errors,
			ElementProduct element, String description) {
		errors.add(new Pair<String, ElementProduct>(description, element));
	}

	public static String getParent(Parent parent, ElementTVEpisode episode) {
		// The lakeview schema has multiple elements called seasonId and
		// seriesId because of the way
		// they enforce at least one of the two is always specified. This blows
		// up JAXB so we need to look
		// through the 'rest' elements to find these two.
		for (JAXBElement<?> restElement : episode.getRest()) {
			if (restElement
					.getName()
					.getLocalPart()
					.equals(Parent.SERIES.equals(parent) ? "SeasonId"
							: "SeriesId"))
				return (String) restElement.getValue();
		}
		throw new RuntimeException("Element not found");
	}

}
