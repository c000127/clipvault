package com.clipvault.app.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
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
        private const val KEY_ALIAS = "clipvault_aes_key"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val IV_SIZE = 12
        private const val TAG_SIZE = 128
    }

    private val keyStore: KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }

    init {
        if (!keyStore.containsAlias(KEY_ALIAS)) {
            generateKey()
        }
    }

    private fun generateKey() {
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
    }

    private fun getKey(): SecretKey {
        val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            ?: throw IllegalStateException("Key not found in Keystore")
        return entry.secretKey
    }

    /**
     * Encrypts plaintext using AES-GCM.
     * Returns Base64(IV[12] + CipherText + AuthTag[16])
     */
    fun encrypt(plaintext: String): String {
        if (plaintext.isBlank()) return ""

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getKey())

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
     * Handles key loss by deleting old key and regenerating.
     */
    fun decrypt(encryptedBase64: String): String {
        if (encryptedBase64.isBlank()) return ""

        return try {
            val combined = Base64.decode(encryptedBase64, Base64.NO_WRAP)

            // Extract IV (first 12 bytes)
            val iv = combined.copyOfRange(0, IV_SIZE)
            val ciphertext = combined.copyOfRange(IV_SIZE, combined.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(TAG_SIZE, iv)
            cipher.init(Cipher.DECRYPT_MODE, getKey(), spec)

            val plaintext = cipher.doFinal(ciphertext)
            String(plaintext, Charsets.UTF_8)
        } catch (e: KeyPermanentlyInvalidatedException) {
            // Key was invalidated (e.g., biometric change)
            handleKeyLoss()
            ""
        } catch (e: Exception) {
            // Try to handle key loss for other key-related errors
            if (e.message?.contains("Key not found") == true ||
                e.message?.contains("unrecoverable") == true) {
                handleKeyLoss()
                ""
            } else {
                throw e
            }
        }
    }

    /**
     * Handles key loss by deleting old key and regenerating.
     * All encrypted data will be lost.
     */
    private fun handleKeyLoss() {
        try {
            keyStore.deleteEntry(KEY_ALIAS)
        } catch (_: Exception) {}
        generateKey()
    }

    /**
     * Deletes the encryption key. Use when clearing all encrypted data.
     */
    fun deleteKey() {
        keyStore.deleteEntry(KEY_ALIAS)
        generateKey()
    }
}
