package org.atlasapi.feeds.radioplayer.upload;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.AuthorizationRequest;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.atlasapi.application.ApplicationConfiguration;
import org.atlasapi.feeds.radioplayer.RadioPlayerFeedCompiler;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.feeds.upload.FileUploadResult.FileUploadResultType;
import org.atlasapi.feeds.upload.FileUploadService;
import org.atlasapi.feeds.upload.RemoteServiceDetails;
import org.atlasapi.feeds.upload.ftp.CommonsFTPFileUploader;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.channel.Channel;
import org.atlasapi.media.channel.ChannelResolver;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.MediaType;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Schedule;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.ContentResolver;
import org.atlasapi.persistence.content.KnownTypeContentResolver;
import org.atlasapi.persistence.content.ScheduleResolver;
import org.atlasapi.persistence.logging.SystemOutAdapterLog;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JMock;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Interval;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.net.HostSpecifier;
import com.google.common.util.concurrent.MoreExecutors;
import com.metabroadcast.common.base.Maybe;
import com.metabroadcast.common.intl.Countries;
import com.metabroadcast.common.security.UsernameAndPassword;
import com.metabroadcast.common.time.DateTimeZones;
import com.metabroadcast.common.time.DayRangeGenerator;

@RunWith(JMock.class)
public class RadioPlayerFileUploaderTest {

    private static final String TEST_PASSWORD = "testpassword";
	private static final String TEST_USERNAME = "test";
	
	private final Mockery context = new Mockery();
	private final KnownTypeContentResolver knownTypeContentResolver = context.mock(KnownTypeContentResolver.class);   
	private final ContentResolver contentResolver = context.mock(ContentResolver.class);
	private final ScheduleResolver scheduleResolver = context.mock(ScheduleResolver.class);
	private final RadioPlayerUploadResultStore recorder = context.mock(RadioPlayerUploadResultStore.class);
	
	private final RadioPlayerService service = RadioPlayerServices.all.get("340");
    private final DateTime day = new DateTime(DateTimeZones.UTC);
    private final Channel channel = new Channel(Publisher.METABROADCAST, "BBC Radio 1", "radio1", false, MediaType.AUDIO, "http://www.bbc.co.uk/radio1");
    private final Set<Channel> channels = ImmutableSet.of(channel);
    private final Set<Publisher> publishers = ImmutableSet.of(Publisher.BBC);
    
	private static File dir;

	private FtpServer server;
	
	@Before
	public void setUp() throws Exception {
	    dir = Files.createTempDir();
        dir.deleteOnExit();

        File files = new File(dir.getAbsolutePath() + File.separator + "files");
        files.mkdir();

        startServer();
	}
	
	@After
	public void shutdown() throws Exception {
	    server.stop();
	}

	@Test
	public void testRun() throws Exception {
	    Map<Channel, List<Item>> channelMap = Maps.newHashMap();
	    channelMap.put(channel, ImmutableList.of(buildItem(service.getServiceUri(), day, day.plus(1))));
	    
	    final Schedule schedule = Schedule.fromChannelMap(channelMap, new Interval(day, day.plusDays(1)));
	    final ChannelResolver channelResolver = context.mock(ChannelResolver.class);
		context.checking(new Expectations(){{
			allowing(channelResolver).fromUri("http://www.bbc.co.uk/services/radio1/england");
			will(returnValue(Maybe.just(channel)));
		    oneOf(scheduleResolver).schedule(with(any(DateTime.class)), with(any(DateTime.class)), with(channels), with(publishers), with(Optional.<ApplicationConfiguration>absent())); will(returnValue(schedule));
		    oneOf(recorder).record(with(successfulUploadResult()));
		}});
		
		RadioPlayerFeedCompiler.init(scheduleResolver, knownTypeContentResolver, contentResolver, channelResolver, publishers);
		
        ImmutableList<RadioPlayerService> services = ImmutableList.of(service);
        RemoteServiceDetails credentials = RemoteServiceDetails.forServer(HostSpecifier.from("127.0.0.1")).withPort(9521).withCredentials(new UsernameAndPassword("test","testpassword")).build();
        FileUploadService fileUploader = new FileUploadService("remoteService", new CommonsFTPFileUploader(credentials));
		
		RadioPlayerScheduledPiUploadTask uploader = new RadioPlayerScheduledPiUploadTask(ImmutableSet.of(fileUploader), new RadioPlayerRecordingExecutor(recorder, MoreExecutors.sameThreadExecutor()), services, new DayRangeGenerator(), new SystemOutAdapterLog(), Iterables.getOnlyElement(publishers));

		Executor executor = MoreExecutors.sameThreadExecutor();
		executor.execute(uploader);
		
		Map<String, File> uploaded = uploadedFiles();
		assertThat(uploaded.size(), is(equalTo(1)));

		String filename = String.format("%4d%02d%02d_340_PI.xml", day.getYear(), day.getMonthOfYear(), day.getDayOfMonth());
		assertThat(uploaded.keySet(), hasItem(filename));
		assertThat(uploaded.get(filename).length(), greaterThan(0L));
	}

    private Map<String, File> uploadedFiles() {
        Map<String, File> uploaded = Maps.uniqueIndex(ImmutableSet.copyOf(dir.listFiles(new FilenameFilter() {
        	@Override
        	public boolean accept(File dir, String name) {
        		return name.endsWith("_PI.xml");
        	}
        })), new Function<File, String>() {
        	@Override
        	public String apply(File input) {
        		return input.getName();
        	}
        });
        return uploaded;
    }

    private Matcher<RadioPlayerUploadResult> successfulUploadResult() {
        return new FileUploadResultTypeMatcher(FileUploadResultType.SUCCESS);
    }
	
