package com.kgg.android.delivers

import android.content.Context
import android.content.Intent
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat.startActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.util.*
import com.bumptech.glide.Glide
import com.kgg.android.delivers.databinding.FragmentMainBinding
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import storyActivity.storyviewActivity

class MainFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    var uid = ""
    val firestore = FirebaseFirestore.getInstance()
    var myLocation = Location(" ")
    var storyList = arrayListOf<Story>()
    var latitude = 0.0
    var longitude = 0.0
    var currentLocation = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()
        val appCollection = firestore.collection("users")

        latitude = 37.631472
        longitude = 127.075987
        Log.d("CheckCurrentLocation", "현재 내 위치 값: $latitude, $longitude")

//        val pref = getSharedPreferences("isFirst", AppCompatActivity.MODE_PRIVATE)
//        val editor = pref.edit()
//        editor.putString("isFirst", "end")
//        editor.commit()


    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var mGeocoder: Geocoder = Geocoder(requireContext(), Locale.KOREAN)
        var mResultList: List<Address>? = null

        try {
            mResultList = mGeocoder.getFromLocation(
                latitude!!.toDouble(), longitude!!.toDouble(), 1
            )
            println("위치 정보 받아오기 성공")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (mResultList != null) {
            Log.d("CheckCurrentLocation", mResultList!![0].getAddressLine(0))
            currentLocation = mResultList!![0].getAddressLine(0)
            var currentplace = view.findViewById<TextView>(R.id.current_Place)
            Log.d("currentloc!", currentLocation.toString())
            currentplace.setText(currentLocation.split(" ").last())
            Log.d("currentlocation", currentplace.text.toString())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val binding = FragmentMainBinding.inflate(inflater, container, false)

        val sAdapter = StoriesAdapter(storyList, requireContext())
        binding.sRecyclerView.adapter = sAdapter


        val layout2 = LinearLayoutManager(requireContext()).also { it.orientation = LinearLayoutManager.HORIZONTAL }
        binding.sRecyclerView.layoutManager = layout2
        binding.sRecyclerView.setHasFixedSize(true)

        var doccol = firestore.collection("story")


        Log.d("CheckStoryList" , storyList.toString())
        Log.d("firestore" , firestore.toString())

        doccol.get()
            .addOnSuccessListener { result ->
                storyList.clear()
                var count = 0
                for (document in result) {
                    val item = Story(
                        document["writer"] as String,
                        document["Location"] as String,
                        document["photo"] as String,
                        document["description"] as String,
                        document["category"] as String,
                        document["latitude"] as Double,
                        document["longitude"] as Double,
                        document["registerDate"] as String
                    )

                    var targetLocation = Location("")
                    targetLocation.latitude =item.latitude
                    targetLocation.longitude = item.longitude


                    var distance = myLocation.distanceTo(targetLocation)

                    Log.d("distance",distance.toString())


                    if (count==0) {
                        storyList.add(item)
                        count++
                        continue
                    }
                    var longt = storyList[count-1].longitude
                    var ltit = storyList[count-1].latitude
                    var docLoc = Location("")
                    docLoc.longitude = longt
                    docLoc.latitude = ltit
                    var dt = myLocation.distanceTo(docLoc)
                    if(distance>=dt){
                        storyList.add(item)
                        count++
                    }
                    else{
                        for(i in 0 until count){
                            var longt = storyList[i].longitude
                            var ltit = storyList[i].latitude
                            var docLoc = Location("")
                            docLoc.longitude = longt
                            docLoc.latitude = ltit
                            var dt = myLocation.distanceTo(docLoc)
                            if(distance<dt){
                                storyList.add(Story("","","","","",0.0,0.0 , "" ))

                                for(c in count downTo i+1){
                                    storyList[c]=storyList[c-1]
                                }
                                storyList[i]=item
                                count++
                                break
                            }
                        }

                    }


                    //    imgArr.add(document["Imgurl"] as String)
                    //   desArr.add(document["description"] as String)
                    if(count==10)
                        break
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

                    val intent = Intent(requireContext(), storyviewActivity::class.java)

                    intent.putExtra("index", pos.toString())
                    //    intent.putStringArrayListExtra("imgArr", imgArr)
                    //  intent.putStringArrayListExtra("desArr", desArr)
                    intent.putParcelableArrayListExtra("StoryArr", storyList)
                    startActivity(intent)
                    activity?.overridePendingTransition(0, 0)

                }

            })
        }


        return binding.root

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

            private val lostPhoto = itemView.findViewById<ImageView>(R.id.story_imgSM)
//            private var storyTitle = itemView.findViewById<TextView>(R.id.story_Title)
//            private var ndaysbefore = itemView.findViewById<TextView>(R.id.ndays_before)
//            private var category = itemView.findViewById<TextView>(R.id.cate_gory)

            fun bind(lostD: Story, context: Context) {

                val pos = adapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    itemView.setOnClickListener {
                        listener?.onItemClick(itemView, lostD, pos)
                    }
                }

                val url = lostD.photo
//                storyTitle.text = lostD.title
//                if(lostD.title?.length!! >=15)
//                    storyTitle.text = lostD.title?.substring(0 until 14)+"..."
//                else
//                    storyTitle.text = lostD.title
////                ndaysbefore.text = lostD.registerDate
//                category.text = lostD.category


                if (lostD.photo != "") {
                    val resourceId = lostD.photo
                    storageRef.child(resourceId).downloadUrl.addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Glide.with(context)
                                .load(task.result)
                                .into(lostPhoto)
                        } else {
                            lostPhoto.setImageResource(R.mipmap.ic_launcher_round)
                        }
                    }
                } else {
                    lostPhoto.setImageResource(R.mipmap.ic_launcher_round)
                }


            }

            var storyOutline: CardView? = null

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