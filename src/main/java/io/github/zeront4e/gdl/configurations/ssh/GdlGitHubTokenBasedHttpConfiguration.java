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
