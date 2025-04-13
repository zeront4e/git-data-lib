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

import io.github.zeront4e.gdl.configurations.common.GdlOnlineConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Promotes changes in a data directory to the corresponding Git repository.
 */
class GitDataChangeInteractionManager implements DataManager.OnDataChangeCallback<GdlData<?>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitDataChangeInteractionManager.class);

    private final GdlGitConfiguration gdlGitConfiguration;

    private final AtomicBoolean changeOccurred;

    private final Runnable pushRunnable;

    private final boolean immediatePush;

    public GitDataChangeInteractionManager(GdlGitConfiguration gdlGitConfiguration) {
        this.gdlGitConfiguration = gdlGitConfiguration;

        changeOccurred = new AtomicBoolean(false);

        pushRunnable = createPushRunnable(gdlGitConfiguration);

        GdlOnlineConfiguration gdlOnlineConfiguration = gdlGitConfiguration.getGdlOnlineConfigurationOrNull();

        if(gdlOnlineConfiguration == null) {
            LOGGER.info("No online configuration provided, disable push operation.");

            immediatePush = false;
        }
        else {
            LOGGER.info("Online configuration provided, enable push operation.");

            if(gdlOnlineConfiguration.getPushDelayMilliseconds() > 0) {
                immediatePush = false;

                LOGGER.info("Starting push thread with a delay of {} milliseconds between pushes.",
                        gdlOnlineConfiguration.getPushDelayMilliseconds());

                Runnable pushThreadRunnable = createPushThreadRunnable(changeOccurred,
                        gdlOnlineConfiguration.getPushDelayMilliseconds(), pushRunnable);

                Thread gitPushThread = createPushThread(pushThreadRunnable);

                gitPushThread.start();
            }
            else {
                immediatePush = true;

                LOGGER.info("Push changes immediately after commit.");
            }
        }
    }

    @Override
    public void onDataAdded(File dataContainerFile, GdlData<?> gdlData) {
        try {
            String message = "chore: add data-object (type: " + gdlData.getData().getClass().getName() +
                    " id: " + gdlData.getId() + ")";

            commitChanges(gdlGitConfiguration, dataContainerFile, message, changeOccurred);
        }
        catch (Exception exception) {
            LOGGER.error("Unable to add data to Git repository.", exception);
        }
    }

    @Override
    public void onDataUpdated(File dataContainerFile, GdlData<?> gdlData) {
        try {
            String message = "chore: update data-object (type: " + gdlData.getData().getClass().getName() +
                    " id: " + gdlData.getId() + ")";

            commitChanges(gdlGitConfiguration, dataContainerFile, message, changeOccurred);
        }
        catch (Exception exception) {
            LOGGER.error("Unable to update data in Git repository.", exception);
        }
    }

    @Override
    public void onDataDeleted(File dataContainerFile, GdlData<?> gdlData) {
        try {
            String message = "chore: delete data-object (type: " + gdlData.getData().getClass().getName() +
                    " id: " + gdlData.getId() + ")";

            commitChanges(gdlGitConfiguration, dataContainerFile, message, changeOccurred);
        }
        catch (Exception exception) {
            LOGGER.error("Unable to delete data in Git repository.", exception);
        }
    }

    private Runnable createPushRunnable(GdlGitConfiguration gdlGitConfiguration) {
        return () -> {
            try {
                if(gdlGitConfiguration != null && !gdlGitConfiguration.isRemoteNotAvailable())
                    gdlGitConfiguration.createPushCommand().call();
            }
            catch (Exception exception) {
                LOGGER.error("An error occurred while trying to push Git changes.", exception);
            }
        };
    }

    private Runnable createPushThreadRunnable(AtomicBoolean changeOccurred, int pushDelayMilliseconds,
                                              Runnable pushRunnable) {
        return () -> {
            long lastPushTime = System.currentTimeMillis();

            try {
                while(!Thread.currentThread().isInterrupted()) {
                    if(System.currentTimeMillis() - lastPushTime >= pushDelayMilliseconds) {
                        if(changeOccurred.get()) {
                            LOGGER.debug("Delay was passed and change occurred. Try to push changes to Git " +
                                    "repository.");

                            changeOccurred.set(false);

                            lastPushTime = System.currentTimeMillis();

                            pushRunnable.run();
                        }
                    }

                    Thread.sleep(1000);
                }
            }
            catch (Exception exception) {
                LOGGER.error("An error occurred while running the Git push thread.", exception);
            }
        };
    }

    private Thread createPushThread(Runnable pushThreadRunnable) {
        Thread gitPushThread = new Thread(pushThreadRunnable, "Git push thread");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            LOGGER.info("Git push thread shutting down...");

            gitPushThread.interrupt();

            try {
                gitPushThread.join();
            }
            catch (InterruptedException exception) {
                LOGGER.error("An error occurred while waiting for the Git push thread to shut down.", exception);
            }

            LOGGER.info("Git push thread shutdown complete.");
        }));

        return gitPushThread;
    }

    private void commitChanges(GdlGitConfiguration gdlGitConfiguration, File dataContainerFile,
                               String message, AtomicBoolean changeOccurred) throws Exception {
        //Set relative file path to the data container file within the local repository directory.

        int repoDirectoryPathLength = gdlGitConfiguration.getGdlBaseConfiguration().getLocalRepositoryDirectory()
                .getAbsolutePath().length();

        String relativeFilePath = dataContainerFile.getAbsolutePath().substring(repoDirectoryPathLength + 1);

        relativeFilePath = relativeFilePath.replace(File.separator, "/");

        if(dataContainerFile.exists()) {
            //Execute add command.

            gdlGitConfiguration.createAddCommand()
                    .addFilepattern(relativeFilePath)
                    .call();
        }
        else {
            //Execute rm command.

            gdlGitConfiguration.createRmCommand()
                    .addFilepattern(relativeFilePath)
                    .setCached(true) //The file was already deleted. Just update the index.
                    .call();
        }

        //Execute commit command.

        gdlGitConfiguration.createCommitCommand()
                .setMessage(message)
                .call();

        changeOccurred.set(true);

        //Execute push runnable, if immediate push is configured.

        if(immediatePush) {
            LOGGER.debug("Perform immediate push.");

            pushRunnable.run();
        }
    }
}
