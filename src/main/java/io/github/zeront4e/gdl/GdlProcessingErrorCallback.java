package io.github.zeront4e.gdl;

import java.io.File;

/**
 * Callback interface for processing errors during data object processing.
 */
public interface GdlProcessingErrorCallback {
    /**
     * Called when an error occurs during data object processing.
     * @param file The file that caused the error.
     * @param exception The exception that occurred.
     */
    void onProcessingError(File file, Exception exception);
}