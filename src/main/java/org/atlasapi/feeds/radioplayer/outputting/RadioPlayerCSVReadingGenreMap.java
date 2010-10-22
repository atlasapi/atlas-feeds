package org.atlasapi.feeds.radioplayer.outputting;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.base.Charsets;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.io.LineProcessor;
import com.google.common.io.Resources;

public class RadioPlayerCSVReadingGenreMap {

	public static final String GENRES_FILE = "radioplayergenres.csv";
	private static final String CSV = "\"([^\"]+?)\",?|([^,]+),?|,";
	private static final Pattern CSV_PATTERN = Pattern.compile(CSV);

	private final Log log = LogFactory.getLog(RadioPlayerCSVReadingGenreMap.class);
	private final Map<String, List<String>> genreMapping;

	public RadioPlayerCSVReadingGenreMap(String genreResourceFilename) {
		this(Resources.getResource(genreResourceFilename));
	}
	
	public RadioPlayerCSVReadingGenreMap(URL genreResourceLocation) {
		genreMapping = loadGenres(genreResourceLocation);
	}

	private Map<String, List<String>> loadGenres(URL url) {
		try {
			return Resources.readLines(url, Charsets.UTF_8, new LineProcessor<Map<String, List<String>>>() {
				Map<String, List<String>> map = Maps.newHashMap();
				int[] selectedColumns = { 1, 2, 3, 8, 9, 10, 11 }; //csv columns to select

				@Override
				public Map<String, List<String>> getResult() {
					return map;
				}

				@Override
				public boolean processLine(String line) throws IOException {
					if (!Strings.isNullOrEmpty(line)) {
						try {
							Matcher m = CSV_PATTERN.matcher(line);
							processMatch(m);
						} catch (Exception e) {
							log.warn(line, e);
						}
					}
					return true;
				}

				private void processMatch(Matcher m) {
					String genreUrl = "http://www.bbc.co.uk/programmes/genres";
					List<String> mappings = Lists.newArrayListWithCapacity(4);
					int matchGroup = 0;
					for (int col = 0; col < selectedColumns.length; col++) {
						int column = selectedColumns[col];
						while (matchGroup <= column && m.find()) { //fast-forward to next relevant group
							matchGroup++;
						}
						
						String group = m.group().substring(0, m.group().length() - 1);
						if ((column == 1 || column == 8) && Strings.isNullOrEmpty(group)){
							return; //this line doesn't have a good genre mapping.
						}
						if (!Strings.isNullOrEmpty(group)) {
							if (column < 8 ) { //part of mapping key
								genreUrl += "/" + sanitize(group);
							} else { //part of mapping value
								mappings.add(group);
							}
						}
					}
					map.put(genreUrl, mappings);
				}

				private String sanitize(String group) {
					return group.toLowerCase().replaceAll("'|-| |,", "").replaceAll("&", "and");
				}
			});
		} catch (IOException e) {
			log.warn("Couldn't load genre map", e);
			return Maps.newHashMap();
		}
	}

	public Set<List<String>> map(Set<String> sourceGenres) {
		Set<List<String>> mappedGenres = Sets.newHashSet();

		if (sourceGenres == null) {
			return mappedGenres;
		}

		for (String genre : sourceGenres) {
			List<String> mapped = genreMapping.get(genre);
			if (mapped != null) {
				mappedGenres.add(mapped);
			}
		}

		return mappedGenres;
	}
	
	public Map<String, List<String>> getMapping() {
		return genreMapping;
	}
}
