package io.github.zeront4e.gdl;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class GdlBaseConfiguration {
    private final File localRepositoryDirectory;
    private final Map<String, String> secretNameSecretMap;
    private final String branch;
    private final String gitRepositoryUrl;
    private final int pushDelayMilliseconds;

    public GdlBaseConfiguration(File localRepositoryDirectory, Map<String, String> secretNameSecretMap) {
        this(localRepositoryDirectory, secretNameSecretMap, "main", null, 0);
    }

    public GdlBaseConfiguration(File localRepositoryDirectory, Map<String, String> secretNameSecretMap,
                                String branch, String gitRepositoryUrl, int pushDelayMilliseconds) {
        this.localRepositoryDirectory = localRepositoryDirectory;
        this.secretNameSecretMap = secretNameSecretMap;
        this.branch = branch;
        this.gitRepositoryUrl = gitRepositoryUrl;
        this.pushDelayMilliseconds = pushDelayMilliseconds;
    }

    public File getLocalRepositoryDirectory() {
        return localRepositoryDirectory;
    }

    public Map<String, String> getSecretNameSecretMap() {
        return secretNameSecretMap;
    }

    public String getBranch() {
        return branch;
    }

    public String getGitRepositoryUrl() {
        return gitRepositoryUrl;
    }

    public int getPushDelayMilliseconds() {
        return pushDelayMilliseconds;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) return true;
        if (object == null || getClass() != object.getClass()) return false;
        GdlBaseConfiguration that = (GdlBaseConfiguration) object;
        return pushDelayMilliseconds == that.pushDelayMilliseconds &&
                Objects.equals(localRepositoryDirectory, that.localRepositoryDirectory) &&
                Objects.equals(secretNameSecretMap, that.secretNameSecretMap) && Objects.equals(branch, that.branch) &&
                Objects.equals(gitRepositoryUrl, that.gitRepositoryUrl);
    }

    @Override
    public int hashCode() {
        return Objects.hash(localRepositoryDirectory, secretNameSecretMap, branch, gitRepositoryUrl,
                pushDelayMilliseconds);
    }

    @Override
    public String toString() {
        return "GdlBaseConfiguration{" +
                "localRepositoryDirectory=" + localRepositoryDirectory +
                ", secretNameSecretMap=" + secretNameSecretMap +
                ", branch='" + branch + '\'' +
                ", gitRepositoryUrl='" + gitRepositoryUrl + '\'' +
                ", pushDelayMilliseconds=" + pushDelayMilliseconds +
                '}';
    }
}