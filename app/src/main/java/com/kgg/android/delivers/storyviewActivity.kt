package storyActivity


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
import com.kgg.android.delivers.R
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.kgg.android.delivers.Story
//import data.user_data.signup_data
import io.grpc.InternalChannelz.id
import kotlinx.android.synthetic.main.activity_storydetail.*
import kotlinx.android.synthetic.main.certification.*
import java.text.SimpleDateFormat
import java.util.*

class storyviewActivity : AppCompatActivity(), StoriesProgressView.StoriesListener {
    private var storiesProgressView: StoriesProgressView? = null
    private var image: ImageView? = null
    private val storage: FirebaseStorage = FirebaseStorage.getInstance("gs://delivers-65049.appspot.com/")
    private val storageRef: StorageReference = storage.reference
    private var counter = 0
    val firestore = FirebaseFirestore.getInstance()
    private var PROGRESS_COUNT = 0;

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

        var registerDate = intent.getStringExtra("registerDate")


/*        image?.setImageResource(resources[0])*/

        counter = intent.getStringExtra("index")!!.toInt()

        var StoryArr = intent.getParcelableArrayListExtra<Story>("StoryArr")
        var currentStory: Story = StoryArr!![counter]
        image = findViewById<View>(R.id.image) as ImageView
        image!!.background = getResources().getDrawable(R.drawable.rounded_corner_border, null)
        image!!.setClipToOutline(true)

        PROGRESS_COUNT = StoryArr!!.size
        Log.d("photo", currentStory.photo)
        if (currentStory.photo != "") {
            val resourceId = currentStory.photo
            storageRef.child(resourceId).downloadUrl.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Glide.with(this)
                        .load(task.result)
                        .into(image!!)

                } else {
                    image!!.setImageResource(R.mipmap.ic_launcher_round)
                }
            }
        } else {
            image!!.setImageResource(R.mipmap.ic_launcher_round)
        }
        var description = findViewById<TextView>(R.id.description)
        var title = currentStory.title
        description.setText(title)

        var area = findViewById<TextView>(R.id.area)
        var lostLocation = currentStory.Location
        area.setText(lostLocation)

        var category = findViewById<TextView>(R.id.sort)
        var sort = currentStory.category
        category.setText(sort)

        var userNickname = findViewById<TextView>(R.id.userNickname)
        var wrtierId = currentStory.writer

        var currTime =  System.currentTimeMillis()
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("ko", "KR"))
        Log.d("registerDate22", currentStory.registerDate)
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

//        firestore.collection("users").document(wrtierId)
//            .get().addOnSuccessListener { document ->
//                val item = signup_data(
//
//                    document["uid"] as String,
//                    document["userId"] as String,
//                    document["name"] as String,
//                    document["registNum_front"] as String,
//                    document["registNum_back"] as String,
//                    document["nickname"] as String,
//                    document["phoneNum"] as String,
//
//                    )
//
//                userNickname.text = item.nickname.toString()
//            }


        storiesProgressView = findViewById(R.id.stories)
        storiesProgressView?.setStoriesCount(PROGRESS_COUNT)
        storiesProgressView?.setStoryDuration(3000L)
        // or
        // storiesProgressView.setStoriesCountWithDurations(durations);

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
    }

    override fun onPrev() {
        if (counter - 1 < 0) return
        counter--
        var StoryArr = intent.getParcelableArrayListExtra<Story>("StoryArr")
        var currentStory: Story = StoryArr!![counter]


        if (currentStory.photo != "") {
            val resourceId = currentStory.photo
            storageRef.child(resourceId).downloadUrl.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Glide.with(this)
                        .load(task.result)
                        .into(image!!)
                } else {
                    image!!.setImageResource(R.mipmap.ic_launcher_round)
                }
            }
        } else {
            image!!.setImageResource(R.mipmap.ic_launcher_round)
        }
        var description = findViewById<TextView>(R.id.description)
        var title = currentStory.title
        description.setText(title)

        var area = findViewById<TextView>(R.id.area)
        var Location = currentStory.Location
        area.setText(Location)

        var category = findViewById<TextView>(R.id.sort)
        var sort = currentStory.category
        category.setText(sort)

        // or
        // storiesProgressView.setStoriesCountWithDurations(durations);



    }

    override fun onNext() {
        counter++
        var StoryArr = intent.getParcelableArrayListExtra<Story>("StoryArr")
        var currentStory: Story = StoryArr!![counter]
        if (currentStory.photo != "") {
            val resourceId = currentStory.photo
            storageRef.child(resourceId).downloadUrl.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Glide.with(this)
                        .load(task.result)
                        .into(image!!)
                } else {
                    image!!.setImageResource(R.mipmap.ic_launcher_round)
                }
            }
        } else {
            image!!.setImageResource(R.mipmap.ic_launcher_round)
        }


        var description = findViewById<TextView>(R.id.description)
        var title = currentStory.title
        description.setText(title)

        var area = findViewById<TextView>(R.id.area)
        var Location = currentStory.Location
        area.setText(Location)

        var writerId = currentStory.writer

        var category = findViewById<TextView>(R.id.sort)
        var sort = currentStory.category
        category.setText(sort)

        // or
        // storiesProgressView.setStoriesCountWithDurations(durations);


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


/*
    private fun getStories(userid: String?) {
        var images: ArrayList<String>? = null
        var storyids: ArrayList<String>? = null
        val reference: DatabaseReferencece = Firebase.getInstance().getReference("Story")
            .child(userid)
        reference.addListenerForSingleValueEvent(object : ValueEventListener,
            StoriesProgressView.StoriesListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                images?.clear()
                storyids?.clear()
                for (snapshot in dataSnapshot.getChildren()) {
                    var story: Story = snapshot.getValue(Story::class.java)
                    var timecurrent = System.currentTimeMillis()
                    if (timecurrent > story.getTimestart() && timecurrent < story.getTimeend()) {
                        (images as ArrayList<String>).add(story.getImageurl)
                        (storyids as ArrayList<String>).add(story.getStoryid())
                    }
                }
                storiesProgressView!!.setStoriesCount(images!!.size)
                storiesProgressView!!.setStoryDuration(5000L)
                storiesProgressView!!.setStoriesListener(this)
                storiesProgressView!!.startStories(counter)
                Glide.with(applicationContext).load(images.get(counter))
                    .into(image!!)
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }

        })
    }

    private fun userInfo(userid: String?) {
        val reference: DatabaseReferebce = Firebase.getInstance().getReference("Story")
            .child(userid)
        reference.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user: User? = dataSnapshot.getValue(User::class.java)
                Glide.with(applicationContext).load(user.getImageurl()).into(story_photo)
                story_username.setText(user, getUsername())
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }


        })
    }
*/


}
