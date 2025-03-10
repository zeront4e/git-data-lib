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
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

public class GdlDataRepositoryTest {
    private static class TestDataObject {
        private String value;

        public TestDataObject() {}

        public TestDataObject(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }
    
    private static class DataWithCachedProps {
        private String standardField;

        @GdlCachedProperty
        private String cachedField;

        public DataWithCachedProps() {}

        public DataWithCachedProps(String standardField, String cachedField) {
            this.standardField = standardField;
            this.cachedField = cachedField;
        }

        public String getStandardField() {
            return standardField;
        }

        public void setStandardField(String standardField) {
            this.standardField = standardField;
        }

        public String getCachedField() {
            return cachedField;
        }

        public void setCachedField(String cachedField) {
            this.cachedField = cachedField;
        }
    }

    @Test
    public void shouldQueryDataFromEmptyRepository() throws IOException {
        //Arrange.

        File tempDirectory = Files.createTempDirectory("test-repo").toFile();

        DataManager dataManager = new DataManager(Map.of(), tempDirectory);

        //Test.

        GdlDataRepository<TestDataObject> repository = new GdlDataRepository<>(dataManager, TestDataObject.class);
        Stream<GdlData<TestDataObject>> result = repository.queryData();

        //Assert.

        assertNotNull(result);

        assertEquals(0, result.count(), "No data should be found in an empty repository.");
    }

    @Test
    public void shouldAddData() throws Exception {
        //Arrange.

        File tempDirectory = Files.createTempDirectory("test-repo").toFile();
        DataManager dataManager = new DataManager(Map.of(), tempDirectory);
        GdlDataRepository<TestDataObject> repository = new GdlDataRepository<>(dataManager, TestDataObject.class);
        TestDataObject testData = new TestDataObject("Test value");

        //Test.

        GdlData<TestDataObject> result = repository.addData(testData);

        //Assert.

        assertNotNull(result, "addData should return a non-null GdlData container.");

        assertNotNull(result.getId(), "GdlData container should have a non-null ID.");

        assertFalse(result.getId().isEmpty(), "GdlData container should have a non-empty ID.");

        assertEquals("Test value", result.getData().getValue(),
                "GdlData container should contain the original data.");

        //Verify data is in cached map by querying.

        long count = repository.queryCachedData().queryData().count();

        assertEquals(1, count, "Repository should contain 1 data entry after adding.");
    }

    @Test
    public void shouldHandleErrorsInQueryData() throws IOException {
        //Arrange.

        File tempDirectory = Files.createTempDirectory("test-repo").toFile();
        DataManager dataManager = new DataManager(Map.of(), tempDirectory);
        GdlDataRepository<TestDataObject> repository = new GdlDataRepository<>(dataManager, TestDataObject.class);

        //Create directory for TestData class.

        File dataDir = new File(tempDirectory, DataManager.DATA_DIRECTORY + "/" +
                TestDataObject.class.getSimpleName());

        dataDir.mkdirs();

        assertTrue(dataDir.exists());
        assertTrue(dataDir.isDirectory());

        //Create a test file with invalid content to force a deserialization error.

        File invalidFile = new File(dataDir, "invalid-data.yaml");

        Files.writeString(invalidFile.toPath(), "invalid: yaml: content: that: will: cause: error");

        AtomicBoolean errorCallbackCalled = new AtomicBoolean(false);

        GdlProcessingErrorCallback errorCallback = (file, exception) -> {
            errorCallbackCalled.set(true);

            assertEquals(invalidFile, file);
            assertNotNull(exception);
        };

        //Test.

        Stream<GdlData<TestDataObject>> result = repository.queryData(errorCallback);

        //Assert.

        assertNotNull(result);
        assertEquals(0, result.count(), "Should return empty stream when all files have errors");
        assertTrue(errorCallbackCalled.get(), "Error callback should have been called");
    }

