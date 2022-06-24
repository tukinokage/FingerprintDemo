package com.example.fingerprintdemo

import android.app.KeyguardManager
import android.content.Context
import android.content.DialogInterface
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import android.util.Log
import android.view.View
import android.widget.Toast
import java.security.KeyStore
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import kotlin.jvm.Throws

/**
 * 普通本地加密,生物验证
 * todo:可考虑增加
 * API在android6.0上提供了支持
 * 6.0以上自定义
 * 9.0以上指纹对话框由系统提供
 * PACK com.example.fingerprintdemo
 * CREATE BY Shay
 * DATE BY 2022/1/17 18:21 星期一
 * <p>
 * DESCRIBE 根据自己需要修改，增加监听器等等
 * <p>
 */
// TODO:2022/1/17 
const val paySecret = "paySecret"
const val paySecretIv = "paySecretIv"
class FingerprintManager{
    companion object{
        private val context = MyApplication.context
        private val executor by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) { Executors.newSingleThreadExecutor() }
        @JvmStatic
        /**@param initPurpose KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT*/
        fun showBiometricPrompDialog(initPurpose :Int, fingerCallback: FingerCallback?){
            var keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (!keyguardManager.isKeyguardSecure){//判断有无绑定指纹密码

            }
            val codeHashMap = HashMap<String, String>()
            /**获得加密的数据和iv后，本地保存起来。需要用的时候，再根据SecretKey、iv初始化出cipher，进行解密cipher.doFinal(byte[] input)*/
            /**为什么这样安全？ -->
             * 因为Cipher根据SecretKey和IV初始化出来，而SecretKey由KeyStore保存在Android系统中，
             * IV由指纹验证自动生成。由这两者生成出来的Cipher进行加解密，即可保证安全。*/
            val enPayCryptedCallback =object : BiometricPrompt.AuthenticationCallback() {//不同系统
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                var entryCrypted = result?.cryptoObject?.cipher?.doFinal("THISISPSW".toByteArray())//byte[]数据向量,加密后得内容
                val iv: ByteArray? = result?.cryptoObject?.cipher?.iv   //加密IV,不同的IV加密后的字符串是不同的，加密和解密需要相同的IV
                Log.i("test", Base64.encodeToString(entryCrypted, Base64.URL_SAFE))
                codeHashMap[paySecret] = Base64.encodeToString(entryCrypted, Base64.URL_SAFE)
                codeHashMap[paySecretIv] = Base64.encodeToString(iv, Base64.URL_SAFE)
                SharedPrenfencesUtil.spInstance.saveString(context, paySecret, codeHashMap[paySecret])
                SharedPrenfencesUtil.spInstance.saveString(context, paySecretIv, codeHashMap[paySecretIv])
                fingerCallback?.onSuccess("设置成功")
                }

                //不同品牌手机取消指纹可能会回调不同方法
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    fingerCallback?.onSuccess("设置失败")
                }

