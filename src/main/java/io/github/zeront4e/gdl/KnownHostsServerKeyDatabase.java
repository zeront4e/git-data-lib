package io.github.zeront4e.gdl;

import org.apache.sshd.client.config.hosts.KnownHostEntry;
import org.apache.sshd.client.config.hosts.KnownHostHashValue;
import org.apache.sshd.common.config.keys.KeyUtils;
import org.apache.sshd.common.config.keys.PublicKeyEntryResolver;
import org.eclipse.jgit.transport.CredentialsProvider;
import org.eclipse.jgit.transport.sshd.ServerKeyDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.security.PublicKey;
import java.util.*;

class KnownHostsServerKeyDatabase implements ServerKeyDatabase {
    private static final Logger LOGGER = LoggerFactory.getLogger(KnownHostsServerKeyDatabase.class);

    private final Map<String, List<PublicKey>> hostPortPublicKeysCacheMap =
            Collections.synchronizedMap(new HashMap<>());

    private final File knownHostsFile;

    KnownHostsServerKeyDatabase(File knownHostsFile) {
        this.knownHostsFile = knownHostsFile;
    }

    @Override
    public List<PublicKey> lookup(String connectAddress, InetSocketAddress remoteAddress, Configuration configuration) {
        List<PublicKey> publicKeyCandidates = getCachedPublicKeyCandidates(remoteAddress);

        LOGGER.debug("Found {} public key candidates for {}.", publicKeyCandidates.size(),
                connectAddress);

        return publicKeyCandidates;
    }

    @Override
    public boolean accept(String connectAddress, InetSocketAddress remoteAddress, PublicKey serverKey,
                          Configuration configuration, CredentialsProvider provider) {
        List<PublicKey> publicKeys = lookup(connectAddress, remoteAddress, configuration);

        for(PublicKey tmpPublicKey : publicKeys) {
            boolean publicKeyMatch = KeyUtils.compareKeys(serverKey, tmpPublicKey);

            if(publicKeyMatch) {
                LOGGER.debug("Public key match found for {}.", connectAddress);

                return true;
            }
        }

        LOGGER.debug("No public key match found for {}.", connectAddress);

        return false;
    }

    private List<PublicKey> getCachedPublicKeyCandidates(InetSocketAddress inetSocketAddress) {
        //Checks if public key candidates for the given remote address are already cached.

        if(hostPortPublicKeysCacheMap.isEmpty()) {
            try {
                if(!knownHostsFile.isFile())
                    throw new Exception("Unable to find known-hosts file.");

                LOGGER.debug("Load known-hosts file (path {}).", knownHostsFile.getAbsolutePath());

                List<PublicKey> keys = findMatchingPublicKeys(knownHostsFile, inetSocketAddress.getHostName(),
                        inetSocketAddress.getPort());

                if(keys.isEmpty())
                    keys = findMatchingPublicKeys(knownHostsFile, inetSocketAddress.getHostName(), null);

                if(!keys.isEmpty()) {
                    hostPortPublicKeysCacheMap.put(inetSocketAddress.getHostName().toLowerCase(Locale.ENGLISH), keys);

                    return keys;
                }
            }
            catch (Exception exception) {
                LOGGER.error("Failed to load known-hosts file (path {}).", knownHostsFile.getAbsolutePath(), exception);
            }
        }

        return hostPortPublicKeysCacheMap.getOrDefault(inetSocketAddress.getHostName().toLowerCase(Locale.ENGLISH),
                List.of());
    }

    private static List<PublicKey> findMatchingPublicKeys(File knownHostsFile, String hostName,
                                                          Integer optionalPort) throws Exception {
        //Finds all matching public keys in the known_hosts file for the given host name and optional port.

        List<PublicKey> keys = new ArrayList<>();

        try {
            List<String> lines = Files.readAllLines(knownHostsFile.toPath());

            for (String tmpLine : lines) {
                KnownHostEntry tmpEntry = KnownHostEntry.parseKnownHostEntry(tmpLine);

                if (tmpEntry != null) {
                    KnownHostHashValue hashValue = tmpEntry.getHashedEntry();

                    boolean parseEntry;

                    if(hashValue == null) {
                        Optional<?> optionalMatch = tmpEntry.getPatterns().stream().filter(tmpPatternEntry -> {
                            String matcherString = hostName;

                            if(optionalPort != null)
                                matcherString += ":" + optionalPort;

                            boolean patternMatch = tmpPatternEntry.getPattern().matcher(matcherString).matches();

                            if(patternMatch)
                                LOGGER.debug("Hash value is null. Pattern match: {} -> {}",
                                        tmpPatternEntry.getPattern(), matcherString);

                            return patternMatch;
                        }).findFirst();

                        parseEntry = optionalMatch.isPresent();
                    }
                    else {
                        int portToMatch = optionalPort == null ? 22 : optionalPort;

                        parseEntry = hashValue.isHostMatch(hostName, portToMatch);

                        if(parseEntry)
                            LOGGER.debug("Hash value is set. Host/port match. Host: {} Port: {}",
                                    hostName, portToMatch);
                    }

                    if(parseEntry) {
                        try {
                            LOGGER.debug("Try to parse known-host public key. Host: {} Port: {} Key type: {}", hostName,
                                    optionalPort, tmpEntry.getKeyEntry().getKeyType());

                            keys.add(tmpEntry.getKeyEntry().resolvePublicKey(null,
                                    PublicKeyEntryResolver.IGNORING));
                        }
                        catch (Exception exception) {
                            LOGGER.error("Failed to parse known-host public key.", exception);
                        }
                    }
                }
            }
        }
        catch (Exception exception) {
            throw new Exception("Failed to parse known-hosts file.", exception);
        }

        return keys;
    }
}
