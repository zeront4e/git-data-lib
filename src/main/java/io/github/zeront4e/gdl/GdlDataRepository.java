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

import io.github.zeront4e.gdl.annotations.GdlCachedProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

/**
 * Class representing a repository-directory to manage data objects.
 * @param <Type> The type of data objects to manage.
 */
public class GdlDataRepository<Type> {
    private static final Logger LOGGER = LoggerFactory.getLogger(GdlDataRepository.class);

    /**
     * Variable to indicate whether the internal data cache of this repository has been filled initially.
     */
    private final AtomicBoolean dataCacheWasFilled = new AtomicBoolean(false);

    /**
     * Map to store the unique identifiers (UIDs) of data objects and their corresponding GdlData objects.
     * This map is used for efficient querying and filtering of cached data objects.
     * The map is synchronized to ensure thread safety.
     */
    private final Map<String, GdlData<Type>> uidCachedGdlDataContainerMap =
            Collections.synchronizedMap(new HashMap<>());

    private final DataManager dataManager;
    private final Class<Type> dataType;

    GdlDataRepository(DataManager dataManager, Class<Type> dataType) {
        this.dataManager = dataManager;
        this.dataType = dataType;
    }

    Map<String, GdlData<Type>> getUidCachedGdlDataContainerMap() {
        return Collections.unmodifiableMap(uidCachedGdlDataContainerMap);
    }

    /**
     * Resets the internal data cache of this repository.
     * This method clears all cached data objects from memory and resets the cache status flag.
     * After calling this method, the next call to {@link #queryCachedData()} or
     * {@link #getCachedData(GdlProcessingErrorCallback)} will reload all data from disk.
     */
    public void resetDataCache() {
        uidCachedGdlDataContainerMap.clear();
        dataCacheWasFilled.set(false);
    }
    
    /**
     * Returns a container for cached data to query and filter all cached data objects.
     * This method initially reads all data object files from the repository
     * directory, deserializes them into data objects, and stores them in memory.
     * @return The data container to query and filter the cached data.
     * */
    public synchronized GdlCachedDataQuery<Type> queryCachedData() {
        return new GdlCachedDataQuery<>(dataManager, this, dataType);
    }

    /**
     * Queries all data objects from the repository directory.
     * This method reads all data object files from the repository directory,
     * deserializes them into data objects, and returns them as a data container stream.
     * @return A stream of data container objects.
     *         Returns an empty stream if no data exists.
     */
    public synchronized Stream<GdlData<Type>> queryData() {
        return dataManager.queryData(dataType);
    }

    /**
     * Queries all data objects from the repository directory.
     * This method reads all data object files from the repository directory,
     * deserializes them into data objects, and returns them as a stream.
     * @param errorCallback A callback to handle deserialization errors. Can be null if no error handling is needed.
     * @return A stream of data container objects.
     *         Returns an empty stream if the data repository directory doesn't exist.
     */
    public Stream<GdlData<Type>> queryData(GdlProcessingErrorCallback errorCallback) {
        return internalQueryData(errorCallback, null);
    }

    /**
     * Adds the given data object to the repository directory and saves it to the disk.
     * This method is synchronized to ensure thread-safety when adding data.
     * @param data The data object to add to the repository.
     * @return A new data container instance containing the added data, along with its generated ID and timestamps.
     * @throws IOException An unexpected I/O error.
     */
    public synchronized GdlData<Type> addData(Type data) throws Exception {
        //Add data.

        GdlData<Type> newGdlData = dataManager.addData(data);

        //Clone data-object, if a cached field is present.

        Optional<?> cachedDataOptional = createCachedDataOptional(data.getClass());

        if(cachedDataOptional.isPresent()) {
            LOGGER.debug("Found cached field in data-object. Clone data-object.");

            Type clonedData = ObjectCloneUtil.deepCloneObject(data);

            GdlData<Type> clonedGdlData = new GdlData<>(newGdlData.getId(), newGdlData.getCreateTimestamp(),
                    newGdlData.getUpdateTimestamp(), clonedData);

            addCachedDataEntry(clonedGdlData);
        }

        //Return the created data container.

        return newGdlData;
    }

    /**
     * Updates the given data object on disk.
     * This method is synchronized to ensure thread-safe access to the file system.
     * @param gdlData The data container object to update.
     * @throws IOException An unexpected I/O error.
     */
    public synchronized void updateData(GdlData<Type> gdlData) throws Exception {
        //Update stored data and container timestamp.

        dataManager.updateData(gdlData);

        //Try to update an existing data-container, if present.

        updateCachedDataEntryIfPresent(gdlData);
    }

