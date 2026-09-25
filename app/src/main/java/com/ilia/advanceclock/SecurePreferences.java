package com.ilia.advanceclock;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** Encrypts sensitive preference values with an app-private Android Keystore key. */
final class SecurePreferences {
    private static final String KEYSTORE = "AndroidKeyStore";
    private static final String KEY_ALIAS = "advance_clock_sensitive_data_v1";
    private static final String PREFIX = "enc:v1:";
    private static final int TAG_BITS = 128;

    private SecurePreferences() {}

    static synchronized String read(
            Context context, String preferencesName, String key, String defaultValue) {
        SharedPreferences preferences = context.getSharedPreferences(
                preferencesName, Context.MODE_PRIVATE);
        String stored = preferences.getString(key, null);
        if (stored == null) return defaultValue;
        if (!stored.startsWith(PREFIX)) {
            write(context, preferencesName, key, stored);
            return stored;
        }
        try {
            byte[] packed = Base64.decode(stored.substring(PREFIX.length()), Base64.NO_WRAP);
            if (packed.length <= 12) throw new IllegalStateException("Invalid encrypted value");
            byte[] iv = new byte[12];
            byte[] ciphertext = new byte[packed.length - iv.length];
            System.arraycopy(packed, 0, iv, 0, iv.length);
            System.arraycopy(packed, iv.length, ciphertext, 0, ciphertext.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(aad(preferencesName, key));
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (Exception error) {
            throw new IllegalStateException("Sensitive data could not be decrypted", error);
        }
    }

    static synchronized void write(
            Context context, String preferencesName, String key, String value) {
        try {
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key());
            cipher.updateAAD(aad(preferencesName, key));
            byte[] ciphertext = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
            byte[] iv = cipher.getIV();
            byte[] packed = new byte[iv.length + ciphertext.length];
            System.arraycopy(iv, 0, packed, 0, iv.length);
            System.arraycopy(ciphertext, 0, packed, iv.length, ciphertext.length);
            context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE).edit()
                    .putString(key, PREFIX + Base64.encodeToString(packed, Base64.NO_WRAP))
                    .apply();
        } catch (Exception error) {
            throw new IllegalStateException("Sensitive data could not be encrypted", error);
        }
    }

    private static SecretKey key() throws Exception {
        KeyStore store = KeyStore.getInstance(KEYSTORE);
        store.load(null);
        java.security.Key existing = store.getKey(KEY_ALIAS, null);
        if (existing instanceof SecretKey) return (SecretKey) existing;
        KeyGenerator generator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES, KEYSTORE);
        generator.init(new KeyGenParameterSpec.Builder(KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build());
        return generator.generateKey();
    }

    private static byte[] aad(String preferencesName, String key) {
        return (preferencesName + '\u0000' + key).getBytes(StandardCharsets.UTF_8);
    }
}
