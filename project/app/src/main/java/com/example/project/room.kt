package com.example.project

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.Api
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.IOException
import java.net.Socket
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.thread


class room : AppCompatActivity()   {
    private var socketservice: socket? = null
    private var isBind = false
    private var tcpClient = Socket()
    private var tcpClientConnectStatus = false
    private var items = ArrayList<String>()
    var roomname = ""
    var leader = ""
    var username = ""
    var info = ""
    private var conn = object: ServiceConnection{
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            isBind = true
            val myBinder = service as socket.SocketBinder
            socketservice = myBinder.service
            val connthread = Thread{
                try{
                    val (Client,userinfo) = socketservice!!.funTCPClientConnect()
                    tcpClient = Client
                    tcpClientConnectStatus = true
                    Log.e("from_socket",userinfo)
                    info = userinfo
                }catch (e: IOException) {
                    Log.e("THREAD ERROR", "THREAD ERROR")
                    e.printStackTrace()
                    tcpClient.close()
                }

            }
            connthread.start()
            connthread.interrupt()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            isBind = false
            Log.d("RESULT","disconnected")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_room)

        val room_name = findViewById<TextView>(R.id.room_name)
        val start_game = findViewById<Button>(R.id.start)
        val mRecyclerView = findViewById<RecyclerView>(R.id.view_user)

        val bundle= intent?.getBundleExtra("bundle_room")
        leader= bundle?.getString("isleader").toString()
        roomname= bundle?.getString("roomname").toString()
        username= bundle?.getString("username").toString()
        room_name.setText(roomname)
        items.add(username)
        val layoutManager = LinearLayoutManager(this)
        layoutManager.orientation = LinearLayoutManager.VERTICAL
        mRecyclerView.layoutManager = layoutManager
        mRecyclerView.adapter = Recycleviewadapter(items)
        //開始遊戲
        start_game.setOnClickListener {
            val bundle_info= Bundle().apply {
                putString("userinfo",info)
                putString("tcpclient", tcpClient.toString())
            }

            val intent = Intent(this, MapsActivity::class.java)
            intent.putExtra("bundle_info", bundle_info)
            startActivity(intent)
        }
        //get service
        val bundle_socket= Bundle().apply {
            putString("roomname", roomname)
            putString("isleader", leader)
            putString("username", username)
        }
        val serviceintent = Intent(this,socket::class.java)
        serviceintent.putExtra("bundle_socket",bundle_socket)
        startService(serviceintent)
        bindService(serviceintent, conn, Context.BIND_AUTO_CREATE)
    }

}