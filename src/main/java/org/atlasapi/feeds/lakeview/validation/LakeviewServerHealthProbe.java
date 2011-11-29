package org.atlasapi.feeds.lakeview.validation;

import java.util.List;

import org.atlasapi.feeds.lakeview.validation.rules.ValidationResult;
import org.atlasapi.feeds.upload.FileUploadResult;
import org.atlasapi.feeds.upload.persistence.FileUploadResultStore;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.health.ProbeResult.ProbeResultEntry;
import com.metabroadcast.common.health.ProbeResult.ProbeResultType;
import com.metabroadcast.common.time.Clock;

public class LakeviewServerHealthProbe implements HealthProbe {

	private Clock clock;
	private LakeviewFileValidator validator;
	private FileUploadResultStore fileUploadResultStore;
	private String remoteServiceName;
	
	public LakeviewServerHealthProbe(Clock clock, LakeviewFileValidator validator, FileUploadResultStore fileUploadResultStore, String serviceName) {
		this.clock = clock;
		this.validator = validator;
		this.fileUploadResultStore = fileUploadResultStore;
		this.remoteServiceName = serviceName;
	}
	
	@Override
	public ProbeResult probe() throws Exception {
		ProbeResult probeResult = new ProbeResult(title());
		
		for(ProbeResultEntry result : validateFeedFile()) {
			probeResult.addEntry(result);
		}
		return probeResult;
	}

	private List<ProbeResultEntry> validateFeedFile() {

		Builder<ProbeResultEntry> results = new ImmutableList.Builder<ProbeResultEntry>();

		results.add(validateFileUpload());
		results.addAll(validateFileContents());
		
		return results.build();
	}
	
	private ProbeResultEntry validateFileUpload() {
		
		ProbeResultEntry result = null;
		FileUploadResult mostRecentResult = Iterables.getFirst(fileUploadResultStore.results(remoteServiceName), null);
		
		if(mostRecentResult == null) {
			result = (new ProbeResultEntry(ProbeResultType.FAILURE, "Azure file upload", "No file upload result found"));
		}
		else {
			DateTime twentyFiveHoursAgo = clock.now().minusHours(25);
			if(mostRecentResult.uploadTime().isBefore(twentyFiveHoursAgo)) {
				result = new ProbeResultEntry(ProbeResultType.FAILURE, "Azure file upload", "No recent file upload. Last upload at " + mostRecentResult.uploadTime().toString(DateTimeFormat.mediumDateTime()));
			}
			else {
				result = new ProbeResultEntry(ProbeResultType.SUCCESS, "Azure file upload", "Last file was uploaded at " + mostRecentResult.uploadTime().toString(DateTimeFormat.mediumDateTime()));
			}
		}
		return result;
	}

	private List<ProbeResultEntry> validateFileContents() {

		List<ProbeResultEntry> validationRulesResults = Lists.transform(validator.validate(),
				new Function<ValidationResult, ProbeResultEntry>() {
					@Override
					public ProbeResultEntry apply(ValidationResult result) {
						ProbeResultType probeResultType = null;
						switch(result.getResult()) {
						case SUCCESS:
							probeResultType = ProbeResultType.SUCCESS;
							break;
						case INFO:
							probeResultType = ProbeResultType.INFO;
							break;
						case FAILURE:
						default:
							probeResultType = ProbeResultType.FAILURE;
						}
						return new ProbeResultEntry(probeResultType, result.getValidationRuleName(), result.getDetails());
					}
				});
		return validationRulesResults;
	}

	@Override
	public String slug() {
		return "lakeview";
	}

	@Override
	public String title() {
		return "Lakeview feed";
	}

}
