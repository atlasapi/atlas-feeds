package org.atlasapi.feeds.lakeview.validation;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.atlasapi.feeds.lakeview.validation.rules.LakeviewFeedValidationRule;
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
import com.metabroadcast.common.time.SystemClock;

public class LakeviewFileValidator {

	private List<LakeviewFeedValidationRule> validationRules;

	public LakeviewFileValidator(List<LakeviewFeedValidationRule> validationRules) {
		this.validationRules = validationRules;
	}

	public List<ValidationResult> validate(InputStream stream) {
		
		Builder<ValidationResult> results = ImmutableList.builder();
		try {
			JAXBContext ctx = JAXBContext
					.newInstance(new Class[] { Feed.class });
			Unmarshaller um = ctx.createUnmarshaller();
			Feed feed = (Feed) um.unmarshal(stream);
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
			
			
			for(LakeviewFeedValidationRule validationRule : validationRules) {
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
		
		List<LakeviewFeedValidationRule> validationRules = Lists.newArrayList();
		validationRules.add(new HeirarchyValidationRule());
		//validationRules.add(new CompletenessValidationRule(null, 10));
		validationRules.add(new UpToDateValidationRule(5, new SystemClock()));
		LakeviewFileValidator validator = new LakeviewFileValidator(validationRules);
		//validator.validate(file);
	}
}
