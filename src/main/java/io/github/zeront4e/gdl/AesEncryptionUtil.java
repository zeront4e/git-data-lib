package io.github.zeront4e.gdl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;

class AesEncryptionUtil {
    private static final Logger LOGGER = LoggerFactory.getLogger(AesEncryptionUtil.class);

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private static final String ALGORITHM = "AES/GCM/NoPadding";

    private static final int AES_KEY_BITS_COUNT = 256;
    private static final int IV_BYTES_COUNT = 12;
    private static final int SALT_BYTES_COUNT = 32;

    private static final int TAG_BITS_COUNT = 128;
    private static final int PBKDF2_ITERATION_COUNT = 65536;

    /**
     * Encrypts a plaintext string using AES encryption and a provided password.
     * The encrypted result is returned as a Base64-encoded string.
     * @param plaintext The string to be encrypted.
     * @param password  The password used for encryption.
     * @return A Base64-encoded string representing the encrypted data.
     * @throws Exception If an error occurs during the encryption process.
     */
    public static String encrypt(String plaintext, String password) throws Exception {
        byte[] encryptedBytes = encrypt(plaintext.getBytes(), password);

        return Base64.getEncoder().encodeToString(encryptedBytes);
    }

    /**
     * Encrypts the given plaintext using AES encryption with the provided password.
     * This method generates a salt and initialization vector (IV), derives a key from the password,
     * and performs the encryption using AES/GCM/NoPadding algorithm.
     * @param plaintext The byte array containing the data to be encrypted.
     * @param password  The password used for deriving the encryption key.
     * @return A byte array containing the encrypted data, including the salt and IV.
     *         The returned array structure is: [salt][iv][encrypted data].
     * @throws Exception If an error occurs during the encryption process, such as
     *                   invalid algorithm, invalid key specification, or encryption failure.
     */
    public static byte[] encrypt(byte[] plaintext, String password) throws Exception {
        byte[] salt = generateSalt();

        SecretKey secretKey = deriveKey(password, salt);

        byte[] iv = generateIV();

        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_BITS_COUNT, iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmParameterSpec);

        byte[] ciphertext = cipher.doFinal(plaintext);

        ByteBuffer byteBuffer = ByteBuffer.allocate(salt.length + iv.length + ciphertext.length);

        byteBuffer.put(salt);
        byteBuffer.put(iv);
        byteBuffer.put(ciphertext);

        return byteBuffer.array();
    }

    /**
     * Decrypts a Base64-encoded encrypted string using AES decryption and a provided password.
     * @param ciphertext The Base64-encoded encrypted string to be decrypted.
     * @param password   The password used for decryption. This should be the same password used for encryption.
     * @return A String containing the decrypted plaintext.
     * @throws Exception If an error occurs during the decryption process, such as
     *                   invalid Base64 encoding, incorrect password, or decryption failure.
     */
    public static String decrypt(String ciphertext, String password) throws Exception {
        byte[] decodedCiphertext = Base64.getDecoder().decode(ciphertext);

        byte[] decryptedBytes = decrypt(decodedCiphertext, password);

        return new String(decryptedBytes);
    }

    /**
     * Decrypts the given ciphertext using AES decryption with the provided password.
     * This method extracts the salt and initialization vector (IV) from the ciphertext,
     * derives the key from the password, and performs the decryption using AES/GCM/NoPadding algorithm.
     * @param ciphertext The byte array containing the encrypted data, including the salt and IV.
     *                   The expected structure is: [salt][iv][encrypted data].
     * @param password   The password used for deriving the decryption key.
     *                   This should be the same password used for encryption.
     * @return A byte array containing the decrypted plaintext data.
     * @throws Exception If an error occurs during the decryption process, such as
     *                   invalid algorithm, incorrect password, or decryption failure.
     */
    public static byte[] decrypt(byte[] ciphertext, String password) throws Exception {
        ByteBuffer byteBuffer = ByteBuffer.wrap(ciphertext);

        byte[] salt = new byte[SALT_BYTES_COUNT];
        byteBuffer.get(salt);

        byte[] iv = new byte[IV_BYTES_COUNT];
        byteBuffer.get(iv);

        byte[] encryptedData = new byte[byteBuffer.remaining()];
        byteBuffer.get(encryptedData);

        SecretKey secretKey = deriveKey(password, salt);

        GCMParameterSpec gcmParameterSpec = new GCMParameterSpec(TAG_BITS_COUNT, iv);

        Cipher cipher = Cipher.getInstance(ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmParameterSpec);

        return cipher.doFinal(encryptedData);
    }

    private static SecretKey deriveKey(String password, byte[] salt) throws Exception {
        //Read globally configured key derivation algorithm.

        String keyDerivationAlgorithm = System.getProperty("git-data-lib.encryption.key-derivation-algorithm");

        if(keyDerivationAlgorithm == null) {
            LOGGER.debug("Unable to find key derivation algorithm property. Try to read environment variable.");

            keyDerivationAlgorithm = System.getenv("GIT_DATA_LIB_ENCRYPTION_KEY_DERIVATION_ALGORITHM");

            if(keyDerivationAlgorithm == null) {
               LOGGER.debug("Unable to find environment variable. Defaulting to SHA256.");

               keyDerivationAlgorithm = "SHA256";
            }
        }

        LOGGER.debug("Using key derivation algorithm: {}", keyDerivationAlgorithm);

        //Derive the key using the specified algorithm.

        if(keyDerivationAlgorithm.equalsIgnoreCase("PBKDF2WithHmacSHA256")) {
            return derivePbkdf2Sha256Key(password, salt);
        }
        else if(keyDerivationAlgorithm.equalsIgnoreCase("SHA256")) {
            return deriveSha256Key(password, salt);
        }
        else {
            LOGGER.warn("Unsupported key derivation algorithm: {} Use fallback algorithm {}.", keyDerivationAlgorithm,
                    "PBKDF2WithHmacSHA256");

            return derivePbkdf2Sha256Key(password, salt);
        }
    }

    private static SecretKey deriveSha256Key(String password, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");

        messageDigest.update(password.getBytes(StandardCharsets.UTF_8));
        messageDigest.update(salt);

        byte[] hash = messageDigest.digest();

        return new SecretKeySpec(hash, "AES");
    }

    private static SecretKey derivePbkdf2Sha256Key(String password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

        KeySpec keySpec = new PBEKeySpec(password.toCharArray(), salt, PBKDF2_ITERATION_COUNT, AES_KEY_BITS_COUNT);

        SecretKey secretKey = secretKeyFactory.generateSecret(keySpec);

        return new SecretKeySpec(secretKey.getEncoded(), "AES");
    }

    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_BYTES_COUNT];

        SECURE_RANDOM.nextBytes(salt);

        return salt;
    }

    private static byte[] generateIV() {
        byte[] iv = new byte[IV_BYTES_COUNT];

        SECURE_RANDOM.nextBytes(iv);

        return iv;
    }
}