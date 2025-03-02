package io.github.zeront4e.gdl.configurations.ssh;

import java.io.File;
import java.util.Objects;

public class GdlKeyBasedSshConfiguration {
    private final File privateKeyFile;
    private final File knownHostsFile;

    private final String optionKeyDecryptionPassword;

    public GdlKeyBasedSshConfiguration(File privateKeyFile, File knownHostsFile) {
        this(privateKeyFile, knownHostsFile, null);
    }

    public GdlKeyBasedSshConfiguration(File privateKeyFile, File knownHostsFile, String optionKeyDecryptionPassword) {
        this.privateKeyFile = privateKeyFile;
        this.knownHostsFile = knownHostsFile;
        this.optionKeyDecryptionPassword = optionKeyDecryptionPassword;
    }

    public File getPrivateKeyFile() {
        return privateKeyFile;
    }

    public File getKnownHostsFile() {
        return knownHostsFile;
    }

    public String getOptionKeyDecryptionPassword() {
        return optionKeyDecryptionPassword;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        GdlKeyBasedSshConfiguration that = (GdlKeyBasedSshConfiguration) object;
        return Objects.equals(privateKeyFile, that.privateKeyFile) &&
                Objects.equals(knownHostsFile, that.knownHostsFile) &&
                Objects.equals(optionKeyDecryptionPassword, that.optionKeyDecryptionPassword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(privateKeyFile, knownHostsFile, optionKeyDecryptionPassword);
    }

    @Override
    public String toString() {
        return "GdlKeyBasedSshConfiguration{" +
                "privateKeyFile=" + privateKeyFile +
                ", knownHostsFile=" + knownHostsFile +
                ", optionKeyDecryptionPassword='" + optionKeyDecryptionPassword + '\'' +
                '}';
    }
}
