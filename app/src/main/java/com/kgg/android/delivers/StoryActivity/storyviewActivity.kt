package com.kgg.android.delivers.StoryActivity


import com.teresaholfeld.stories.StoriesProgressView
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.kgg.android.delivers.R
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.kgg.android.delivers.data.Story
//import data.user_data.signup_data
import kotlinx.android.synthetic.main.activity_storydetail.*
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import android.widget.LinearLayout
import com.kgg.android.delivers.chatActivity.ChatActivity

// 가경
// 스토리 디테일 보기 페이지
// activity_storydetail

// 은지 - 채팅으로 넘어가기 구현

class storyviewActivity : AppCompatActivity(), StoriesProgressView.StoriesListener {
    private var storiesProgressView: StoriesProgressView? = null
    private var image: ImageView? = null
    private val storage: FirebaseStorage = FirebaseStorage.getInstance("gs://delivers-65049.appspot.com/")
    private val storageRef: StorageReference = storage.reference
    private var counter = 0
    private var PROGRESS_COUNT = 0;
    private lateinit var auth: FirebaseAuth
    var uid = ""


    private var pressTime = 0L
    private var limit = 500L

    private val onTouchListener = View.OnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                pressTime = System.currentTimeMillis()
                storiesProgressView?.pause()

                return@OnTouchListener false
            }
            MotionEvent.ACTION_UP -> {
                val now = System.currentTimeMillis()
                storiesProgressView?.resume()
                return@OnTouchListener limit < now - pressTime
            }
        }
        false
    }



    override fun onCreate(savedInstanceState: Bundle?) {


        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        setContentView(R.layout.activity_storydetail)

        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()

        counter = intent.getStringExtra("index")!!.toInt()

        var StoryArr = intent.getParcelableArrayListExtra<Story>("StoryArr")
        var currentStory: Story = StoryArr!![counter]
        image = findViewById<View>(R.id.image) as ImageView
        image!!.background = getResources().getDrawable(R.drawable.rounded_corner_border, null)
        image!!.setClipToOutline(true)

        //스토리에서 채팅하기 버튼 누르면 채팅방으로 이동
        val chatButton = findViewById<LinearLayout>(R.id.DMBtn)
        chatButton.setOnClickListener{
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("postId", currentStory.postId)
            Log.d("story","${currentStory.postId}")
        }

        PROGRESS_COUNT = StoryArr!!.size
        currentStory.photo?.let { Log.d("photo", it) }
        if (currentStory.photo != "") {
            val resourceId = currentStory.photo
            if (resourceId != null) {
                storageRef.child(resourceId).downloadUrl.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Glide.with(this)
                            .load(task.result)
                            .into(image!!)

                    } else {
                        image!!.setImageResource(R.mipmap.ic_launcher_round)
                    }
                }
            }
        } else {
            image!!.setImageResource(R.mipmap.ic_launcher_round)
        }
        var description = findViewById<TextView>(R.id.description)
        var title = currentStory.description
        description.setText(title)

        var area = findViewById<TextView>(R.id.area)
        var lostLocation = currentStory.location
        area.setText(lostLocation)

        var category = findViewById<TextView>(R.id.sort)
        var sort = currentStory.category
        category.setText(sort)

        var currTime =  System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("ko", "KR"))
        var registerTime = dateFormat.parse(currentStory.registerDate).time

        var diffTime: Long = (currTime - registerTime) / 1000
        var SEC = 60
        var MIN = 60
        var HOUR = 24
        var DAY = 30
        var MONTH = 12

        var msg: String? = null
        if (diffTime < SEC) {
            msg = "방금 전"
        } else if ((diffTime / SEC) < MIN) {
            diffTime /= SEC
            msg = diffTime.toString() + "분 전"
        } else if ((diffTime / (MIN*SEC)) < HOUR) {
            diffTime /= (MIN*SEC)
            msg = diffTime.toString() + "시간 전"
        } else if ((diffTime / (MIN*SEC*HOUR)) < DAY) {
            diffTime /= (MIN*SEC*HOUR)
            msg = diffTime.toString() + "일 전"
        } else if ((diffTime / (MIN*SEC*HOUR*DAY)) < MONTH) {
            diffTime /= (MIN*SEC*HOUR*DAY)
            msg = diffTime.toString() + "달 전"
        } else {
            diffTime /= (MIN*SEC*HOUR*DAY*MONTH)
            msg = diffTime.toString() + "년 전"
        }

        daysbefore.text = msg




        storiesProgressView = findViewById(R.id.stories)
        storiesProgressView?.setStoriesCount(PROGRESS_COUNT)
        storiesProgressView?.setStoryDuration(3000L)
        storiesProgressView?.setStoriesListener(this)
        storiesProgressView?.startStories(counter)


        // bind reverse view
        val reverse = findViewById<View>(R.id.reverse)
        reverse.setOnClickListener {storiesProgressView?.reverse()
        }
        reverse.setOnTouchListener(onTouchListener)

        // bind skip view
        val skip = findViewById<View>(R.id.skip)
        skip.setOnClickListener { storiesProgressView?.skip() }
        skip.setOnTouchListener(onTouchListener)



        // 채팅방으로 전환

        DMBtn.setOnClickListener {

        }

        // 본인이 올린 스토리면 채팅 버튼 숨기기
        if( uid == currentStory.writer){
            DMBtn.visibility = View. INVISIBLE
        }


    }

    override fun onPrev() {
        if (counter - 1 < 0) return
        counter--
        var StoryArr = intent.getParcelableArrayListExtra<Story>("StoryArr")
        var currentStory: Story = StoryArr!![counter]


        if (currentStory.photo != "") {
            val resourceId = currentStory.photo
            if (resourceId != null) {
                storageRef.child(resourceId).downloadUrl.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Glide.with(this)
                            .load(task.result)
                            .into(image!!)
                    } else {
                        image!!.setImageResource(R.mipmap.ic_launcher_round)
                    }
                }
            }
        } else {
            image!!.setImageResource(R.mipmap.ic_launcher_round)
        }
        var description = findViewById<TextView>(R.id.description)
        var title = currentStory.description
        description.setText(title)

        var area = findViewById<TextView>(R.id.area)
        var Location = currentStory.location
        area.setText(Location)

        var category = findViewById<TextView>(R.id.sort)
        var sort = currentStory.category
        category.setText(sort)

    }

    override fun onNext() { // 옆으로 넘기기
        counter++
        var StoryArr = intent.getParcelableArrayListExtra<Story>("StoryArr")
        var currentStory: Story = StoryArr!![counter]
        if (currentStory.photo != "") {
            val resourceId = currentStory.photo
            if (resourceId != null) {
                storageRef.child(resourceId).downloadUrl.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Glide.with(this)
                            .load(task.result)
                            .into(image!!)
                    } else {
                        image!!.setImageResource(R.mipmap.ic_launcher_round)
                    }
                }
            }
        } else {
            image!!.setImageResource(R.mipmap.ic_launcher_round)
        }


        var description = findViewById<TextView>(R.id.description)
        var title = currentStory.description
        description.setText(title)

        var area = findViewById<TextView>(R.id.area)
        var Location = currentStory.location
        area.setText(Location)

        var category = findViewById<TextView>(R.id.sort)
        var sort = currentStory.category
        category.setText(sort)


    }

    override fun onComplete() {
        finish()
    }

    override fun onDestroy() {
        // Very important !
        storiesProgressView?.destroy()
        super.onDestroy()
    }

    override fun onPause() {
        storiesProgressView!!.pause()
        super.onPause()
    }

    override fun onRestart() {
        storiesProgressView!!.resume()
        super.onRestart()
    }



}
