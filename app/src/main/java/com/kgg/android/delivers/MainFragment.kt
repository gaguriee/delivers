package com.kgg.android.delivers

import android.Manifest
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
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
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
import kotlin.collections.ArrayList

class MainFragment : Fragment(), OnMapReadyCallback, Overlay.OnClickListener {

    private lateinit var auth: FirebaseAuth
    var uid = ""
    val firestore = FirebaseFirestore.getInstance()
    var myLocation = Location(" ")
    var storyList = arrayListOf<Story>()
    var latitude = 0.0
    var longitude = 0.0
    var currentLocation = ""

    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null // 현재 위치를 가져오기 위한 변수
    lateinit var mLastLocation: Location // 위치 값을 가지고 있는 객체
    var mLocationRequest: LocationRequest = LocationRequest.create()// 위치 정보 요청의 매개변수를 저장하는
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
        Log.d("CheckCurrentLocation", "현재 내 위치 값: $latitude, $longitude")

        if (checkPermissionForLocation(requireContext())) {
            startLocationUpdates()
            Log.d("location test","${latitude}, ${longitude}")

        }

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
        binding = FragmentMainBinding.inflate(inflater, container, false)

        main_map = binding.mainMap
        main_map.onCreate(savedInstanceState)
        //



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
                        document["location"] as String,
                        document["photo"] as String,
                        document["description"] as String,
                        document["category"] as String,
                        document["latitude"] as Double,
                        document["longitude"] as Double,
                        document["registerDate"] as String,
                        document["postId"] as String,
                    )

                    var bitmap = createUserBitmap(check_category(document["category"] as String))

                    var marker = Marker()
                    marker.position = LatLng(document["latitude"] as Double, document["longitude"] as Double)
                    marker.icon = OverlayImage.fromBitmap(bitmap)
                    // marker.tag = document["postId"] as String
                    marker.tag = document["postId"] as String
                    marker.onClickListener = this
                    markerArr.add(marker)





                    var targetLocation = Location("")
                    targetLocation.latitude =item.latitude!!
                    targetLocation.longitude = item.longitude!!


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


                    //    imgArr.add(document["Imgurl"] as String)
                    //   desArr.add(document["description"] as String)
                    if(count==10)
                        break
                }

                if (sAdapter != null) {
                    sAdapter.notifyDataSetChanged()
                    main_map.getMapAsync(this)
                }
                main_map.getMapAsync(this)

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


        // Map 파트

        var gpsBtn = binding.currentGpsMain // 현재 위치 버튼

//        // gps 버튼 눌렀을 때 현재 위치로 이동되도록
//        gpsBtn.setOnClickListener{
//
//
//            val lm: LocationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
//            var userNowLocation: Location? = null
//            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
//                //User has previously accepted this permission
//                if (ActivityCompat.checkSelfPermission(requireContext(),
//                        Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
//                    userNowLocation = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
//                }
//            } else {
//                //Not in api-23, no need to prompt
//                userNowLocation = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
//            }
//
//
//            //위도 , 경도
//            var lat = userNowLocation?.latitude
//            var long = userNowLocation?.longitude
//
//
//        }

        main_map.getMapAsync(this)




        return binding.root

    }
    override fun onMapReady(naverMap: NaverMap) {
        MainFragment.naverMain = naverMap

        val uiSettings = naverMap.uiSettings

        // 제일 처음

        var camPos = CameraPosition(
            LatLng(37.631472, 127.075987),
            16.0
        )
        naverMap.cameraPosition = camPos

        for(i in markerArr){
            i.map = naverMap
        }


        // gps 버튼 눌렀을 때 현재 위치로 이동되도록
        binding.currentGpsMain.setOnClickListener{
            val lm: LocationManager = requireContext().getSystemService(Context.LOCATION_SERVICE) as LocationManager
            var userNowLocation: Location? = null
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
                //위도 , 경도

                naverMap.cameraPosition = CameraPosition(LatLng(lat as Double,long as Double),16.0)
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
                    )
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

    // gps 버튼 눌렀을 때 현재 위치 받아오는 함수들
    private fun startLocationUpdates() {

        //FusedLocationProviderClient의 인스턴스를 생성.
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        if (ActivityCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(requireContext(),Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        // 기기의 위치에 관한 정기 업데이트를 요청하는 메서드 실행
        // 지정한 루퍼 스레드(Looper.myLooper())에서 콜백(mLocationCallback)으로 위치 업데이트를 요청
        mFusedLocationProviderClient!!.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper())
    }

    // 시스템으로 부터 위치 정보를 콜백으로 받음
    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            // 시스템에서 받은 location 정보를 onLocationChanged()에 전달
            locationResult.lastLocation
            onLocationChanged(locationResult.lastLocation)
        }
    }

    // 시스템으로 부터 받은 위치정보를 화면에 갱신해주는 메소드
    fun onLocationChanged(location: Location) {
        mLastLocation = location
        latitude = mLastLocation!!.latitude
        longitude = mLastLocation!!.longitude


        // mLastLocation.latitude // 갱신 된 위도
        // mLastLocation.longitude // 갱신 된 경도

    }


    // 위치 권한이 있는지 확인하는 메서드
    private fun checkPermissionForLocation(context: Context): Boolean {
        // Android 6.0 Marshmallow 이상에서는 위치 권한에 추가 런타임 권한이 필요
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                // 권한이 없으므로 권한 요청 알림 보내기
                ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_PERMISSION_LOCATION)
                false
            }
        } else {
            true
        }
    }

    // 사용자에게 권한 요청 후 결과에 대한 처리 로직
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates()

            } else {
                Log.d("ttt", "onRequestPermissionsResult() _ 권한 허용 거부")
            }
        }
    }


    fun check_category(category:String):Int{
        var id = 0
        when(category)
        {
            "chicken" -> id =R.drawable.chicken //치킨
            "hamburger"-> id =R.drawable.hamburger //버거
            "pizza" -> id =R.drawable.pizza //피자
            "coffee"->id =R.drawable.coffee //카페디저트
            "bread"-> id =R.drawable.bread //샌드위치
            "meat"-> id =R.drawable.meat //고기
            "salad"-> id =R.drawable.salad //샐러드
            "sushi"-> id =R.drawable.sushi //회초밥
            "guitar"-> id =R.drawable.guitar //기타
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

    // 커스텀 마커 이미지
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
            val bitmap = BitmapFactory.decodeResource(resources, id) // 카테고리별로 사진 바뀌어야 함!!!!
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
                    storageRef.child(resourceId!!).downloadUrl.addOnCompleteListener { task ->
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