package com.example.lean.core.datastore.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import com.example.lean.core.common.log.Log
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

/** Seals values that must not be readable off disk. A seam so tests run without a Keystore. */
interface ValueCipher {
    fun encrypt(plaintext: String): String

    /** The plaintext, or null when [encoded] cannot be decrypted with this device's key. */
    fun decrypt(encoded: String): String?
}

/** AES-256-GCM with a key held by the Android Keystore. */
@Singleton
internal class KeystoreCipher @Inject constructor() : ValueCipher {

    /**
     * Looked up once: each Keystore lookup is a binder call. Created under the lock, because two
     * first callers generating a key each would leave the first one's ciphertext unreadable.
     */
    @Volatile
    private var key: SecretKey? = null

    override fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)
    }

    override fun decrypt(encoded: String): String? = try {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        require(combined.size > IV_LENGTH) { "Ciphertext shorter than its IV." }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            secretKey(),
            GCMParameterSpec(TAG_LENGTH_BITS, combined, 0, IV_LENGTH),
        )
        String(cipher.doFinal(combined, IV_LENGTH, combined.size - IV_LENGTH), Charsets.UTF_8)
    } catch (unreadable: Exception) {
        // Exception rather than GeneralSecurityException: some OEM Keystores throw their own types.
        Log.w("Stored value could not be decrypted", unreadable, TAG)
        key = null
        null
    }

    private fun secretKey(): SecretKey = key ?: synchronized(this) {
        key ?: loadOrCreate().also { key = it }
    }

    private fun loadOrCreate(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                // No setUserAuthenticationRequired: these values are read at cold start, before any
                // prompt can show.
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val TAG = "KeystoreCipher"
        const val PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "lean_datastore_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val IV_LENGTH = 12
        const val TAG_LENGTH_BITS = 128
    }
}
