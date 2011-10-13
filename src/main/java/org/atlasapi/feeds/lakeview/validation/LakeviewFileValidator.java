package org.atlasapi.feeds.lakeview.validation;

import java.io.File;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.atlasapi.feeds.lakeview.validation.rules.CompletenessValidationRule;
import org.atlasapi.feeds.lakeview.validation.rules.FeedValidationRule;
import org.atlasapi.feeds.lakeview.validation.rules.HeirarchyValidationRule;
import org.atlasapi.feeds.lakeview.validation.rules.UpToDateValidationRule;
import org.atlasapi.feeds.lakeview.validation.rules.ValidationResult;
import org.atlasapi.feeds.lakeview.validation.rules.ValidationResult.ValidationResultType;
import org.atlasapi.generated.ElementMovie;
import org.atlasapi.generated.ElementProduct;
import org.atlasapi.generated.ElementTVEpisode;
import org.atlasapi.generated.ElementTVSeason;
import org.atlasapi.generated.ElementTVSeries;
import org.atlasapi.generated.Feed;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.inject.internal.Lists;
import com.metabroadcast.common.time.Clock;
import com.metabroadcast.common.time.SystemClock;
import com.sun.tools.javac.util.Pair;

public class LakeviewFileValidator {

	private List<FeedValidationRule> validationRules;

	public LakeviewFileValidator(List<FeedValidationRule> validationRules) {
		this.validationRules = validationRules;
	}

	public List<ValidationResult> validate(File file) {
		
		Builder<ValidationResult> results = ImmutableList.builder();
		try {
			JAXBContext ctx = JAXBContext
					.newInstance(new Class[] { Feed.class });
			Unmarshaller um = ctx.createUnmarshaller();
			Feed feed = (Feed) um.unmarshal(file);
			List<ElementProduct> f = feed.getMovieOrTVEpisodeOrTVSeason();

			FeedItemStore itemStore = new FeedItemStore();
			
			for (ElementProduct ep : f) {
				if (ep instanceof ElementTVEpisode) {
					itemStore.addEpisode((ElementTVEpisode) ep);
				} else if (ep instanceof ElementTVSeason) {
					itemStore.addSeries((ElementTVSeason) ep);
				} else if (ep instanceof ElementTVSeries) {
					itemStore.addBrand((ElementTVSeries) ep);
				} else if (ep instanceof ElementMovie) {
					// do nothing for now
				} else
					throw new IllegalArgumentException("Unknown element type "
							+ ep.getClass().getName());
			}
			
			
			for(FeedValidationRule validationRule : validationRules) {
				ValidationResult result = validationRule.validate(itemStore);
				results.add(result);
			}
			
		} catch (JAXBException e) {
			results.add(new ValidationResult("File parse", ValidationResultType.FAILURE, e.getMessage()));
		}
		
		return results.build();
	}

	public static void main(String[] args) {
		File file = new File("/Users/tom/lakeview.xml");
		
		List<FeedValidationRule> validationRules = Lists.newArrayList();
		validationRules.add(new HeirarchyValidationRule());
		//validationRules.add(new CompletenessValidationRule(null, 10));
		validationRules.add(new UpToDateValidationRule(5, new SystemClock()));
		LakeviewFileValidator validator = new LakeviewFileValidator(validationRules);
		validator.validate(file);
	}
}
