package com.kgg.android.delivers.UploadActivity


import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.get
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.kgg.android.delivers.ApiService
import com.kgg.android.delivers.MainActivity
import com.kgg.android.delivers.data.Story
import kotlinx.android.synthetic.main.activity_fast_create.*
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.classifier.Classifications
import org.tensorflow.lite.task.vision.classifier.ImageClassifier
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import retrofit2.Retrofit
import java.text.SimpleDateFormat
import java.util.*

//import io.reactivex.functions.Action;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import java.io.*


// 가경
// 스토리 사진 및 설명 최종 등록 페이지

class UploadActivity: AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    private var imgUri : Uri? = null
    private var myimg: Bitmap? = null

    lateinit var apiService: ApiService // for uploading image

    var latitude = 0.0
    var longitude = 0.0
    var Location = ""
    var category = ""
    var bool = true

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(com.kgg.android.delivers.R.layout.activity_fast_create)

        auth = FirebaseAuth.getInstance()

        initRetrofitClient()

        latitude = intent.getStringExtra("latitude")?.toDouble()!!
        longitude = intent.getStringExtra("longitude")?.toDouble()!!
        category = intent.getStringExtra("category")!!
        Location = intent.getStringExtra("location")!!
        bool = intent.getBooleanExtra("bool", true)

        if(bool){
            val intent = Intent(Intent.ACTION_PICK)
            intent.type = "image/*"
            startActivityForResult(intent, 10)
            imageAttach.scaleType = ImageView.ScaleType.CENTER_CROP
        }


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

    // init retrofit
    private fun initRetrofitClient() {
        val client = OkHttpClient.Builder().build()
        // baseUrl indicates our server
        apiService =
            Retrofit.Builder().baseUrl("http://117.17.189.202:8080").client(client).build().create<ApiService>(
                ApiService::class.java
            )
    }



    // object detection 함수
    private fun runObjectDetection(bitmap: Bitmap) {
        val image = TensorImage.fromBitmap(bitmap)
        val options = ObjectDetector.ObjectDetectorOptions.builder()
            .setMaxResults(5)
            .setScoreThreshold(0.5f)
            .build()
        val detector = ObjectDetector.createFromFileAndOptions(
            this, // the application context
            "model1.tflite", // must be same as the filename in assets folder
            options
        )
        val option2 = ImageClassifier.ImageClassifierOptions.builder().setMaxResults(5).setScoreThreshold(0.5f).build()
        val detect2 = ImageClassifier.createFromFileAndOptions(this,"model.tflite",option2)

        val results = detector.detect(image) // detection 결과!!!
        val results2 = detect2.classify(image) // detection 결과!!!
        Log.d("itm","${results2} hello!!")
        debugPrint(results2)

    }
    private fun debugPrint(results : List<Classifications>) {
        var labels = ""
        for ((i, obj) in results.withIndex()) {


            for ((j, category) in obj.categories.withIndex()) {
                labels = labels + " " + category.displayName
                Log.d(TAG, "    Label $j: ${category.label}")
                Log.d(TAG, "    Label $j: ${category.displayName}")
                val confidence: Int = category.score.times(100).toInt()
                Log.d(TAG, "    Confidence: ${confidence}%")
            }
        }
        labels = "Let's eat" + labels + " together~!"
        postTitle.setText(labels)
        Log.d(TAG,"${labels}")
    }



    override fun onActivityResult(requestCode : Int, resultCode : Int, data : Intent?){
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            10 ->
                if (resultCode == RESULT_OK) {
                    imgUri = data?.data!!
                    myimg = MediaStore.Images.Media.getBitmap(this.contentResolver, imgUri) as Bitmap
                    runObjectDetection(MediaStore.Images.Media.getBitmap(this.contentResolver, imgUri))
                    Glide.with(this).load(imgUri).into(imageAttach)
                }
            else {
                    finish() // 사진 선택이 안된 채로 뒤로 가기가 눌렸을 경우 액티비티 종t
                }

        }
    }



    private fun createFastLost() {
        val intent = Intent(applicationContext, MainActivity::class.java)

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
        story.bool = true


        fbFirestore.collection("story")
            .add(story)
            .addOnSuccessListener { documentReference ->

                imageName = ("${documentReference.id}.png")

                fbFirestore.collection("story").document(documentReference.id).update("photo", "images/story/${imageName}")
                fbFirestore.collection("story").document(documentReference.id).update("postId", documentReference.id)
                

                Log.d(TAG, "DocumentSnapshot written with ID: ${documentReference.id}")
                val imgRef : StorageReference = fbFireStorage.getReference("images/story/${imageName}")
                if (imgUri!=null)
                    imgRef.putFile(imgUri!!)
                Toast.makeText(this, "등록이 완료되었습니다.", Toast.LENGTH_SHORT).show()

                startActivity(intent)
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)



            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }


    }

}