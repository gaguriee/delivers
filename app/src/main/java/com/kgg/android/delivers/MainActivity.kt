package com.kgg.android.delivers


import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.room.Room
import com.kgg.android.delivers.UploadActivity.UploadFragment
import kotlinx.android.synthetic.main.activity_main.*
import com.kgg.android.delivers.chatActivity.ChatFragment
import com.kgg.android.delivers.myActivity.MyPageFragment


// 가경, 보영, 은지
// Main 화면, fragment 전환 activity


val fragmentMain by lazy { MainFragment() }
val fragmentUpload by lazy { UploadFragment() }
val fragmentMypage by lazy { MyPageFragment() }
//private val fragmentCreate by lazy { basic_createFragment() }
private val fragmentChat by lazy { ChatFragment() }
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
                    R.id.chat -> {
                        change(fragmentChat)
                    }
                    R.id.mypage -> {
                        change(fragmentMypage)
                    }
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


