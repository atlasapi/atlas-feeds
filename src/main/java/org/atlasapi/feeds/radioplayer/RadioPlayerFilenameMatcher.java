package org.atlasapi.feeds.radioplayer;

import static com.metabroadcast.common.base.Maybe.HAS_VALUE;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.metabroadcast.common.base.Maybe;

public abstract class RadioPlayerFilenameMatcher {

    private static Pattern pattern = Pattern.compile("([0-9]{8})_([0-9A-Za-z\\_]+)_(PI|SI|OD)");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.forPattern("yyyyMMdd").withZone(DateTimeZone.UTC);

    public static RadioPlayerFilenameMatcher on(String filename) {
        Matcher m = pattern.matcher(filename);
        if (m.matches()) {
            return new RadioPlayerFilenameMatch(m.group(1), m.group(2), m.group(3));
        }
        return new RadioPlayerFilenameMiss();
    }
    
    public static boolean hasMatch(RadioPlayerFilenameMatcher matcher) {
        return matcher.matches() && Iterables.all(ImmutableSet.of(matcher.date(), matcher.service(), matcher.type()), HAS_VALUE);
    }

    public abstract Maybe<LocalDate> date();

    public abstract Maybe<RadioPlayerService> service();

    public abstract Maybe<RadioPlayerFeedCompiler> type();

    public abstract boolean matches();

    private static class RadioPlayerFilenameMatch extends RadioPlayerFilenameMatcher {

        private final LocalDate date;
        private Maybe<RadioPlayerService> service;
        private RadioPlayerFeedCompiler type;

        public RadioPlayerFilenameMatch(String date, String service, String feedType) {
            this.date = DATE_FORMAT.parseDateTime(date).toLocalDate();
            this.service = Maybe.fromPossibleNullValue(RadioPlayerServices.all.get(service));
            this.type = RadioPlayerFeedCompiler.valueOf(feedType);
        }

        @Override
        public Maybe<LocalDate> date() {
            return Maybe.just(date);
        }

        @Override
        public Maybe<RadioPlayerService> service() {
            return service;
        }

        @Override
        public Maybe<RadioPlayerFeedCompiler> type() {
            return Maybe.just(type);
        }

        @Override
        public boolean matches() {
            return true;
        }

    }

    private static class RadioPlayerFilenameMiss extends RadioPlayerFilenameMatcher {

        @Override
        public Maybe<LocalDate> date() {
            return Maybe.nothing();
        }

        @Override
        public Maybe<RadioPlayerService> service() {
            return Maybe.nothing();
        }

        @Override
        public Maybe<RadioPlayerFeedCompiler> type() {
            return Maybe.nothing();
        }

        @Override
        public boolean matches() {
            return false;
        }

    }

}
