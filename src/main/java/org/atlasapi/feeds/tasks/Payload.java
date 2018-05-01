package org.atlasapi.feeds.tasks;

import static com.google.common.base.Preconditions.checkNotNull;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Pattern;

import com.google.common.base.Charsets;
import org.apache.commons.codec.binary.Base64;
import org.joda.time.DateTime;

import com.google.common.base.Objects;
import com.google.common.base.Throwables;

public final class Payload {

    private final String payload;
    private final DateTime created;

    private static final Pattern TVAMAIN_PATTERN = Pattern.compile("<TVAMain[^>]+>");
    private static final Pattern NS_PATTERN = Pattern.compile("</?\bns[0-9]?:");

    public Payload(String payload, DateTime created) {
        this.payload = checkNotNull(payload);
        this.created = checkNotNull(created);
    }

    public String payload() {
        return payload;
    }

    public DateTime created() {
        return created;
    }

    public String hash() {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            //We have seen namespaces swapping order in the TVAMain element tag. This causes
            //different hashes without actual difference in the xml. We will remove the namespacing.
            String p = TVAMAIN_PATTERN.matcher(payload).replaceAll("");
            p = NS_PATTERN.matcher(p).replaceAll("");

            md.update(p.getBytes("UTF-8"));
            byte[] digest = md.digest();

            return new String(Base64.encodeBase64(digest, false), "UTF-8");
        } catch (NoSuchAlgorithmException | UnsupportedEncodingException e) {
            throw Throwables.propagate(e);
        }
    }

    public boolean hasChanged(String previousHash) {
        return !hash().equals(previousHash);
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(Payload.class)
                .add("payload", payload)
                .add("created", created)
                .toString();
    }
    
    @Override
    public int hashCode() {
        return Objects.hashCode(payload);
    }
    
    @Override
    public boolean equals(Object that) {
        if (this == that) {
            return true;
        }
        
        if (!(that instanceof Payload)) {
            return false;
        }
        
        Payload other = (Payload) that;
        return payload.equals(other.payload);
    }
}
