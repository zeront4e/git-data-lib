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

package io.github.zeront4e.gdl;

import io.github.zeront4e.gdl.configurations.common.GdlBaseConfiguration;
import io.github.zeront4e.gdl.configurations.common.GdlLocalConfiguration;
import io.github.zeront4e.gdl.configurations.common.GdlOnlineConfiguration;
import io.github.zeront4e.gdl.configurations.ssh.*;
import org.apache.sshd.common.config.keys.FilePasswordProvider;
import org.apache.sshd.common.util.security.SecurityUtils;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.transport.sshd.JGitKeyCache;
import org.eclipse.jgit.transport.sshd.SshdSessionFactory;
import org.eclipse.jgit.transport.sshd.SshdSessionFactoryBuilder;
import org.eclipse.jgit.util.FS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.util.*;

/**
 * Configuration to access a (protected) Git repository.
 */
public class GdlGitConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(GdlGitConfiguration.class);

    private final GdlBaseConfiguration gdlBaseConfiguration;

    private final Object gitAuthConfiguration;

    private Git cachedGitInstance = null;

    public GdlGitConfiguration(GdlLocalConfiguration gdlLocalConfiguration) {
        gdlBaseConfiguration = gdlLocalConfiguration;

        gitAuthConfiguration = null;
    }

    public GdlGitConfiguration(GdlOnlineConfiguration gdlOnlineConfiguration,
                               GdlKeyBasedSshConfiguration gdlKeyBasedSshConfiguration) throws Exception {
        gdlBaseConfiguration = gdlOnlineConfiguration;

        gitAuthConfiguration = gdlKeyBasedSshConfiguration;

        setupKeyBasedSshSessionFactory(gdlKeyBasedSshConfiguration.getOptionKeyDecryptionPassword());
    }

    public GdlGitConfiguration(GdlOnlineConfiguration gdlOnlineConfiguration,
                               GdlPasswordBasedSshConfiguration gdlPasswordBasedSshConfiguration) throws Exception {
        gdlBaseConfiguration = gdlOnlineConfiguration;

        gitAuthConfiguration = gdlPasswordBasedSshConfiguration;

        setupPasswordHintBasedSshSessionFactory();
    }

    public GdlGitConfiguration(GdlOnlineConfiguration gdlOnlineConfiguration,
                               GdlTokenBasedSshConfiguration gdlTokenBasedSshConfiguration) throws Exception {
        gdlBaseConfiguration = gdlOnlineConfiguration;

        gitAuthConfiguration = gdlTokenBasedSshConfiguration;

        setupPasswordHintBasedSshSessionFactory();
    }

    public GdlGitConfiguration(GdlOnlineConfiguration gdlOnlineConfiguration,
                               GdlPasswordBasedHttpConfiguration gdlPasswordBasedHttpConfiguration) {
        gdlBaseConfiguration = gdlOnlineConfiguration;

        gitAuthConfiguration = gdlPasswordBasedHttpConfiguration;
    }

    public GdlGitConfiguration(GdlOnlineConfiguration gdlOnlineConfiguration,
                               GdlTokenBasedHttpConfiguration gdlTokenBasedHttpConfiguration) {
        gdlBaseConfiguration = gdlOnlineConfiguration;

        gitAuthConfiguration = gdlTokenBasedHttpConfiguration;
    }

    boolean isRemoteNotAvailable() {
        if(gdlBaseConfiguration instanceof GdlLocalConfiguration)
            return true;

        boolean isGitRepositoryNotAvailable = true;

        if(gdlBaseConfiguration instanceof GdlOnlineConfiguration gdlOnlineConfiguration)
            isGitRepositoryNotAvailable = gdlOnlineConfiguration.getGitRepositoryString() == null;

        boolean isAuthenticationMissing = gitAuthConfiguration == null;

        return isGitRepositoryNotAvailable || isAuthenticationMissing;
    }

    GdlBaseConfiguration getGdlBaseConfiguration() {
        return gdlBaseConfiguration;
    }

    GdlLocalConfiguration getGdlLocalConfigurationOrNull() {
        if(gdlBaseConfiguration instanceof GdlLocalConfiguration gdlLocalConfiguration)
            return gdlLocalConfiguration;

        return null;
    }

    GdlOnlineConfiguration getGdlOnlineConfigurationOrNull() {
        if(gdlBaseConfiguration instanceof GdlOnlineConfiguration gdlOnlineConfiguration)
            return gdlOnlineConfiguration;

        return null;
    }

    AddCommand createAddCommand() throws GitAPIException, IOException {
        return getCachedGitInstance().add();
    }

    CommitCommand createCommitCommand() throws GitAPIException, IOException {
        CommitCommand commitCommand = getCachedGitInstance().commit();

        if(gitAuthConfiguration instanceof GdlPasswordBasedSshConfiguration gdlPasswordBasedSshConfiguration) {
            LOGGER.debug("Using password-based SSH authentication for commit-command.");

            UsernamePasswordCredentialsProvider credentialsProvider =
                    createPasswordCredentialsProvider(gdlPasswordBasedSshConfiguration.getUsername(),
                            gdlPasswordBasedSshConfiguration.getPassword());

            commitCommand.setCredentialsProvider(credentialsProvider);
        }
        else if(gitAuthConfiguration instanceof GdlPasswordBasedHttpConfiguration gdlPasswordBasedHttpConfiguration) {
            UsernamePasswordCredentialsProvider credentialsProvider;

            if(gitAuthConfiguration instanceof GdlGitHubTokenBasedHttpConfiguration) {
                LOGGER.debug("Using token-based GitHub HTTP authentication for commit-command.");

                credentialsProvider = createPasswordCredentialsProvider(gdlPasswordBasedHttpConfiguration.getPassword(),
                        gdlPasswordBasedHttpConfiguration.getUsername());
            }
            else {
                if(gitAuthConfiguration instanceof GdlTokenBasedHttpConfiguration) {
                    LOGGER.debug("Using token-based HTTP authentication for commit-command.");
                }
                else {
                    LOGGER.debug("Using password-based HTTP authentication for commit-command.");
                }

                credentialsProvider = createPasswordCredentialsProvider(gdlPasswordBasedHttpConfiguration.getUsername(),
                        gdlPasswordBasedHttpConfiguration.getPassword());
            }

            commitCommand.setCredentialsProvider(credentialsProvider);
        }
        else {
            LOGGER.debug("Using key-based SSH authentication for commit-command.");
        }

        return commitCommand;
    }

    PushCommand createPushCommand() throws GitAPIException, IOException {
        PushCommand pushCommand = getCachedGitInstance().push();

        if(gitAuthConfiguration instanceof GdlPasswordBasedSshConfiguration gdlPasswordBasedSshConfiguration) {
            LOGGER.debug("Using password-based SSH authentication for push-command.");

            UsernamePasswordCredentialsProvider credentialsProvider =
                    createPasswordCredentialsProvider(gdlPasswordBasedSshConfiguration.getUsername(),
                            gdlPasswordBasedSshConfiguration.getPassword());

            pushCommand.setCredentialsProvider(credentialsProvider);
        }
        else if(gitAuthConfiguration instanceof GdlPasswordBasedHttpConfiguration gdlPasswordBasedHttpConfiguration) {
            UsernamePasswordCredentialsProvider credentialsProvider;

            if(gitAuthConfiguration instanceof GdlGitHubTokenBasedHttpConfiguration) {
                LOGGER.debug("Using token-based GitHub HTTP authentication for push-command.");

                credentialsProvider = createPasswordCredentialsProvider(gdlPasswordBasedHttpConfiguration.getPassword(),
                        gdlPasswordBasedHttpConfiguration.getUsername());
            }
            else {
                if(gitAuthConfiguration instanceof GdlTokenBasedHttpConfiguration) {
                    LOGGER.debug("Using token-based HTTP authentication for push-command.");
                }
                else {
                    LOGGER.debug("Using password-based HTTP authentication for push-command.");
                }

                credentialsProvider = createPasswordCredentialsProvider(gdlPasswordBasedHttpConfiguration.getUsername(),
                        gdlPasswordBasedHttpConfiguration.getPassword());
            }

            pushCommand.setCredentialsProvider(credentialsProvider);
        }
        else {
            LOGGER.debug("Using key-based SSH authentication for push-command.");
        }

        return pushCommand;
    }

    void setupLocalRepositoryOrFail() throws GitAPIException, IOException {
        getCachedGitInstance();
    }

    private Git getCachedGitInstance() throws IOException, GitAPIException {
        if(cachedGitInstance == null) {
            LOGGER.info("Try to create cached repository instance.");

            if(isLocalRepositoryDirectoryAvailable()) {
                LOGGER.info("A local repository already exists. Open existing Git repository.");

                cachedGitInstance = openExistingRepositoryOrFail();
            }
            else {
                if(isRemoteNotAvailable()) {
                    LOGGER.info("No local repository was found and no remote is set. Create a new Git repository.");

                    cachedGitInstance = createLocalRepositoryOrFail();
                }
                else {
                    LOGGER.info("No local repository was found, but a remote is set. Clone the existing Git " +
                            "repository.");

                    cachedGitInstance = cloneRepositoryOrFail();
                }
            }
        }

        return cachedGitInstance;
    }

    private boolean isLocalRepositoryDirectoryAvailable() {
        File localRepositoryDirectory = getExistingLocalRepositoryDirectoryOrNull();

        if(localRepositoryDirectory == null)
            return false;

        return localRepositoryDirectory.isDirectory();
    }

    private Git openExistingRepositoryOrFail() throws IOException {
        //Open the repository from the given directory.

        File localRepositoryDirectory = getExistingLocalRepositoryDirectoryOrNull();

        if(localRepositoryDirectory == null)
            throw new IOException("The local repository directory does not exist.");

        LOGGER.info("Try to open local repository: {}", localRepositoryDirectory.getAbsolutePath());

        return Git.open(localRepositoryDirectory);
    }

    private Git createLocalRepositoryOrFail() throws IOException, GitAPIException {
        //Creates a local repository for the given directory.

        File localRepositoryDirectory = gdlBaseConfiguration.getLocalRepositoryDirectory();

        LOGGER.info("Try to create new local repository: {}", localRepositoryDirectory.getAbsolutePath());

        localRepositoryDirectory.mkdirs();

        if(!localRepositoryDirectory.isDirectory())
            throw new IOException("Unable to create the local repository directory.");

        return Git.init().setDirectory(localRepositoryDirectory).call();
    }

    private Git cloneRepositoryOrFail() throws GitAPIException {
        GdlOnlineConfiguration gdlOnlineConfiguration = getGdlOnlineConfigurationOrNull();

        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(gdlOnlineConfiguration.getGitRepositoryString())
                .setDirectory(gdlOnlineConfiguration.getLocalRepositoryDirectory())
                .setBranch(gdlOnlineConfiguration.getBranch());

        LOGGER.info("Try to clone repository: {}", gdlOnlineConfiguration.getGitRepositoryString());

        if(gitAuthConfiguration instanceof GdlPasswordBasedSshConfiguration gdlPasswordBasedSshConfiguration) {
            LOGGER.info("Using password based SSH authentication to clone the repository.");

            UsernamePasswordCredentialsProvider credentialsProvider =
                    createPasswordCredentialsProvider(gdlPasswordBasedSshConfiguration.getUsername(),
                            gdlPasswordBasedSshConfiguration.getPassword());

            cloneCommand.setCredentialsProvider(credentialsProvider);
        }
        else if(gitAuthConfiguration instanceof GdlPasswordBasedHttpConfiguration gdlPasswordBasedHttpConfiguration) {
            LOGGER.debug("Using password-based HTTP authentication for clone-command.");

            UsernamePasswordCredentialsProvider credentialsProvider;

            if(gitAuthConfiguration instanceof GdlGitHubTokenBasedHttpConfiguration) {
                LOGGER.info("Using token based GitHub HTTP authentication to clone the repository.");

                credentialsProvider = createPasswordCredentialsProvider(gdlPasswordBasedHttpConfiguration.getPassword(),
                        gdlPasswordBasedHttpConfiguration.getUsername());
            }
            else {
                LOGGER.info("Using password based HTTP authentication to clone the repository.");

                credentialsProvider = createPasswordCredentialsProvider(gdlPasswordBasedHttpConfiguration.getUsername(),
                        gdlPasswordBasedHttpConfiguration.getPassword());
            }

            cloneCommand.setCredentialsProvider(credentialsProvider);
        }
        else {
            LOGGER.debug("Using key-based SSH authentication for clone-command.");
        }

        return cloneCommand.call();
    }

    private File getExistingLocalRepositoryDirectoryOrNull() {
        if(!gdlBaseConfiguration.getLocalRepositoryDirectory().isDirectory())
            return null;

        File gitDirectory = new File(gdlBaseConfiguration.getLocalRepositoryDirectory(), ".git");

        if(!gitDirectory.isDirectory())
            return null;

        return gdlBaseConfiguration.getLocalRepositoryDirectory();
    }

    private void setupPasswordHintBasedSshSessionFactory() throws Exception {
        //Setup SshdSessionFactory with password authentication hint.

        SshdSessionFactoryBuilder sshdSessionFactoryBuilder = createSshSessionFactoryBuilder();

        sshdSessionFactoryBuilder.setPreferredAuthentications("password");

        //Set global SshdSessionFactory instance.

        setupGlobalSshdSessionFactory(sshdSessionFactoryBuilder);
    }

    private void setupKeyBasedSshSessionFactory(String optionalPrivateKeyPassword) throws Exception {
        //Load private key from file.

        File privateKeyFile = getPrivateKeyFileOrNull();

        if(privateKeyFile == null)
            throw new Exception("No private key file found.");

        LOGGER.info("Try to use private key file: {}", privateKeyFile.getAbsolutePath());

        FilePasswordProvider filePasswordProvider = null;

        if(optionalPrivateKeyPassword != null)
            filePasswordProvider = (session, resourceKey, retryIndex) -> optionalPrivateKeyPassword;

        byte[] keyByteArray = Files.readAllBytes(privateKeyFile.toPath());

        Iterable<KeyPair> keyPairs = SecurityUtils.loadKeyPairIdentities(null, null,
                new ByteArrayInputStream(keyByteArray), filePasswordProvider);

        //Setup SshdSessionFactory with key pairs.

        SshdSessionFactoryBuilder sshdSessionFactoryBuilder = createSshSessionFactoryBuilder();

        sshdSessionFactoryBuilder.setPreferredAuthentications("publickey")
                .setDefaultKeysProvider(directory -> keyPairs);

        //Set global SshdSessionFactory instance.

        setupGlobalSshdSessionFactory(sshdSessionFactoryBuilder);
    }

    private SshdSessionFactoryBuilder createSshSessionFactoryBuilder() throws Exception {
        //Create and set a temporary directory (this is a mandatory requirement and a dummy).

        Path temporaryDirectory;

        try {
            temporaryDirectory = Files.createTempDirectory("ssh-temp-dir");

            temporaryDirectory.toFile().deleteOnExit();
        }
        catch (IOException ioException) {
            throw new Exception("Failed to create temporary directory.", ioException);
        }

        SshdSessionFactoryBuilder sshSessionFactoryBuilder = new SshdSessionFactoryBuilder();

        sshSessionFactoryBuilder.setHomeDirectory(temporaryDirectory.toFile())
                .setSshDirectory(temporaryDirectory.toFile());

        //Parse and set the given known hosts file.

        File knownHostsFile = getKnownHostsFileOrNull();

        if(knownHostsFile != null) {
            KnownHostsServerKeyDatabase knownHostsServerKeyDatabase = new KnownHostsServerKeyDatabase(knownHostsFile);

            LOGGER.debug("Try to use known-hosts file: {}", knownHostsFile.getAbsolutePath());

            sshSessionFactoryBuilder.setDefaultKnownHostsFiles(file -> List.of(knownHostsFile.toPath()));

            sshSessionFactoryBuilder.setServerKeyDatabase((file, file2) -> knownHostsServerKeyDatabase);
        }

        return sshSessionFactoryBuilder;
    }

    private void setupGlobalSshdSessionFactory(SshdSessionFactoryBuilder sshdSessionFactoryBuilder) {
        //Set the global SshSessionFactory instance.

        SshdSessionFactory sshSessionFactory = sshdSessionFactoryBuilder.build(new JGitKeyCache());

        SshSessionFactory.setInstance(sshSessionFactory);
    }

    private UsernamePasswordCredentialsProvider createPasswordCredentialsProvider(String username, String password) {
        //Create a UsernamePasswordCredentialsProvider with the given username and password.

        return new UsernamePasswordCredentialsProvider(username, password);
    }

    private File getKnownHostsFileOrNull() {
        //Returns the default known-hosts file if it exists, or null otherwise.

        if(gitAuthConfiguration instanceof GdlKeyBasedSshConfiguration gdlKeyBasedSshConfiguration) {
            LOGGER.debug("Try to load known-hosts file from key-based SSH configuration.");

            return gdlKeyBasedSshConfiguration.getKnownHostsFile();
        }

        if(gitAuthConfiguration instanceof GdlPasswordBasedSshConfiguration gdlPasswordBasedSshConfiguration) {
            LOGGER.debug("Try to load known-hosts file from password-based SSH configuration.");

            return gdlPasswordBasedSshConfiguration.getKnownHostsFile();
        }

        LOGGER.debug("Try to load known-hosts file from default location.");

        File defaultKnownHostsFile = new File(FS.DETECTED.userHome(), ".ssh/known_hosts");

        if(!defaultKnownHostsFile.isFile()) {
            LOGGER.debug("Unable to find known-hosts file. Path: {}", defaultKnownHostsFile.getAbsolutePath());

            return null;
        }

        return defaultKnownHostsFile;
    }

    private File getPrivateKeyFileOrNull() {
        //Returns a default private key file if it exists, or null otherwise.

        if(gitAuthConfiguration instanceof GdlKeyBasedSshConfiguration gdlKeyBasedSshConfiguration) {
            LOGGER.debug("Try to load private key file from key-based SSH configuration.");

            return gdlKeyBasedSshConfiguration.getPrivateKeyFile();
        }

        LOGGER.debug("Try to load private key file from default location.");

        File userHomeFile = FS.DETECTED.userHome();

        if(userHomeFile.isDirectory()) {
            File[] files = userHomeFile.listFiles((dir, name) -> {
                String loweredFileName = name.toLowerCase(Locale.ENGLISH);

                return (loweredFileName.startsWith("id_") || loweredFileName.contains("_id_")) &&
                        !loweredFileName.endsWith(".pub");
            });

            if(files != null && files.length > 0) {
                File firstMatchingPrivateKeyFile = files[0];

                LOGGER.debug("Return first found private key file: {}", firstMatchingPrivateKeyFile.getAbsolutePath());

                return firstMatchingPrivateKeyFile;
            }
        }

        return null;
    }
}
