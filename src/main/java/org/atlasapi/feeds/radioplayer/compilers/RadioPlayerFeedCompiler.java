package org.atlasapi.feeds.radioplayer.compilers;

import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.atlasapi.feeds.radioplayer.outputting.RadioPlayerXMLOutputter;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;

public abstract class RadioPlayerFeedCompiler {

	protected final String filenamePattern;
	protected final RadioPlayerXMLOutputter outputter;
	protected final Pattern pattern;
	
	public RadioPlayerFeedCompiler(String filenamePattern, RadioPlayerXMLOutputter outputter) {
		this.filenamePattern = filenamePattern;
		this.outputter = outputter;
		this.pattern = Pattern.compile(filenamePattern);
	}

	public String getFilenamePattern() {
		return filenamePattern;
	}

	public Pattern getPattern() {
		return pattern;
	}

	public abstract void compileFeedFor(Matcher matcher, KnownTypeQueryExecutor queryExecutor, OutputStream out)
	throws IOException;
}