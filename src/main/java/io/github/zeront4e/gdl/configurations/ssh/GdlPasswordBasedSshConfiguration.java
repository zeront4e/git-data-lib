package io.github.zeront4e.gdl.configurations.ssh;

import java.io.File;
import java.util.Objects;

public class GdlPasswordBasedSshConfiguration {
    private final String username;
    private final String password;
    private final File knownHostsFile;

    public GdlPasswordBasedSshConfiguration(String username, String password, File knownHostsFile) {
        this.username = username;
        this.password = password;
        this.knownHostsFile = knownHostsFile;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

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

    @Override
    public String toString() {
        return "GdlPasswordBasedSshConfiguration{" +
                "username='" + username + '\'' +
                ", password='" + password + '\'' +
                ", knownHostsFile=" + knownHostsFile +
                '}';
    }
}
