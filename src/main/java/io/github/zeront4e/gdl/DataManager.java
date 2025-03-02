package io.github.zeront4e.gdl;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.github.zeront4e.gdl.annotations.GdlDataRepositoryName;
import io.github.zeront4e.gdl.annotations.GdlSecretDataRepository;
import io.github.zeront4e.gdl.annotations.GdlSecretProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.stream.Stream;

/**
 * Class to manage data-objects stored in a local directory.
 */
class DataManager {
    interface DataSkipCallback {
        boolean skipFile(File file);
    }

    private record SecretContainer(String secretName, String secretValue) {

    }

    private record DataObjectDecryptionResult(boolean decryptionOccurred, GdlData<?> data) {

    }

    public interface OnDataChangeCallback<Type> {
        void onDataAdded(File file, Type data);
        void onDataUpdated(File file, Type data);
        void onDataDeleted(File file, Type data);
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(DataManager.class);

    /**
     * Directory to store serialized data.
     */
    public static final String DATA_DIRECTORY = "serialized-data";

    /**
     * Prefix to signalize encrypted data fields (a single property is encrypted).
     */
    public static final String ENCRYPTED_PROPERTY_PREFIX = "[git-data-lib-encrypted-property]";

    /**
     * Prefix to signalize encrypted data-objects (the whole object is encrypted).
     */
    public static final String ENCRYPTED_DATA_PREFIX = "[git-data-lib-encrypted-data]";

    private final Map<String, String> secretNameSecretMap;
    private final File localRepoDirectoryFile;
    private final OnDataChangeCallback<GdlData<?>> dataChangeCallback;

    private final ObjectMapper objectMapper;

    DataManager(Map<String, String> secretNameSecretMap, File localRepoDirectoryFile) {
        this(secretNameSecretMap, localRepoDirectoryFile, null);
    }

    DataManager(Map<String, String> secretNameSecretMap, File localRepoDirectoryFile,
                OnDataChangeCallback<GdlData<?>> dataChangeCallback) {
        this.secretNameSecretMap = secretNameSecretMap;
        this.localRepoDirectoryFile = localRepoDirectoryFile;
        this.dataChangeCallback = dataChangeCallback;

        objectMapper = new ObjectMapper(new YAMLFactory());
        objectMapper.findAndRegisterModules();
    }

    public File getLocalRepoDirectoryFile() {
        return localRepoDirectoryFile;
    }

    <Type> Stream<GdlData<Type>> queryData(Class<Type> dataType) {
        return queryData(dataType, null);
    }

    synchronized <Type> Stream<GdlData<Type>> queryData(Class<Type> dataType,
                                                        GdlProcessingErrorCallback errorCallback) {
        return queryData(dataType, errorCallback, null);
    }

    synchronized <Type> Stream<GdlData<Type>> queryData(Class<Type> dataType, GdlProcessingErrorCallback errorCallback,
                                                        DataSkipCallback dataSkipCallback) {
        File dataContainersDirectory = getDataContainerDirectory(dataType);

        if (!dataContainersDirectory.exists())
            return Stream.empty();

        File[] dataContainerFiles = dataContainersDirectory.listFiles(File::isFile);

        if (dataContainerFiles != null) {
            JavaType javaType = objectMapper.getTypeFactory().constructParametricType(GdlData.class, dataType);

            return Stream.of(dataContainerFiles).map(tmpFile -> {
                //Check if the file should be skipped.

                if(dataSkipCallback!= null && dataSkipCallback.skipFile(tmpFile))
                    return null;

                //Try to parse the file content into a data-object container.

                try {
                    return objectMapper.<GdlData<Type>>readValue(tmpFile, javaType);
                }
                catch (Exception exception) {
                    LOGGER.warn("Failed to deserialize data from file: {}", tmpFile.getAbsolutePath(), exception);

                    if(errorCallback != null)
                        errorCallback.onProcessingError(tmpFile, exception);

                    return null;
                }
            }).filter(Objects::nonNull);
        }

        return Stream.empty();
    }

