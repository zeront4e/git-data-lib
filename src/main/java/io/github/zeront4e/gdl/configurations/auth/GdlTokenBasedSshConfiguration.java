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

package io.github.zeront4e.gdl.configurations.auth;

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
