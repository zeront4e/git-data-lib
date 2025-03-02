package io.github.zeront4e.gdl;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

public class AesEncryptionUtilTest {
    @Test
    public void shouldSuccessfullyEncryptAndDecryptStringWithValidPassword() throws Exception {
        // Given
        String originalText = "This is a secret message that needs to be encrypted";
        String password = "SuperSecretPassword123!";

        // When
        String encryptedText = AesEncryptionUtil.encrypt(originalText, password);
        String decryptedText = AesEncryptionUtil.decrypt(encryptedText, password);

        // Then
        assertNotNull(encryptedText, "Encrypted text should not be null");
        assertNotEquals(originalText, encryptedText, "Encrypted text should be different from original text");
        assertEquals(originalText, decryptedText, "Decrypted text should match the original text");
    }
    
    @Test
    public void shouldThrowExceptionWhenDecryptingWithIncorrectPassword() throws Exception {
        // Given
        String originalText = "This is a secret message that needs to be encrypted";
        String correctPassword = "CorrectPassword123";
        String incorrectPassword = "WrongPassword456";
        
        // When
        String encryptedText = AesEncryptionUtil.encrypt(originalText, correctPassword);
        
        // Then
        assertThrows(Exception.class, () -> {
            AesEncryptionUtil.decrypt(encryptedText, incorrectPassword);
        }, "Decryption with incorrect password should throw an exception");
    }
    
    @Test
    public void shouldSuccessfullyEncryptAndDecryptEmptyString() throws Exception {
        // Given
        String emptyText = "";
        String password = "ValidPassword123!";
        
        // When
        String encryptedText = AesEncryptionUtil.encrypt(emptyText, password);
        String decryptedText = AesEncryptionUtil.decrypt(encryptedText, password);
        
        // Then
        assertNotNull(encryptedText, "Encrypted text should not be null");
        assertNotEquals(emptyText, encryptedText, "Encrypted text should be different from empty text");
        assertEquals(emptyText, decryptedText, "Decrypted text should be an empty string");
    }
    
    @Test
    public void shouldSuccessfullyEncryptAndDecryptByteArraysDirectly() throws Exception {
        // Given
        byte[] originalData = "This is a binary message to be encrypted".getBytes(StandardCharsets.UTF_8);
        String password = "StrongByteArrayPassword123!";
        
        // When
        byte[] encryptedData = AesEncryptionUtil.encrypt(originalData, password);
        byte[] decryptedData = AesEncryptionUtil.decrypt(encryptedData, password);
        
        // Then

        assertNotNull(encryptedData, "Encrypted data should not be null");

        assertNotEquals(new String(originalData), new String(encryptedData),
                "Encrypted data should be different from original data");

        assertTrue(encryptedData.length > originalData.length,
                "Encrypted data should be longer due to salt and IV");

        assertArrayEquals(originalData, decryptedData, "Decrypted data should match the original data");
    }

    @Test
    public void shouldGenerateDifferentCiphertextForSamePlaintextDueToRandomSaltAndIV() throws Exception {
        // Given
        String plaintext = "This is a test message";
        String password = "TestPassword123";

        // When
        String firstEncryption = AesEncryptionUtil.encrypt(plaintext, password);
        String secondEncryption = AesEncryptionUtil.encrypt(plaintext, password);

        // Then

        assertNotNull(firstEncryption, "First encryption should not be null");

        assertNotNull(secondEncryption, "Second encryption should not be null");

        assertNotEquals(firstEncryption, secondEncryption,
                "Encrypting the same text twice should produce different results due to random salt/IV");

        // Verify both can be decrypted correctly

        String firstDecryption = AesEncryptionUtil.decrypt(firstEncryption, password);
        String secondDecryption = AesEncryptionUtil.decrypt(secondEncryption, password);

        assertEquals(plaintext, firstDecryption, "First decryption should match the original text");
        assertEquals(plaintext, secondDecryption, "Second decryption should match the original text");
    }
}