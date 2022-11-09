package com.kgg.android.delivers

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.kgg.android.delivers.loginActivity.loginActivity


class IntroActivity : AppCompatActivity() {

    val SPLASH_TIME: Long = 2000

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_intro)

        val pref = getSharedPreferences("isFirst", AppCompatActivity.MODE_PRIVATE)
        val first = pref.getString("isFirst", "none")


        // 최초실행인지 확인하는
        if (first == "none" ) { //최초 실행이면 로그인
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
                startActivity(Intent(this, MainFragment::class.java))
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                finish()

            }, SPLASH_TIME)
        }
    }
}