package org.atlasapi.feeds.radioplayer;

import com.google.common.primitives.Ints;

public class RadioPlayerService {

	private final int radioplayerId;
	private final String name;
	private String serviceUri;
	private String dabServiceId = "00.0000.0000.0";
	private String scheduleUri;
	
	public RadioPlayerService(int radioplayerId, String name) {
		this.radioplayerId = radioplayerId;
		this.name = name;
	}
	
	public int getRadioplayerId() {
		return radioplayerId;
	}
	
	public String getName() {
		return name;
	}

	public RadioPlayerService withDabServiceId(String serviceId) {
		this.dabServiceId = serviceId;
		return this;
	}
	
	public String getDabServiceId() {
		return dabServiceId;
	}
	
	public RadioPlayerService withServiceUriSuffix(String serviceUri) {
		this.serviceUri = serviceUri;
		return this;
	}

	public String getServiceUri() {
		return "http://www.bbc.co.uk/services/" + (serviceUri != null ? serviceUri : name);
	}
	
	public RadioPlayerService withScheduleUri(String scheduleUri) {
		this.scheduleUri = scheduleUri;
		return this;
	}
	
	public String getScheduleUri() {
		return (scheduleUri != null) ? scheduleUri : String.format("http://www.bbc.co.uk/%s/programmes/schedules", name);
	}

	@Override
	public boolean equals(Object that) {
		if (this == that) {
			return true;
		}
		if (that instanceof RadioPlayerService) {
			return ((RadioPlayerService)that).radioplayerId == radioplayerId;
		}
		return false;
	}

	@Override
	public int hashCode() {
		return Ints.hashCode(radioplayerId);
	}
	
	@Override
	public String toString() {
		return String.format("[%d] %s (%s)", radioplayerId, name, serviceUri);
	}
}