    synchronized <Type> GdlData<Type> loadDataContainerFromDisk(String id, Class<Type> dataType) throws Exception {
        //Try to obtain data-object container file.

        File dataContainersDirectory = getDataContainerDirectory(dataType);

        File dataContainerFile = new File(dataContainersDirectory.getAbsolutePath() + "/" + id + ".yaml");

        if(!dataContainerFile.isFile())
            throw new IOException("Data container file not found: " + dataContainerFile.getAbsolutePath());

        //Deserialize data-object container.

        GdlData<?> dataContainer = tryToParseDataObjectContainer(dataContainerFile, dataType);

        //Decrypt data-object, if encrypted.

        dataContainer = decryptDataContainerObjectIfEncrypted(dataType, dataContainer);

        //Decrypt data-object fields, if encrypted.

        decryptDataContainerFieldsIfAnnotated(dataContainer.getData());

        //Return decrypted data type.

        return (GdlData<Type>) dataContainer;
    }

    <Type> GdlData<Type> addData(Type data) throws Exception {
        //Obtain data container directory.

        File dataContainersDirectory = getDataContainerDirectory(data.getClass());

        if (!dataContainersDirectory.exists())
            dataContainersDirectory.mkdirs();

        //Create a data container file.

        String id = UUID.randomUUID().toString();

        File dataContainerFile = new File(dataContainersDirectory.getAbsolutePath() + "/" + id + ".yaml");

        //Create data-object container.

        long timestamp = System.currentTimeMillis();

        GdlData<Type> gdlData = new GdlData<>(id, timestamp, timestamp, data);

        //Serialize and write data-object container to disk.

        serializeDataContainerToDisk(dataContainerFile, gdlData);

        if(dataChangeCallback != null)
            dataChangeCallback.onDataAdded(dataContainerFile, gdlData);

        return gdlData;
    }

    <Type> void updateData(GdlData<Type> gdlData) throws Exception {
        gdlData.setUpdateTimestamp(System.currentTimeMillis());

        File dataContainersDirectory = getDataContainerDirectory(gdlData.getData().getClass());

        File dataContainerFile = new File(dataContainersDirectory.getAbsolutePath() + "/" +
                gdlData.getId() + ".yaml");

        serializeDataContainerToDisk(dataContainerFile, gdlData);

        if(dataChangeCallback != null)
            dataChangeCallback.onDataUpdated(dataContainerFile, gdlData);
    }

    <Type> boolean deleteData(GdlData<Type> gdlData) {
        File dataContainersDirectory = getDataContainerDirectory(gdlData.getData().getClass());

        File dataContainerFile = new File(dataContainersDirectory.getAbsolutePath() + "/" +
                gdlData.getId() + ".yaml");

        if(dataContainerFile.isFile()) {
            boolean fileWasDeleted = dataContainerFile.delete();

            if(fileWasDeleted && dataChangeCallback != null)
                dataChangeCallback.onDataDeleted(dataContainerFile, gdlData);

            return fileWasDeleted;
        }

        return false;
    }

    private <Type> File getDataContainerDirectory(Class<Type> dataType) {
        GdlDataRepositoryName gdlDataRepositoryName = dataType.getAnnotation(GdlDataRepositoryName.class);

        String repositoryNameValue = gdlDataRepositoryName != null ? gdlDataRepositoryName.value() :
                dataType.getSimpleName();

        return new File(localRepoDirectoryFile.getAbsolutePath() + "/" + DATA_DIRECTORY + "/" +
                repositoryNameValue);
    }

    private <Type> void serializeDataContainerToDisk(File dataContainerFile, GdlData<Type> gdlData) throws Exception {
        //Create data-object container with encrypted data, if necessary.

        GdlData<?> dataContainerToSerialize = encryptDataObjectIfAnnotated(gdlData);

        //Serialize and write data-object container to disk.

        LOGGER.debug("Serializing data-object container to file. Path: {}", dataContainerFile.getAbsolutePath());

        objectMapper.writeValue(dataContainerFile, dataContainerToSerialize);
    }

