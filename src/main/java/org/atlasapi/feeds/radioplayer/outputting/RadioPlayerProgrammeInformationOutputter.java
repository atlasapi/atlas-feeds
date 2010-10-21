package org.atlasapi.feeds.radioplayer.outputting;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import nu.xom.Attribute;
import nu.xom.Element;

import org.atlasapi.feeds.radioplayer.RadioPlayerServiceIdentifier;
import org.atlasapi.media.entity.Brand;
import org.atlasapi.media.entity.Broadcast;
import org.atlasapi.media.entity.Episode;
import org.atlasapi.media.entity.Item;
import org.atlasapi.media.entity.Location;
import org.atlasapi.media.entity.Policy;
import org.atlasapi.media.entity.Series;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.joda.time.format.ISOPeriodFormat;

import com.google.common.base.Strings;

public class RadioPlayerProgrammeInformationOutputter extends RadioPlayerXMLOutputter {
	
	/* <epg xmlns:epg="http://www.radioplayer.co.uk/schemas/10/epgDataTypes" 
	 *		xmlns="http://www.radioplayer.co.uk/schemas/10/epgSchedule" 
	 *		xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
	 *		xmlns:radioplayer="http://www.radioplayer.co.uk/schemas/10/rpDataTypes"
	 *		xsi:schemaLocation="http://www.radioplayer.co.uk/schemas/10/epgSchedule http://www.radioplayer.co.uk/schemas/10/epgSchedule_10.xsd"
	 * />
	 */

	private static final String ORIGINATOR = "Metabroadcast";
	private static final String ONDEMAND_LOCATION = "http://bbcradioplayer.metabroadcast.com/";

	@Override
	public Element createFeed(DateTime day, RadioPlayerServiceIdentifier id, Iterable<Item> items) {
		Element epgElem = createElement("epg", EPGSCHEDULE);
		EPGDATATYPES.addDeclarationTo(epgElem);
		XSI.addDeclarationTo(epgElem);
		RADIOPLAYER.addDeclarationTo(epgElem);
		epgElem.addAttribute(new Attribute("xsi:schemaLocation", XSI.getUri(), SCHEMALOCATION));
		epgElem.addAttribute(new Attribute("system", "DAB"));
		epgElem.addAttribute(new Attribute("xml:lang", "http://www.w3.org/XML/1998/namespace", "en"));
		
		Element schedule = createElement("schedule", EPGSCHEDULE );
		schedule.addAttribute(new Attribute("originator", ORIGINATOR));
		schedule.addAttribute(new Attribute("version", "1"));
		schedule.addAttribute(new Attribute("creationTime", DATE_TIME_FORMAT.print(day)));
		
		schedule.appendChild(scopeElement(day, id));
		
		for (Item item : items) {
			schedule.appendChild(createProgrammeElement(item, day, id));
		}
		
		epgElem.appendChild(schedule);
		return epgElem;
	}

	private Element createProgrammeElement(Item item, DateTime day, RadioPlayerServiceIdentifier id) {
		Element programme = createElement("programme", EPGSCHEDULE);
		programme.addAttribute(new Attribute("shortId","0"));
		programme.addAttribute(new Attribute("id", item.getCanonicalUri().replace("http://","crid://")));
		
		String title = itemTitle(item);
		programme.appendChild(stringElement("mediumName", EPGDATATYPES, MEDIUM_TITLE.truncatePossibleNull(title)));
		programme.appendChild(stringElement("longName", EPGDATATYPES, LONG_TITLE.truncatePossibleNull(title)));
		
		Broadcast broadcast = broadcastFrom(item, id.getBroadcastUri());
		programme.appendChild(locationElement(item, broadcast, day,id));
		programme.appendChild(mediaDescription(stringElement("shortDescription", EPGDATATYPES, SHORT_DESC.truncatePossibleNull(item.getDescription()))));
		if (!Strings.isNullOrEmpty(item.getImage())) {
			programme.appendChild(mediaDescription(imageDescriptionElem(item)));
		}
//		for(String genre : item.getGenres()){
//			//add genres
//		}
		
		Location location = locationFrom(item);
		if(location != null){
			programme.appendChild(ondemandElement(item, location, day, id));
		}
		
		return programme;
	}
	
	private String itemTitle(Item item) {
		String title = Strings.nullToEmpty(item.getTitle());
		if (item instanceof Episode) {
			Brand brand = ((Episode) item).getBrand();
			if (brand != null && !Strings.isNullOrEmpty(brand.getTitle())) {
				String brandTitle = brand.getTitle();
				if (!brandTitle.equals(title)) {
					return brandTitle + " : " + title;
				}
			}
			Series series = ((Episode) item).getSeriesSummary();
			if (series != null && !Strings.isNullOrEmpty(series.getTitle())) {
				String seriesTitle = series.getTitle();
				if (!seriesTitle.equals(title)) {
					return seriesTitle + " : " + title;
				}
			}			
		}
		return title;
	}

