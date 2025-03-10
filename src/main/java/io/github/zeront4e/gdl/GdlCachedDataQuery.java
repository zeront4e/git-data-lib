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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * Helper class to query and filter cached data objects.
 * This class provides methods to set filters, control data loading behavior,
 * and query cached data with optional error handling.
 * @param <Type> The type of data being cached and queried.
 */
public class GdlCachedDataQuery<Type> {
    /**
     * Defines a filter interface for data entries.
     * This interface provides a mechanism to filter unwanted data entries based on custom criteria.
     * @param <Type> The type of data being filtered.
     */
    public interface GdlDataFilter<Type> {
        /**
         * Determines whether a given data entry should be accepted or filtered out.
         * @param data The data entry to be evaluated.
         * @return Returns true if the data entry should be accepted, false if it should be filtered out.
         */
        boolean accept(Type data);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(GdlCachedDataQuery.class);

    private GdlDataFilter<GdlData<Type>> filter = null;

    private boolean loadFilteredDataFromDisk = false;

    private final DataManager dataManager;
    private final GdlDataRepository<Type> gdlDataRepository;
    private final Class<Type> dataType;

    GdlCachedDataQuery(DataManager dataManager, GdlDataRepository<Type> gdlDataRepository,
                       Class<Type> dataType) {
        this.dataManager = dataManager;
        this.gdlDataRepository = gdlDataRepository;
        this.dataType = dataType;
    }

    /**
     * Retrieves the current filter, set for this container.
     * @return The current filter, or null if no filter is set.
     */
    public GdlDataFilter<GdlData<Type>> getFilter() {
        return filter;
    }

    /**
     * Sets a new filter for this container.
     * @param filter The filter to be set.
     * @return This data container instance for method chaining.
     */
    public GdlCachedDataQuery<Type> setFilter(GdlDataFilter<GdlData<Type>> filter) {
        this.filter = filter;

        return this;
    }

    /**
     * Checks if filtered data should be loaded from disk.
     * @return true if filtered data should be loaded from disk, false otherwise.
     */
    public boolean isLoadFilteredDataFromDisk() {
        return loadFilteredDataFromDisk;
    }

    /**
     * Sets whether filtered data should be loaded from disk.
     * @param loadFilteredDataFromDisk Set to "true" to load filtered data from disk or "false" otherwise, if only the
     *                                 cached (incomplete) data should be returned.
     * @return This data container instance for method chaining.
     */
    public GdlCachedDataQuery<Type> setLoadFilteredDataFromDisk(boolean loadFilteredDataFromDisk) {
        this.loadFilteredDataFromDisk = loadFilteredDataFromDisk;

        return this;
    }

    /**
     * Queries the cached data objects without error handling. A warning message is logged if there is a
     * processing error.
     * @return A stream of data containers, containing the cached data or the full data representation, depending on
     * the configuration.
     */
    public Stream<GdlData<Type>> queryData() {
        return queryData(null);
    }

    /**
     * Queries the cached data objects with error handling. The callback is called if there is a
     * processing error.
     * @param errorCallback The callback to handle deserialization errors, or null for no error handling.
     * @return A stream of data containers, containing the cached data or the full data representation, depending on
     * the configuration.
     */
    public Stream<GdlData<Type>> queryData(GdlProcessingErrorCallback errorCallback) {
        Stream<GdlData<Type>> stream = gdlDataRepository.getCachedData(errorCallback);

        if(filter != null) {
            LOGGER.debug("Applying filter to stream of data container entries.");

            stream = stream.filter(filter::accept);
        }

        if(!loadFilteredDataFromDisk) {
            LOGGER.debug("Loading full data for all filtered data container entries.");

            return stream;
        }

        return stream.map(tmpGdlData -> {
            try {
                return dataManager.loadDataContainerFromDisk(tmpGdlData.getId(), dataType);
            }
            catch (Exception exception) {
                LOGGER.warn("Failed to load data from disk for entry {}.", tmpGdlData.getId(), exception);

                return null;
            }
        }).filter(Objects::nonNull);
    }
}
