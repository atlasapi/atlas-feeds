package org.atlasapi.feeds.radioplayer.upload;

import com.google.common.base.Objects;

public class FTPCredentials {

	private final String server;
	private final int port;
	private final String username;
	private final String password;

	private FTPCredentials(String server, int port, String username, String password) {
		this.server = server;
		this.port = port;
		this.username = username;
		this.password = password;
	}

	public String server() {
		return server;
	}

	public int port() {
		return port;
	}

	public String username() {
		return username;
	}

	public String password() {
		return password;
	}
	
	@Override
	public boolean equals(Object that) {
		if(this == that) {
			return true;
		}
		if(that instanceof FTPCredentials) {
			FTPCredentials other = (FTPCredentials) that;
			return Objects.equal(other.server, server) && other.port == port && Objects.equal(other.username, username) && Objects.equal(other.password, password);
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return Objects.hashCode(server, port, username, password);
	}
	
	@Override
	public String toString() {
		return String.format("%s@%s:%d", username, server, port);
	}

	public static RadioPlayerFTPCredentialsBuilder forServer(String server) {
		return new RadioPlayerFTPCredentialsBuilder(server);
	}
	
	public static class RadioPlayerFTPCredentialsBuilder {

		private final String server;
		private int port = 21;
		private String username;
		private String password;

		public RadioPlayerFTPCredentialsBuilder(String server) {
			this.server = server;
		}
		
		public FTPCredentials build() {
			return new FTPCredentials(server, port, username, password);
		}
		
		public RadioPlayerFTPCredentialsBuilder withPort(int port) {
			this.port = port;
			return this;
		}
		
		public RadioPlayerFTPCredentialsBuilder withUsername(String username) {
			this.username = username;
			return this;
		}
		
		public RadioPlayerFTPCredentialsBuilder withPassword(String password) {
			this.password = password;
			return this;
		}
	}
	
}