	private static class FileUploadResultTypeMatcher extends TypeSafeMatcher<RadioPlayerUploadResult> {
	    
	    private final FileUploadResultType type;

        public FileUploadResultTypeMatcher(FileUploadResultType type) {
            this.type = type;
        }
	    
        @Override
        public void describeTo(Description desc) {
            desc.appendText(type.toNiceString());
            desc.appendText(" upload");
        }

        @Override
        public boolean matchesSafely(RadioPlayerUploadResult upload) {
            return type == upload.getUpload().type();
        }
    };

	private void startServer() throws FtpException {
		FtpServerFactory serverFactory = new FtpServerFactory();

		ListenerFactory factory = new ListenerFactory();

		factory.setPort(9521);

		serverFactory.addListener("default", factory.createListener());

		serverFactory.setUserManager(new TestUserManager(new TestUser(TEST_USERNAME, TEST_PASSWORD, dir)));
		
		server = serverFactory.createServer();

		server.start();
	}

	public static Item buildItem(String service, DateTime transmissionStart, DateTime transmissionEnd) {
		
		Item testItem = new Episode("http://www.bbc.co.uk/programmes/b00f4d9c", "bbc:b00f4d9c", Publisher.BBC);
		testItem.setTitle("BBC Electric Proms: Saturday Night Fever");
		testItem.setDescription("Another chance to hear Robin Gibb perform the Bee Gees' classic disco album with the BBC Concert Orchestra. It was recorded"
				+ " for the BBC Electric Proms back in October 2008, marking 30 years since Saturday Night Fever soundtrack topped the UK charts.");
		testItem.setGenres(ImmutableSet.of("http://www.bbc.co.uk/programmes/genres/music", "http://ref.atlasapi.org/genres/atlas/music"));
		testItem.setImage("http://www.bbc.co.uk/iplayer/images/episode/b00v6bbc_640_360.jpg");

		Version version = new Version();

		Broadcast broadcast = new Broadcast(service, transmissionStart, transmissionEnd);
		version.addBroadcast(broadcast);

		Encoding encoding = new Encoding();
		Location location = new Location();
		location.setUri("http://www.bbc.co.uk/iplayer/episode/b00f4d9c");
		Policy policy = new Policy();
		policy.setAvailabilityEnd(new DateTime(2010, 8, 28, 23, 40, 19, 0, TIMEZONE));
		policy.setAvailabilityStart(new DateTime(2010, 9, 4, 23, 02, 00, 0, TIMEZONE));
		policy.addAvailableCountry(Countries.GB);
		location.setPolicy(policy);
		location.setTransportType(TransportType.LINK);
		encoding.addAvailableAt(location);
		version.addManifestedAs(encoding);

		testItem.addVersion(version);

		return testItem;
	}

	private static final DateTimeZone TIMEZONE = DateTimeZone.forOffsetHours(8);
	
	public static class TestUser implements User {

	    private final String TEST_USERNAME;
	    private final String TEST_PASSWORD;
	    private final File homeDir;

	    public TestUser(String name, String password, File homeDir) {
	        this.TEST_USERNAME = name;
	        this.TEST_PASSWORD = password;
	        this.homeDir = homeDir;
	    }
	    
	    @Override
	    public String getName() {
	        return TEST_USERNAME;
	    }

	    @Override
	    public String getPassword() {
	        return TEST_PASSWORD;
	    }

	    @Override
	    public List<Authority> getAuthorities() {
	        return ImmutableList.<Authority> of(new WritePermission());
	    }

	    @Override
	    public List<Authority> getAuthorities(Class<? extends Authority> clazz) {
	        if (clazz.equals(WritePermission.class)) {
	            return ImmutableList.<Authority> of(new WritePermission());
	        }
	        return ImmutableList.<Authority> of();
	    }

	    @Override
	    public AuthorizationRequest authorize(AuthorizationRequest request) {
	        return new WritePermission().authorize(request);
	    }

	    @Override
	    public int getMaxIdleTime() {
	        return 0;
	    }

	    @Override
	    public boolean getEnabled() {
	        return true;
	    }

	    @Override
	    public String getHomeDirectory() {
	        return homeDir.getAbsolutePath();
	    }
	};
	
	public static class TestUserManager implements UserManager {
	    
	    private final String TEST_USERNAME;
	    private final User testUser;

	    public TestUserManager(User user) {
	        this.TEST_USERNAME = user.getName();
	        this.testUser = user;
	    }

	    @Override
	    public User getUserByName(String username) throws FtpException {
	        if (username == TEST_USERNAME) {
	            return testUser;
	        }
	        return null;
	    }

	    @Override
	    public String[] getAllUserNames() throws FtpException {
	        return new String[] { TEST_USERNAME };
	    }

	    @Override
	    public void delete(String username) throws FtpException {
	        // no-op
	    }

	    @Override
	    public void save(User user) throws FtpException {
	    }

	    @Override
	    public boolean doesExist(String username) throws FtpException {
	        return username.equals(TEST_USERNAME);
	    }

	    @Override
	    public User authenticate(Authentication authentication) throws AuthenticationFailedException {
	        if (authentication instanceof UsernamePasswordAuthentication) {
	            UsernamePasswordAuthentication upauth = (UsernamePasswordAuthentication) authentication;
	            if (upauth.getUsername().equals(TEST_USERNAME)) {
	                return testUser;
	            }
	        }
	        throw new AuthenticationFailedException();
	    }

	    @Override
	    public String getAdminName() throws FtpException {
	        return "admin";
	    }

	    @Override
	    public boolean isAdmin(String username) throws FtpException {
	        return username.equals("admin");
	    }

	}
}