    private <Type> GdlData<Type> decryptDataContainerObjectIfEncrypted(Class<Type> dataType, GdlData<?> deserializedDataContainer) throws Exception {
        GdlSecretDataRepository gdlSecretDataRepository = dataType.getAnnotation(GdlSecretDataRepository.class);

        if(gdlSecretDataRepository != null && deserializedDataContainer.getData() instanceof String encryptedDataString) {
            if (encryptedDataString.startsWith(ENCRYPTED_DATA_PREFIX)) {
                String encryptedData = encryptedDataString.substring(ENCRYPTED_DATA_PREFIX.length());

                String secretName = gdlSecretDataRepository.value();

                String secretValue = secretNameSecretMap.get(secretName);

                if (secretValue == null)
                    throw new Exception("Unable to find secret. Name: " + secretName);

                try {
                    String decryptedData = AesEncryptionUtil.decrypt(encryptedData, secretValue);

                    Type decryptedType = objectMapper.readValue(decryptedData, dataType);

                    deserializedDataContainer = new GdlData<>(deserializedDataContainer.getId(),
                            deserializedDataContainer.getCreateTimestamp(), deserializedDataContainer.getUpdateTimestamp(),
                            decryptedType);
                }
                catch (Exception exception) {
                    throw new Exception("Failed to decrypt encrypted data in data-container, despite set prefix.",
                            exception);
                }
            }
        }

        return (GdlData<Type>) deserializedDataContainer;
    }

    private <Type> GdlData<?> encryptDataObjectIfAnnotated(GdlData<Type> gdlData) throws Exception {
        Type data = gdlData.getData();

        //Encrypt data-object fields, if annotation is present.

        Type dataWithEncryptedFields = encryptDataContainerFieldsIfAnnotated(data);

        //Encrypt whole data-object, if annotation is present.

        String encryptedDataContainerString = encryptDataContainerIfAnnotatedOrNull(dataWithEncryptedFields);

        GdlData<?> dataContainerToSerialize;

        if(encryptedDataContainerString != null) {
            dataContainerToSerialize = new GdlData<>(gdlData.getId(), gdlData.getCreateTimestamp(),
                    gdlData.getUpdateTimestamp(), encryptedDataContainerString);
        }
        else {
            dataContainerToSerialize = new GdlData<>(gdlData.getId(), gdlData.getCreateTimestamp(),
                    gdlData.getUpdateTimestamp(), dataWithEncryptedFields);
        }

        return dataContainerToSerialize;
    }

    private <Type> String encryptDataContainerIfAnnotatedOrNull(Type data) throws Exception {
        GdlSecretDataRepository gdlSecretDataRepository = data.getClass().getAnnotation(GdlSecretDataRepository.class);

        if(gdlSecretDataRepository == null)
            return null;

        String secretName = gdlSecretDataRepository.value();

        String secretValue = secretNameSecretMap.get(secretName);

        if(secretValue == null) {
            LOGGER.warn("Unable to find secret \"{}\". The data-object of type {} won't be encrypted.", secretName,
                    data.getClass().getName());

            return null;
        }
        else {
            LOGGER.debug("Encrypting data for secret \"{}\".", secretName);

            String serializedData = objectMapper.writeValueAsString(data);

            return ENCRYPTED_DATA_PREFIX + AesEncryptionUtil.encrypt(serializedData, secretValue);
        }
    }

