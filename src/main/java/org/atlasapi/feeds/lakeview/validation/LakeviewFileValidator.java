package org.atlasapi.feeds.lakeview.validation;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.atlasapi.feeds.lakeview.LakeviewContentFetcher;
import org.atlasapi.feeds.lakeview.LakeviewFeedCompiler;
import org.atlasapi.feeds.lakeview.XmlFeedOutputter;
import org.atlasapi.feeds.lakeview.validation.rules.LakeviewFeedValidationRule;
import org.atlasapi.feeds.lakeview.validation.rules.ValidationResult;
import org.atlasapi.feeds.lakeview.validation.rules.ValidationResult.ValidationResultType;
import org.atlasapi.generated.lakeview.ElementFeed;
import org.atlasapi.generated.lakeview.ElementItem;
import org.atlasapi.generated.lakeview.ElementMovie;
import org.atlasapi.generated.lakeview.ElementTVEpisode;
import org.atlasapi.generated.lakeview.ElementTVSeason;
import org.atlasapi.generated.lakeview.ElementTVSeries;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.persistence.logging.AdapterLog;
import org.atlasapi.persistence.logging.AdapterLogEntry;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;

public class LakeviewFileValidator {

	private List<LakeviewFeedValidationRule> validationRules;
	private AdapterLog log;
	private LakeviewContentFetcher contentFetcher;
	private LakeviewFeedCompiler feedCompiler;
	private XmlFeedOutputter feedOutputter;

	public LakeviewFileValidator(LakeviewContentFetcher contentFetcher, LakeviewFeedCompiler feedCompiler, XmlFeedOutputter feedOutputter, List<LakeviewFeedValidationRule> validationRules, AdapterLog log) {
		this.validationRules = validationRules;
		this.log = log;
		this.contentFetcher = contentFetcher;
		this.feedCompiler = feedCompiler;
		this.feedOutputter = feedOutputter;
	}

	public List<ValidationResult> validate() {
		
		Builder<ValidationResult> results = ImmutableList.builder();
		try {
			ByteArrayOutputStream os = new ByteArrayOutputStream();
			feedOutputter.outputTo(feedCompiler.compile(contentFetcher.fetchContent(Publisher.C4_PMLSD)), os);
			InputStream is = new ByteArrayInputStream(os.toByteArray());
		
			JAXBContext ctx = JAXBContext
					.newInstance(new Class[] { ElementFeed.class });
			Unmarshaller um = ctx.createUnmarshaller();
			ElementFeed feed = (ElementFeed) um.unmarshal(is);

			FeedItemStore itemStore = new FeedItemStore();
			
			for (ElementItem ep : feed.getMovieOrTVEpisodeOrTVSeason()) {
				if (ep instanceof ElementTVEpisode) {
					itemStore.addEpisode((ElementTVEpisode) ep);
				} else if (ep instanceof ElementTVSeason) {
					itemStore.addSeries((ElementTVSeason) ep);
				} else if (ep instanceof ElementTVSeries) {
					itemStore.addBrand((ElementTVSeries) ep);
				} else if (ep instanceof ElementMovie) {
					itemStore.addMovie((ElementMovie) ep);
				} else
					throw new IllegalArgumentException("Unknown element type "
							+ ep.getClass().getName());
			}
			
			
			for(LakeviewFeedValidationRule validationRule : validationRules) {
				try {
					ValidationResult result = validationRule.validate(itemStore);
					results.add(result);
				}
				catch(Exception e) {
					ValidationResult result = new ValidationResult(validationRule.getRuleName(), ValidationResultType.FAILURE, "Exception during validation");
					results.add(result);
					log.record(AdapterLogEntry.errorEntry().withDescription("Exception during validation").withCause(e));
				}
			}
			
		} catch (JAXBException e) {
			results.add(new ValidationResult("Feed document parse", ValidationResultType.FAILURE, e.getMessage()));
			log.record(AdapterLogEntry.errorEntry().withDescription("Could not parse lakeview file").withCause(e));
		} catch (IOException ex) {
			results.add(new ValidationResult("Feed document parse", ValidationResultType.FAILURE, ex.getMessage()));
			log.record(AdapterLogEntry.errorEntry().withDescription("Could not parse lakeview file").withCause(ex));
		}
		
		return results.build();
	}
}