                //当遇到不可恢复的错误并且操作完成时调用。一般是错误次数超过系统预设的次数（一般是5次）或者其他原因导致的识别失败都会进入这个函数。
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    fingerCallback?.onSuccess(errString as String)
                }
            }


            val dePayCryptedCallback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    val cipher = result?.cryptoObject?.cipher
                    //Log.d(this.javaClass.simpleName, Base64.encodeToString())
                    var secretKey = SharedPrenfencesUtil.spInstance.getString(context, paySecret)
                    var doFinal = cipher?.doFinal(Base64.decode(secretKey, Base64.URL_SAFE))
                    if(doFinal == null){
                        Log.d(FingerprintManager.javaClass.simpleName, "the secret is n-one")
                        fingerCallback?.onSuccess(" 没有密码")
                    }else{
                        Log.d(FingerprintManager.javaClass.simpleName, "secret is "+String(doFinal))
                        fingerCallback?.onSuccess(String(doFinal))
                    }

                }
                //不同品牌手机取消指纹可能会回调不同方法
                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Log.e(FingerprintManager.javaClass.simpleName, "fingerPrintFailed")
                    fingerCallback?.onFailed("fingerPrintFailed")
                }

                //当遇到不可恢复的错误并且操作完成时调用。一般是错误次数超过系统预设的次数（一般是5次）或者其他原因导致的识别失败都会进入这个函数。
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    Log.e(errorCode.toString(), errString as String)
                    fingerCallback?.onFailed(errString)
                }
            }
            try {
                when(initPurpose){
                    KeyProperties.PURPOSE_DECRYPT ->{
                        authenticationFingerprint(KeyProperties.PURPOSE_DECRYPT, dePayCryptedCallback, SharedPrenfencesUtil.spInstance.getString(context, paySecretIv))
                    }
                    KeyProperties.PURPOSE_ENCRYPT ->{
                        authenticationFingerprint( KeyProperties.PURPOSE_ENCRYPT, enPayCryptedCallback, null)
                    }
                }
            }catch (e:Exception){
                e.message?.let { Log.e(FingerprintManager.javaClass.simpleName, it) }
            }
        }

        //验证
        @JvmStatic
        @Throws(Exception::class)
        /**
         * @param initPurpose KeyProperties.PURPOSE_ENCRYPT or KeyProperties 用于判断解码逻辑还是加密逻辑
         * @param iv 解密传入存储在本地的iv, 加密时为null
         * */
        private fun authenticationFingerprint(initPurpose :Int,  callback:BiometricPrompt.AuthenticationCallback, iv:String?){//

            var biometricPrompt = BiometricPrompt.Builder(context).setTitle("指纹测试")
                .setNegativeButton(" 测试返回", Executors.newSingleThreadExecutor()) { dialog, which ->
                    Toast.makeText(context, "已关闭", Toast.LENGTH_SHORT).show()
                }
                .build()
            val cipher = when(initPurpose){
                KeyProperties.PURPOSE_ENCRYPT ->{ SceretManager.getEnctyptCipher(KEYSTROKENAMES.FingerPrintKey ) }//创建密钥
                KeyProperties.PURPOSE_DECRYPT ->{
                    SceretManager.getDectyptCipher(
                        KEYSTROKENAMES.FingerPrintKey,
                        iv)}//IV用于解密
                else ->{throw Exception("HAS NOT CRYPT MODE")}
            }
            if(cipher != null){
                biometricPrompt.authenticate(
                    BiometricPrompt.CryptoObject(cipher),
                    CancellationSignal(),
                    executor,
                    callback)
            }else {
                throw Exception("HAS NOT SECRET KEY")
            }

            /*var biometricPrompt = BiometricPrompt.Builder(context).setTitle("")
                .setNegativeButton("按下指纹", executor, object : View.OnClickListener,
                    DialogInterface.OnClickListener {
                    override fun onClick(v: View?) {
                        Toast.makeText(context, "开始取消", Toast.LENGTH_SHORT)
                    }

                    override fun onClick(dialog: DialogInterface?, which: Int) {
                        TODO("Not yet implemented")
                    }

                }).build()

            biometricPrompt.authenticate()*/

//            KeyGenParameterSpec.Builder("TEST", initPurpose).build()

        }



        //    isHardwareDetected() 判断是否有硬件支持
        //    isKeyguardSecure() 判断是否设置锁屏，因为一个手机最少要有两种登录方式
        //    hasEnrolledFingerprints() 判断系统中是否添加至少一个指纹
        /*@JvmStatic
        fun checkIsSupport(context: Context):FingerPrintInitType{
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M){
                return FingerPrintInitType.NOT_SUPPORT
            } else if (BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE){

            }
        }*/
    }
   public interface FingerCallback{
        fun onSuccess(msg:String);
        fun onFailed(msg:String);
    }


    enum class FingerPrintInitType{
        NOT_SUPPORT,//不支持
        NOT_FINGER_HARDWARE,//无指纹硬件
        NONE_FINGER,//手机没有录入指纹
        HAS_FINGER;//录入指纹
    }
}