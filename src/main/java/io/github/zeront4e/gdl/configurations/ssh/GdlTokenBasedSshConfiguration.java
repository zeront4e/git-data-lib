package io.github.zeront4e.gdl.configurations.ssh;

import java.io.File;

public class GdlTokenBasedSshConfiguration extends GdlPasswordBasedSshConfiguration {
    public GdlTokenBasedSshConfiguration(String token, File knownHostsFile) {
        super("", token, knownHostsFile);
    }

    @Override
    public String toString() {
        return "GdlTokenBasedSshConfiguration{" +
                "token='" + getPassword() + '\'' +
                ", knownHostsFile=" + getKnownHostsFile() +
                '}';
    }
}
