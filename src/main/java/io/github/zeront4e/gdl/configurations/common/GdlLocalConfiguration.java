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

package io.github.zeront4e.gdl.configurations.common;

import java.io.File;
import java.util.Map;

/**
 * Configuration class for local Git repository operations.
 * This class extends the base configuration to provide specific functionality
 * for working with local Git repositories.
 */
public class GdlLocalConfiguration extends GdlBaseConfiguration {
    
    /**
     * Constructs a new GdlLocalConfiguration with the specified parameters.
     * @param localRepositoryDirectory The directory where the local Git repository is located.
     * @param secretNameSecretMap A map containing secret names as keys and their corresponding secret values.
     * @param branch The Git branch to operate on.
     */
    public GdlLocalConfiguration(File localRepositoryDirectory, Map<String, String> secretNameSecretMap, String branch) {
        super(localRepositoryDirectory, secretNameSecretMap, branch);
    }

    /**
     * Returns a string representation of this configuration.
     * The string includes the local repository directory, secret map entries (with masked values),
     * and the branch name.
     * @return A string representation of this configuration
     */
    @Override
    public String toString() {
        return "GdlLocalConfiguration{" +
                "localRepositoryDirectory=" + getLocalRepositoryDirectory() +
                ", secretNameSecretMap=" + createSecretEntriesKeyValueString() +
                ", branch='" + getBranch() + '\'' +
                '}';
    }
}