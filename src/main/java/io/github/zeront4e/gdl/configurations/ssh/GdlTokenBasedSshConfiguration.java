package io.github.zeront4e.gdl.configurations.ssh;

import java.io.File;

/**
 * Configuration class for SSH connections that use token-based authentication.
 * This class extends the password-based SSH configuration but uses a token instead of a username/password pair.
 */
public class GdlTokenBasedSshConfiguration extends GdlPasswordBasedSshConfiguration {
    /**
     * Constructs a new token-based SSH configuration.
     * @param token The authentication token to use for SSH connections.
     * @param knownHostsFile The file containing known hosts for SSH verification, or null if not used.
     */
    public GdlTokenBasedSshConfiguration(String token, File knownHostsFile) {
        super("", token, knownHostsFile);
    }

    /**
     * Returns a string representation of this configuration.
     * The token is obfuscated for security reasons.
     * @return A string representation of this configuration.
     */
    @Override
    public String toString() {
        return "GdlTokenBasedSshConfiguration{" +
                "token='" + getObfuscatedPasswordSubstring(getPassword()) + '\'' +
                ", knownHostsFile=" + getKnownHostsFile() +
                '}';
    }
}
