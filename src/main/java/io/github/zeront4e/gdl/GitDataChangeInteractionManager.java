package io.github.zeront4e.gdl;

import org.eclipse.jgit.api.AddCommand;
import org.eclipse.jgit.api.CommitCommand;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Promotes changes in a data directory to the corresponding Git repository.
 */
public class GitDataChangeInteractionManager implements DataManager.OnDataChangeCallback<GdlData<?>> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GitDataChangeInteractionManager.class);

    private final GdlGitConfiguration gdlGitConfiguration;

    private final AtomicBoolean changeOccurred;

    GitDataChangeInteractionManager(GdlGitConfiguration gdlGitConfiguration) {
        this.gdlGitConfiguration = gdlGitConfiguration;

        changeOccurred = new AtomicBoolean(false);

        Runnable pushRunnable = createPushRunnable(gdlGitConfiguration);

        if(gdlGitConfiguration.getBaseConfiguration().getPushDelayMilliseconds() > 0) {
            LOGGER.info("Starting push thread with a delay of {} milliseconds between pushes.",
                    gdlGitConfiguration.getBaseConfiguration().getPushDelayMilliseconds());

            Runnable pushThreadRunnable = createPushThreadRunnable(changeOccurred,
                    gdlGitConfiguration.getBaseConfiguration().getPushDelayMilliseconds(), pushRunnable);

            Thread gitPushThread = createPushThread(pushThreadRunnable);
            gitPushThread.start();
        }
        else {
            LOGGER.info("Push changes immediately after commit.");
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

    private Runnable createPushThreadRunnable(AtomicBoolean commitOccurred, int pushDelayMilliseconds,
                                              Runnable pushRunnable) {
        return () -> {
            long lastPushTime = System.currentTimeMillis();

            try {
                while(!Thread.currentThread().isInterrupted()) {
                    if(System.currentTimeMillis() - lastPushTime >= pushDelayMilliseconds) {
                        if(commitOccurred.get()) {
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
        AddCommand addCommand = gdlGitConfiguration.createAddCommand().addFilepattern(dataContainerFile.getName());
        addCommand.call();

        CommitCommand commitCommand = gdlGitConfiguration.createCommitCommand();
        commitCommand.setMessage(message).call();

        changeOccurred.set(true);
    }
}