    @Test
    public void shouldUpdateData() throws Exception {
        //Arrange.

        File tempDirectory = Files.createTempDirectory("test-repo").toFile();
        DataManager dataManager = new DataManager(Map.of(), tempDirectory);
        GdlDataRepository<TestDataObject> repository = new GdlDataRepository<>(dataManager, TestDataObject.class);

        //Create a test data object and add it to repository.

        TestDataObject testObject = new TestDataObject("Original value");
        GdlData<TestDataObject> addedData = repository.addData(testObject);

        //Verify the original name before update.

        Optional<GdlData<TestDataObject>> initialData = repository.queryData()
                .filter(tmpData -> tmpData.getId().equals(addedData.getId()))
                .findFirst();

        assertTrue(initialData.isPresent(), "The initial data should be present.");

        assertEquals("Original value", initialData.get().getData().getValue(),
                "Initial data should have the original name.");

        //Update the test data object.

        addedData.getData().setValue("Updated value");
        repository.updateData(addedData);

        //Assert.

        Optional<GdlData<TestDataObject>> updatedData = repository.queryData()
                .filter(tmpData -> tmpData.getId().equals(addedData.getId()))
                .findFirst();

        assertTrue(updatedData.isPresent(), "The updated data should be present.");

        assertEquals("Updated value", updatedData.get().getData().getValue(),
                "The queried data should have an updated name.");
    }

    @Test
    public void shouldDeleteData() throws Exception {
        //Arrange.

        File tempDirectory = Files.createTempDirectory("test-repo").toFile();
        DataManager dataManager = new DataManager(Map.of(), tempDirectory);
        GdlDataRepository<TestDataObject> repository = new GdlDataRepository<>(dataManager, TestDataObject.class);

        //Create and add a test data object to the repository.

        TestDataObject testObject = new TestDataObject("Test Object");
        GdlData<TestDataObject> addedData = repository.addData(testObject);

        //Verify the data exists before deletion.

        Stream<GdlData<TestDataObject>> dataBeforeDeletion = repository.queryCachedData().queryData();

        assertEquals(1, dataBeforeDeletion.count(),
                "Repository should contain 1 data entry before deletion.");

        //Test.

        boolean result = repository.deleteData(addedData);

        //Assert.

        assertTrue(result, "deleteData should return true when successful.");

        //Verify the data is no longer in the repository.

        Stream<GdlData<TestDataObject>> dataAfterDeletion = repository.queryCachedData().queryData();

        assertEquals(0, dataAfterDeletion.count(),
                "Repository should contain 0 data entries after deletion.");

        //Verify the file is physically deleted from disk.

        File dataDir = new File(tempDirectory, DataManager.DATA_DIRECTORY + "/" +
                TestDataObject.class.getSimpleName());

        File[] files = dataDir.listFiles();
        
        assertTrue(files == null || files.length == 0,
                "No files should exist in the data directory after deletion.");
    }

    @Test
    public void shouldRefuseDeletionOfNonExistentData() throws IOException {
        //Arrange.
        
        File tempDirectory = Files.createTempDirectory("test-repo").toFile();
        DataManager dataManager = new DataManager(Map.of(), tempDirectory);
        GdlDataRepository<TestDataObject> repository = new GdlDataRepository<>(dataManager, TestDataObject.class);

        //Create a data container with a non-existent ID.
        
        TestDataObject testObject = new TestDataObject("Non-existent object");
        
        GdlData<TestDataObject> nonExistentData = new GdlData<>("non-existent-id", 0L, 
                0L, testObject);

        //Test.
        
        boolean result = repository.deleteData(nonExistentData);

        //Assert.
        
        assertFalse(result, "deleteData should return false when the data does not exist");

        //Verify the cache state.
        
        Stream<GdlData<TestDataObject>> dataAfterDeletion = repository.queryCachedData().queryData();
        
        assertEquals(0, dataAfterDeletion.count(),
                "Repository should still contain 0 data entries after attempting to delete non-existent data.");
    }

