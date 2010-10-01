package org.atlasapi.feeds.radioplayer;

public class RadioPlayerServiceIdentifier {
	
	private final int radioplayerId;
	private final String broadcastUri;
	private final String name;
	private final String serviceID;
	
	/**
	 * Create and identifier for services with both DAB EPG serviceID and radioplayerId
	 * @param serviceID
	 * @param radioplayerId
	 * @param broadcastUri
	 */
	public RadioPlayerServiceIdentifier(int radioplayerId, String name, String broadcastUri, String serviceID) {
		this.serviceID = serviceID;
		this.name = name;
		this.radioplayerId = radioplayerId;
		this.broadcastUri = broadcastUri;
	}
	
	/**
	 * Create an identifier for services sans DAB EPG serviceID, uses default value 00.0000.0000.0 for the serviceId
	 * @param radioPlayerId - the services radioplayer identification number
	 * @param broadcastUri
	 */
	public RadioPlayerServiceIdentifier(int radioPlayerId, String name, String broadcastUri){
		this(radioPlayerId, broadcastUri, name, "00.0000.0000.0");
	}

	/**
	 * 
	 * @return the radioplayer identifier
	 */
	public int getRadioplayerId() {
		return radioplayerId;
	}

	/**
	 * Get the services DAB EPG spec. 00.0000.0000.0 if the service does not have one.
	 * <ecc>.<eid>.<sid>.<scids>
	 * @return the DAB EPG spec identifier
	 */
	public String getServiceID() {
		return serviceID;
	}

	public String getBroadcastUri() {
		return broadcastUri;
	}

	public String getName() {
		return name;
	}

}