	private Element locationElement(Item item, Broadcast broadcast, DateTime day, RadioPlayerServiceIdentifier id) {
		Element locationElement = createElement("location", EPGDATATYPES);
	
		Element timeElement = createElement("time", EPGDATATYPES);
		timeElement.addAttribute(new Attribute("time", DATE_TIME_FORMAT.print(broadcast.getTransmissionTime())));
		
		Duration duration = new Duration(broadcast.getBroadcastDuration().intValue() * 1000);
		timeElement.addAttribute(new Attribute("duration", ISOPeriodFormat.standard().print(duration.toPeriod())));
		
		Element bearerElement = createElement("bearer", EPGDATATYPES);
		bearerElement.addAttribute(new Attribute("radioplayerId", id.getRadioplayerId()+""));
		bearerElement.addAttribute(new Attribute("id", id.getServiceID().replace("_", ".")));

		locationElement.appendChild(timeElement);
		locationElement.appendChild(bearerElement);
		return locationElement;
	}

	private Element mediaDescription(Element childElem) {
		Element descriptionElement = createElement("mediaDescription", EPGDATATYPES);
		descriptionElement.appendChild(childElem);
		return descriptionElement;
	}
	
	private Element imageDescriptionElem(Item item) {
		Element imageElement = createElement("multimedia", EPGDATATYPES);
		imageElement.addAttribute(new Attribute("mimeValue", "image/jpeg"));
		imageElement.addAttribute(new Attribute("url", imageLocationFrom(item)));
		imageElement.addAttribute(new Attribute("width", "86"));
		imageElement.addAttribute(new Attribute("height", "48"));
		return imageElement;
	}
	
	private String imageLocationFrom(Item item) {
		Pattern p = Pattern.compile("(.*)_\\d+_\\d+.jpg");
		Matcher m = p.matcher(item.getImage());
		if (m.matches()) {
			return m.group(1) + "_86_48.jpg";
		}
		return item.getImage();
	}

	private Element ondemandElement(Item item, Location location, DateTime day, RadioPlayerServiceIdentifier id) {
		Element ondemandElement = createElement("ondemand", EPGDATATYPES);
		
		ondemandElement.appendChild(stringElement("player", RADIOPLAYER, ONDEMAND_LOCATION + id.getName() +"/"+ item.getCurie().substring(item.getCurie().indexOf(":")+1)));

		Policy policy = location.getPolicy();
		if (policy != null) {
//			Set<Country> countries = policy.getAvailableCountries();
//			if (!countries.contains(Countries.ALL)) {
//				String spaceDelimted = Joiner.on(' ').join(Iterables.transform(countries, Country.UNPACK_COUNTRY_CODE));
//				Element restrictionElem = createElement("restriction", RADIOPLAYER);
//				restrictionElem.addAttribute(new Attribute("relationship","allow"));
//				restrictionElem.appendChild(spaceDelimted);
//				ondemandElement.appendChild(restrictionElem);
//			} else {
//				Element restrictionElem = createElement("restriction", RADIOPLAYER);
//				restrictionElem.addAttribute(new Attribute("relationship","deny"));
//				ondemandElement.appendChild(restrictionElem);
//			}
			
			DateTime availableTill = policy.getAvailabilityEnd();
			DateTime availableFrom = policy.getAvailabilityStart();
			if (availableTill != null && availableFrom != null) {
				Element availabilityElem = createElement("availability", RADIOPLAYER);
				Element availabilityScopeElem = createElement("scope", RADIOPLAYER);
				availabilityScopeElem.addAttribute(new Attribute("startTime", DATE_TIME_FORMAT.print(availableFrom)));
				availabilityScopeElem.addAttribute(new Attribute("stopTime", DATE_TIME_FORMAT.print(availableTill)));
				availabilityElem.appendChild(availabilityScopeElem);
				ondemandElement.appendChild(availabilityElem);
			}

		}
		
		return ondemandElement;
	}

	private Element scopeElement(DateTime day, RadioPlayerServiceIdentifier id) {
		Element scope = createElement("scope", EPGSCHEDULE);
		scope.addAttribute(new Attribute("startTime",DATE_TIME_FORMAT.print(day)));
		scope.addAttribute(new Attribute("stopTime", DATE_TIME_FORMAT.print(day.plusDays(1))));
		
		Element service = createElement("serviceScope", EPGSCHEDULE);
		service.addAttribute(new Attribute("id",id.getServiceID().replaceAll("_", ".")));
		service.addAttribute(new Attribute("radioplayerId",String.valueOf(id.getRadioplayerId())));
		scope.appendChild(service);
		return scope;
	}
}
