package com.example.fingerprintdemo

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import java.lang.Exception
import java.security.KeyStore
import java.util.logging.Logger
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import kotlin.jvm.Throws

/**
 * PACK com.example.fingerprintdemo
 * CREATE BY Shay
 * DATE BY 2022/1/27 14:25 星期四
 * <p>
 * DESCRIBE
 * keystore加密
 * <p>
 */
// TODO:2022/1/27
class SceretManager(){

    companion object{
        val mKeyStore:KeyStore by lazy { KeyStore.getInstance("AndroidKeyStore") }
        @JvmStatic
        @Throws(Exception::class)
        /**不存在key时在keystore创建key
         * @param keyStoreAlias 新建的keyname, 使用者自命名
         * */
        fun generateKey(keystrokenames: KEYSTROKENAMES){
            try {
                val generator =
                    KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
                val mKeyStore:KeyStore = KeyStore.getInstance("AndroidKeyStore")
                mKeyStore.load(null)
                val purpose = KeyProperties.PURPOSE_DECRYPT or KeyProperties.PURPOSE_ENCRYPT
                val keyGenParametersBuilder = KeyGenParameterSpec.Builder(keystrokenames.keyalias, purpose)
                generator.init(keyGenParametersBuilder
                    .setUserAuthenticationRequired(true)
                    .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
                    .build())
                generator.generateKey()
            }catch (e:Exception){
                Log.e("aksERROR", e.toString())
               throw e
            }
        }

        //
        /**获取用于加解密的cipher
         * @param keystrokenames keyname
         * @param cryptoMODE 解/加密 Cipher.DECRYPT_MODE or Cipher.ENCRYPT_MODE
         * @param iv IvParameterSpec
         * */
        private fun getCipher(keystrokenames: KEYSTROKENAMES, cryptoMODE: Int, iv:IvParameterSpec?):Cipher?{
            mKeyStore.load(null)
            val key = mKeyStore.getKey(keystrokenames.keyalias, null)
            var cipher:Cipher? = null
            key?.let {

                /*AES分为几种模式，比如ECB，CBC，CFB等等，这些模式除了ECB由于没有使用IV而不太安全，其他模式差别并没有太明显，
                大部分的区别在IV和KEY来计算密文的方法略有区别。*/
                cipher =
                    Cipher.getInstance(KeyProperties.KEY_ALGORITHM_AES + "/"
                            + KeyProperties.BLOCK_MODE_CBC + "/"
                            + KeyProperties.ENCRYPTION_PADDING_PKCS7)
                if (iv == null){
                    cipher?.init(cryptoMODE, it)
                }else{
                    cipher?.init(cryptoMODE, it, iv)
                }
            }
            return cipher
        }

        /**获取用于加密的cipher
         * @param keystrokenames keyname
         * @param
         * */
        fun getEnctyptCipher(keystrokenames: KEYSTROKENAMES):Cipher?{
            return getCipher(keystrokenames, Cipher.ENCRYPT_MODE, null)
        }

        /**获取用于解密的cipher
         * @param keystrokenames keyname
         * @param iv 已保存的scertIV
         * @param iv String 加密时产生的向量
         * */
        fun getDectyptCipher(keystrokenames: KEYSTROKENAMES, iv: String?):Cipher?{
           return getCipher(keystrokenames, Cipher.DECRYPT_MODE, IvParameterSpec(Base64.decode(iv, Base64.URL_SAFE)))
        }

    }
}

enum class KEYSTROKENAMES(val keyalias:String){
    FingerPrintKey("FingerPrintKey")
}
