package com.kgg.android.delivers.loginActivity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityCompat.finishAffinity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.kgg.android.delivers.MainActivity
import com.kgg.android.delivers.R
import data.user_data
import kotlinx.android.synthetic.main.activity_userinfo.*

class userinfoActivity : AppCompatActivity() {

    private var auth: FirebaseAuth? = null
    var fbFirestore: FirebaseFirestore? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_userinfo)
        auth = Firebase.auth
        var bool : Boolean? = false;


        //        기존 회원일 경우 (회원 정보가 존재할 경우) 현재 activity 생략




        val docref =fbFirestore?.collection("users")?.document(auth?.uid.toString())

        Log.d("TAG", "docref: "+docref)

        docref?.get()?.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result
                if (docref != null) {
                    if (document.exists()) {
                        val intent = Intent(applicationContext, MainActivity::class.java)
                        startActivity(intent)
                        Log.d("TAG", "Error: already existing user1")
                    } // 회원 정보 존재, 바로 메인 화면으로 pass
                    Log.d("TAG", "Error: already existing user2")
                }

            } else {
                Log.d("TAG", "Error: already existing user")
            }
        }



        finish_btn.setOnClickListener {
            if (setnickname.getText().toString().equals("")){
                Toast.makeText(
                    this, "활동명(닉네임)을 입력해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            else if (setEmailAddress.getText().toString().equals("")){
                Toast.makeText(
                    this, "이메일을 입력해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            val phoneNum = intent.getStringExtra("phoneNum").toString()
            Log.e("phoneNum", phoneNum)
            createAccount(setEmailAddress.text.toString(),  setnickname.text.toString())
        }
    }


    // 계정 생성
    private fun createAccount(email: String, nickname: String) {

        if (email.isNotEmpty() && nickname.isNotEmpty() ) {

                        Toast.makeText(
                            this, "계정 생성 완료.",
                            Toast.LENGTH_SHORT
                        ).show()
                        val phoneNum = intent.getStringExtra("phoneNum")
                        val intent = Intent(applicationContext, MainActivity::class.java)

                        startActivity(intent)
                        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                        val fbFirestore = Firebase.firestore
                        var userInfo = user_data()

                        userInfo.uid = auth?.uid
                        userInfo.nickname = nickname
                        userInfo.email = email
                        userInfo.phoneNum = phoneNum.toString()


                        fbFirestore?.collection("users")?.document(auth?.uid.toString())?.set(userInfo)

                    } else {
                        Toast.makeText(
                            this, "계정 생성에 실패하였습니다.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
        }
    private var lastTimeBackPressed : Long = 0

    override fun onBackPressed() {
        if(System.currentTimeMillis() - lastTimeBackPressed >= 1500) {
            lastTimeBackPressed = System.currentTimeMillis()
            Toast.makeText(this, "'뒤로' 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT).show()
        } else{
            finishAffinity()
        }
    }
}