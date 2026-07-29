package com.capstone.houseviewingapp.data.local

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

object AuthTokenLocalStore {
    private const val PREF_NAME = "auth_token_local_pref"
    private const val KEY_ACCESS_TOKEN = "access_token"
    private const val KEY_REFRESH_TOKEN = "refresh_token"
    private const val KEY_LOGIN_ID = "login_id"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_ALIAS = "house_viewing_auth_token_key"
    private const val ENCRYPTED_PREFIX = "v1:"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_IV_LENGTH_BYTES = 12

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    fun getAccessToken(context: Context): String? =
        readEncrypted(context, KEY_ACCESS_TOKEN)

    fun getRefreshToken(context: Context): String? =
        readEncrypted(context, KEY_REFRESH_TOKEN)

    fun getOrCreateDeviceId(context: Context): String {
        readEncrypted(context, KEY_DEVICE_ID)?.let { return it }

        val deviceId = UUID.randomUUID().toString()
        writeEncrypted(context, KEY_DEVICE_ID, deviceId)
        return deviceId
    }

    fun saveTokens(context: Context, accessToken: String, refreshToken: String) {
        prefs(context).edit()
            .putString(KEY_ACCESS_TOKEN, encrypt(accessToken))
            .putString(KEY_REFRESH_TOKEN, encrypt(refreshToken))
            .apply()
    }

    fun saveLoginId(context: Context, loginId: String) {
        prefs(context).edit().putString(KEY_LOGIN_ID, loginId).apply()
    }

    fun getLoginId(context: Context): String? =
        prefs(context).getString(KEY_LOGIN_ID, null)

    fun clear(context: Context) {
        prefs(context).edit()
            .remove(KEY_ACCESS_TOKEN)
            .remove(KEY_REFRESH_TOKEN)
            .remove(KEY_LOGIN_ID)
            .apply()
    }

    private fun readEncrypted(context: Context, key: String): String? {
        val stored = prefs(context).getString(key, null) ?: return null
        return decrypt(stored)
    }

    private fun writeEncrypted(context: Context, key: String, value: String) {
        prefs(context).edit().putString(key, encrypt(value)).apply()
    }

    private fun encrypt(plainText: String): String {
        val iv = ByteArray(GCM_IV_LENGTH_BYTES).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return ENCRYPTED_PREFIX +
                Base64.encodeToString(iv, Base64.NO_WRAP) +
                ":" +
                Base64.encodeToString(cipherText, Base64.NO_WRAP)
    }

    private fun decrypt(stored: String): String {
        if (!stored.startsWith(ENCRYPTED_PREFIX)) {
            return stored
        }

        val parts = stored.removePrefix(ENCRYPTED_PREFIX).split(":")
        require(parts.size == 2) { "Invalid encrypted token format" }
        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val cipherText = Base64.decode(parts[1], Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateSecretKey(), GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
        return String(cipher.doFinal(cipherText), Charsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let {
            return it.secretKey
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }
}
