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
import java.util.Objects;

/**
 * Base configuration class for Git Data Library operations.
 * This abstract class provides common configuration properties needed for Git operations.
 */
public abstract class GdlBaseConfiguration {
    private final File localRepositoryDirectory;
    private final Map<String, String> secretNameSecretMap;
    private final String branch;

    /**
     * Constructs a new GdlBaseConfiguration with the specified parameters.
     * @param localRepositoryDirectory The local directory where the Git repository is located or will be cloned to.
     * @param secretNameSecretMap A map containing secret names as keys and their corresponding secret values.
     * @param branch The Git branch to operate on.
     */
    public GdlBaseConfiguration(File localRepositoryDirectory, Map<String, String> secretNameSecretMap, String branch) {
        this.localRepositoryDirectory = localRepositoryDirectory;
        this.secretNameSecretMap = secretNameSecretMap;
        this.branch = branch;
    }

    /**
     * Returns the local repository directory.
     * @return The File object representing the local Git repository directory.
     */
    public File getLocalRepositoryDirectory() {
        return localRepositoryDirectory;
    }

    /**
     * Returns the map of secret names to their corresponding secret values.
     * @return A map where keys are secret names and values are the actual secrets.
     */
    public Map<String, String> getSecretNameSecretMap() {
        return secretNameSecretMap;
    }

    /**
     * Returns the Git branch name configured for operations.
     * @return The name of the Git branch.
     */
    public String getBranch() {
        return branch;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        GdlBaseConfiguration that = (GdlBaseConfiguration) object;
        return Objects.equals(localRepositoryDirectory, that.localRepositoryDirectory) &&
                Objects.equals(secretNameSecretMap, that.secretNameSecretMap) && Objects.equals(branch, that.branch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localRepositoryDirectory, secretNameSecretMap, branch);
    }

    @Override
    public String toString() {
        return "GdlBaseConfiguration{" +
                "localRepositoryDirectory=" + localRepositoryDirectory +
                ", secretNameSecretMap=" + createSecretEntriesKeyValueString() +
                ", branch='" + branch + '\'' +
                '}';
    }

    protected String createSecretEntriesKeyValueString() {
        StringBuilder secretEntries = new StringBuilder();

        for (Map.Entry<String, String> entry : secretNameSecretMap.entrySet()) {
            secretEntries.append(entry.getKey()).append("=").append("...").append(" ");
        }

        return secretEntries.toString().trim();
    }
}
