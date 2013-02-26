package org.atlasapi.feeds.tvanytime;

import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Film;
import org.atlasapi.media.entity.Series;

import tva.metadata._2010.GroupInformationType;

public interface GroupInformationGenerator {
    public GroupInformationType generateForFilm(Film film);
    public GroupInformationType generateForEpisode(Episode episode);
    public GroupInformationType generateForSeries(Series series);
    public GroupInformationType generateForBrand(Brand brand);
}