    /**
     * Deletes the specified data object from disk.
     * This method is synchronized to ensure thread-safe access to the file system.
     * @param gdlData The data container object to delete.
     * @return boolean Returns true if the data was successfully deleted, false otherwise.
     *                 A false return value might indicate that the data was not found
     *                 or could not be deleted.
     */
    public synchronized boolean deleteData(GdlData<Type> gdlData) {
        uidCachedGdlDataContainerMap.remove(gdlData.getId());

        return dataManager.deleteData(gdlData);
    }

    synchronized Stream<GdlData<Type>> getCachedData(GdlProcessingErrorCallback errorCallback) {
        if(!dataCacheWasFilled.get()) {
            dataCacheWasFilled.set(true);
            
            LOGGER.debug("Try to fill data cache without already cached data-object containers.");

            Set<String> cachedUids = uidCachedGdlDataContainerMap.keySet();

            DataManager.DataSkipCallback dataSkipCallback = file -> {
                String fileNameUid = file.getName().replace(".yml", "");

                return cachedUids.contains(fileNameUid);
            };

            internalQueryData(errorCallback, dataSkipCallback).forEach(this::addCachedDataEntry);
        }

        return uidCachedGdlDataContainerMap.values().stream();
    }

    synchronized void updateCachedDataEntryIfPresent(GdlData<Type> gdlData) {
        GdlData<Type> cachedGdlData = uidCachedGdlDataContainerMap.get(gdlData.getId());

        if(cachedGdlData != null) {
            //Update container properties.

            cachedGdlData.setCreateTimestamp(gdlData.getCreateTimestamp());
            cachedGdlData.setUpdateTimestamp(gdlData.getUpdateTimestamp());

            //Update cached fields.

            for(Field tmpField : cachedGdlData.getData().getClass().getDeclaredFields()) {
                boolean isPrimitive = tmpField.getType().isPrimitive();

                boolean cachedProperty = tmpField.getAnnotation(GdlCachedProperty.class) != null;

                if(isPrimitive || cachedProperty) {
                    //We update the value of the field, because it is primitive or a cached property.

                    try {
                        Field foreignField = null;

                        try {
                            foreignField = gdlData.getData().getClass().getDeclaredField(tmpField.getName());
                        }
                        catch (Exception exception) {
                            //Ignore...
                        }

                        if(foreignField != null) {
                            boolean isModifierPrivate = Modifier.isPrivate(tmpField.getModifiers()) ||
                                    Modifier.isPrivate(foreignField.getModifiers());

                            if(isModifierPrivate) {
                                tmpField.setAccessible(true);
                                foreignField.setAccessible(true);
                            }

                            Object valueToUpdate = foreignField.get(gdlData.getData());

                            tmpField.set(cachedGdlData.getData(), valueToUpdate);

                            if(isModifierPrivate) {
                                tmpField.setAccessible(false);
                                foreignField.setAccessible(false);
                            }
                        }
                        else {
                            LOGGER.warn("Unable to find field {} from cached data-object in class of updated " +
                                    "data-object {}.", tmpField.getName(), gdlData.getData().getClass().getName());
                        }
                    }
                    catch (Exception exception) {
                        LOGGER.warn("Unable to update value of field {} in class {}.", tmpField.getName(),
                                gdlData.getData().getClass().getName(), exception);
                    }
                }
            }
        }
    }

    private void addCachedDataEntry(GdlData<Type> gdlData) {
        for(Field tmpField : gdlData.getData().getClass().getDeclaredFields()) {
            boolean isPrimitive = tmpField.getType().isPrimitive();

            if(!isPrimitive) {
                boolean cachedProperty = tmpField.getAnnotation(GdlCachedProperty.class) != null;

                if(!cachedProperty) {
                    //We delete the value of the field, because it is not a cached property.

                    try {
                        int modifiers = tmpField.getModifiers();

                        boolean isModifierPrivate = Modifier.isPrivate(modifiers);

                        if(isModifierPrivate)
                            tmpField.setAccessible(true);

                        tmpField.set(gdlData.getData(), null);

                        if(isModifierPrivate)
                            tmpField.setAccessible(false);
                    }
                    catch (Exception exception) {
                        LOGGER.warn("Unable to delete value of field {} in class {}.", tmpField.getName(),
                                gdlData.getData().getClass().getName(), exception);
                    }
                }
            }
        }

        uidCachedGdlDataContainerMap.put(gdlData.getId(), gdlData);
    }

    private Optional<?> createCachedDataOptional(Class<?> dataTypeClass) {
        return Arrays.stream(dataTypeClass.getDeclaredFields())
                .filter(tmpField -> tmpField.isAnnotationPresent(GdlCachedProperty.class))
                .findFirst();
    }

    private synchronized Stream<GdlData<Type>> internalQueryData(GdlProcessingErrorCallback errorCallback,
                                                                 DataManager.DataSkipCallback dataSkipCallback) {
        return dataManager.queryData(dataType, errorCallback, dataSkipCallback);
    }
}
