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

import java.util.Objects;

/**
 * Configuration class for password-based HTTP authentication.
 * This class stores username and password credentials for HTTP connections.
 */
public class GdlPasswordBasedHttpConfiguration {
    private final String username;
    private final String password;

    /**
     * Constructs a new password-based HTTP configuration.
     * @param username The username for authentication.
     * @param password The password for authentication.
     */
    public GdlPasswordBasedHttpConfiguration(String username, String password) {
        this.username = username;
        this.password = password;
    }

    /**
     * Returns the username for authentication.
     * @return The username.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Returns the password for authentication.
     * @return The password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Compares this configuration with another object for equality.
     * Two configurations are considered equal if they have the same username and password.
     * @param object The object to compare with.
     * @return True if the objects are equal, false otherwise.
     */
    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        GdlPasswordBasedHttpConfiguration that = (GdlPasswordBasedHttpConfiguration) object;
        return Objects.equals(username, that.username) && Objects.equals(password, that.password);
    }

    /**
     * Returns a hash code value for this configuration.
     * @return A hash code value based on the username and password.
     */
    @Override
    public int hashCode() {
        return Objects.hash(username, password);
    }

    /**
     * Returns a string representation of this configuration.
     * The password is partially obfuscated for security reasons.
     * @return A string representation of this configuration.
     */
    @Override
    public String toString() {
        return "GdlPasswordBasedHttpConfiguration{" +
                "username='" + username + '\'' +
                ", password='" + getObfuscatedPasswordSubstring(password) + '\'' +
                '}';
    }

    protected String getObfuscatedPasswordSubstring(String password) {
        int obfuscatedLength = Math.min(password.length(), 4);

        return password.substring(0, obfuscatedLength) + "...";
    }
}
