package com.example.fingerprintdemo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.security.keystore.KeyProperties
import android.widget.Button
import android.widget.TextView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val enButton =  findViewById<Button>(R.id.button)
        val deButton =  findViewById<Button>(R.id.button2)
        val textview = findViewById<TextView>(R.id.textview)
        enButton.setOnClickListener {
            textview.text = "加密"
            SceretManager.generateKey(KEYSTROKENAMES.FingerPrintKey)
            FingerprintManager.showBiometricPrompDialog(KeyProperties.PURPOSE_ENCRYPT, object :FingerprintManager.FingerCallback{
                override fun onSuccess(msg: String) {
                    runOnUiThread{
                        textview.text = msg
                    }
                }

                override fun onFailed(msg: String) {
                    runOnUiThread{
                        textview.text = msg
                    }
                }

            })
        }
        deButton.setOnClickListener{
            FingerprintManager.showBiometricPrompDialog(KeyProperties.PURPOSE_DECRYPT, object :FingerprintManager.FingerCallback{
                override fun onSuccess(msg: String) {
                    runOnUiThread{
                        textview.text = "密码$msg"
                    }
                }

                override fun onFailed(msg: String) {
                    runOnUiThread{
                        textview.text = msg
                    }
                }

            })}
    }
}