    private <Type> Type encryptDataContainerFieldsIfAnnotated(Type data) throws Exception {
        //Check if any of the data field should be encrypted.

        Field[] fields = data.getClass().getDeclaredFields();

        Map<Field, SecretContainer> fieldSecretMap = new HashMap<>();

        for(Field tmpField : fields) {
            GdlSecretProperty gdlSecretProperty = tmpField.getAnnotation(GdlSecretProperty.class);

            if(gdlSecretProperty != null) {
                String secretName = gdlSecretProperty.value();

                String secretValue = secretNameSecretMap.get(secretName);

                if(secretValue == null) {
                    throw new Exception("Unable to find secret \"" + secretName + "\". The field " +
                            tmpField.getName() + " of type " + data.getClass().getName() + " can't be encrypted.");
                }
                else {
                    fieldSecretMap.put(tmpField, new SecretContainer(secretName, secretValue));
                }
            }
        }

        //Encrypt the data fields to protect, if supported.

        if(fieldSecretMap.isEmpty())
            return data;

        Type clonedData = ObjectCloneUtil.deepCloneParameterizedObject(data);

        fieldSecretMap.forEach((tmpField, secretContainer) -> {
            //Extract object value.

            Object fieldValueObject;

            try {
                int modifiers = tmpField.getModifiers();

                boolean isModifierPrivate = Modifier.isPrivate(modifiers);

                if(isModifierPrivate)
                    tmpField.setAccessible(true);

                fieldValueObject = tmpField.get(data);

                if(isModifierPrivate)
                    tmpField.setAccessible(false);
            }
            catch (Exception exception) {
                LOGGER.error("Failed to get value of field {} of type {} for secret \"{}\".",
                        tmpField.getName(), tmpField.getType().getName(), secretContainer.secretName(), exception);

                return;
            }

            if(fieldValueObject == null)
                return;

            //Try to encrypt the field value, if it's a supported type.

            if(String.class.equals(tmpField.getType())) {
                encryptStringFieldValue(tmpField, clonedData, secretContainer);
            }
            else {
                LOGGER.error("Field {} of type {} is not supported for encryption. The field value won't be " +
                                "encrypted. Is the correct version of this library set?", tmpField.getName(),
                        tmpField.getType().getName());
            }
        });

        return clonedData;
    }

    private void encryptStringFieldValue(Field stringField, Object dataObject, SecretContainer secretContainer) {
        //Encrypt object value.

        LOGGER.debug("Encrypting string field {} of type {} for secret \"{}\".", stringField.getName(),
                stringField.getType().getName(), secretContainer.secretName());

        try {
            int modifiers = stringField.getModifiers();

            boolean isModifierPrivate = Modifier.isPrivate(modifiers);

            if(isModifierPrivate)
                stringField.setAccessible(true);

            String stringToEncrypt = stringField.get(dataObject).toString();

            String encryptedString = ENCRYPTED_PROPERTY_PREFIX +
                    AesEncryptionUtil.encrypt(stringToEncrypt, secretContainer.secretValue());

            stringField.set(dataObject, encryptedString);

            if(isModifierPrivate)
                stringField.setAccessible(false);
        }
        catch (Exception exception) {
            LOGGER.error("Failed to encrypt string field {} of type {} for secret \"{}\".",
                    stringField.getName(), stringField.getType().getName(), secretContainer.secretName(),
                    exception);
        }
    }

