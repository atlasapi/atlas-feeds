package org.atlasapi.feeds.lakeview.validation;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.GZIPInputStream;

import org.atlasapi.feeds.lakeview.validation.rules.ValidationResult;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.metabroadcast.common.health.HealthProbe;
import com.metabroadcast.common.health.ProbeResult;
import com.metabroadcast.common.health.ProbeResult.ProbeResultEntry;
import com.metabroadcast.common.health.ProbeResult.ProbeResultType;
import com.metabroadcast.common.time.Clock;

public class LakeviewServerHealthProbe implements HealthProbe {

	private Clock clock;
	private LakeviewFileValidator validator;
	private AzureLatestFileDownloader azureFileDownloader;
	
	public LakeviewServerHealthProbe(Clock clock, LakeviewFileValidator validator, AzureLatestFileDownloader azureFileDownloader) {
		this.clock = clock;
		this.validator = validator;
		this.azureFileDownloader = azureFileDownloader;
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
		List<ProbeResultEntry> results = Lists.newArrayList();
	
		AzureFileAndMetadata file = azureFileDownloader.getLatestFile();
		
		if(file == null) {
			results.add(new ProbeResultEntry(ProbeResultType.FAILURE, "Azure file upload", "No file present"));
		}
		else {
			DateTime twentyFiveHoursAgo = clock.now().minusHours(25);
			if(file.getLastModTime().isBefore(twentyFiveHoursAgo)) {
				results.add(new ProbeResultEntry(ProbeResultType.FAILURE, "Azure file upload", "No recent file upload: " + file.getLastModTime().toString(DateTimeFormat.mediumDateTime())));
			}
			else {
				results.add(new ProbeResultEntry(ProbeResultType.SUCCESS, "Azure file upload", "Last file was uploaded at " + file.getLastModTime().toString(DateTimeFormat.mediumDateTime())));
			}
			
			results.addAll(validateFileContents(file));
		}
		
		return ImmutableList.copyOf(results);
	}

	private List<ProbeResultEntry> validateFileContents(AzureFileAndMetadata file) {
		InputStream is = null;
		try {
			is = new GZIPInputStream(new ByteArrayInputStream(file.getContents()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		List<ProbeResultEntry> validationRulesResults = Lists.transform(validator.validate(is),
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
