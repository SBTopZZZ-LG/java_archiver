import Utilities.CipherKit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.crypto.AEADBadTagException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive unit tests for CipherKit encryption/decryption functionality
 */
class CipherKitTest {

    private byte[] testNonce;
    private String testPassword;

    @BeforeEach
    void setUp() {
        testNonce = CipherKit.generateNonce(12);
        testPassword = "testpass123";
    }

    @Test
    @DisplayName("CipherKit should generate valid nonce")
    void testNonceGeneration() {
        byte[] nonce = CipherKit.generateNonce(12);
        
        assertThat(nonce).isNotNull();
        assertThat(nonce).hasSize(12);
        
        // Generate another nonce and ensure they're different (highly likely)
        byte[] anotherNonce = CipherKit.generateNonce(12);
        assertThat(nonce).isNotEqualTo(anotherNonce);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 12, 16, 32, 64})
    @DisplayName("CipherKit should generate nonces of various sizes")
    void testNonceGenerationVariousSizes(int size) {
        byte[] nonce = CipherKit.generateNonce(size);
        
        assertThat(nonce).isNotNull();
        assertThat(nonce).hasSize(size);
    }

    @Test
    @DisplayName("CipherKit should handle zero size nonce")
    void testNonceGenerationZeroSize() {
        byte[] nonce = CipherKit.generateNonce(0);
        
        assertThat(nonce).isNotNull();
        assertThat(nonce).hasSize(0);
    }

    @Test
    @DisplayName("CipherKit should handle negative nonce size gracefully")
    void testNonceGenerationNegativeSize() {
        assertThatThrownBy(() -> CipherKit.generateNonce(-1))
            .isInstanceOf(NegativeArraySizeException.class);
    }

    @Test
    @DisplayName("CipherKit should create instance with valid password")
    void testCipherKitCreation() throws NoSuchAlgorithmException, InvalidKeySpecException {
        CipherKit kit = new CipherKit(testNonce, testPassword);
        
        assertThat(kit).isNotNull();
        assertThat(kit.nonce).isEqualTo(testNonce);
    }

    @ParameterizedTest
    @ValueSource(strings = {"123456", "password", "verylongpassword", "pass!@#$%^&*()"})
    @DisplayName("CipherKit should handle various valid passwords")
    void testCipherKitCreationVariousPasswords(String password) throws Exception {
        CipherKit kit = new CipherKit(testNonce, password);
        
        assertThat(kit).isNotNull();
        assertThat(kit.nonce).isEqualTo(testNonce);
    }

    @Test
    @DisplayName("CipherKit should handle null nonce")
    void testCipherKitCreationNullNonce() {
        assertThatThrownBy(() -> new CipherKit(null, testPassword))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("CipherKit should handle null password")
    void testCipherKitCreationNullPassword() {
        assertThatThrownBy(() -> new CipherKit(testNonce, null))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("CipherKit should handle empty password")
    void testCipherKitCreationEmptyPassword() throws Exception {
        // Empty password is actually allowed, just creates a weak key
        CipherKit kit = new CipherKit(testNonce, "");
        assertThat(kit).isNotNull();
        
        // Test encryption/decryption still works
        byte[] data = "test".getBytes();
        byte[] encrypted = kit.exec(data, CipherKit.CipherMode.ENCRYPT);
        byte[] decrypted = kit.exec(encrypted, CipherKit.CipherMode.DECRYPT);
        assertThat(decrypted).isEqualTo(data);
    }

    @Test
    @DisplayName("CipherKit should encrypt and decrypt data correctly")
    void testEncryptDecrypt() throws Exception {
        CipherKit kit = new CipherKit(testNonce, testPassword);
        byte[] originalData = "Hello, World!".getBytes();
        
        byte[] encryptedData = kit.exec(originalData, CipherKit.CipherMode.ENCRYPT);
        byte[] decryptedData = kit.exec(encryptedData, CipherKit.CipherMode.DECRYPT);
        
        assertThat(encryptedData).isNotEqualTo(originalData);
        assertThat(decryptedData).isEqualTo(originalData);
        assertThat(new String(decryptedData)).isEqualTo("Hello, World!");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "a", "short", "medium length text", "very long text that spans multiple lines and contains various characters including numbers 12345 and symbols !@#$%^&*()"})
    @DisplayName("CipherKit should handle various data sizes for encryption/decryption")
    void testEncryptDecryptVariousDataSizes(String input) throws Exception {
        CipherKit kit = new CipherKit(testNonce, testPassword);
        byte[] originalData = input.getBytes();
        
        byte[] encryptedData = kit.exec(originalData, CipherKit.CipherMode.ENCRYPT);
        byte[] decryptedData = kit.exec(encryptedData, CipherKit.CipherMode.DECRYPT);
        
        assertThat(decryptedData).isEqualTo(originalData);
        assertThat(new String(decryptedData)).isEqualTo(input);
    }

    @Test
    @DisplayName("CipherKit should handle binary data encryption/decryption")
    void testEncryptDecryptBinaryData() throws Exception {
        CipherKit kit = new CipherKit(testNonce, testPassword);
        byte[] originalData = new byte[]{0, 1, 2, 3, 127, -1, -128, 64, -64};
        
        byte[] encryptedData = kit.exec(originalData, CipherKit.CipherMode.ENCRYPT);
        byte[] decryptedData = kit.exec(encryptedData, CipherKit.CipherMode.DECRYPT);
        
        assertThat(encryptedData).isNotEqualTo(originalData);
        assertThat(decryptedData).isEqualTo(originalData);
    }

    @Test
    @DisplayName("CipherKit should handle large data encryption/decryption")
    void testEncryptDecryptLargeData() throws Exception {
        CipherKit kit = new CipherKit(testNonce, testPassword);
        byte[] originalData = new byte[100000]; // 100KB
        for (int i = 0; i < originalData.length; i++) {
            originalData[i] = (byte) (i % 256);
        }
        
        byte[] encryptedData = kit.exec(originalData, CipherKit.CipherMode.ENCRYPT);
        byte[] decryptedData = kit.exec(encryptedData, CipherKit.CipherMode.DECRYPT);
        
        assertThat(encryptedData).isNotEqualTo(originalData);
        assertThat(decryptedData).isEqualTo(originalData);
    }

    @Test
    @DisplayName("CipherKit should produce different ciphertext for same data with different nonces")
    void testDifferentNoncesProduceDifferentCiphertext() throws Exception {
        byte[] nonce1 = CipherKit.generateNonce(12);
        byte[] nonce2 = CipherKit.generateNonce(12);
        byte[] originalData = "Same data".getBytes();
        
        CipherKit kit1 = new CipherKit(nonce1, testPassword);
        CipherKit kit2 = new CipherKit(nonce2, testPassword);
        
        byte[] encrypted1 = kit1.exec(originalData, CipherKit.CipherMode.ENCRYPT);
        byte[] encrypted2 = kit2.exec(originalData, CipherKit.CipherMode.ENCRYPT);
        
        assertThat(encrypted1).isNotEqualTo(encrypted2);
    }

    @Test
    @DisplayName("CipherKit should fail decryption with wrong password")
    void testDecryptionWithWrongPassword() throws Exception {
        CipherKit encryptKit = new CipherKit(testNonce, testPassword);
        CipherKit decryptKit = new CipherKit(testNonce, "wrongpassword");
        
        byte[] originalData = "Secret data".getBytes();
        byte[] encryptedData = encryptKit.exec(originalData, CipherKit.CipherMode.ENCRYPT);
        
        assertThatThrownBy(() -> decryptKit.exec(encryptedData, CipherKit.CipherMode.DECRYPT))
            .isInstanceOf(AEADBadTagException.class);
    }

    @Test
    @DisplayName("CipherKit should fail decryption with wrong nonce")
    void testDecryptionWithWrongNonce() throws Exception {
        byte[] wrongNonce = CipherKit.generateNonce(12);
        
        CipherKit encryptKit = new CipherKit(testNonce, testPassword);
        CipherKit decryptKit = new CipherKit(wrongNonce, testPassword);
        
        byte[] originalData = "Secret data".getBytes();
        byte[] encryptedData = encryptKit.exec(originalData, CipherKit.CipherMode.ENCRYPT);
        
        assertThatThrownBy(() -> decryptKit.exec(encryptedData, CipherKit.CipherMode.DECRYPT))
            .isInstanceOf(AEADBadTagException.class);
    }

    @Test
    @DisplayName("CipherKit should handle corrupted ciphertext gracefully")
    void testDecryptionWithCorruptedData() throws Exception {
        CipherKit kit = new CipherKit(testNonce, testPassword);
        byte[] originalData = "Secret data".getBytes();
        byte[] encryptedData = kit.exec(originalData, CipherKit.CipherMode.ENCRYPT);
        
        // Corrupt the ciphertext
        encryptedData[0] = (byte) ~encryptedData[0];
        
        assertThatThrownBy(() -> kit.exec(encryptedData, CipherKit.CipherMode.DECRYPT))
            .isInstanceOf(AEADBadTagException.class);
    }

    @Test
    @DisplayName("CipherKit should handle null data for encryption")
    void testEncryptionWithNullData() throws Exception {
        CipherKit kit = new CipherKit(testNonce, testPassword);
        
        assertThatThrownBy(() -> kit.exec(null, CipherKit.CipherMode.ENCRYPT))
            .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("CipherKit should handle empty data for encryption")
    void testEncryptionWithEmptyData() throws Exception {
        CipherKit kit = new CipherKit(testNonce, testPassword);
        byte[] emptyData = new byte[0];
        
        byte[] encryptedData = kit.exec(emptyData, CipherKit.CipherMode.ENCRYPT);
        byte[] decryptedData = kit.exec(encryptedData, CipherKit.CipherMode.DECRYPT);
        
        assertThat(decryptedData).isEqualTo(emptyData);
        assertThat(decryptedData).hasSize(0);
    }

    @Test
    @DisplayName("CipherKit should maintain consistent encryption/decryption across multiple operations")
    void testConsistentMultipleOperations() throws Exception {
        CipherKit kit = new CipherKit(testNonce, testPassword);
        byte[] originalData = "Consistent test data".getBytes();
        
        // Perform multiple encrypt/decrypt cycles
        for (int i = 0; i < 5; i++) {
            byte[] encryptedData = kit.exec(originalData, CipherKit.CipherMode.ENCRYPT);
            byte[] decryptedData = kit.exec(encryptedData, CipherKit.CipherMode.DECRYPT);
            
            assertThat(decryptedData).isEqualTo(originalData);
        }
    }
}