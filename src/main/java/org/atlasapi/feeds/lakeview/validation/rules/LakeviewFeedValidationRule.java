package org.atlasapi.feeds.lakeview.validation.rules;

import org.atlasapi.feeds.lakeview.validation.FeedItemStore;

public interface LakeviewFeedValidationRule {

	ValidationResult validate(FeedItemStore feedItemStore);

	String getRuleName();
}
