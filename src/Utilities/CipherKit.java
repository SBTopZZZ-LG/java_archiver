package Utilities;

import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public class CipherKit {
    public SecretKey key;
    public byte[] nonce;
    public char[] password;

    public CipherKit(byte[] nonce, String password) throws NoSuchAlgorithmException, InvalidKeySpecException {
        key = generateSecretKey(password, nonce);
        this.nonce = nonce;
        this.password = password.toCharArray();
    }

    public static SecretKey generateSecretKey(String password, byte[] nonce) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), nonce, 65536, 128); // AES-128
        SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
        byte[] key = secretKeyFactory.generateSecret(spec).getEncoded();

        return new SecretKeySpec(key, "AES");
    }

    public enum CipherMode {
        ENCRYPT,
        DECRYPT
    }
    public byte[] exec(byte[] bytes, CipherMode mode) throws InvalidAlgorithmParameterException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, IllegalBlockSizeException, BadPaddingException {
        return buildCipher(key, nonce, mode).doFinal(bytes);
    }
    public static Cipher buildCipher(SecretKey key, byte[] nonce, CipherMode mode) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        GCMParameterSpec parameterSpec = new GCMParameterSpec(128, nonce);

        cipher.init(mode == CipherMode.ENCRYPT ? Cipher.ENCRYPT_MODE : Cipher.DECRYPT_MODE, key, parameterSpec);

        return cipher;
    }

    public static byte[] generateNonce(int nonceLength) {
        byte[] nonce = new byte[nonceLength];
        new SecureRandom().nextBytes(nonce);

        return nonce;
    }
}
