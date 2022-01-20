package com.example.project

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.CountDownTimer
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.example.project.databinding.ActivityMapsBinding
import com.google.android.gms.common.api.Api
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.Circle
import com.google.android.gms.maps.model.CircleOptions
import java.io.*
import java.net.Socket
import kotlin.concurrent.thread
import android.view.View

import android.R.attr.name
import android.text.Layout
import android.widget.*
import androidx.constraintlayout.widget.ConstraintSet


const val REQUEST_LOCATION_PERMISSION = 1
const val REQUEST_ENABLE_GPS = 2

private var items : ArrayList<String> = ArrayList()  //定義資料清單
private lateinit var madapter: ArrayAdapter<String>   //定義 adapter 繼承 ArrayAdapter
lateinit var shopdialog: AlertDialog.Builder
lateinit var dialog: AlertDialog

class MapsActivity : AppCompatActivity(),
    GoogleMap.OnMyLocationButtonClickListener,
    GoogleMap.OnMyLocationClickListener, OnMapReadyCallback {
    //---------------------------------------------------------------------------
    private var socketservice: socket? = null
    private var isBind = false
    private var tcpClient = Socket()
    private var tcpClientConnectStatus = false
    private val encodingFormat = "GBK"
    private var getsocket = object: ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isBind = true
            val myBinder = service as socket.SocketBinder
            socketservice = myBinder.service
            val sthread = Thread{
                try {
                    tcpClient = socketservice!!.get_socket()
                    tcpClientConnectStatus = true
                    Log.d("tcpclient", tcpClient.toString())
                } catch (e: IOException) {
                    Log.e("THREAD ERROR", "THREAD ERROR")
                    e.printStackTrace()
                    tcpClient.close()
                }

            }
            sthread.start()
            sthread.interrupt()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBind = false
            Log.d("RESULT","disconnected")
        }
    }
    //---------------------------------------------------------------------------
    private lateinit var mMap: GoogleMap
    private lateinit var binding: ActivityMapsBinding
    private lateinit var mContext: Context
    private lateinit var mFusedLocationProviderClient: FusedLocationProviderClient
    private var locationPermissionGranted = false
    private var circle_list: MutableList<Circle>  = mutableListOf()
    var radius = 250.0 //鬼的半徑
    var remain_time = 0
    var user_info = ""
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMapsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        mContext = this
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        val time = findViewById<TextView>(R.id.time)
        //---------------------------------------------------------------------------
        val bundle= intent?.getBundleExtra("bundle_info")
        user_info = bundle?.getString("bundle_info").toString()
        Log.d("userinfo",user_info)
        val serviceintent = Intent(this,socket::class.java)
        startService(serviceintent)
        bindService(serviceintent, getsocket, Context.BIND_AUTO_CREATE)

        val shop = findViewById<Button>(R.id.btn_shop)
        shop.setOnClickListener {
            //val intent_shop = Intent(this, shop::class.java)
            //startActivity(intent_shop)
            /*items.add("item1 10")
            items.add("item2 20")
            shopdialog = AlertDialog.Builder(this)
            val shopView = View.inflate(this, R.layout.shop, null)
            val itemlist = findViewById<ListView>(R.id.LV_ITEM)
            madapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, items)
            itemlist.adapter = madapter
            shopdialog.setView(shopView)
            shopdialog.setTitle("test")
            dialog = shopdialog.create()
            dialog.show()*/
            /*val builder = AlertDialog.Builder(this)
            builder.setTitle("商店管理系統")
            val animals = arrayOf("item1", "item2", "item3", "item4")
            val checkedItems = booleanArrayOf(false, false, false, false)
            builder.setMultiChoiceItems(animals, checkedItems) { dialog, which, isChecked ->
                // user checked or unchecked a box
            }
            builder.setPositiveButton("確認購買"){
                    dialog, which ->
            }
            builder.setNegativeButton("Cancel", null)
            val dialog = builder.create()
            dialog.show()*/
            /*AlertDialog.Builder(this).apply {
                //构建一个对话框
                //apply标准函数自动返回调用对象本身
                setTitle("商店管理系統")//表示
                //setMessage("手机太冷了")//内容
                setView(shopView)
                setCancelable(false)//是否使用Back关闭对话框
                setPositiveButton("ok"){//确认按钮点击事件
                        dialog, which ->
                }

                setNegativeButton("取消"){ //设置取消按钮
                        dialog, which ->
                }
                show()

            }*/
        }
        //---------------------------------------------------------------------------
        object : CountDownTimer(200000, 1000) {

            override fun onFinish() {
                time.text = getString(R.string.done)
            }

            override fun onTick(millisUntilFinished: Long) {
                remain_time = (millisUntilFinished/1000).toInt()
                if (remain_time < 100){
                    radius = 120.0
                    time.setTextColor(Color.rgb(255, 0, 0))
                }
                if (remain_time < 50){
                    radius = 80.0
                    time.setTextColor(Color.rgb(255, 0, 0))
                }
                time.text = getString(R.string.remain).plus("${remain_time}")
            }

        }.start()



    }

    private fun getLocationPermission() {
        //檢查權限
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            //已獲取到權限
            locationPermissionGranted = true
            checkGPSState()
        } else {
            //詢問要求獲取權限
            requestLocationPermission()
        }
    }

    private fun checkGPSState() {
        val locationManager = mContext.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            AlertDialog.Builder(mContext)
                .setTitle("GPS 尚未開啟")
                .setMessage("使用此功能需要開啟 GSP 定位功能")
                .setPositiveButton("前往開啟",
                    DialogInterface.OnClickListener { _, _ ->
                        startActivityForResult(
                            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS), REQUEST_ENABLE_GPS
                        )
                    })
                .setNegativeButton("取消", null)
                .show()
        } else {
            Toast.makeText(this, "已獲取到位置權限且GPS已開啟，可以準備開始獲取經緯度", Toast.LENGTH_SHORT).show()
            getDeviceLocation()
        }
    }
    //刪除舊的範圍
    private fun delete_old_circle() {
        for(i in 0 until circle_list.size) {
            var circle: Circle? = circle_list.get(i)
            circle?.remove()
            Log.d("HKT","delete")
        }
        circle_list.clear()
    }
    private fun getDeviceLocation() {
        try {
            if (locationPermissionGranted
            ) {
                val locationRequest = LocationRequest()
                //開啟回到我的位置按鈕，使用這個按鈕前必須確定有存取地圖的權限，不然會閃退
                mMap.isMyLocationEnabled = true
                locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                //更新頻率
                locationRequest.interval = 10000
                //更新次數，若沒設定，會持續更新
                //locationRequest.numUpdates = 2
                mFusedLocationProviderClient.requestLocationUpdates(
                    locationRequest,
                    object : LocationCallback() {
                        override fun onLocationResult(locationResult: LocationResult?) {
                            locationResult ?: return
                            Log.d(
                                "HKT",
                                "緯度:${locationResult.lastLocation.latitude} , 經度:${locationResult.lastLocation.longitude} "
                            )
                            val wthread = Thread{
                                var tcpClientOutputStream: OutputStream? = null
                                try {
                                    if(tcpClient.isConnected){
                                        tcpClientOutputStream = tcpClient.getOutputStream()
                                        val printWriter =
                                            PrintWriter(
                                                OutputStreamWriter(
                                                    tcpClientOutputStream,
                                                    encodingFormat
                                                ), true
                                            )
                                        //定時傳送位置給server
                                        val mylocation = "${locationResult.lastLocation.latitude} ${locationResult.lastLocation.longitude}"
                                        printWriter.write(mylocation)
                                        printWriter.flush()
                                        Log.d("HKT", mylocation + "\n")
                                    }
                                } catch (e: IOException) {
                                    Log.e("THREAD ERROR", "THREAD ERROR")
                                    e.printStackTrace()
                                    tcpClientOutputStream?.close()
                                    tcpClient.close()
                                }

                            }
                            val rthread = Thread{
                                val reader = tcpClient.getInputStream()
                                try{
                                    val buf = ByteArray(1024)
                                    var len = 0
                                    while (reader.read(buf).let {len = it;it!=-1}){
                                        val str = String(buf, 0, len)
                                        Log.d("HKT",str)
                                    }
                                } catch (e: IOException) {
                                    Log.e("THREAD ERROR", "THREAD ERROR")
                                    e.printStackTrace()
                                    reader?.close()
                                    tcpClient.close()
                                }

                            }
                            wthread.start()
                            wthread.interrupt()
                            rthread.start()
                            rthread.interrupt()
                            //val currentLocation =
                            //    LatLng(
                            //        locationResult.lastLocation.latitude,
                            //        locationResult.lastLocation.longitude
                            //    )
                            val currentLocation =
                                LatLng(
                                    locationResult.lastLocation.latitude,
                                    locationResult.lastLocation.longitude
                                )
                            //mMap?.addMarker(
                            //    MarkerOptions().position(currentLocation).title("現在位置")
                            //)
                            mMap?.moveCamera(
                                CameraUpdateFactory.newLatLngZoom(
                                    currentLocation, 17f
                                )
                            )
                            var circle: Circle? = null
                            //添加半徑
                            fun drawCircle(latitude: Double, longitude: Double, radius: Double) {
                                val circleOptions = CircleOptions()
                                    .center(LatLng(latitude, longitude))
                                    .radius(radius)
                                    .strokeWidth(5.0f)
                                    .strokeColor(Color.RED)
                                delete_old_circle()
                                circle = mMap?.addCircle(circleOptions) // Draw new circle.
                                circle_list.add(circle!!)
                            }
                            drawCircle(locationResult.lastLocation.latitude,locationResult.lastLocation.longitude,radius)
                        }
                    },
                    null
                )

            } else {
                getLocationPermission()
            }
        } catch (e: SecurityException) {
            Log.e("Exception: %s", e.message, e)
        }
    }

    private fun requestLocationPermission() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            )
        ) {
            AlertDialog.Builder(this)
                .setMessage("需要位置權限")
                .setPositiveButton("確定") { _, _ ->
                    ActivityCompat.requestPermissions(
                        this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        REQUEST_LOCATION_PERMISSION
                    )
                }
                .setNegativeButton("取消") { _, _ -> requestLocationPermission() }
                .show()
        } else {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), REQUEST_LOCATION_PERMISSION
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                if (grantResults.isNotEmpty()) {
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        //已獲取到權限
                        locationPermissionGranted = true
                        checkGPSState()
                    } else if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                        if (!ActivityCompat.shouldShowRequestPermissionRationale(
                                this,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        ) {
                            //權限被永久拒絕
                            Toast.makeText(this, "位置權限已被關閉，功能將會無法正常使用", Toast.LENGTH_SHORT).show()

                            AlertDialog.Builder(this)
                                .setTitle("開啟位置權限")
                                .setMessage("此應用程式，位置權限已被關閉，需開啟才能正常使用")
                                .setPositiveButton("確定") { _, _ ->
                                    val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                                    startActivityForResult(intent, REQUEST_LOCATION_PERMISSION)
                                }
                                .setNegativeButton("取消") { _, _ -> requestLocationPermission() }
                                .show()
                        } else {
                            //權限被拒絕
                            Toast.makeText(this, "位置權限被拒絕，功能將會無法正常使用", Toast.LENGTH_SHORT).show()
                            requestLocationPermission()
                        }
                    }
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            REQUEST_LOCATION_PERMISSION -> {
                getLocationPermission()
            }
            REQUEST_ENABLE_GPS -> {
                checkGPSState()
            }
        }
    }
    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.setOnMyLocationButtonClickListener(this)
        mMap.setOnMyLocationClickListener(this)
        getLocationPermission()
    }

    //按下右上回我的位置時顯示
    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT)
            .show()
        return false
    }

    //按下藍色點會顯示目前座標
    override fun onMyLocationClick(location: Location) {
        Toast.makeText(this, "Current location:\n$location", Toast.LENGTH_LONG)
            .show()
    }
}