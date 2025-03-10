/*
Copyright 2025 zeront4e (https://github.com/zeront4e)

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/

package io.github.zeront4e.gdl.configurations.ssh;

import java.io.File;
import java.util.Objects;

/**
 * Configuration class for SSH authentication using key-based authentication.
 * This class holds the necessary files and credentials for establishing
 * secure SSH connections.
 */
public class GdlKeyBasedSshConfiguration {
    private final File privateKeyFile;

    private final File knownHostsFile;

    private final String optionKeyDecryptionPassword;

    /**
     * Creates a new SSH configuration with key-based authentication.
     * @param privateKeyFile The file containing the private key for authentication.
     * @param knownHostsFile The file containing known hosts information.
     */
    public GdlKeyBasedSshConfiguration(File privateKeyFile, File knownHostsFile) {
        this(privateKeyFile, knownHostsFile, null);
    }

    /**
     * Creates a new SSH configuration with key-based authentication and optional key decryption.
     * @param privateKeyFile The file containing the private key for authentication.
     * @param knownHostsFile The file containing known hosts information.
     * @param optionKeyDecryptionPassword The password to decrypt the private key, or null if the key is not encrypted.
     */
    public GdlKeyBasedSshConfiguration(File privateKeyFile, File knownHostsFile, String optionKeyDecryptionPassword) {
        this.privateKeyFile = privateKeyFile;
        this.knownHostsFile = knownHostsFile;
        this.optionKeyDecryptionPassword = optionKeyDecryptionPassword;
    }

    /**
     * Returns the private key file used for SSH authentication.
     * @return The private key file.
     */
    public File getPrivateKeyFile() {
        return privateKeyFile;
    }

    /**
     * Returns the known hosts file containing trusted SSH server fingerprints.
     * @return The known hosts file.
     */
    public File getKnownHostsFile() {
        return knownHostsFile;
    }

    /**
     * Returns the password used to decrypt the private key, if any.
     * @return The decryption password, or null if the key is not encrypted.
     */
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
