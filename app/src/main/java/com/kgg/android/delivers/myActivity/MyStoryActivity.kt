package com.kgg.android.delivers.myActivity

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.kgg.android.delivers.R
import com.kgg.android.delivers.StoryActivity.storyviewActivity
import com.kgg.android.delivers.data.Story
import com.kgg.android.delivers.databinding.ActivityMyStoryBinding
import kotlinx.android.synthetic.main.activity_main.*

class MyStoryActivity : AppCompatActivity() {

    val firestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance("gs://delivers-65049.appspot.com/")
    private val storageRef: StorageReference = storage.reference

    private val fireDatabase = FirebaseDatabase.getInstance().reference

    var uid = ""

    var storyList = arrayListOf<Story>()

    private lateinit var auth: FirebaseAuth
    var nickname:String = ""
    var email:String = ""
    var phoneNum:String = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMyStoryBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        supportActionBar!!.setDisplayShowTitleEnabled(false)
        toolbar.setTitle("")

        auth = FirebaseAuth.getInstance()

        uid = auth.currentUser?.uid.toString()

        val sAdapter = StoriesAdapter(storyList, this)
        binding.myRecyclerView.adapter = sAdapter


        val layout2 = LinearLayoutManager(this).also { it.orientation = LinearLayoutManager.HORIZONTAL }
        binding.myRecyclerView.layoutManager = layout2
        binding.myRecyclerView.setHasFixedSize(true)

        var doccol = firestore.collection("story")


        Log.d("CheckStoryList" , storyList.toString())
        Log.d("firestore" , firestore.toString())





        doccol.whereEqualTo("writer","${uid}").get()
            .addOnSuccessListener { result ->
                storyList.clear()
                for (document in result) {
                    val item = Story(
                        document["writer"] as String,
                        document["location"] as String,
                        document["photo"] as String,
                        document["description"] as String,
                        document["category"] as String,
                        document["latitude"] as Double,
                        document["longitude"] as Double,
                        document["registerDate"] as String,
                        document["postId"] as String,
                    )
                    storyList.add(item)

                }

                if (sAdapter != null) {
                    sAdapter.notifyDataSetChanged()
                }

                Log.d("CheckStoryList" , storyList.toString())
            }.addOnFailureListener { exception ->
                Log.w("ListActivity22", "Error getting documents: $exception")
            }

        if (sAdapter != null) {
            sAdapter.setOnItemClickListener(object : StoriesAdapter.OnItemClickListener {
                override fun onItemClick(v: View, data: Story, pos: Int) {

                    val intent = Intent(this@MyStoryActivity, storyviewActivity::class.java)

                    intent.putExtra("index", pos.toString())
                    //    intent.putStringArrayListExtra("imgArr", imgArr)
                    //  intent.putStringArrayListExtra("desArr", desArr)
                    intent.putParcelableArrayListExtra("StoryArr", storyList)
                    startActivity(intent)
                    this@MyStoryActivity.overridePendingTransition(0, 0)

                }

            })
        }



    }

    class StoriesAdapter(private val stories: List<Story>, context: Context) :
        RecyclerView.Adapter<StoriesAdapter.StoriesViewHolder>() {
        private val storage: FirebaseStorage =
            FirebaseStorage.getInstance("gs://delivers-65049.appspot.com/")
        private val storageRef: StorageReference = storage.reference

        interface OnItemClickListener {
            fun onItemClick(v: View, data: Story, pos: Int)
        }

        private var listener: OnItemClickListener? = null
        fun setOnItemClickListener(listener: OnItemClickListener) {
            this.listener = listener
        }


        private val context: Context

        inner class StoriesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {


            open fun check_category(category:String):Int{
                var id = 0
                when(category)
                {
                    "chicken" -> id = R.drawable.chicken //치킨
                    "hamburger"-> id = R.drawable.hamburger //버거
                    "pizza" -> id = R.drawable.pizza //피자
                    "coffee"->id = R.drawable.coffee //카페디저트
                    "bread"-> id = R.drawable.bread //샌드위치
                    "meat"-> id = R.drawable.meat //고기
                    "salad"-> id = R.drawable.salad //샐러드
                    "sushi"-> id = R.drawable.sushi //회초밥
                    "guitar"-> id = R.drawable.guitar //기타
                }
                return id
            }


            private val lostPhoto = itemView.findViewById<ImageView>(R.id.story_imgSM)
//            private var storyTitle = itemView.findViewById<TextView>(R.id.story_Title)
//            private var ndaysbefore = itemView.findViewById<TextView>(R.id.ndays_before)
//            private var category = itemView.findViewById<TextView>(R.id.cate_gory)

            fun bind(storyD: Story, context: Context) {

                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    itemView.setOnClickListener {
                        listener?.onItemClick(itemView, storyD, pos)
                    }
                }


                if (storyD.photo != "") {
                    val resourceId = storyD.photo
                    storageRef.child(resourceId!!).downloadUrl.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Glide.with(context)
                                .load(task.result)
                                .into(lostPhoto)
                        } else {
                            lostPhoto.setImageResource(check_category(storyD.category.toString()))
                        }
                    }
                } else {
                    lostPhoto.setImageResource(check_category(storyD.category.toString()))
                }


            }


            init {
                itemView.findViewById<View>(R.id.storyOutline)
            }
        }




        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StoriesViewHolder {
            var view = LayoutInflater.from(context).inflate(R.layout.story_recycler, parent, false)
            return StoriesViewHolder(view)
        }


        @RequiresApi(Build.VERSION_CODES.O)
        override fun onBindViewHolder(holder: StoriesViewHolder, position: Int) {

            holder.bind(stories[position], context)

        }

        override fun getItemCount(): Int {
            return stories.size
        }

        // viewholder


        init {
            this.context = context
        }

    }



}