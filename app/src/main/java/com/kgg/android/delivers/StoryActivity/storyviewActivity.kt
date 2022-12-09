package com.kgg.android.delivers.StoryActivity


import android.app.ListActivity
import com.teresaholfeld.stories.StoriesProgressView
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
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
import android.widget.*
import androidx.core.content.ContentProviderCompat.requireContext
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.getValue
import com.kgg.android.delivers.MainActivity
import com.kgg.android.delivers.chatActivity.ChatActivity
import com.kgg.android.delivers.data.ChatRoom

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
    private val fireDatabase = FirebaseDatabase.getInstance().reference
    var userInfo = FirebaseFirestore.getInstance().collection("users") //작업할 컬렉션
    val firestore = FirebaseFirestore.getInstance()


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
//        uid = "WoKw1NJYG8TB9Z4GDWh4H5e9ieh1"





        counter = intent.getStringExtra("index")!!.toInt()

        var StoryArr = intent.getParcelableArrayListExtra<Story>("StoryArr")
        var currentStory: Story = StoryArr!![counter]
        image = findViewById<View>(R.id.image) as ImageView
        image!!.background = getResources().getDrawable(R.drawable.rounded_corner_border, null)
        image!!.setClipToOutline(true)

//        //스토리에서 채팅하기 버튼 누르면 채팅방으로 이동
//        val chatButton = findViewById<LinearLayout>(R.id.DMBtn)
//        chatButton.setOnClickListener{
//            val intent = Intent(this, ChatActivity::class.java)
//            intent.putExtra("postId", currentStory.postId)
//            Log.d("story","${currentStory.postId}")
//        }

        // 메뉴 버튼
        menu.setOnClickListener {

            if(auth.currentUser?.uid.toString()==currentStory.writer) { // 자신이 올린 게시글일 때
                var popupMenu = PopupMenu(this, it)
                menuInflater?.inflate(R.menu.detail_menu, popupMenu.menu)
                popupMenu.show()

                popupMenu.setOnMenuItemClickListener {

                    when(it.itemId){
                        R.id.delete -> { //삭제
                            firestore.collection("story").document("${currentStory.postId}")
                                .delete()
                            val intent = Intent(this, MainActivity::class.java)
                            startActivity(intent)
                            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                            return@setOnMenuItemClickListener true
                        }
                        else -> {
                            return@setOnMenuItemClickListener true
                        }                        }
                    }
                }
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
                        image!!.setImageResource(R.drawable.gray_bg)
                    }
                }
            }
        } else {
            image!!.setImageResource(R.drawable.gray_bg)
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

        findViewById<TextView>(R.id.daysbefore).text = msg



        // 닉네임 가져오기
        userInfo
            .whereEqualTo("uid", "${currentStory.writer}") //uid가 destinationUid와 일치하는 문서 가져오기
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    var nickname = document["nickname"] as String
                    findViewById<TextView>(R.id.userNickname).text = nickname.toString()


                }

            }
            .addOnFailureListener { exception ->
                Log.d("Chatting", "Error getting documents: $exception")
            }




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

        DMBtn.setOnClickListener {
            clickDMButton(currentStory)
        }

        // 본인이 올린 스토리면 채팅 버튼 숨기기
        if( uid == currentStory.writer){
            DMBtn.visibility = View. INVISIBLE
        }
        else{
            DMBtn.visibility = View.VISIBLE
        }


    }
    fun clickDMButton(currentStory:Story){
        var currentStory = currentStory
        // 채팅방으로 전환
            try {
                val intent = Intent(this, ChatActivity::class.java)

                intent.putExtra("destinationUid", currentStory.writer) //상대방의 id를 넘겨줌
                Log.d("Chatting", "destinationUid: ${currentStory.writer}")
                intent.putExtra("postId", currentStory.postId) //채팅방 포스트 id넘겨줌
                var postId = currentStory.postId
                Log.d("Chatting", "postID : ${currentStory.postId}")

                fireDatabase.child("chatrooms")//채팅방 데이터에서 myUid가 true인 데이터 조회
                    .orderByChild("users/${uid}")
                    .equalTo(true)
                    .addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onCancelled(error: DatabaseError) {
                            Log.d("Chatting", "Fail to read data")
                        }

                        override fun onDataChange(snapshot: DataSnapshot) {
                            if (snapshot.hasChildren()) {
                                for (data in snapshot.children) {
                                    var chatRoom = data.getValue<ChatRoom>()
                                    if (chatRoom != null) {
                                        if (chatRoom.postId == currentStory.postId) { //해당 채팅방 데이터의 postId가 스토리 postId와 같을 경우 해당 데이터의 key값을 채팅방 id로 할당
                                            var chatRoomId = data.key!!
                                            Log.d("Chatting", "ChatRoom information: $chatRoom")
                                            intent.putExtra("ChatRoomId", chatRoomId)
                                            Log.d("Chatting", "ChatRoomId : $chatRoomId")
                                            startActivity(intent)
                                        } else {//해당 유저에 대한 채팅방 데이터는 있지만 해당 포스트에 대한 이용자의 채팅방이 존재하지 않을 경우
                                            intent.putExtra("ChatRoomId", "")
                                            startActivity(intent)
                                        }
                                    }
                                    break
                                }
                            } else {//해당 유저에 대한 채팅방 데이터가 존재하지 않을 경우
                                intent.putExtra("ChatRoomId", "")
                                startActivity(intent)
                            }
                        }

                    })


            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this, "채팅방 이동 중 문제가 발생하였습니다.", Toast.LENGTH_SHORT).show()
                Log.d("chatting", "채팅방 이동 중 문제 발생")
            } //에러 처리


    }

    override fun onPrev() {
        if (counter - 1 < 0) return
        counter--
        var StoryArr = intent.getParcelableArrayListExtra<Story>("StoryArr")
        var currentStory: Story = StoryArr!![counter]

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

        findViewById<TextView>(R.id.daysbefore).text = msg

        // 닉네임 가져오기
        userInfo
            .whereEqualTo("uid", "${currentStory.writer}") //uid가 destinationUid와 일치하는 문서 가져오기
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    var nickname = document["nickname"] as String
                    findViewById<TextView>(R.id.userNickname).text = nickname.toString()
                }

            }
            .addOnFailureListener { exception ->
                Log.d("Chatting", "Error getting documents: $exception")
            }


        if (currentStory.photo != "") {
            val resourceId = currentStory.photo
            if (resourceId != null) {
                storageRef.child(resourceId).downloadUrl.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Glide.with(this)
                            .load(task.result)
                            .into(image!!)
                    } else {
                        image!!.setImageResource(R.drawable.gray_bg)
                    }
                }
            }
        } else {
            image!!.setImageResource(R.drawable.gray_bg)
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

        DMBtn.setOnClickListener {
            clickDMButton(currentStory)
        }

    }

    override fun onNext() { // 옆으로 넘기기
        counter++
        var StoryArr = intent.getParcelableArrayListExtra<Story>("StoryArr")
        var currentStory: Story = StoryArr!![counter]


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

        findViewById<TextView>(R.id.daysbefore).text = msg

        // 닉네임 가져오기
        userInfo
            .whereEqualTo("uid", "${currentStory.writer}") //uid가 destinationUid와 일치하는 문서 가져오기
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    var nickname = document["nickname"] as String
                    findViewById<TextView>(R.id.userNickname).text = nickname.toString()
                }

            }
            .addOnFailureListener { exception ->
                Log.d("Chatting", "Error getting documents: $exception")
            }

        if (currentStory.photo != "") {
            val resourceId = currentStory.photo
            if (resourceId != null) {
                storageRef.child(resourceId).downloadUrl.addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Glide.with(this)
                            .load(task.result)
                            .into(image!!)
                    } else {
                        image!!.setImageResource(R.drawable.gray_bg)
                    }
                }
            }
        } else {
            image!!.setImageResource(R.drawable.gray_bg)
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

        DMBtn.setOnClickListener {
            clickDMButton(currentStory)
        }


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
