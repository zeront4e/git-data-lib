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
