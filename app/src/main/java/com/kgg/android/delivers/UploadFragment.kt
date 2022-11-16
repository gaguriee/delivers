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
import androidx.core.app.ActivityCompat
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.kgg.android.delivers.databinding.FragmentMainBinding
import com.kgg.android.delivers.databinding.FragmentUploadBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPOIItem.*
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView
import storyActivity.storyviewActivity
import java.util.*
import kotlin.collections.HashMap

class UploadFragment : Fragment() {
    // 스토리 등록 페이지

    private lateinit var auth: FirebaseAuth

    // 위도, 경도 값 전역변수로 선언
    private var latitude:Double? = 0.0
    private var longitude:Double? = 0.0
    var currentLocation = ""

    private lateinit var mapview:MapView
    var mapViewContainer: RelativeLayout? = null

    // 카테고리 이름 담을 변수 선언
    var category_name = ""
    var chkCategory = 0






    var uid = ""
    val firestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance("gs://delivers-65049.appspot.com/")
    private val storageRef: StorageReference = storage.reference


    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null // 현재 위치를 가져오기 위한 변수
    lateinit var mLastLocation: Location // 위치 값을 가지고 있는 객체
    var mLocationRequest: LocationRequest = LocationRequest.create()// 위치 정보 요청의 매개변수를 저장하는
    private val REQUEST_PERMISSION_LOCATION = 10

    // 맵 커스텀 이벤트 리스너
    private val mapEventListener = CustomMapViewEventListener()

    lateinit var bindingFin:FragmentUploadBinding






    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()
        val appCollection = firestore.collection("users")

        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        mLocationRequest =  LocationRequest.create().apply {

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        }
        latitude = 37.631472
        longitude = 127.075987

