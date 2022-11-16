package com.kgg.android.delivers

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.*
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.core.app.ActivityCompat
import androidx.core.graphics.createBitmap
import androidx.fragment.app.Fragment
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import com.kgg.android.delivers.databinding.FragmentMapSearchBinding


class MapSearchFragment : Fragment(), OnMapReadyCallback {
    // 실패한 fragment입니당.... UploadFragment로 가주세용
    private lateinit var auth: FirebaseAuth

    private lateinit var mView: MapView

    lateinit var gpsBtn: ImageButton


    lateinit var mContext: Context

    var uid = ""
    val firestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance("gs://delivers-65049.appspot.com/")
    private val storageRef: StorageReference = storage.reference
    var myLocation = Location(" ")
    var storyList = arrayListOf<Story>()
    var latitude = 0.0
    var longitude = 0.0

    private var mFusedLocationProviderClient: FusedLocationProviderClient? = null // 현재 위치를 가져오기 위한 변수
    lateinit var mLastLocation: Location // 위치 값을 가지고 있는 객체
    var mLocationRequest: LocationRequest = LocationRequest.create()// 위치 정보 요청의 매개변수를 저장하는
    private val REQUEST_PERMISSION_LOCATION = 10
    //lateinit var gmap:GoogleMap

    override fun onAttach(context: Context) {
        super.onAttach(context)

        if (context is MainActivity) {
            mContext = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        uid = auth.currentUser?.uid.toString()
        val appCollection = firestore.collection("users")







    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)






    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        // 구글 api 연동
        val binding = FragmentMapSearchBinding.inflate(inflater, container, false)
        //if (checkPermissionForLocation(requireContext())) {
            //startLocationUpdates()
            //Log.d("location test","${latitude}, ${longitude}")

        //}


        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        mLocationRequest =  LocationRequest.create().apply {

            priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        }

        mView = binding.mapView
        mView.onCreate(savedInstanceState)
        mView.getMapAsync(this)

















        return binding.root
    }

    // 지도 관련
    override fun onMapReady(googleMap: GoogleMap) {
        var gmap = googleMap

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            //User has previously accepted this permission
            if (ActivityCompat.checkSelfPermission(requireContext(),
                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                gmap.isMyLocationEnabled = true
            }
        } else {
            //Not in api-23, no need to prompt
            gmap.isMyLocationEnabled = true
        }



        gmap.uiSettings.setAllGesturesEnabled(true)
        var bitmap:Bitmap = createUserBitmap() // category 값 받아야 함
        var marker = LatLng(latitude,longitude) // intial location은 current location
        gmap!!.moveCamera(CameraUpdateFactory.newLatLng(marker)) // camera가 표시되는 위치를 말하는 건가?
        gmap!!.moveCamera(CameraUpdateFactory.zoomTo(15f))
        gmap!!.addMarker(MarkerOptions().position(marker).icon(BitmapDescriptorFactory.fromBitmap(bitmap)).title("여기")) // marker image 변경하기 (custom으로,,)






        gmap!!.setOnMapClickListener (object:GoogleMap.OnMapClickListener{
            override fun onMapClick(point: LatLng){
                gmap!!.clear()
                var lat:Double = point.latitude
                var long:Double = point.longitude
                latitude = lat
                longitude = long
                marker = LatLng(latitude,longitude)
                gmap!!.addMarker(MarkerOptions().position(marker).icon(BitmapDescriptorFactory.fromBitmap(bitmap)).title("여기")) // marker image 변경하기 (custom으로,,)
                gmap!!.moveCamera(CameraUpdateFactory.newLatLng(marker)) // camera가 표시되는 위치를 말하는 건가?
                gmap!!.moveCamera(CameraUpdateFactory.zoomTo(15f))
                Log.d("location test","${latitude}, ${longitude}") //현재 위치
            }
        })

        gpsBtn!!.setOnClickListener {
            if (checkPermissionForLocation(mContext)) {
                startLocationUpdates() // 현재 유저의 위치 받아오기
                Log.d("location test","${latitude}, ${longitude}") //현재 위치
                var currentLocat = LatLng(latitude,longitude)
                gmap!!.clear()
                gmap!!.addMarker(MarkerOptions().position(currentLocat).icon(BitmapDescriptorFactory.fromBitmap(bitmap)).title("여기"))
                gmap!!.moveCamera(CameraUpdateFactory.newLatLng(currentLocat)) // camera focus 움직이기
                gmap!!.moveCamera(CameraUpdateFactory.zoomTo(15f))


            }
        }
    }

    // 커스텀 마커
    private fun createUserBitmap(): Bitmap{
        var result:Bitmap = createBitmap(1000,1000)
        try {
            result = Bitmap.createBitmap(dp(62f).toInt(), dp(76f).toInt(), Bitmap.Config.ARGB_8888)
            result.eraseColor(Color.TRANSPARENT)
            val canvas = Canvas(result)

            val drawable = resources.getDrawable(R.drawable.marker)

            drawable.setBounds(0, 0, dp(62f).toInt(), dp(76f).toInt())
            drawable.draw(canvas)
            val roundPaint = Paint(Paint.ANTI_ALIAS_FLAG)
            val bitmapRect = RectF()
            canvas.save()
            val bitmap = BitmapFactory.decodeResource(resources, R.drawable.donut) // 카테고리별로 사진 바뀌어야 함!!!!
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
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(mContext)
        if (ActivityCompat.checkSelfPermission(mContext, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
            && ActivityCompat.checkSelfPermission(mContext,Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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