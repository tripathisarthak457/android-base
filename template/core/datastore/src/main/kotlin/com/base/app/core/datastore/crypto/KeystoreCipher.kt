package com.base.app.core.datastore.crypto

import android.security.keystore.KeyGenParameterSpec
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
 * AES-256-GCM through the Android Keystore, for the handful of values that must not be readable
 * off disk — access and refresh tokens, chiefly.
 *
 * ## Why not just trust app-private storage
 *
 * A DataStore file lives in the app's private directory, which is unreadable on a stock device
 * and trivially readable on a rooted one, on an emulator, or through any backup that captures
 * app data. Encrypting with a key that never leaves the Keystore's hardware-backed store means
 * the file on disk is useless on its own.
 *
 * ## The IV is stored with the ciphertext
 *
 * GCM needs a unique initialisation vector per encryption, and reusing one with the same key is
 * a catastrophic failure — it leaks the plaintext relationship between the two messages. The
 * cipher generates a fresh IV each time and it is prefixed to the output, which is standard and
 * safe: an IV is not secret, it only has to be unique.
 *
 * ## Decryption failure returns null rather than throwing
 *
 * The key is invalidated by events outside the app's control: the user adding or removing a
 * screen lock on some OEM builds, a restore onto a different device, a Keystore corruption. The
 * correct response is "this session is gone, sign in again", not a crash loop on launch that only
 * a reinstall escapes.
 */
@Singleton
class KeystoreCipher @Inject constructor() {

    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey())
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        val combined = cipher.iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encoded: String): String? = runCatching {
        val combined = Base64.decode(encoded, Base64.NO_WRAP)
        require(combined.size > IV_LENGTH) { "Ciphertext shorter than its IV." }

        val iv = combined.copyOfRange(0, IV_LENGTH)
        val payload = combined.copyOfRange(IV_LENGTH, combined.size)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, secretKey(), GCMParameterSpec(TAG_LENGTH_BITS, iv))
        String(cipher.doFinal(payload), Charsets.UTF_8)
    }.getOrNull()

    private fun secretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(KEY_SIZE_BITS)
                // Deliberately *not* setUserAuthenticationRequired: these values are read during
                // cold start, before any screen exists to prompt on, and requiring authentication
                // would make the app unusable until the user happened to unlock at the right time.
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    private companion object {
        const val PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "base_app_datastore_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_SIZE_BITS = 256
        const val IV_LENGTH = 12
        const val TAG_LENGTH_BITS = 128
    }
}
