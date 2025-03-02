package io.github.zeront4e.gdl;

import io.github.zeront4e.gdl.configurations.ssh.GdlKeyBasedSshConfiguration;
import io.github.zeront4e.gdl.configurations.ssh.GdlPasswordBasedSshConfiguration;
import io.github.zeront4e.gdl.configurations.ssh.GdlTokenBasedSshConfiguration;
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

    private final GdlKeyBasedSshConfiguration gdlKeyBasedSshConfiguration;
    private final GdlPasswordBasedSshConfiguration gdlPasswordBasedSshConfiguration;

    private Git cachedGitInstance = null;

    public GdlGitConfiguration(GdlBaseConfiguration gdlBaseConfiguration) {
        this.gdlBaseConfiguration = gdlBaseConfiguration;

        this.gdlKeyBasedSshConfiguration = null;
        this.gdlPasswordBasedSshConfiguration = null;
    }

    public GdlGitConfiguration(GdlBaseConfiguration gdlBaseConfiguration,
                               GdlKeyBasedSshConfiguration gdlKeyBasedSshConfiguration) throws Exception {
        this.gdlBaseConfiguration = gdlBaseConfiguration;

        this.gdlKeyBasedSshConfiguration = gdlKeyBasedSshConfiguration;
        this.gdlPasswordBasedSshConfiguration = null;

        setupKeyBasedSshSessionFactory(gdlKeyBasedSshConfiguration.getOptionKeyDecryptionPassword());
    }

    public GdlGitConfiguration(GdlBaseConfiguration gdlBaseConfiguration,
                               GdlPasswordBasedSshConfiguration gdlPasswordBasedSshConfiguration) throws Exception {
        this.gdlBaseConfiguration = gdlBaseConfiguration;

        this.gdlKeyBasedSshConfiguration = null;
        this.gdlPasswordBasedSshConfiguration = gdlPasswordBasedSshConfiguration;

        setupPasswordHintBasedSshSessionFactory();
    }

    public GdlGitConfiguration(GdlBaseConfiguration gdlBaseConfiguration,
                               GdlTokenBasedSshConfiguration gdlTokenBasedSshConfiguration) throws Exception {
        this(gdlBaseConfiguration, (GdlPasswordBasedSshConfiguration) gdlTokenBasedSshConfiguration);
    }

    boolean isRemoteNotAvailable() {
        return getBaseConfiguration().getGitRepositoryUrl() == null ||
                (gdlKeyBasedSshConfiguration == null && gdlPasswordBasedSshConfiguration == null);
    }

    GdlBaseConfiguration getBaseConfiguration() {
        return gdlBaseConfiguration;
    }

    AddCommand createAddCommand() throws GitAPIException, IOException {
        return getCachedGitInstance().add();
    }

    CommitCommand createCommitCommand() throws GitAPIException, IOException {
        CommitCommand commitCommand = getCachedGitInstance().commit();

        if(gdlPasswordBasedSshConfiguration != null) {
            LOGGER.debug("Using password-based SSH authentication for commit-command.");

            UsernamePasswordCredentialsProvider credentialsProvider =
                    createUsernamePasswordCredentialsProvider(gdlPasswordBasedSshConfiguration.getUsername(),
                            gdlPasswordBasedSshConfiguration.getPassword());

            commitCommand.setCredentialsProvider(credentialsProvider);
        }
        else {
            LOGGER.debug("Using key-based SSH authentication for commit-command.");
        }

        return commitCommand;
    }

    PushCommand createPushCommand() throws GitAPIException, IOException {
        PushCommand pushCommand = getCachedGitInstance().push();

        if(gdlPasswordBasedSshConfiguration != null) {
            LOGGER.debug("Using password-based SSH authentication for push-command.");

            UsernamePasswordCredentialsProvider credentialsProvider =
                    createUsernamePasswordCredentialsProvider(gdlPasswordBasedSshConfiguration.getUsername(),
                            gdlPasswordBasedSshConfiguration.getPassword());

            pushCommand.setCredentialsProvider(credentialsProvider);
        }
        else {
            LOGGER.debug("Using key-based SSH authentication for push-command.");
        }

        return pushCommand;
    }

    private Git getCachedGitInstance() throws IOException, GitAPIException {
        if(cachedGitInstance == null) {
            if(isLocalRepositoryDirectoryAvailable()) {
                cachedGitInstance = openExistingRepositoryOrFail();
            }
            else {
                if(isRemoteNotAvailable()) {
                    cachedGitInstance = createLocalRepositoryOrFail();
                }
                else {
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
        CloneCommand cloneCommand = Git.cloneRepository()
                .setURI(gdlBaseConfiguration.getGitRepositoryUrl())
                .setDirectory(gdlBaseConfiguration.getLocalRepositoryDirectory())
                .setBranch(gdlBaseConfiguration.getBranch());

        LOGGER.info("Try to clone repository: {}", gdlBaseConfiguration.getGitRepositoryUrl());

        if(gdlPasswordBasedSshConfiguration != null) {
            LOGGER.debug("Using password-based SSH authentication.");

            UsernamePasswordCredentialsProvider credentialsProvider =
                    createUsernamePasswordCredentialsProvider(gdlPasswordBasedSshConfiguration.getUsername(),
                            gdlPasswordBasedSshConfiguration.getPassword());

            cloneCommand.setCredentialsProvider(credentialsProvider);
        }
        else {
            LOGGER.debug("Using key-based SSH authentication.");
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

        LOGGER.debug("Found private key file: {}", privateKeyFile.getAbsolutePath());

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
        }
        catch (IOException ioException) {
            throw new Exception("Failed to create temporary directory.", ioException);
        }

        SshdSessionFactoryBuilder sshSessionFactoryBuilder = new SshdSessionFactoryBuilder();

        sshSessionFactoryBuilder.setHomeDirectory(temporaryDirectory.toFile())
                .setSshDirectory(temporaryDirectory.toFile());

        //Parse and set the given known hosts file.

        File knownHostsFile = getKnownHostsFileOrNull();

        sshSessionFactoryBuilder.setServerKeyDatabase((ignoredHomeDirectory, ignoredSshDirectory) ->
                new KnownHostsServerKeyDatabase(knownHostsFile));

        return sshSessionFactoryBuilder;
    }

    private void setupGlobalSshdSessionFactory(SshdSessionFactoryBuilder sshdSessionFactoryBuilder) {
        //Set the global SshSessionFactory instance.

        SshdSessionFactory sshSessionFactory = sshdSessionFactoryBuilder.build(new JGitKeyCache());

        SshSessionFactory.setInstance(sshSessionFactory);
    }

    private UsernamePasswordCredentialsProvider createUsernamePasswordCredentialsProvider(String username,
                                                                                          String password) {
        //Create a UsernamePasswordCredentialsProvider with the given username and password.

        return new UsernamePasswordCredentialsProvider(username, password);
    }

    private File getKnownHostsFileOrNull() {
        //Returns the default known-hosts file if it exists, or null otherwise.

        if(gdlKeyBasedSshConfiguration != null) {
            LOGGER.debug("Try to load known-hosts file from key-based SSH configuration.");

            return gdlKeyBasedSshConfiguration.getKnownHostsFile();
        }

        if(gdlPasswordBasedSshConfiguration != null) {
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

        if(gdlKeyBasedSshConfiguration != null) {
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
