package org.atlasapi.feeds.lakeview.validation;

import java.util.List;
import java.util.Map;

import org.atlasapi.generated.lakeview.ElementMovie;
import org.atlasapi.generated.lakeview.ElementProduct;
import org.atlasapi.generated.lakeview.ElementTVEpisode;
import org.atlasapi.generated.lakeview.ElementTVSeason;
import org.atlasapi.generated.lakeview.ElementTVSeries;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.internal.ImmutableMap;
import com.google.inject.internal.Lists;
import com.google.inject.internal.Maps;

public class FeedItemStore {

	private Map<String, ElementTVSeries> brands = Maps.newHashMap();
	private Map<String, ElementTVSeason> series = Maps.newHashMap();
	private Map<String, ElementTVEpisode> episodes = Maps.newHashMap();
	private Map<String, ElementMovie> movies = Maps.newHashMap();
	
	private List<String> errors;

	public FeedItemStore() {
		this.errors = Lists.newArrayList();
	}

	public void addSeries(ElementTVSeason aSeries) {
		Preconditions.checkNotNull(aSeries);
		Preconditions.checkNotNull(aSeries.getItemId());

		if (series.put(aSeries.getItemId(), aSeries) != null) {
			addError(aSeries, "Duplicate series");
		}
	}

	public void addBrand(ElementTVSeries brand) {
		Preconditions.checkNotNull(brand);
		Preconditions.checkNotNull(brand.getItemId());

		if (brands.put(brand.getItemId(), brand) != null) {
			addError(brand, "Duplicate brand");
		}
	}

	public void addEpisode(ElementTVEpisode episode) {
		Preconditions.checkNotNull(episode);
		Preconditions.checkNotNull(episode.getItemId());

		if (episodes.put(episode.getItemId(), episode) != null) {
			addError(episode, "Duplicate episode");
		}
	}
	
	public void addMovie(ElementMovie movie) {
		Preconditions.checkNotNull(movie);
		Preconditions.checkNotNull(movie.getItemId());
		
		if (movies.put(movie.getItemId(), movie) != null) {
			addError(movie, "Duplicate episode");
		}
		
	}

	private void addError(ElementProduct element, String message) {
		errors.add(message + ": " + element.getItemId());
	}

	public Map<String, ElementTVSeries> getBrands() {
		return ImmutableMap.copyOf(brands);
	}

	public Map<String, ElementTVSeason> getSeries() {
		return ImmutableMap.copyOf(series);
	}

	public Map<String, ElementTVEpisode> getEpisodes() {
		return ImmutableMap.copyOf(episodes);
	}
	
	public Map<String, ElementMovie> getMovies() {
		return ImmutableMap.copyOf(movies);
	}

	public List<String> getErrors() {
		return ImmutableList.copyOf(errors);
	}

}
