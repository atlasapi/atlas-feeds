package org.atlasapi.feeds.lakeview.validation;

import java.util.List;
import java.util.Map;

import org.atlasapi.generated.ElementProduct;
import org.atlasapi.generated.ElementTVEpisode;
import org.atlasapi.generated.ElementTVSeason;
import org.atlasapi.generated.ElementTVSeries;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.internal.ImmutableMap;
import com.google.inject.internal.Lists;
import com.google.inject.internal.Maps;
import com.sun.tools.javac.util.Pair;

public class FeedItemStore {

	private Map<String, ElementTVSeries> brands = Maps.newHashMap();
	private Map<String, ElementTVSeason> series = Maps.newHashMap();
	private Map<String, ElementTVEpisode> episodes = Maps.newHashMap();
	private List<Pair<String, ElementProduct>> errors;

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

	private void addError(ElementProduct element, String message) {
		errors.add(new Pair<String, ElementProduct>(message, element));
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

	public List<Pair<String, ElementProduct>> getErrors() {
		return ImmutableList.copyOf(errors);
	}

}
