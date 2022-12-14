package com.kgg.android.delivers

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.cardview.widget.CardView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.startActivity
import androidx.core.graphics.createBitmap
import androidx.core.view.size
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.bumptech.glide.Glide
import com.bumptech.glide.Glide.init
import com.kgg.android.delivers.RoomDB.AppDatabase
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.kgg.android.delivers.RoomDB.model.Read
import com.kgg.android.delivers.data.Story
import com.kgg.android.delivers.databinding.FragmentMainBinding
import com.kgg.android.delivers.StoryActivity.storyviewActivity
import com.kgg.android.delivers.UploadActivity.UploadFragment
import com.kgg.android.delivers.databinding.FragmentUploadBinding
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.CameraPosition
import java.util.*
import com.naver.maps.map.NaverMap
import com.naver.maps.map.MapView
import com.naver.maps.map.OnMapReadyCallback
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.Overlay
import com.naver.maps.map.overlay.OverlayImage
import kotlinx.android.synthetic.main.activity_storydetail.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import kotlin.collections.ArrayList

// ?????? ?????????
// ?????? - ????????? ????????? ?????? -> ????????? ?????????
// ?????? - ?????? ?????? (Naver Map API)

class  MainFragment : Fragment(), OnMapReadyCallback, Overlay.OnClickListener {

    lateinit var db: AppDatabase
    private lateinit var auth: FirebaseAuth
    var uid = ""
    val firestore = FirebaseFirestore.getInstance()
    var myLocation = Location(" ")
    var storyList = arrayListOf<Story>()
    var latitude = 0.0
    var longitude = 0.0
    var currentLocation = ""
    var readList : List<String> = listOf("")
    lateinit var apiService: ApiService // for getting image




    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null // ?????? ????????? ???????????? ?????? ??????
    lateinit var mLastLocation: Location // ?????? ?????? ????????? ?????? ??????
    var mLocationRequest: LocationRequest = LocationRequest.create()// ?????? ?????? ????????? ??????????????? ????????????
    private val REQUEST_PERMISSION_LOCATION = 10
    // Main Map
    companion object{
        lateinit var naverMain: NaverMap
    }

    private lateinit var main_map: MapView
    private lateinit var binding: FragmentMainBinding
    var markerArr = ArrayList<Marker>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()
        val appCollection = firestore.collection("users")






        latitude = 37.631472
        longitude = 127.075987
        Log.d("CheckCurrentLocation", "?????? ??? ?????? ???: $latitude, $longitude")

        if (checkPermissionForLocation(requireContext())) {
            startLocationUpdates()
            Log.d("location test","${latitude}, ${longitude}")

        }

    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        var mGeocoder: Geocoder = Geocoder(requireContext(), Locale.KOREAN)
        var mResultList: List<Address>? = null

        try {
            mResultList = mGeocoder.getFromLocation(
                latitude!!.toDouble(), longitude!!.toDouble(), 1
            )
            println("?????? ?????? ???????????? ??????")
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
        binding = FragmentMainBinding.inflate(inflater, container, false)

        main_map = binding.mainMap
        main_map.onCreate(savedInstanceState)



        var doccol = firestore.collection("story")


        // ?????? ????????????
        doccol.get()
            .addOnSuccessListener { result ->
                var count = 0
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
                        document["bool"] as Boolean
                    )

                    if(item.bool == true){
                        var bitmap = createUserBitmap(check_category(document["category"] as String))

                        var marker = Marker()
                        marker.position = LatLng(document["latitude"] as Double, document["longitude"] as Double)
                        marker.icon = OverlayImage.fromBitmap(bitmap)
                        // marker.tag = document["postId"] as String
                        marker.tag = document["postId"] as String
                        marker.onClickListener = this
                        markerArr.add(marker)
                    }

                }

