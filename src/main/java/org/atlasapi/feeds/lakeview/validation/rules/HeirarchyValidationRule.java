package org.atlasapi.feeds.lakeview.validation.rules;

import java.util.List;

import javax.xml.bind.JAXBElement;

import org.atlasapi.feeds.lakeview.validation.FeedItemStore;
import org.atlasapi.feeds.lakeview.validation.rules.ValidationResult.ValidationResultType;
import org.atlasapi.generated.lakeview.ElementItem;
import org.atlasapi.generated.lakeview.ElementTVEpisode;
import org.atlasapi.generated.lakeview.ElementTVSeason;

import com.google.common.collect.Lists;
import com.metabroadcast.common.base.Maybe;

/**
 * Validation rule to ensure the episode/series/brand hierarchy is consistent
 * 
 * @author tom
 * 
 */
public class HeirarchyValidationRule implements LakeviewFeedValidationRule {

	private enum Parent {
		SERIES, BRAND
	};

	public HeirarchyValidationRule() {
	}

	@Override
	public ValidationResult validate(FeedItemStore feedItemStore) {
		List<String> errors = Lists.newArrayList();

		validateEpisodes(errors, feedItemStore);
		validateSeries(errors, feedItemStore);

		if(!errors.isEmpty()) {
			return new ValidationResult(getRuleName(), ValidationResultType.FAILURE, errors.toString());
		}
		else {
			return new ValidationResult(getRuleName(), ValidationResultType.SUCCESS, "Hierarchy consistent");
		}
	}
	
	@Override
	public String getRuleName() {
		return "Consistent heirarchy check";
	}

	// An Atlas Series, which is a season in C4 nomenclature
	private void validateSeries(List<String> errors, FeedItemStore feedItemStore) {
		for (ElementTVSeason season : feedItemStore.getSeries().values()) {
			if (!feedItemStore.getBrands().containsKey(season.getSeriesId())) {
				addError(errors, season, "Brand not present");
			}
		}
	}

	private void validateEpisodes(List<String> errors, FeedItemStore feedItemStore) {
		for (ElementTVEpisode episode : feedItemStore.getEpisodes().values()) {
		    Maybe<String> series = getSeries(episode);
		    Maybe<String> season = getSeason(episode);
			if (series.hasValue() && !feedItemStore.getBrands().containsKey(series.requireValue())) {
				addError(errors, episode, "Brand not present");
			}

			if(season.hasValue() && !feedItemStore.getSeries().containsKey(season.requireValue())) {
			    addError(errors, episode, "Series not present");			    
			}
			
			if(!(season.hasValue() || series.hasValue())) {
			    addError(errors, episode, "No parent");
			}
		}
	}

	private void addError(List<String> errors,
			ElementItem element, String description) {
		errors.add(description + element.getItemId());
	}

	public static Maybe<String> getSeries(ElementTVEpisode episode) {
		// The lakeview schema has multiple elements called seasonId and
		// seriesId because of the way
		// they enforce at least one of the two is always specified. This blows
		// up JAXB so we need to look
		// through the 'rest' elements to find these two.
		for (JAXBElement<?> restElement : episode.getRest()) {
			if (restElement.getName().getLocalPart().equals("SeriesId")) {
			    return Maybe.just((String) restElement.getValue());
			}
			
		}
		return Maybe.nothing();
	}
	
	   public static Maybe<String> getSeason(ElementTVEpisode episode) {
	        for (JAXBElement<?> restElement : episode.getRest()) {
	            if (restElement.getName().getLocalPart().equals("SeasonId")) {
	                return Maybe.just((String) restElement.getValue());
	            }
	            
	        }
	        return Maybe.nothing();
	    }

}
