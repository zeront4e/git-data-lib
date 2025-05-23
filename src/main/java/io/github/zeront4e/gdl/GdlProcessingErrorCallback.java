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