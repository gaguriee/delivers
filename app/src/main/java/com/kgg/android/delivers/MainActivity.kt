package com.kgg.android.delivers


import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.os.Bundle
import java.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.kgg.android.delivers.MainFragment
import com.kgg.android.delivers.UploadFragment
import com.kgg.android.delivers.MapSearchFragment
import com.kgg.android.delivers.R
import kotlinx.android.synthetic.main.activity_main.*
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.Base64.getEncoder

val fragmentMain by lazy { MainFragment() }
val fragmentUpload1 by lazy { MapSearchFragment() }
val fragmentUpload by lazy { UploadFragment() }

//private val fragmentCreate by lazy { basic_createFragment() }
//private val fragmentChat by lazy { chatFragment() }
//private val fragmentMore by lazy { moreFragment() }
//val fragmentList by lazy { listFragment() }

class MainActivity : AppCompatActivity() {

    val manager = supportFragmentManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        toolbar.setTitle("")
        toolbar.setSubtitle("")
        initNavigationBar()
    }



    private fun initNavigationBar() {
        bottom_navi.run {
            setOnNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.home -> {
                        change(fragmentMain)
                    }

                    R.id.upload -> {
                        change(fragmentUpload)
                    }
//                    R.id.chat -> {
//                        change(fragmentCreate)
//                    }
//                    R.id.mypage -> {
//                        change(fragmentChat)
//                    }
                }
                true
            }
            selectedItemId = R.id.home
        }
    }

    fun change(fragment: Fragment) {
        supportFragmentManager
            .beginTransaction()
            .replace(R.id.changeFragment, fragment)
            .commitNow()
    }


    private var lastTimeBackPressed: Long = 0

    override fun onBackPressed() { //뒤로가기 처리
        if (System.currentTimeMillis() - lastTimeBackPressed >= 1500) {
            lastTimeBackPressed = System.currentTimeMillis()
            Toast.makeText(this, "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show()
        } else {
            finishAffinity()
        }
    }
}