    private <Type> void decryptDataContainerFieldsIfAnnotated(Type data) throws Exception {
        //Check if any of the data field should be decrypted.

        Field[] fields = data.getClass().getDeclaredFields();

        Map<Field, SecretContainer> fieldSecretMap = new HashMap<>();

        for(Field tmpField : fields) {
            GdlSecretProperty gdlSecretProperty = tmpField.getAnnotation(GdlSecretProperty.class);

            if(gdlSecretProperty != null) {
                String secretName = gdlSecretProperty.value();

                String secretValue = secretNameSecretMap.get(secretName);

                if(secretValue == null) {
                    throw new Exception("Unable to find secret \"" + secretName +"\". The field " +
                            tmpField.getName() + " of type " + data.getClass().getName() + " won't be decrypted.");
                }
                else {
                    fieldSecretMap.put(tmpField, new SecretContainer(secretName, secretValue));
                }
            }
        }

        //Decrypt the encrypted data fields, if present.

        if(fieldSecretMap.isEmpty())
            return;

        for(Map.Entry<Field, SecretContainer> tmpEntry : fieldSecretMap.entrySet()) {
            Field field = tmpEntry.getKey();
            SecretContainer secretContainer = tmpEntry.getValue();

            //Extract object value.

            Object fieldValueObject;

            try {
                int modifiers = field.getModifiers();

                boolean isModifierPrivate = Modifier.isPrivate(modifiers);

                if(isModifierPrivate)
                    field.setAccessible(true);

                fieldValueObject = field.get(data);

                if(isModifierPrivate)
                    field.setAccessible(false);
            }
            catch (Exception exception) {
                LOGGER.error("Failed to get value of field {} of type {} for secret \"{}\".",
                        field.getName(), field.getType().getName(), secretContainer.secretName(), exception);

                return;
            }

            if(fieldValueObject == null)
                return;

            //Try to decrypt the field value, if it's a supported type.

            if(String.class.equals(field.getType())) {
                decryptStringFieldValue(field, data, secretContainer);
            }
            else {
                LOGGER.error("Field {} of type {} is not supported for decryption. The field value won't be " +
                                "decrypted. Is the correct version of this library set?", field.getName(),
                        field.getType().getName());
            }
        }
    }

    private void decryptStringFieldValue(Field stringField, Object dataObject, SecretContainer secretContainer) throws Exception {
        //Encrypt object value.

        LOGGER.debug("Decrypting string field {} of type {} for secret \"{}\".", stringField.getName(),
                stringField.getType().getName(), secretContainer.secretName());

        int modifiers = stringField.getModifiers();

        boolean isModifierPrivate = Modifier.isPrivate(modifiers);

        if(isModifierPrivate)
            stringField.setAccessible(true);

        String stringToDecrypt = stringField.get(dataObject).toString();

        if(stringToDecrypt.startsWith(ENCRYPTED_PROPERTY_PREFIX)) {
            try {
                stringToDecrypt = stringToDecrypt.substring(ENCRYPTED_PROPERTY_PREFIX.length());

                String decryptedString = AesEncryptionUtil.decrypt(stringToDecrypt, secretContainer.secretValue());

                stringField.set(dataObject, decryptedString);
            }
            catch (Exception exception) {
                throw new Exception("Unable to decrypt field " + stringField.getName() + " of type " +
                        stringField.getType().getName() + ", despite the set annotation. Is the secret correct?",
                        exception);
            }
        }
        else {
            LOGGER.warn("The field {} of type {} is not encrypted, despite the set annotation (missing prefix). " +
                    "The field value won't be decrypted.", stringField.getName(), stringField.getType().getName());
        }

        if(isModifierPrivate)
            stringField.setAccessible(false);
    }

    private GdlData<?> tryToParseDataObjectContainer(File dataContainerFile, Class<?> dataType) throws IOException {
        //Try to detect the marker string to indicate that the whole data-object is encrypted.

        boolean tryToParseStringContainer = dataType.getAnnotation(GdlSecretDataRepository.class) != null;

        //Parse the data-object.

        JavaType javaType;

        GdlData<?> dataContainer;

        if(tryToParseStringContainer) {
            javaType = objectMapper.getTypeFactory().constructParametricType(GdlData.class, String.class);

            try {
                dataContainer = objectMapper.readValue(dataContainerFile, javaType);
            }
            catch (Exception exception) {
                LOGGER.debug("Failed to deserialize encrypted data-object, despite set marker string. Attempt " +
                        "regular deserialization.", exception);

                javaType = objectMapper.getTypeFactory().constructParametricType(GdlData.class, dataType);

                dataContainer = objectMapper.readValue(dataContainerFile, javaType);
            }
        }
        else {
            javaType = objectMapper.getTypeFactory().constructParametricType(GdlData.class, dataType);

            dataContainer = objectMapper.readValue(dataContainerFile, javaType);
        }

        return dataContainer;
    }
}
