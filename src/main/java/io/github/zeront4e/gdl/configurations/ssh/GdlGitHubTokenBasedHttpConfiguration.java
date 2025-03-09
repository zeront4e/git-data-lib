package io.github.zeront4e.gdl.configurations.ssh;

/**
 * Configuration class for GitHub HTTP connections using token-based authentication.
 * This class extends the password-based HTTP configuration but specifically handles
 * GitHub personal access tokens for authentication.
 */
public class GdlGitHubTokenBasedHttpConfiguration extends GdlPasswordBasedHttpConfiguration {
    /**
     * Creates a new GitHub token-based HTTP configuration.
     * @param token The GitHub personal access token to use for authentication.
     *              This token will be used instead of a password when connecting to GitHub.
     */
    public GdlGitHubTokenBasedHttpConfiguration(String token) {
        super("", token);
    }

    /**
     * Returns a string representation of this configuration.
     * @return A string containing the token information (masked for security).
     */
    @Override
    public String toString() {
        return "GdlGitHubTokenBasedHttpConfiguration{" +
                "token='" + getObfuscatedPasswordSubstring(getPassword()) + '\'' +
                '}';
    }
}
