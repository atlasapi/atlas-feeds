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
import java.util.concurrent.Executor;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.listener.ListenerFactory;
import org.atlasapi.content.criteria.ContentQuery;
import org.atlasapi.content.criteria.attribute.Attribute;
import org.atlasapi.content.criteria.attribute.Attributes;
import org.atlasapi.feeds.radioplayer.RadioPlayerService;
import org.atlasapi.feeds.radioplayer.RadioPlayerServices;
import org.atlasapi.media.TransportType;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Countries;
import org.atlasapi.media.entity.Encoding;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Playlist;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Publisher;
import org.atlasapi.media.entity.Version;
import org.atlasapi.persistence.content.query.KnownTypeQueryExecutor;
import org.atlasapi.persistence.content.query.QueryFragmentExtractor;
import org.atlasapi.persistence.logging.NullAdapterLog;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.common.util.concurrent.MoreExecutors;
import com.metabroadcast.common.time.DateTimeZones;

public class RadioPlayerFileUploaderTest {

	private static final String TEST_PASSWORD = "testpassword";
	private static final String TEST_USERNAME = "test";

	private static File dir;

	private FtpServer server;

	@Test
	public void testRun() throws Exception {
		try {
			dir = Files.createTempDir();
			System.out.println(dir);
			dir.deleteOnExit();
			File files = new File(dir.getAbsolutePath() + File.separator + "files");
			files.mkdir();

			startServer();

			KnownTypeQueryExecutor queryExecutor = new KnownTypeQueryExecutor() {
				
				@Override
				public List<Playlist> executePlaylistQuery(ContentQuery query) {
					return null;
				}
				
				@Override
				public List<Item> executeItemQuery(ContentQuery query) {
					return ImmutableList.of(buildItem(query));
				}
				
				@Override
				public List<Brand> executeBrandQuery(ContentQuery query) {
					return null;
				}
			};
			
			ImmutableList<RadioPlayerService> services = ImmutableList.of(RadioPlayerServices.all.get("340"));
			RadioPlayerFTPCredentials credentials = RadioPlayerFTPCredentials.forServer("localhost").withPort(9521).withUsername("test").withPassword("testpassword").build();
			int lookAhead = 0, lookBack = 0;
			RadioPlayerFileUploader uploader = new RadioPlayerFileUploader(credentials, "files", queryExecutor, new NullAdapterLog()).withServices(services).withLookAhead(lookAhead).withLookBack(lookBack);

			Executor executor = MoreExecutors.sameThreadExecutor();

			executor.execute(uploader);

			Map<String, File> uploaded = Maps.uniqueIndex(ImmutableSet.copyOf(files.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.endsWith("PI.xml");
				}
			})), new Function<File, String>() {
				@Override
				public String apply(File input) {
					return input.getName();
				}
			});
			
			assertThat(uploaded.size(), is(equalTo(1)));

			DateTime day = new DateTime(DateTimeZones.UTC);
		
			String filename = String.format("%4d%02d%02d_340_PI.xml", day.getYear(), day.getMonthOfYear(), day.getDayOfMonth());
			assertThat(uploaded.keySet(), hasItem(filename));
			assertThat(uploaded.get(filename).length(), greaterThan(0L));

		} finally {
			server.stop();
		}

	}

	private void startServer() throws FtpException {
		FtpServerFactory serverFactory = new FtpServerFactory();

		ListenerFactory factory = new ListenerFactory();

		factory.setPort(9521);

		serverFactory.addListener("default", factory.createListener());

		serverFactory.setUserManager(new TestUserManager(new TestUser(TEST_USERNAME, TEST_PASSWORD, dir)));
		
		server = serverFactory.createServer();

		server.start();
	}




	public static Item buildItem(ContentQuery query) {
		String service = (String) QueryFragmentExtractor.extract(query, ImmutableSet.<Attribute<?>>of(Attributes.BROADCAST_ON)).requireValue().getValue().get(0);
		DateTime transmissionStart = ((DateTime)QueryFragmentExtractor.extract(query, ImmutableSet.<Attribute<?>>of(Attributes.BROADCAST_TRANSMISSION_TIME)).requireValue().getValue().get(0)).plusHours(18).plusMinutes(30);
		DateTime transmissionEnd = transmissionStart.plusHours(1);
		
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
}