        if (checkPermissionForLocation(requireContext())) {
            startLocationUpdates()
            Log.d("location test","${latitude}, ${longitude}")

        }






    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)



    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val binding = FragmentUploadBinding.inflate(inflater, container, false)
        bindingFin = FragmentUploadBinding.inflate(inflater, container, false)

        mapview = MapView(requireActivity())


        // initialize map & single marker

        // binding.mapView.setMapViewEventListener(mapEventListener)
        var marker = MapPOIItem()


        // category list

        var categoryList = arrayListOf<Category>(
            Category("chicken","chicken",R.drawable.chicken),
            Category("hamburger","hamburger",R.drawable.hamburger),
            Category("pizza","pizza",R.drawable.pizza),
            Category("coffee","coffee",R.drawable.coffee),
            Category("taco","taco",R.drawable.taco),
            Category("banana","banana",R.drawable.banana),
            Category("bread","bread",R.drawable.bread),
            Category("donut","donut",R.drawable.donut),
            Category("salad","salad",R.drawable.salad),
            Category("sushi","sushi",R.drawable.sushi),
            Category("guitar","guitar",R.drawable.guitar)
        )

        // 이미지 bitmap 초기화, 제일 처음엔 흰바탕
        var bitmap:Bitmap = createUserBitmap(R.drawable.motorscooter)

        // category adapter
        val fAdapter = CategoryAdapter(requireContext(),categoryList)
        // 카테고리 누르면 마커 이미지 변환되고, 카테고리 이름 변수에 카테고리가 저장됨
        /*fAdapter.setOnItemClickListener(object:CategoryAdapter.OnItemClickListener{
            override fun onItemClick(v:View,data:Category,pos:Int){
                bitmap = createUserBitmap(data.id)
                Toast.makeText(requireContext(), data.title,Toast.LENGTH_SHORT).show()
                marker.customImageBitmap = bitmap
                category_name = data.title
            }
        })*/

        binding.fRecyclerView.adapter = fAdapter

        // category recycler view
        val layout2 = LinearLayoutManager(requireContext()).also { it.orientation = LinearLayoutManager.HORIZONTAL }
        binding.fRecyclerView.layoutManager = layout2
        binding.fRecyclerView.setHasFixedSize(true)

        var mGeocoder: Geocoder = Geocoder(requireContext(), Locale.KOREAN)
        var mResultList: List<Address>? = null






        var gpsBtn = binding.currentGps // 현재 위치 버튼



        marker.itemName = "현 위치" // 어차피 화면에는 안나옴
        marker.isDraggable = true //
        marker.isCustomImageAutoscale = false
        marker.isShowCalloutBalloonOnTouch = false
        // 제일 처음엔 학교 주소로 초기화
        marker.mapPoint =MapPoint.mapPointWithGeoCoord(37.631472, 127.075987)
        marker.markerType = MarkerType.CustomImage
        marker.customImageBitmap = bitmap

        // 마커의 좌표값을 저장할 객체 -> marker의 userobject에 저장할 애들
        var locationMap = HashMap<String, Double?>()
        // 제일 처음에 초기화된 좌표값을 넣음
        locationMap.put("latitude",latitude)
        locationMap.put("longitude",longitude)

        marker.userObject = locationMap
        // 현 위치에 마커 찍기
        marker.mapPoint = MapPoint.mapPointWithGeoCoord(latitude!!, longitude!!)

        mapview.setMapCenterPoint(marker.mapPoint,true)
        mapview.addPOIItem(marker)

        // mapview event listener
        mapview.setMapViewEventListener(object:MapView.MapViewEventListener{
            override fun onMapViewInitialized(p0: MapView?) {
            }
            override fun onMapViewCenterPointMoved(p0: MapView?, p1: MapPoint?) {
            }
            override fun onMapViewZoomLevelChanged(p0: MapView?, p1: Int) {
            }
            override fun onMapViewSingleTapped(p0: MapView?, p1: MapPoint?) {
            }
            override fun onMapViewDoubleTapped(p0: MapView?, p1: MapPoint?) {

            }
            override fun onMapViewLongPressed(p0: MapView?, p1: MapPoint?) {
            }
            override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {
            }
            override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {
                var markers = p0!!.poiItems.iterator()

                while(markers.hasNext()){
                    var marker: MapPOIItem = markers.next() as MapPOIItem
                    marker.mapPoint = p0!!.mapCenterPoint
                    latitude = marker.mapPoint.mapPointGeoCoord.latitude
                    longitude = marker.mapPoint.mapPointGeoCoord.longitude
                }
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
                }
                binding.addressText.text = currentLocation
            }
            override fun onMapViewMoveFinished(p0: MapView?, p1: MapPoint?) {

                var markers = p0!!.poiItems.iterator()
                while(markers.hasNext()) {
                    var marker: MapPOIItem = markers.next() as MapPOIItem
                    marker.mapPoint = p0!!.mapCenterPoint
                    latitude = marker.mapPoint.mapPointGeoCoord.latitude
                    longitude = marker.mapPoint.mapPointGeoCoord.longitude
                }
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
                }
                binding.addressText.text = currentLocation

            }
        })

        mapview.setPOIItemEventListener(object:MapView.POIItemEventListener{
            override fun onPOIItemSelected(mapView: MapView?, poiItem: MapPOIItem?) {
            }
            override fun onCalloutBalloonOfPOIItemTouched(mapView: MapView?, poiItem: MapPOIItem?) {
            }
            override fun onCalloutBalloonOfPOIItemTouched(mapView: MapView?, poiItem: MapPOIItem?, buttonType: MapPOIItem.CalloutBalloonButtonType?) {
            }
            override fun onDraggablePOIItemMoved(mapView: MapView?, poiItem: MapPOIItem?, mapPoint: MapPoint?) {
                // 마커의 속성 중 isDraggable = true 일 때 마커를 이동시켰을 경우
                latitude = poiItem!!.mapPoint.mapPointGeoCoord.latitude
                longitude = poiItem!!.mapPoint.mapPointGeoCoord.longitude
                try {
                    mResultList = mGeocoder.getFromLocation(
                        latitude!!.toDouble(), longitude!!.toDouble(), 1
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (mResultList != null) {
                    Log.d("CheckCurrentLocation", mResultList!![0].getAddressLine(0))
                    currentLocation = mResultList!![0].getAddressLine(0)
                }
                binding.addressText.text = currentLocation
            }
        })




        mapViewContainer = RelativeLayout(requireContext())
        mapViewContainer?.layoutParams = RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)
        binding.mapContainer.addView(mapViewContainer)
        mapViewContainer?.addView(mapview)









        // gps 버튼 눌렀을 때 현재 위치로 이동되도록
        gpsBtn.setOnClickListener{

            mapview.currentLocationTrackingMode =
                MapView.CurrentLocationTrackingMode.TrackingModeOnWithoutHeading  //이 부분

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

            // Toast.makeText(requireContext(),"좌표 ${longitude}  ${latitude}",Toast.LENGTH_SHORT).show()

            //위도 , 경도
            latitude = userNowLocation?.latitude
            longitude = userNowLocation?.longitude
            val uNowPosition = MapPoint.mapPointWithGeoCoord(latitude!!, longitude!!)

            // 현 위치에 마커 찍기

            mapview.setMapCenterPoint(uNowPosition,true)

            marker.mapPoint =uNowPosition
            mapview.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOff // trackingmode 해제

        }



        // 등록 버튼
        binding.uploadComp.setOnClickListener{
            // 좌표 담을 변수
            // marker.mapPoint.mapPointGeoCoord : 마커의 좌표값
            // 이거 쓰면 됨, latitude랑 longitude가 안되면, marker.mapPoint.mapPointGeoCoord 이거 그대로 가져다 쓰면 됩니당.

            var markers2 = mapview.poiItems.iterator()
            while(markers2.hasNext()) {
                var marker: MapPOIItem = markers2.next() as MapPOIItem
                // marker.mapPoint = p0!!.mapCenterPoint
                latitude = marker.mapPoint.mapPointGeoCoord.latitude
                longitude = marker.mapPoint.mapPointGeoCoord.longitude
            }

            if(fAdapter.selectPos!=-1){ // 카테고리 설정하면,
                latitude = marker.mapPoint.mapPointGeoCoord.latitude
                longitude = marker.mapPoint.mapPointGeoCoord.longitude
                category_name = categoryList[fAdapter.selectPos].title

                // 주소로 변환해주는 코드
                try {
                    mResultList = mGeocoder.getFromLocation(
                        latitude!!.toDouble(), longitude!!.toDouble(), 1
                    )

                } catch (e: Exception) {
                    e.printStackTrace()
                }
                if (mResultList != null) {
                    Log.d("CheckCurrentLocation", mResultList!![0].getAddressLine(0))
                    currentLocation = mResultList!![0].getAddressLine(0)
                }
                binding.addressText.text = currentLocation


                //category_name
                Toast.makeText(requireContext(),"${latitude} ${longitude} ${category_name} ${currentLocation}",Toast.LENGTH_SHORT).show()

            }else{ // 카테고리 미선택 시,
                Toast.makeText(requireContext()," 카테고리를 설정해주세요.",Toast.LENGTH_SHORT).show()
            }



        }










        return binding.root
    }



    override fun onPause() {
        mapViewContainer?.removeView(mapview)
        bindingFin.mapContainer.removeView(mapview)
        bindingFin.mapContainer.removeAllViews()
        super.onPause()
    }

    override fun onDestroyView() {
        mapViewContainer?.removeView(mapview)
        bindingFin.mapContainer.removeView(mapview)
        bindingFin.mapContainer.removeAllViews()
        super.onDestroyView()
    }
    override fun onDestroy() {
        bindingFin.mapContainer.removeView(mapview)
        bindingFin.mapContainer.removeAllViews()
        super.onDestroy()
    }




    // 지도 이벤트 리스너!!!
    class CustomMapViewEventListener: MapView.MapViewEventListener{

        override fun onMapViewInitialized(p0: MapView?) {

        }

        override fun onMapViewCenterPointMoved(p0: MapView?, p1: MapPoint?) {

        }

        override fun onMapViewZoomLevelChanged(p0: MapView?, p1: Int) {

        }

        override fun onMapViewSingleTapped(p0: MapView?, p1: MapPoint?) {

        }

        override fun onMapViewDoubleTapped(p0: MapView?, p1: MapPoint?) {

        }

        override fun onMapViewLongPressed(p0: MapView?, p1: MapPoint?) {

        }

        override fun onMapViewDragStarted(p0: MapView?, p1: MapPoint?) {

        }

        override fun onMapViewDragEnded(p0: MapView?, p1: MapPoint?) {


            var markers = p0!!.poiItems.iterator()
            while(markers.hasNext()){
                var marker: MapPOIItem = markers.next() as MapPOIItem
                marker.mapPoint = p0!!.mapCenterPoint
            }


        }

        override fun onMapViewMoveFinished(p0: MapView?, p1: MapPoint?) {

            var markers = p0!!.poiItems.iterator()
            while(markers.hasNext()){
                var marker: MapPOIItem = markers.next() as MapPOIItem
                marker.mapPoint = p0!!.mapCenterPoint

            }

            // 지도의 중심 좌표
            // 마커 위치 설정
        }

    }










    // 커스텀 마커 이미지
    private fun createUserBitmap(id:Int): Bitmap{
        var result:Bitmap = createBitmap(1000,1000)
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
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.gift) // 카테고리별로 사진 바뀌어야 함!!!!
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

    class CategoryAdapter(val context: Context, val myList: ArrayList<Category>): RecyclerView.Adapter<CategoryAdapter.Holder>() {
        var selectPos = -1

        interface OnItemClickListener{
            fun onItemClick(v: View, data: Category, pos : Int)
        }
        private var listener : OnItemClickListener? = null // reference variable

        fun setOnItemClickListener(listener : OnItemClickListener) {
            this.listener = listener
        }

        inner class Holder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val contentPhoto = itemView?.findViewById<ImageView>(R.id.imgView_item) // 카테고리 넣을 거


            fun bind (mine: Category, context: Context) {
                /* if there is no image, set android basic image.*/
                if (mine.photo != "") {
                    val resourceId = context.resources.getIdentifier(mine.photo, "drawable", context.packageName)
                    contentPhoto?.setImageResource(resourceId)
                } else {
                    contentPhoto?.setImageResource(R.mipmap.ic_launcher)
                }
                /* binding data! */

                val pos = adapterPosition
                if(pos!= RecyclerView.NO_POSITION)
                {
                    itemView.setOnClickListener {
                        listener?.onItemClick(itemView,mine,pos)
                    }
                }


            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val view = LayoutInflater.from(context).inflate(R.layout.food_recycler, parent, false)
            return Holder(view)
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            holder?.bind(myList[position], context)

            if(selectPos == position) {
                holder.contentPhoto.setBackgroundColor(Color.parseColor("#55A8ED"))
            } else {
                holder.contentPhoto.setBackgroundColor(Color.TRANSPARENT)
            }


            holder.contentPhoto.setOnClickListener {
                var beforePos = selectPos
                selectPos = position

                notifyItemChanged(beforePos)
                notifyItemChanged(selectPos)
            }



        }

        override fun getItemCount(): Int {
            return myList.size
        }



    }





}
