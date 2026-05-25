package com.clipvault.app.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.security.KeyStore
import javax.crypto.BadPaddingException
import javax.crypto.Cipher
import javax.crypto.IllegalBlockSizeException
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * CryptoManager handles AES-GCM encryption/decryption using Android Keystore.
 * Key alias: clipvault_aes_key
 * Format: Base64(IV[12] + CipherText + AuthTag[16])
 */
@Singleton
class CryptoManager @Inject constructor() {

    companion object {
        private const val TAG = "CryptoManager"
        private const val KEY_ALIAS = "clipvault_aes_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_SIZE = 128
    }

    private var keyStore: KeyStore? = null
    private var isInitialized = false

    @Synchronized
    private fun ensureInitialized() {
        if (isInitialized) return
        try {
            val ks = KeyStore.getInstance(ANDROID_KEYSTORE)
            ks.load(null)
            keyStore = ks
            if (!ks.containsAlias(KEY_ALIAS)) {
                generateKeyInternal()
            }
            isInitialized = true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize KeyStore, cryptography will be unavailable", e)
            isInitialized = false
        }
    }

    private fun generateKey() {
        ensureInitialized()
        generateKeyInternal()
    }

    private fun generateKeyInternal() {
        try {
            val keyGenerator = KeyGenerator.getInstance(
                KeyProperties.KEY_ALGORITHM_AES,
                ANDROID_KEYSTORE
            )
            val spec = KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .setIsStrongBoxBacked(true)
                .build()

            try {
                keyGenerator.init(spec)
                keyGenerator.generateKey()
            } catch (e: Exception) {
                // Fallback without StrongBox if not supported
                Log.w(TAG, "StrongBox not available, falling back to software keystore", e)
                val fallbackSpec = KeyGenParameterSpec.Builder(
                    KEY_ALIAS,
                    KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                )
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build()
                keyGenerator.init(fallbackSpec)
                keyGenerator.generateKey()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to generate key in Android Keystore", e)
        }
    }

    private fun getKey(): SecretKey? {
        ensureInitialized()
        val ks = keyStore ?: return null
        try {
            if (!ks.containsAlias(KEY_ALIAS)) {
                generateKeyInternal()
            }
            val entry = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting key from Keystore, attempting regeneration", e)
        }
        // If entry is null or exception occurred, regenerate key and try one more time
        generateKeyInternal()
        return try {
            val entry = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            entry?.secretKey
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve secret key after regeneration", e)
            null
        }
    }

    /**
     * Encrypts plaintext using AES-GCM.
     * Returns Base64(IV[12] + CipherText + AuthTag[16])
     */
    fun encrypt(plaintext: String): String {
        if (plaintext.isBlank()) return ""
        ensureInitialized()
        if (!isInitialized) {
            Log.e(TAG, "Cannot encrypt: CryptoManager is not initialized")
            return ""
        }

        return try {
            encryptInternal(plaintext)
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.w(TAG, "Key permanently invalidated during encryption, regenerating", e)
            handleKeyLoss()
            try {
                encryptInternal(plaintext)
            } catch (ex: Exception) {
                Log.e(TAG, "Encryption failed after key regeneration", ex)
                ""
            }
        } catch (e: Exception) {
            if (e.message?.contains("Key not found") == true ||
                e.message?.contains("unrecoverable") == true) {
                Log.w(TAG, "Key lost during encryption, regenerating", e)
                handleKeyLoss()
                try {
                    encryptInternal(plaintext)
                } catch (ex: Exception) {
                    Log.e(TAG, "Encryption failed after key regeneration", ex)
                    ""
                }
            } else {
                Log.e(TAG, "Encryption failed with unexpected error", e)
                ""
            }
        }
    }

    private fun encryptInternal(plaintext: String): String {
        val key = getKey() ?: throw IllegalStateException("Keystore key not available")
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)

        val iv = cipher.iv
        val ciphertext = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        // Combine IV + Ciphertext
        val combined = ByteArray(iv.size + ciphertext.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(ciphertext, 0, combined, iv.size, ciphertext.size)

        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    /**
     * Decrypts Base64(IV[12] + CipherText + AuthTag[16]) using AES-GCM.
     * Handles all crypto exceptions gracefully — never crashes, returns "" on failure.
     */
    fun decrypt(encryptedBase64: String): String {
        val trimmed = encryptedBase64.trim()
        if (trimmed.isBlank()) return ""
        ensureInitialized()
        if (!isInitialized) {
            Log.e(TAG, "Cannot decrypt: CryptoManager is not initialized")
            return ""
        }

        return try {
            // Try Base64.NO_WRAP first, fall back to DEFAULT if it fails
            val combined = try {
                Base64.decode(trimmed, Base64.NO_WRAP)
            } catch (e: IllegalArgumentException) {
                Base64.decode(trimmed, Base64.DEFAULT)
            }

            if (combined.size <= IV_SIZE) {
                Log.e(TAG, "Encrypted data too short: ${combined.size} bytes")
                return ""
            }

            // Extract IV (first 12 bytes)
            val iv = combined.copyOfRange(0, IV_SIZE)
            val ciphertext = combined.copyOfRange(IV_SIZE, combined.size)

            val key = getKey() ?: throw IllegalStateException("Keystore key not available")
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)

            val plaintext = cipher.doFinal(ciphertext)
            String(plaintext, Charsets.UTF_8)
        } catch (e: KeyPermanentlyInvalidatedException) {
            Log.w(TAG, "Key permanently invalidated, regenerating", e)
            handleKeyLoss()
            ""
        } catch (e: BadPaddingException) {
            // Data corrupted or wrong key — cannot recover
            Log.e(TAG, "Decryption failed: bad padding (data corrupted or key mismatch)", e)
            ""
        } catch (e: IllegalBlockSizeException) {
            // Data corrupted
            Log.e(TAG, "Decryption failed: illegal block size (data corrupted)", e)
            ""
        } catch (e: Exception) {
            if (e.message?.contains("Key not found") == true ||
                e.message?.contains("unrecoverable") == true) {
                Log.w(TAG, "Key lost, regenerating", e)
                handleKeyLoss()
                ""
            } else {
                Log.e(TAG, "Decryption failed with unexpected error", e)
                ""
            }
        }
    }

    /**
     * Handles key loss by deleting old key and regenerating.
     * All encrypted data will be lost.
     */
    private fun handleKeyLoss() {
        try {
            keyStore?.deleteEntry(KEY_ALIAS)
        } catch (_: Exception) {}
        generateKeyInternal()
    }

    /**
     * Deletes the encryption key. Use when clearing all encrypted data.
     */
    fun deleteKey() {
        ensureInitialized()
        try {
            keyStore?.deleteEntry(KEY_ALIAS)
        } catch (_: Exception) {}
        generateKeyInternal()
    }
}
