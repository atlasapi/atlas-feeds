package org.atlasapi.feeds.radioplayer;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;

import com.metabroadcast.common.base.Maybe;

public abstract class RadioPlayerFilenameMatcher {

	private static Pattern pattern = Pattern.compile("([0-9]{8})_([0-9A-Za-z\\_]+)_(PI|SI|OD)");
	
	public static RadioPlayerFilenameMatcher on(String filename) {
		Matcher m = pattern.matcher(filename);
		if(m.matches()) {
			return new RadioPlayerFilenameMatch(m.group(1), m.group(2), m.group(3));
		}
		return new RadioPlayerFilenameMiss();
	}
	
	public abstract Maybe<DateTime> date();
	
	public abstract Maybe<RadioPlayerService> service();
	
	public abstract Maybe<RadioPlayerFeedType> type();
	
	public abstract boolean matches();
	
	private static class RadioPlayerFilenameMatch extends RadioPlayerFilenameMatcher {

		private final DateTime date;
		private Maybe<RadioPlayerService> service;
		private RadioPlayerFeedType type;

		public RadioPlayerFilenameMatch(String date, String service, String feedType) {
			this.date = DateTimeFormat.forPattern("yyyyMMdd").withZone(DateTimeZone.UTC).parseDateTime(date);
			this.service = Maybe.fromPossibleNullValue(RadioPlayerServices.all.get(service));
			this.type = RadioPlayerFeedType.valueOf(feedType);
		}

		@Override
		public Maybe<DateTime> date() {
			return Maybe.just(date);
		}

		@Override
		public Maybe<RadioPlayerService> service() {
			return service;
		}

		@Override
		public Maybe<RadioPlayerFeedType> type() {
			return Maybe.just(type);
		}

		@Override
		public boolean matches() {
			return true;
		}

	}
	
	private static class RadioPlayerFilenameMiss extends RadioPlayerFilenameMatcher {

		@Override
		public Maybe<DateTime> date() {
			return Maybe.nothing();
		}

		@Override
		public Maybe<RadioPlayerService> service() {
			return Maybe.nothing();
		}

		@Override
		public Maybe<RadioPlayerFeedType> type() {
			return Maybe.nothing();
		}

		@Override
		public boolean matches() {
			return false;
		}

	}
	
}
