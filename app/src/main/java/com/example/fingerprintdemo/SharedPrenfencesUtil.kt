package com.example.fingerprintdemo

import android.content.Context
import android.content.SharedPreferences

/**
 * PACK com.example.fingerprintdemo
 * CREATE BY Shay
 * DATE BY 2022/6/21 15:58 星期二
 * <p>
 * DESCRIBE
 *
 * <p>
 */
// TODO:2022/6/21 
const val defaultSpName = "Data"
class SharedPrenfencesUtil private constructor(){
    companion object{
        val spInstance:SharedPrenfencesUtil by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) { SharedPrenfencesUtil() }
    }

    fun getString(context: Context, keyStr: String): String? {
        var sharedPreferences = context.getSharedPreferences(defaultSpName, Context.MODE_PRIVATE)
        return sharedPreferences.getString(keyStr, "")
    }

    fun saveString(context: Context, keyStr: String, valueStr:String?){
        val edit = context.getSharedPreferences(defaultSpName, Context.MODE_PRIVATE).edit()
        if (valueStr == null){
            edit.putString(keyStr, "");
        }else{
            edit.putString(keyStr, valueStr);
        }

        edit.apply()
    }
}