package io.github.zeront4e.gdl.configurations.ssh;

import java.io.File;
import java.util.Objects;

/**
 * Configuration class for password-based SSH authentication.
 * This class stores the credentials and known hosts file needed for establishing
 * SSH connections using password authentication.
 */
public class GdlPasswordBasedSshConfiguration {
    private final String username;
    private final String password;
    private final File knownHostsFile;

    /**
     * Constructs a new password-based SSH configuration.
     * @param username The SSH username for authentication.
     * @param password The SSH password for authentication.
     * @param knownHostsFile The file containing known SSH hosts, used for host verification.
     */
    public GdlPasswordBasedSshConfiguration(String username, String password, File knownHostsFile) {
        this.username = username;
        this.password = password;
        this.knownHostsFile = knownHostsFile;
    }

    /**
     * Returns the SSH username.
     * @return The username for SSH authentication.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the SSH password.
     * @return The password for SSH authentication.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Returns the known hosts file.
     * @return The file containing known SSH hosts.
     */
    public File getKnownHostsFile() {
        return knownHostsFile;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        GdlPasswordBasedSshConfiguration that = (GdlPasswordBasedSshConfiguration) object;
        return Objects.equals(username, that.username) && Objects.equals(password, that.password) &&
                Objects.equals(knownHostsFile, that.knownHostsFile);
    }

    @Override
    public int hashCode() {
        return Objects.hash(username, password, knownHostsFile);
    }

    /**
     * Returns a string representation of this configuration with the password obfuscated.
     * @return A string representation of this configuration.
     */
    @Override
    public String toString() {
        return "GdlPasswordBasedSshConfiguration{" +
                "username='" + username + '\'' +
                ", password='" + getObfuscatedPasswordSubstring(password) + '\'' +
                ", knownHostsFile=" + knownHostsFile +
                '}';
    }

    protected String getObfuscatedPasswordSubstring(String password) {
        int obfuscatedLength = Math.min(password.length(), 4);

        return password.substring(0, obfuscatedLength) + "...";
    }
}
