package io.github.zeront4e.gdl.configurations.ssh;

/**
 * Configuration class for HTTP connections that use token-based authentication.
 * This class extends the password-based configuration but specifically handles token authentication
 * by using an empty username and the provided token as the password.
 */
public class GdlTokenBasedHttpConfiguration extends GdlPasswordBasedHttpConfiguration {
    
    /**
     * Creates a new token-based HTTP configuration.
     * @param token The authentication token to use for HTTP connections.
     */
    public GdlTokenBasedHttpConfiguration(String token) {
        super("", token);
    }

    /**
     * Returns a string representation of this configuration.
     * The token is obfuscated for security purposes.
     * @return A string representation of this configuration.
     */
    @Override
    public String toString() {
        return "GdlTokenBasedHttpConfiguration{" +
                "token='" + getObfuscatedPasswordSubstring(getPassword()) + '\'' +
                '}';
    }
}