                main_map.getMapAsync(this)
            }.addOnFailureListener { exception ->
                Log.w("ListActivity22", "Error getting documents: $exception")
            }




        // Map ??????

        main_map.getMapAsync(this)




        return binding.root

    }
    override fun onMapReady(naverMap: NaverMap) {
        MainFragment.naverMain = naverMap

        val uiSettings = naverMap.uiSettings

        // ?????? ??????

        var camPos = CameraPosition(
            LatLng(37.631472, 127.075987),
            16.0
        )
        naverMap.cameraPosition = camPos

        for(i in markerArr){
            i.map = naverMap
        }


        // gps ?????? ????????? ??? ?????? ????????? ???????????????
        binding.currentGpsMain.setOnClickListener{
            val lm: LocationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var userNowLocation: Location? = null


            if(lm.isProviderEnabled(LocationManager.GPS_PROVIDER)){
                if (checkPermissionForLocation(requireContext())) {
                    if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        //User has previously accepted this permission
                        if (ActivityCompat.checkSelfPermission(requireContext(),
                                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            userNowLocation = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                        }
                    } else {
                        //Not in api-23, no need to prompt
                        userNowLocation = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                    }
                    var lat = userNowLocation?.latitude
                    var long = userNowLocation?.longitude
                    if(lat!=null&&long!=null){
                        //?????? , ??????

                        naverMap.cameraPosition = CameraPosition(LatLng(lat as Double,long as Double),16.0)
                    }
                }else{
                    Toast.makeText(requireContext(),"GPS ????????? ??????????????????.",Toast.LENGTH_SHORT).show()
                }



            }else{
                Toast.makeText(requireContext(),"GPS??? ????????????.",Toast.LENGTH_SHORT).show()
            }



        }

        // var mGeo: Geocoder = Geocoder(requireContext(), Locale.KOREAN)
        // var mResultL: List<Address>? = null





    }

    override fun onClick(p0: Overlay): Boolean {
        if(p0 is Marker){

            var postid = p0.tag as String
            val intent = Intent(requireContext(), storyviewActivity::class.java)

            intent.putExtra("postId", postid)
            intent.putExtra("index","0")
            var storyList = arrayListOf<Story>()
            firestore.collection("story").document("$postid")
                .get().addOnSuccessListener { document ->
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
                        document["bool"] as Boolean
                    )
                    if(item.bool == true)
                        storyList.add(item)
                        Log.d("storyViewActivity_only","Error3 getting documents:")

                        intent.putParcelableArrayListExtra("StoryArr", storyList)
                        context?.startActivity(intent)

                        Log.d("storyViewActivity_only"," no Error getting documents:")
                }.addOnFailureListener { exception ->
                    Log.d("storyViewActivity_only","Error getting documents: $exception")
                }

            return true
        }
        return false
    }

    // gps ?????? ????????? ??? ?????? ?????? ???????????? ?????????
    private fun startLocationUpdates() {

        //FusedLocationProviderClient??? ??????????????? ??????.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        // ????????? ????????? ?????? ?????? ??????????????? ???????????? ????????? ??????
        // ????????? ?????? ?????????(Looper.myLooper())?????? ??????(mLocationCallback)?????? ?????? ??????????????? ??????
        mFusedLocationProviderClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
    }

    // ??????????????? ?????? ?????? ????????? ???????????? ??????
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // ??????????????? ?????? location ????????? onLocationChanged()??? ??????
            locationResult.lastLocation
            onLocationChanged(locationResult.lastLocation)
        }
    }

    // ??????????????? ?????? ?????? ??????????????? ????????? ??????????????? ?????????
    fun onLocationChanged(location: Location) {
        mLastLocation = location
        latitude = mLastLocation!!.latitude
        longitude = mLastLocation!!.longitude


        // mLastLocation.latitude // ?????? ??? ??????
        // mLastLocation.longitude // ?????? ??? ??????

    }


    // ?????? ????????? ????????? ???????????? ?????????
    private fun checkPermissionForLocation(context: Context): Boolean {
        // Android 6.0 Marshmallow ??????????????? ?????? ????????? ?????? ????????? ????????? ??????
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                // ????????? ???????????? ?????? ?????? ?????? ?????????
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION_LOCATION)
                false
            }
        } else {
            true
        }
    }

    // ??????????????? ?????? ?????? ??? ????????? ?????? ?????? ??????
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()

            } else {
                Log.d("ttt", "onRequestPermissionsResult() _ ?????? ?????? ??????")
            }
        }
    }


    open fun check_category(category:String):Int{
        var id = 0
        when(category)
        {
            "chicken" -> id =R.drawable.chicken //??????
            "hamburger"-> id = R.drawable.hamburger //??????
            "pizza" -> id = R.drawable.pizza //??????
            "coffee"->id =R.drawable.coffee //???????????????
            "bread"-> id =R.drawable.bread //????????????
            "meat"-> id = R.drawable.meat //??????
            "salad"-> id =R.drawable.salad //?????????
            "sushi"-> id =R.drawable.sushi //?????????
            "guitar"-> id =R.drawable.guitar //?????????
        }
        return id
    }

    override fun onStart() {
        super.onStart()
        main_map.onStart()
    }

    override fun onResume() {
        super.onResume()
        main_map.onResume()

        //Room DB

        db = Room.databaseBuilder(
            requireContext(),
            AppDatabase::class.java,
            "read_DB"
        ).build()
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("RoomDB", db.readDao().getAll().toString())        }

        CoroutineScope(Dispatchers.IO).launch {
            readList = db.readDao().getAllPid()
        }


        val sAdapter = StoriesAdapter(storyList, requireContext())
        binding.sRecyclerView.adapter = sAdapter


        val layout2 = LinearLayoutManager(requireContext()).also { it.orientation = LinearLayoutManager.HORIZONTAL }
        binding.sRecyclerView.layoutManager = layout2
        binding.sRecyclerView.setHasFixedSize(true)

        var doccol = firestore.collection("story")


        Log.d("CheckStoryList" , storyList.toString())
        Log.d("firestore" , firestore.toString())

        var readStory = arrayListOf<Story>()
        doccol.get()
            .addOnSuccessListener { result ->
                storyList.clear()
                var count = 0
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
                        document["bool"] as Boolean
                    )

                    // 24????????? ?????? post??? ?????? ????????????

                    var currTime =  System.currentTimeMillis()
                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("ko", "KR"))
                    var registerTime = dateFormat.parse(item.registerDate).time

                    var diffTime: Long = (currTime - registerTime) / 1000
                    if (diffTime >= 60*60*24) {
                        doccol.document(item.postId.toString())
                            .update("bool", false)
                    }


                    var targetLocation = Location("")
                    targetLocation.latitude =item.latitude!!
                    targetLocation.longitude = item.longitude!!


                    var distance = myLocation.distanceTo(targetLocation)

                    Log.d("distance",distance.toString())


                    if(item.bool == true){
                        if (item.postId in readList){
                            readStory.add(item)
                            continue
                        }
                        if (count==0 ) {
                            storyList.add(item)
                            count++
                            continue
                        }
                        var longt = storyList[count-1].longitude
                        var ltit = storyList[count-1].latitude
                        var docLoc = Location("")
                        docLoc.longitude = longt!!
                        docLoc.latitude = ltit!!
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
                                docLoc.longitude = longt!!
                                docLoc.latitude = ltit!!
                                var dt = myLocation.distanceTo(docLoc)
                                if(distance<dt){
                                    storyList.add(Story("","","","","",0.0,0.0 , "", "" ))
                                    for(c in count downTo i+1){
                                        storyList[c]=storyList[c-1]
                                    }
                                    storyList[i]=item
                                    count++
                                    break
                                }
                            }

                        }
                    }
                }

                Log.d("alreadyread", storyList.toString())
                storyList.addAll(readStory)

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
    }

    override fun onPause() {
        super.onPause()
        main_map.onPause()
    }


    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        main_map.onSaveInstanceState(outState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        main_map.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        main_map.onLowMemory()
    }

    // ????????? ?????? ?????????
    private fun createUserBitmap(id:Int): Bitmap {
        var result: Bitmap = createBitmap(1000,1000)
        try {
            result = Bitmap.createBitmap(dp(62f).toInt(), dp(76f).toInt(), Bitmap.Config.ARGB_8888)
            result.eraseColor(Color.TRANSPARENT)
            val canvas = Canvas(result)

            val drawable = resources.getDrawable(R.drawable.marker1)

            drawable.setBounds(0, 0, dp(62f).toInt(), dp(76f).toInt())
            drawable.draw(canvas)
            val roundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val bitmapRect = RectF()
            canvas.save()
            val bitmap = BitmapFactory.decodeResource(resources, id) // ?????????????????? ?????? ???????????? ???!!!!
            //Bitmap bitmap = BitmapFactory.decodeFile(path.toString()); /*generate bitmap here if your image comes from any url*/
            if (bitmap != null) {
                val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                val matrix = Matrix()
                val scale: Float = dp(45.0f) / bitmap.width.toFloat()
                matrix.postTranslate(dp(22.0f), dp(15.0f))
                matrix.postScale(scale, scale)
                roundPaint.setShader(shader)
                shader.setLocalMatrix(matrix)
                bitmapRect[dp(5.0f), dp(5.0f), dp(57.0f)] = dp(52.0f + 5.0f)
                canvas.drawRoundRect(bitmapRect, dp(26.0f), dp(26.0f), roundPaint)
            }
            canvas.restore()
            try {
                canvas.setBitmap(null)
            } catch (e: Exception) {
            }
        } catch (t: Throwable) {
            t.printStackTrace()
        }
        return result

    }

    //
    fun dp(value: Float): Float {
        return if (value == 0f) {
            0.0f
        } else Math.ceil((resources.displayMetrics.density * value).toDouble()).toFloat()
    }

    class StoriesAdapter(private val stories: ArrayList<Story>, context: Context) :
        RecyclerView.Adapter<StoriesAdapter.StoriesViewHolder>() {

        open fun check_category(category:String):Int{
            var id = 0
            when(category)
            {
                "chicken" -> id =R.drawable.chicken //??????
                "hamburger"-> id = R.drawable.hamburger //??????
                "pizza" -> id = R.drawable.pizza //??????
                "coffee"->id =R.drawable.coffee //???????????????
                "bread"-> id =R.drawable.bread //????????????
                "meat"-> id = R.drawable.meat //??????
                "salad"-> id =R.drawable.salad //?????????
                "sushi"-> id =R.drawable.sushi //?????????
                "guitar"-> id =R.drawable.guitar //??????
            }
            return id
        }


        private val storage: FirebaseStorage =
            FirebaseStorage.getInstance("gs://delivers-65049.appspot.com/")
        private val storageRef: StorageReference = storage.reference

        interface OnItemClickListener {
            fun onItemClick(v: View, data: Story, pos: Int){

            }
        }

        private var listener: OnItemClickListener? = null
        fun setOnItemClickListener(listener: OnItemClickListener) {
            this.listener = listener

        }


        private val context: Context

        inner class StoriesViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

            private val Photo = itemView.findViewById<ImageView>(R.id.story_imgSM)
            private val Icon = itemView.findViewById<ImageView>(R.id.storyIcon)
            private val storyOutline = itemView.findViewById<CardView>(R.id.storyOutline)


            @SuppressLint("ResourceAsColor")
            fun bind(storyD: Story, context: Context) {

                if(fragmentMain.readList.contains(storyD.postId)){
                    val color: Int = R.color.black
                    storyOutline.setCardBackgroundColor(color)
                }
                else{
                    val color: Int =  Color.parseColor("#31A9F3")
                    storyOutline.setCardBackgroundColor(color)
                }

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
                            Icon.setImageResource(check_category(storyD.category.toString()))
                            Glide.with(context)
                                .load(task.result)
                                .into(Photo)
                        } else {
                            Photo.setImageResource(check_category(storyD.category.toString()))
                            Icon.setImageResource(check_category(storyD.category.toString()))

                        }
                    }
                } else {
                    Photo.setImageResource(check_category(storyD.category.toString()))
                    Icon.setImageResource(check_category(storyD.category.toString()))

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

            holder.itemView.setOnClickListener {
                CoroutineScope(Dispatchers.IO).launch {
                    if(!(stories[position].postId in fragmentMain.readList))
                        fragmentMain.db.readDao().insertRead(Read(stories[position].postId))
                }
                Log.d("readList", fragmentMain.readList.toString())
                val intent = Intent( context, storyviewActivity::class.java)


                intent.putExtra("postId", stories[position].postId)
                intent.putExtra("index",position.toString())
                intent.putParcelableArrayListExtra("StoryArr", stories)
                context.startActivity(intent)

            }

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