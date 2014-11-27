package org.atlasapi.feeds.youview.revocation;


public interface RevokedContentStore {

    /**
     * Marks a content uri as Revoked, indicating that it should not
     * be uploaded to YouView.
     * 
     * @param uri the uri of the content to be revoked
     */
    void revoke(String uri);
    
    /**
     * Removes a content uri from the Revoked content list. Returns true
     * if content was revoked, and false if content was not already revoked.
     * 
     * @param uri the uri of the content to be unrevoked
     * @return true if content already revoked, false otherwise
     */
    boolean unrevoke(String uri);
    
    /**
     * Checks if a given content uri is revoked.
     * @param uri the uri of the content to check for revocation
     * @return true if content is revoked, false otherwise
     */
    boolean isRevoked(String uri);
}
