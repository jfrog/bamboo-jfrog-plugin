package org.jfrog.bamboo.config;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bouncycastle.util.encoders.Base32;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.annotation.concurrent.ThreadSafe;
import javax.crypto.*;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

@ThreadSafe
public class EncryptionHelper {

    private static final Logger log = LogManager.getLogger(EncryptionHelper.class);
    private static final String AES_ENCRYPTION_SCHEME = "AES";
    private static final int KEY_LENGTH = 128;
    private static final String uiKey = generateRandomKey();
    private static final String dbKey = "Im54vK3LcFh3r8DCVqWUMw==";
    private static final ThreadLocal<Cipher> threadLocalEncrypter = ThreadLocal.withInitial(() -> {
        try {
            return Cipher.getInstance(AES_ENCRYPTION_SCHEME);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            log.error("Cannot create encrypter", e);
        }
        return null;
    });
    private static final ThreadLocal<Cipher> threadLocalDecrypter = ThreadLocal.withInitial(() -> {
        try {
            return Cipher.getInstance(AES_ENCRYPTION_SCHEME);
        } catch (NoSuchAlgorithmException | NoSuchPaddingException e) {
            log.error("Cannot create decrypter", e);
        }
        return null;
    });

    @NotNull
    public static String decrypt(@Nullable String data) {
        if (StringUtils.isEmpty(data)) {
            return "";
        }

        try {
            final byte[] encrypted = Base64.getMimeDecoder().decode(data);
            return decryptWithKey(dbKey, encrypted);
        } catch (Exception e) {
            try {
                final byte[] encrypted = Base32.decode(data);
                return decryptWithKey(uiKey, encrypted);
            } catch (Exception ee) {
                throw new RuntimeException("Failed to decrypt.", ee);
            }
        }
    }

    public static String decryptIfNeeded(String s) {
        try {
            s = decrypt(s);
        } catch (RuntimeException e) {
            // Ignore. The field may not be encrypted.
        }
        return s;
    }

    /**
     * Encrypts data with the constant DB key. Use this method to encrypt configuration data. For example, sensitive data which is meant
     * to be saved to the DB.
     * Use the 'decrypt' method for the opposite operation.
     *
     * @param stringToEncrypt - Nullable string to encrypt.
     * @return - Encrypted data.
     */
    @NotNull
    public static String encryptForConfig(@Nullable String stringToEncrypt) {
        if (StringUtils.isEmpty(stringToEncrypt)) {
            return "";
        }

        try {
            final byte[] encrypted = getEncrypter(dbKey).doFinal(stringToEncrypt.getBytes(StandardCharsets.UTF_8));
            return Base64.getMimeEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt.", e);
        }
    }

    /**
     * Encrypts data with the changing generated key. Use this method to encrypt sensitive data before it is presented in the UI.
     * Use the 'decrypt' method for the opposite operation.
     *
     * @param stringToEncrypt - Nullable string to encrypt.
     * @return - Encrypted data.
     */
    @NotNull
    public static String encryptForUi(@Nullable String stringToEncrypt) {
        if (StringUtils.isEmpty(stringToEncrypt)) {
            return "";
        }

        try {
            final byte[] encrypted = getEncrypter(uiKey).doFinal(stringToEncrypt.getBytes(StandardCharsets.UTF_8));
            return Base32.toBase32String(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt.", e);
        }
    }

    private static String decryptWithKey(String key, byte[] encrypted) throws InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        return new String(getDecrypter(key).doFinal(encrypted), StandardCharsets.UTF_8);
    }

    private static String generateRandomKey() {
        SecureRandom rnd = new SecureRandom();
        byte[] keyBytes = new byte[KEY_LENGTH / 8]; // Divide by 8 to convert bits to bytes
        rnd.nextBytes(keyBytes);
        return Base64.getEncoder().encodeToString(keyBytes);
    }

    private static SecretKey generateSecret(String key) {
        return new SecretKeySpec(Base64.getDecoder().decode(key), AES_ENCRYPTION_SCHEME);
    }

    private static Cipher getDecrypter(String key) throws InvalidKeyException {
        SecretKey secretKey = generateSecret(key);
        final Cipher decrypter = threadLocalDecrypter.get();
        decrypter.init(Cipher.DECRYPT_MODE, secretKey);
        return decrypter;
    }

    private static Cipher getEncrypter(String key) throws InvalidKeyException {
        SecretKey secretKey = generateSecret(key);
        final Cipher encrypter = threadLocalEncrypter.get();
        encrypter.init(Cipher.ENCRYPT_MODE, secretKey);
        return encrypter;
    }
}
