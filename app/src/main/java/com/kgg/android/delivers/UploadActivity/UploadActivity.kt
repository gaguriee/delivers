package com.kgg.android.delivers.UploadActivity


import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.kgg.android.delivers.MainActivity
import com.kgg.android.delivers.R
import com.kgg.android.delivers.data.Story
import com.kgg.android.delivers.loginActivity.userinfoActivity
import kotlinx.android.synthetic.main.activity_fast_create.*
import kotlinx.android.synthetic.main.certification.*
import java.text.SimpleDateFormat
import java.util.*

// 가경
// 스토리 사진 및 설명 최종 등록 페이지

class UploadActivity: AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private lateinit var imgUri : Uri

    var latitude = 0.0
    var longitude = 0.0
    var Location = ""
    var category = ""


    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_fast_create)

        auth = FirebaseAuth.getInstance()


        latitude = intent.getStringExtra("latitude")?.toDouble()!!
        longitude = intent.getStringExtra("longitude")?.toDouble()!!
        category = intent.getStringExtra("category")!!
        Location = intent.getStringExtra("location")!!

        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        startActivityForResult(intent, 10)
        imageAttach.scaleType = ImageView.ScaleType.CENTER_CROP



        val closeBtn = imageView

        closeBtn.setOnClickListener {
            finish()
        }

        fastregisterBtn.setOnClickListener {
            if (postTitle.text.toString().equals("")) {
                Toast.makeText(
                    this, "내용을 입력해주세요.",
                    Toast.LENGTH_SHORT
                ).show()
            } else {
                createFastLost()
            }
        }


    }

    override fun onActivityResult(requestCode : Int, resultCode : Int, data : Intent?){
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            10 ->
                if (resultCode == RESULT_OK) {
                    imgUri = data?.data!!
                    Glide.with(this).load(imgUri).into(imageAttach)
                }

        }
    }


    private fun createFastLost() {
        val intent = Intent(applicationContext, MainActivity::class.java)
        startActivity(intent)
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        val title = postTitle.text

        val fbFirestore = Firebase.firestore
        val fbFireStorage = FirebaseStorage.getInstance("gs://delivers-65049.appspot.com/")



        val story = Story()

        var imageName = ""

        story.writer = auth.currentUser?.uid.toString()
        // 현재시간을 가져오기
        val now: Long = System.currentTimeMillis()

        // 현재 시간을 Date 타입으로 변환
        val date = Date(now)

        // 날짜, 시간을 가져오고 싶은 형태 선언
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("ko", "KR"))
        val stringTime = dateFormat.format(date)
        story.registerDate = stringTime
        story.description = title.toString()
        story.location = Location.toString()
        story.category = category
        story.latitude = latitude!!
        story.longitude = longitude!!


        fbFirestore.collection("story")
            .add(story)
            .addOnSuccessListener { documentReference ->

                imageName = ("${documentReference.id}.png")

                fbFirestore.collection("story").document(documentReference.id).update("photo", "images/story/${imageName}")
                fbFirestore.collection("story").document(documentReference.id).update("postId", documentReference.id)

                Log.d(TAG, "DocumentSnapshot written with ID: ${documentReference.id}")
                val imgRef : StorageReference = fbFireStorage.getReference("images/story/${imageName}")
                imgRef.putFile(imgUri)
                Toast.makeText(this, "등록이 완료되었습니다.", Toast.LENGTH_SHORT).show()


                //비동기 처리나 순차 처리를 한 번 해야 할 듯.. photo나 postId가 가끔 안들어가는 현상이 생김


//                while(true){
//                    val docref =fbFirestore?.collection("story")?.document(documentReference.id)
//
//                    docref?.get()?.addOnCompleteListener { task ->
//                        if (task.isSuccessful) {
//                            val document = task.result
//                            if (document.getString("photo")== null || document.getString("postId")==null) {
//                            } // photo 가 들어가기 전,
//                            else {
//                                // photo 값 들어감
////                                val intent = Intent(this, MainActivity::class.java)
////                                startActivity(intent)
//
//                            }
//                        }
//                    }
//                }



            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }


    }

}