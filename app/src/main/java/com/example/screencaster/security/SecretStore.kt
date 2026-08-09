package com.example.screencaster.security
import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
class SecretStore(private val context:Context){ private val alias="screencaster_stream_key"; private fun key():SecretKey{ val ks=KeyStore.getInstance("AndroidKeyStore").apply{load(null)}; (ks.getKey(alias,null) as? SecretKey)?.let{return it}; return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore").run{ init(KeyGenParameterSpec.Builder(alias,KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setUserAuthenticationRequired(false).build()); generateKey() } }
 fun save(value:String){ val c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.ENCRYPT_MODE,key()); val enc=c.doFinal(value.trim().toByteArray()); context.getSharedPreferences("secret",0).edit().putString("iv",android.util.Base64.encodeToString(c.iv,0)).putString("key",android.util.Base64.encodeToString(enc,0)).apply() }
 fun read():String?{ val p=context.getSharedPreferences("secret",0); val iv=p.getString("iv",null)?:return null; val enc=p.getString("key",null)?:return null; val c=Cipher.getInstance("AES/GCM/NoPadding"); c.init(Cipher.DECRYPT_MODE,key(),GCMParameterSpec(128,android.util.Base64.decode(iv,0))); return String(c.doFinal(android.util.Base64.decode(enc,0))) }
 fun clear(){ context.getSharedPreferences("secret",0).edit().clear().apply() }
}