    @Test
    public void shouldAddDataWithCachedProperties() throws Exception {
        //Arrange.

        File tempDirectory = Files.createTempDirectory("test-cached-props").toFile();
        DataManager dataManager = new DataManager(Map.of(), tempDirectory);

        GdlDataRepository<DataWithCachedProps> repository = new GdlDataRepository<>(dataManager,
                DataWithCachedProps.class);

        //Add data with both fields populated.

        DataWithCachedProps testData = new DataWithCachedProps("standard-value", "cached-value");
        GdlData<DataWithCachedProps> addedData = repository.addData(testData);

        assertEquals("standard-value", addedData.getData().getStandardField());
        assertEquals("cached-value", addedData.getData().getCachedField());

        //Check if the data to cache was stored in memory.

        GdlData<DataWithCachedProps> cachedData  = repository.getUidCachedGdlDataContainerMap().get(addedData.getId());

        assertNotNull(cachedData, "The cached data-container should be present in memory.");

        assertEquals(cachedData.getId(), addedData.getId(), "The IDs should match.");

        assertEquals(cachedData.getCreateTimestamp(), addedData.getCreateTimestamp(),
                "The create timestamps should match.");

        assertEquals(cachedData.getUpdateTimestamp(), addedData.getUpdateTimestamp(),
                "The update timestamps should match.");

        assertNotNull(cachedData.getData(), "The data should be present in the cached data-container.");

        assertNull(cachedData.getData().getStandardField(),
                "The standard field should not be present in the cached data-container.");

        assertEquals("cached-value", cachedData.getData().getCachedField(),
                "The cached field should be present in the cached data-container.");

        //Query data from the repository to get the cached instance.

        GdlData<DataWithCachedProps> queriedCachedData = repository.queryCachedData().queryData().findFirst()
                .orElse(null);

        //Assert.

        assertNotNull(queriedCachedData, "Cached data should be retrievable after initial loading");

        assertNull(queriedCachedData.getData().getStandardField(),
                "The standard field should not be present in the cached data-container.");

        assertEquals("cached-value", queriedCachedData.getData().getCachedField(),
                "The cached field should be present in the cached data-container.");
    }

    @Test
    public void shouldUpdateDataWithCachedProperties() throws Exception {
        //Arrange.

        File tempDirectory = Files.createTempDirectory("test-cached-props").toFile();
        DataManager dataManager = new DataManager(Map.of(), tempDirectory);

        GdlDataRepository<DataWithCachedProps> repository = new GdlDataRepository<>(dataManager,
                DataWithCachedProps.class);

        //Add data with both fields populated.

        DataWithCachedProps testData = new DataWithCachedProps("standard-value", "cached-value");
        GdlData<DataWithCachedProps> data = repository.addData(testData);

        //Update the data.

        testData.setStandardField("updated-standard-value");
        testData.setCachedField("updated-cached-value");

        repository.updateData(data);

        //Check if the data to cache was updated in memory.

        GdlData<DataWithCachedProps> cachedData  = repository.getUidCachedGdlDataContainerMap().get(data.getId());

        assertNotNull(cachedData, "The cached data-container should be present in memory.");

        assertEquals(cachedData.getId(), data.getId(), "The IDs should match.");

        assertEquals(cachedData.getCreateTimestamp(), data.getCreateTimestamp(),
                "The create timestamps should match.");

        assertEquals(cachedData.getUpdateTimestamp(), data.getUpdateTimestamp(),
                "The update timestamps should match.");

        assertNotNull(cachedData.getData(), "The data should be present in the cached data-container.");

        assertNull(cachedData.getData().getStandardField(),
                "The standard field should not be present in the cached data-container.");

        assertEquals(testData.getCachedField(), cachedData.getData().getCachedField(),
                "The cached field should be present in the cached data-container.");

        //Get the updated cached instance.

        GdlData<DataWithCachedProps> updatedCachedData = repository.queryCachedData().queryData().findFirst()
                .orElse(null);

        //Assert updates worked correctly for cached properties.

        assertNotNull(updatedCachedData, "Updated cached data should be retrievable.");

        assertNull(updatedCachedData.getData().getStandardField(),
                "The standard field should not be present in the cached data-container.");

        assertEquals(testData.getCachedField(), updatedCachedData.getData().getCachedField(),
                "The cached field should be present in the cached data-container.");
    }
}