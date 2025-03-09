package io.github.zeront4e.gdl.configurations.common;

import java.io.File;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration class for online Git repository operations.
 * Extends the base configuration with additional parameters specific to remote Git operations.
 */
public class GdlOnlineConfiguration extends GdlBaseConfiguration {
    private final String gitRepositoryString;
    private final int pushDelayMilliseconds;

    /**
     * Constructs a new online Git configuration.
     * @param localRepositoryDirectory The local directory where the Git repository is located.
     * @param secretNameSecretMap A map containing secret names and their corresponding secret values.
     * @param branch The Git branch to operate on.
     * @param gitRepositoryString The remote Git repository URL or identifier string.
     * @param pushDelayMilliseconds The delay in milliseconds before pushing changes to the remote repository.
     */
    public GdlOnlineConfiguration(File localRepositoryDirectory, Map<String, String> secretNameSecretMap, String branch,
                                  String gitRepositoryString, int pushDelayMilliseconds) {
        super(localRepositoryDirectory, secretNameSecretMap, branch);

        this.gitRepositoryString = gitRepositoryString;
        this.pushDelayMilliseconds = pushDelayMilliseconds;
    }

    /**
     * Returns the Git repository URL or identifier string.
     * @return The Git repository string used for remote operations.
     */
    public String getGitRepositoryString() {
        return gitRepositoryString;
    }

    /**
     * Returns the configured delay before pushing changes to the remote repository.
     * @return The push delay in milliseconds.
     */
    public int getPushDelayMilliseconds() {
        return pushDelayMilliseconds;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        if (!super.equals(object)) return false;
        GdlOnlineConfiguration that = (GdlOnlineConfiguration) object;
        return pushDelayMilliseconds == that.pushDelayMilliseconds && Objects.equals(gitRepositoryString,
                that.gitRepositoryString);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), gitRepositoryString, pushDelayMilliseconds);
    }

    /**
     * Returns a string representation of this GdlOnlineConfiguration instance.
     * The string includes the local repository directory, secret entries (keys only),
     * branch name, Git repository string, and push delay in milliseconds.
     * @return A string representation of this configuration object.
     */
    @Override
    public String toString() {
        return "GdlOnlineConfiguration{" +
                "localRepositoryDirectory=" + getLocalRepositoryDirectory() +
                ", secretNameSecretMap=" + createSecretEntriesKeyValueString() +
                ", branch='" + getBranch() + '\'' +
                ", gitRepositoryString='" + gitRepositoryString + '\'' +
                ", pushDelayMilliseconds=" + pushDelayMilliseconds +
                '}';
    }
}