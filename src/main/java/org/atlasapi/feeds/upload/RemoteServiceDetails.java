package org.atlasapi.feeds.upload;

import static com.google.common.base.Preconditions.checkNotNull;

import com.google.common.base.Objects;
import com.google.common.net.HostSpecifier;
import com.metabroadcast.common.security.UsernameAndPassword;

public class RemoteServiceDetails {

    public static RemoteServiceDetailsBuilder forServer(HostSpecifier server) {
        return new RemoteServiceDetailsBuilder(server);
    }
    
    public static class RemoteServiceDetailsBuilder {

        private final HostSpecifier server;
        private int port = 21;
        private UsernameAndPassword usernameAndPassword;

        public RemoteServiceDetailsBuilder(HostSpecifier server) {
            this.server = checkNotNull(server);
        }

        public RemoteServiceDetails build() {
            return new RemoteServiceDetails(server, port, usernameAndPassword);
        }

        public RemoteServiceDetailsBuilder withPort(int port) {
            this.port = port;
            return this;
        }

        public RemoteServiceDetailsBuilder withCredentials(UsernameAndPassword usernameAndPassword) {
            this.usernameAndPassword = usernameAndPassword;
            return this;
        }
    }

    private final HostSpecifier server;
    private final int port;
    private UsernameAndPassword usernameAndPassword;

    private RemoteServiceDetails(HostSpecifier server, int port, UsernameAndPassword usernameAndPassword) {
        this.server = checkNotNull(server);
        this.port = port;
        this.usernameAndPassword = usernameAndPassword;
    }

    public HostSpecifier server() {
        return server;
    }

    public int port() {
        return port;
    }

    public UsernameAndPassword credentials() {
        return usernameAndPassword;
    }
    
    public RemoteServiceDetails copyWithCredentials(UsernameAndPassword usernameAndPassword) {
        return new RemoteServiceDetails(server, port, usernameAndPassword);
    }
    
    public boolean hasCredentials() {
        return usernameAndPassword != null;
    }

    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        if (that instanceof RemoteServiceDetails) {
            RemoteServiceDetails other = (RemoteServiceDetails) that;
            return other.server.equals(server) && other.port == port && Objects.equal(other.usernameAndPassword, usernameAndPassword);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(server, port, usernameAndPassword);
    }

    @Override
    public String toString() {
        if(usernameAndPassword != null) {
            return String.format("%s@%s:%d", usernameAndPassword.username(), server, port);
        }
        return String.format("%s:%d", server, port);
    }

}
