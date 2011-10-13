package org.atlasapi.feeds.lakeview.validation;

import java.io.File;
import java.util.List;

import org.atlasapi.feeds.lakeview.validation.rules.FeedValidationRule;
import org.atlasapi.feeds.lakeview.validation.rules.HeirarchyValidationRule;
import org.atlasapi.feeds.lakeview.validation.rules.UpToDateValidationRule;
import org.atlasapi.feeds.lakeview.validation.rules.ValidationResult;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.health.ProbeResult.ProbeResultEntry;
import com.metabroadcast.common.health.ProbeResult.ProbeResultType;
import com.metabroadcast.common.time.SystemClock;

public class LakeviewServerHealthProbe implements HealthProbe {

	@Override
	public ProbeResult probe() throws Exception {
		ProbeResult probeResult = new ProbeResult(title());
		
		for(ProbeResultEntry result : validateFeedFile()) {
			probeResult.addEntry(result);
		}
		return probeResult;
	}

	private List<ProbeResultEntry> validateFeedFile() {
		File file = new File("/Users/tom/lakeview.xml");

		List<FeedValidationRule> validationRules = Lists.newArrayList();
		validationRules.add(new HeirarchyValidationRule());
		// validationRules.add(new CompletenessValidationRule(null, 10));
		validationRules.add(new UpToDateValidationRule(5, new SystemClock()));
		LakeviewFileValidator validator = new LakeviewFileValidator(
				validationRules);
		List<ValidationResult> results = validator.validate(file);

		return Lists.transform(results,
				new Function<ValidationResult, ProbeResultEntry>() {
					@Override
					public ProbeResultEntry apply(ValidationResult result) {
						ProbeResultType probeResultType = null;
						switch(result.getResult()) {
						case SUCCESS:
							probeResultType = ProbeResultType.SUCCESS;
							break;
						case FAILURE:
							probeResultType = ProbeResultType.FAILURE;
							break;
						case INFO:
							probeResultType = ProbeResultType.INFO;
							break;
						default:
							probeResultType = ProbeResultType.FAILURE;
						}
						return new ProbeResultEntry(probeResultType, result.getValidationRuleName(), result.getDetails());
					}
				});
	}

	@Override
	public String slug() {
		return "lakeview";
	}

	@Override
	public String title() {
		return "Lakeview feed validation";
	}

}
