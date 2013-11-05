package org.atlasapi.application.auth;


public class InvalidApiKeyException extends Exception {
    private static final long serialVersionUID = -8204400513571208163L;
    private final String apiKey;
    
    public InvalidApiKeyException(String apiKey) {
        this.apiKey = apiKey;
    }

    @Override
    public String getMessage() {
        return "Invalid API key: " + apiKey;
    }
}
