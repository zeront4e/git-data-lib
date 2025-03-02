package io.github.zeront4e.gdl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.zeront4e.gdl.annotations.GdlSecretDataRepository;
import io.github.zeront4e.gdl.annotations.GdlSecretProperty;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class DataManagerTest {
    private static class TestData {
        private String value;

        public TestData() {}

        public TestData(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestData testData = (TestData) o;
            return Objects.equals(value, testData.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public static class TestDataWithSecretField {
        @GdlSecretProperty("test-secret")
        private String sensitiveData = "sensitive-information";

        public String getSensitiveData() {
            return sensitiveData;
        }
    }

    @GdlSecretDataRepository("test-secret")
    public static class TestDataWithRepositorySecret {
        private String name;
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @GdlSecretDataRepository("test-secret")
    public static class TestDataWithRepositorySecretAndFieldSecret {
        @GdlSecretProperty("test-secret2")
        private String name;

        @GdlSecretProperty("test-secret2")
        private String value;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }
    }

    @Test
    void shouldReturnEmptyStreamForQueryDataWithEmptyDirectory() throws Exception {
        //Create a temporary directory for the test.
        File tempDir = Files.createTempDirectory("test-repo").toFile();

        try {
            //Create a data manager with the temp directory.
            Map<String, String> secretMap = new HashMap<>();
            DataManager dataManager = new DataManager(secretMap, tempDir, null);

            //Query data for a class that has never been stored.
            Stream<GdlData<TestData>> result = dataManager.queryData(TestData.class);

            //Verify that the returned stream is empty.
            assertEquals(0, result.count());
        }
        finally {
            //Clean up temp directory.
            tempDir.delete();
        }
    }

    @Test
    public void shouldThrowExceptionForMissingDataContainerFile() throws Exception {
        //Arrange.

        Map<String, String> secretMap = new HashMap<>();

        File tempDirectory = Files.createTempDirectory("test-repo").toFile();

        DataManager.OnDataChangeCallback<GdlData<?>> callback = null; //No need for callback in this test.

        DataManager dataManager = new DataManager(secretMap, tempDirectory, callback);

        //No need to mock files - the actual file simply won't exist in the temp directory.
        String nonExistentId = "non-existent-id";

        assertThrows(IOException.class, () -> {
            dataManager.loadDataContainerFromDisk(nonExistentId, TestData.class);
        });

        //Clean up.

        tempDirectory.delete();
    }

    @Test
    public void shouldAddTestData() throws Exception {
        //Setup.

        File tempDir = Files.createTempDirectory("test-repo").toFile();
        Map<String, String> secretMap = new HashMap<>();

        AtomicReference<File> addedFile = new AtomicReference<>();
        AtomicReference<GdlData<?>> addedData = new AtomicReference<>();

        DataManager.OnDataChangeCallback<GdlData<?>> callback = new DataManager.OnDataChangeCallback<>() {
            @Override
            public void onDataAdded(File file, GdlData<?> data) {
                addedFile.set(file);
                addedData.set(data);
            }

            @Override
            public void onDataUpdated(File file, GdlData<?> data) {}

            @Override
            public void onDataDeleted(File file, GdlData<?> data) {}
        };

        DataManager dataManager = new DataManager(secretMap, tempDir, callback);

        TestData testData = new TestData("test-value");

        //Execute.

        GdlData<TestData> result = dataManager.addData(testData);

        //Verify basic properties.

        assertNotNull(result);
        assertEquals(testData, result.getData());
        assertNotNull(result.getId());
        assertTrue(result.getCreateTimestamp() > 0);
        assertEquals(result.getCreateTimestamp(), result.getUpdateTimestamp());

        //Verify file structure.

        File dataDir = new File(tempDir, DataManager.DATA_DIRECTORY + "/" + TestData.class.getSimpleName());

        assertTrue(dataDir.exists());
        assertTrue(dataDir.isDirectory());

        //Verify file was created.

        File dataFile = new File(dataDir, result.getId() + ".yaml");

        assertTrue(dataFile.exists());
        assertTrue(dataFile.isFile());

        //Verify callback was called correctly.

        assertEquals(dataFile, addedFile.get());
        assertEquals(result, addedData.get());

        //Verify file content.

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.findAndRegisterModules();

        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(GdlData.class, TestData.class);

        GdlData<TestData> loadedData = objectMapper.readValue(dataFile, javaType);

        assertEquals(result.getId(), loadedData.getId());
        assertEquals(result.getCreateTimestamp(), loadedData.getCreateTimestamp());
        assertEquals(result.getUpdateTimestamp(), loadedData.getUpdateTimestamp());
        assertEquals(result.getData(), loadedData.getData());

        //Clean up.

        dataFile.delete();
        tempDir.delete();
    }

    @Test
    public void shouldUpdateTestData() throws Exception {
        //Arrange.

        File tempDir = Files.createTempDirectory("test-repo").toFile();

        Map<String, String> secretMap = new HashMap<>();

        AtomicReference<File> updatedFile = new AtomicReference<>();
        AtomicReference<GdlData<?>> updatedData = new AtomicReference<>();

        DataManager.OnDataChangeCallback<GdlData<?>> callback = new DataManager.OnDataChangeCallback<>() {
            @Override
            public void onDataAdded(File file, GdlData<?> data) {}

            @Override
            public void onDataUpdated(File file, GdlData<?> data) {
                updatedFile.set(file);
                updatedData.set(data);
            }

            @Override
            public void onDataDeleted(File file, GdlData<?> data) {}
        };

        DataManager dataManager = new DataManager(secretMap, tempDir, callback);

        //First add some test data.

        TestData testObject = new TestData("initial value");

        GdlData<TestData> addedData = dataManager.addData(testObject);

        long originalTimestamp = addedData.getUpdateTimestamp();

        //Wait a bit to ensure timestamp difference.
        Thread.sleep(100);

        //Update the data.

        testObject.setValue("updated value");

        dataManager.updateData(addedData);

        //Assert.

        assertTrue(addedData.getUpdateTimestamp() > originalTimestamp);
        assertNotNull(updatedFile.get());
        assertEquals(updatedData.get(), addedData);

        //Verify data was persisted to disk.

        File dataDir = new File(tempDir, DataManager.DATA_DIRECTORY + "/" + TestData.class.getSimpleName());

        File expectedFile = new File(dataDir.getAbsolutePath() + "/" + addedData.getId() + ".yaml");

        assertTrue(expectedFile.exists());

        //Read the file and verify content.

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.findAndRegisterModules();

        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(GdlData.class, TestData.class);

        GdlData<TestData> persistedData = objectMapper.readValue(expectedFile, javaType);

        assertEquals(addedData.getId(), persistedData.getId());
        assertEquals(addedData.getUpdateTimestamp(), persistedData.getUpdateTimestamp());
        assertEquals("updated value", persistedData.getData().getValue());
    }

    @Test
    public void shouldDeleteTestData() throws Exception {
        //Setup.

        File tempDir = Files.createTempDirectory("test-repo").toFile();

        Map<String, String> secretMap = new HashMap<>();

        DataManager dataManager = new DataManager(secretMap, tempDir);

        TestData testData = new TestData("test-value");

        //Execute.

        GdlData<TestData> result = dataManager.addData(testData);

        //Verify basic properties.

        assertNotNull(result);
        assertEquals(testData, result.getData());
        assertNotNull(result.getId());
        assertTrue(result.getCreateTimestamp() > 0);
        assertEquals(result.getCreateTimestamp(), result.getUpdateTimestamp());

        //Verify file structure.

        File dataDir = new File(tempDir, DataManager.DATA_DIRECTORY + "/" + TestData.class.getSimpleName());

        assertTrue(dataDir.exists());
        assertTrue(dataDir.isDirectory());

        //Verify file was created.

        File dataFile = new File(dataDir, result.getId() + ".yaml");

        assertTrue(dataFile.exists());
        assertTrue(dataFile.isFile());

        //Delete the created data.

        dataManager.deleteData(result);

        //Verify the data has been deleted.

        assertFalse(dataFile.exists());

        //Clean up.

        dataFile.delete();
        tempDir.delete();
    }

    @Test
    public void shouldAddDataWithEncryptedFields() throws Exception {
        //Setup test data.

        Map<String, String> secretMap = Map.of("test-secret", "secretValue123456");

        File tempDir = Files.createTempDirectory("test-repo").toFile();

        DataManager dataManager = new DataManager(secretMap, tempDir);

        //Create original test data.

        TestDataWithSecretField originalData = new TestDataWithSecretField();
        String originalSensitiveData = originalData.getSensitiveData();

        //Add data (this will encrypt the field with the annotation).

        GdlData<TestDataWithSecretField> addedData = dataManager.addData(originalData);

        //Verify file structure.

        File dataDir = new File(tempDir, DataManager.DATA_DIRECTORY + "/" +
                TestDataWithSecretField.class.getSimpleName());

        assertTrue(dataDir.exists());
        assertTrue(dataDir.isDirectory());

        //Verify file was created.

        File dataFile = new File(dataDir, addedData.getId() + ".yaml");

        assertTrue(dataFile.exists());
        assertTrue(dataFile.isFile());

        //Verify the data of the serialized data-object is encrypted.

        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.findAndRegisterModules();

        JavaType javaType = objectMapper.getTypeFactory().constructParametricType(GdlData.class,
                TestDataWithSecretField.class);

        GdlData<TestDataWithSecretField> persistedData = objectMapper.readValue(dataFile, javaType);

        assertTrue(persistedData.getData().getSensitiveData().startsWith(DataManager.ENCRYPTED_PROPERTY_PREFIX));

        //Verify the field data of the returned data-object isn't encrypted.

        Field sensitiveDataField = TestDataWithSecretField.class.getDeclaredField("sensitiveData");
        sensitiveDataField.setAccessible(true);

        String encryptedValue = (String) sensitiveDataField.get(addedData.getData());
        assertFalse(encryptedValue.startsWith(DataManager.ENCRYPTED_PROPERTY_PREFIX));

        //Load data from disk (this should decrypt the field).

        GdlData<TestDataWithSecretField> loadedData = dataManager.loadDataContainerFromDisk(addedData.getId(),
                TestDataWithSecretField.class);

        //Verify the field is properly decrypted.

        assertEquals(originalSensitiveData, loadedData.getData().getSensitiveData());

        //Cleanup.

        dataFile.delete();
        tempDir.delete();
    }


    @Test
    public void shouldAddDataWithDataObjectEncryption() throws Exception {
        //Setup.

        Map<String, String> secretMap = Map.of("test-secret", "secretValue123456");

        File tempDir = Files.createTempDirectory("test-repo").toFile();

        DataManager dataManager = new DataManager(secretMap, tempDir);

        //Create test data with GdlSecretDataRepository annotation.

        TestDataWithRepositorySecret testData = new TestDataWithRepositorySecret();
        testData.setName("Secret Name");
        testData.setValue("Secret Value");

        //Add data (this should trigger encryption).

        GdlData<TestDataWithRepositorySecret> addedData = dataManager.addData(testData);
        String addedId = addedData.getId();

        //Verify file structure.

        File dataDir = new File(tempDir, DataManager.DATA_DIRECTORY + "/" +
                TestDataWithRepositorySecret.class.getSimpleName());

        assertTrue(dataDir.exists());
        assertTrue(dataDir.isDirectory());

        //Verify file was created.

        File dataFile = new File(dataDir, addedData.getId() + ".yaml");

        assertTrue(dataFile.exists());
        assertTrue(dataFile.isFile());

        //Check the file contains encrypted data.

        String fileContent = Files.readString(dataFile.toPath());

        assertTrue(fileContent.contains("[git-data-lib-encrypted-data]"));

        assertFalse(fileContent.contains("Secret Name")); //Ensure the raw data is not present.
        assertFalse(fileContent.contains("Secret Value")); //Ensure the raw data is not present.

        //Load data (this should trigger decryption).

        GdlData<TestDataWithRepositorySecret> loadedData = dataManager.loadDataContainerFromDisk(addedId,
                TestDataWithRepositorySecret.class);

        //Verify decrypted data matches original.

        TestDataWithRepositorySecret decryptedData = loadedData.getData();

        assertEquals("Secret Name", decryptedData.getName());
        assertEquals("Secret Value", decryptedData.getValue());

        //Cleanup.

        dataFile.delete();
        tempDir.delete();
    }

    @Test
    public void shouldAddDataWithDataObjectEncryptionAndEncryptedFields() throws Exception {
        //Setup.

        Map<String, String> secretMap = Map.of("test-secret", "secretValue123456",
                "test-secret2", "secretValue654321");

        File tempDir = Files.createTempDirectory("test-repo").toFile();

        DataManager dataManager = new DataManager(secretMap, tempDir);

        //Create test data with GdlSecretDataRepository annotation.

        TestDataWithRepositorySecretAndFieldSecret testData = new TestDataWithRepositorySecretAndFieldSecret();
        testData.setName("Secret Name");
        testData.setValue("Secret Value");

        //Add data (this should trigger encryption).

        GdlData<TestDataWithRepositorySecretAndFieldSecret> addedData = dataManager.addData(testData);
        String addedId = addedData.getId();

        //Verify file structure.

        File dataDir = new File(tempDir, DataManager.DATA_DIRECTORY + "/" +
                TestDataWithRepositorySecretAndFieldSecret.class.getSimpleName());

        assertTrue(dataDir.exists());
        assertTrue(dataDir.isDirectory());

        //Verify file was created.

        File dataFile = new File(dataDir, addedData.getId() + ".yaml");

        assertTrue(dataFile.exists());
        assertTrue(dataFile.isFile());

        //Check the file contains encrypted data.

        String fileContent = Files.readString(dataFile.toPath());

        assertTrue(fileContent.contains("[git-data-lib-encrypted-data]"));

        assertFalse(fileContent.contains("Secret Name")); //Ensure the raw data is not present.
        assertFalse(fileContent.contains("Secret Value")); //Ensure the raw data is not present.

        //Load data (this should trigger decryption).

        GdlData<TestDataWithRepositorySecretAndFieldSecret> loadedData = dataManager.loadDataContainerFromDisk(addedId,
                TestDataWithRepositorySecretAndFieldSecret.class);

        //Verify decrypted data matches original.

        TestDataWithRepositorySecretAndFieldSecret decryptedData = loadedData.getData();

        assertEquals("Secret Name", decryptedData.getName());
        assertEquals("Secret Value", decryptedData.getValue());

        //Verify that the fields were actually encrypted.

        secretMap = Map.of("test-secret", "secretValue123456"); //Add repository secret only.

        dataManager = new DataManager(secretMap, tempDir);

        DataManager finalDataManager = dataManager;

        assertThrows(Exception.class, () -> {
            finalDataManager.loadDataContainerFromDisk(addedId, TestDataWithRepositorySecretAndFieldSecret.class);
        }, "Expected IOException for encrypted fields without field-related secret.");

        //Cleanup.

        dataFile.delete();
        tempDir.delete();
    }

    @Test
    public void shouldCallErrorCallbackForCorruptedDataObjectFile() throws IOException {
        //Setup.

        Map<String, String> secretMap = new HashMap<>();
        
        File tempDirectory = Files.createTempDirectory("test-repo").toFile();

        AtomicBoolean errorCallbackCalled = new AtomicBoolean(false);
        GdlProcessingErrorCallback errorCallback = (file, exception) -> errorCallbackCalled.set(true);

        DataManager dataManager = new DataManager(secretMap, tempDirectory);

        //Create directory for TestData class.

        File dataDir = new File(tempDirectory, DataManager.DATA_DIRECTORY + "/" + TestData.class.getSimpleName());
        dataDir.mkdirs();

        assertTrue(dataDir.exists());
        assertTrue(dataDir.isDirectory());

        //Create a corrupt data file.

        File corruptFile = new File(dataDir, "corrupt-data.yaml");

        Files.write(corruptFile.toPath(), "This is not valid YAML{".getBytes());

        //Create a valid data file.

        TestData validTestData = new TestData("value-value");

        assertDoesNotThrow(() -> {
            dataManager.addData(validTestData);
        });

        //Test.

        List<GdlData<TestData>> results = dataManager.queryData(TestData.class, errorCallback).toList();

        //Verify.

        assertTrue(errorCallbackCalled.get(), "Error callback should have been called for corrupt file.");

        assertEquals(1, results.size(), "Should have one valid result.");

        assertEquals(validTestData.getValue(), results.get(0).getData().getValue(),
                "Should have parsed valid file correctly.");
    }
}
