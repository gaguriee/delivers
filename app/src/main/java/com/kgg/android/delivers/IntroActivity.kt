package com.kgg.android.delivers

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat.startActivity
import com.google.firebase.auth.FirebaseAuth
import com.kgg.android.delivers.loginActivity.loginActivity
import com.kgg.android.delivers.loginActivity.userinfoActivity


// 가경
// 앱 실행 시 페이지 전환 알고리즘

class IntroActivity : AppCompatActivity() {

    val SPLASH_TIME: Long = 2000
    private lateinit var auth: FirebaseAuth
    private lateinit var uid : String

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()

        val pref = getSharedPreferences("isFirst", AppCompatActivity.MODE_PRIVATE)
        val first = pref.getString("isFirst", "none")
        Log.d("current uid?" , uid )

        // 최초실행인지 확인하는
        if (first == "none" || uid == "null" ) { //최초 실행이거나 로그인 상태가 아니면 로그인
            val editor = pref.edit()
            editor.putString("isFirst", "intro")
            editor.commit()
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, loginActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()

            }, SPLASH_TIME)
        } else{ //아니면 바로 메인페이지로
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, MainActivity::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()

            }, SPLASH_TIME)
        }
    }
}