package com.kgg.android.delivers


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.kgg.android.delivers.databinding.FragmentUploadBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import net.daum.mf.map.api.MapPOIItem
import net.daum.mf.map.api.MapPOIItem.*
import net.daum.mf.map.api.MapPoint
import net.daum.mf.map.api.MapView
import java.time.LocalDateTime
import java.util.*
import kotlin.collections.HashMap

class UploadFragment : Fragment() {
    // 스토리 등록 페이지

    private lateinit var auth: FirebaseAuth

    // 위도, 경도 값 전역변수로 선언
    private var latitude:Double? = 0.0
    private var longitude:Double? = 0.0




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


        // initialize map & single marker
        var mapview:MapView = binding.mapView
        binding.mapView.setMapViewEventListener(mapEventListener)
        var marker = MapPOIItem()
        var bitmap:Bitmap = createUserBitmap()


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

        binding.mapView.setMapCenterPoint(marker.mapPoint,true)
        binding.mapView.addPOIItem(marker)

        if(binding.mapView.mapViewEventListener !=null){
            latitude = marker.mapPoint.mapPointGeoCoord.latitude
            longitude = marker.mapPoint.mapPointGeoCoord.longitude
        }

        // 생명 주기 동안 계속 실행하는 코드
        // 자동으로 latitude와 longitude의 값을 업데이트하기 위해 life cycle 동안 계속 실행하는 코루틴
        GlobalScope.launch {
            latitude = marker.mapPoint.mapPointGeoCoord.latitude
            longitude = marker.mapPoint.mapPointGeoCoord.longitude
        }


        // 좌표 담을 변수
        // marker.mapPoint.mapPointGeoCoord : 마커의 좌표값
        // 이거 쓰면 됨, latitude랑 longitude가 안되면, marker.mapPoint.mapPointGeoCoord 이거 그대로 가져다 쓰면 됩니당.
        latitude = marker.mapPoint.mapPointGeoCoord.latitude
        longitude = marker.mapPoint.mapPointGeoCoord.longitude





        // gps 버튼 눌렀을 때 현재 위치로 이동되도록
        gpsBtn.setOnClickListener{

            binding.mapView.currentLocationTrackingMode =
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

            binding.mapView.setMapCenterPoint(uNowPosition,true)

            marker.mapPoint =uNowPosition
            marker.markerType = MarkerType.CustomImage
            marker.customImageBitmap = bitmap
            binding.mapView.currentLocationTrackingMode = MapView.CurrentLocationTrackingMode.TrackingModeOff // trackingmode 해제








        }










        return binding.root
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
    private fun createUserBitmap(): Bitmap{
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
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.bagel) // 카테고리별로 사진 바뀌어야 함!!!!
            //Bitmap bitmap = BitmapFactory.decodeFile(path.toString()); /*generate bitmap here if your image comes from any url*/
            if (bitmap != null) {
                val shader = BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
                val matrix = Matrix()
                val scale: Float = dp(52.0f) / bitmap.width.toFloat()
                matrix.postTranslate(dp(5.0f), dp(5.0f))
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





}

