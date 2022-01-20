package com.example.project

import android.app.Service
import android.content.Intent
import android.os.*
import android.util.Log
import java.io.*
import java.net.InetSocketAddress
import java.net.Socket


class socket : Service() {
    //初始化常量
    companion object {
        const val TCP_CLIENT_GET_MSG_BUNDLE = "tcpClientGetMsg"
        const val TCP_CLIENT_GET_MSG = 4555
        const val TCP_CLIENT_CONNECT_STATUS_BUNDLE = "TCPConnectStatus"
        const val TCP_CLIENT_CONNECT_STATUS_MSG = 1245
    }

    private var tcpClient = Socket()
    private val encodingFormat = "GBK"
    private var tcpClientConnectStatus = false
    private var tcpClientTargetServerIP = "192.168.10.31"

    //private var tcpClientTargetServerIP = "138.3.211.111"
    private var tcpClientTargetServerPort = 8001
    private var tcpClientOutputStream: OutputStream? = null
    private var tcpClientInputStreamReader: InputStreamReader? = null
    private val tcpClientReceiveBuffer = StringBuffer()

    //--------------------------------------
    var roomname = ""
    var leader = ""
    var getip = ""
    var username = ""
    var userinfo = ""
    inner class SocketBinder : Binder() {
        val service: socket
            get() = this@socket
    }

    private val binder = SocketBinder()

    override fun onBind(intent: Intent): IBinder? {
        Log.e("socket info", "id ${Thread.currentThread().id.toString()}")
        val bundle = intent?.getBundleExtra("bundle_socket")
        leader = bundle?.getString("isleader").toString()
        roomname = bundle?.getString("roomname").toString()
        username = bundle?.getString("username").toString()
        if (roomname != null) {
            Log.e("socket RESULT", roomname)
        }
        if (leader != null) {
            Log.e("socket RESULT", leader)
        }
        return binder
        //return null
    }

    override fun onCreate() {
        Log.e("socket RESULT", "socket service created");

    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e("socket info", startId.toString() + " " + Thread.currentThread())
        return super.onStartCommand(intent, flags, startId)
    }

    //進入房間時建立socket連線
    fun funTCPClientConnect():Pair<Socket,String> {
        tcpClient = Socket()
        tcpClient.connect(
            InetSocketAddress(tcpClientTargetServerIP, tcpClientTargetServerPort),
            4000
        )
        Log.e("socket RESULT", "tcpclient ${tcpClient.toString()}")
        tcpClient.keepAlive = true

        /*val reader = tcpClient.getInputStream()
        val buf = ByteArray(1024)
        var len = 0
        while (reader.read(buf).let {len = it;it!=-1}){
            val str = String(buf, 0, len)
            Log.e("alluser",str)
            if(str.split())
        }*/

        val printWriter =
            PrintWriter(OutputStreamWriter(tcpClient.getOutputStream(), encodingFormat), true)
        tcpClientConnectStatus = true
        tcpClientInputStreamReader = InputStreamReader(tcpClient.getInputStream(), "GBK")
        val bufferedReader = BufferedReader(tcpClientInputStreamReader)
        val readLine = bufferedReader.readLine()
        Log.e("read",readLine)
        userinfo += "${readLine} ${roomname} ${username}"
        Log.e("userinfo", userinfo)
        printWriter.write(userinfo)
        printWriter.flush()

        return Pair(tcpClient,userinfo)
    }

    //客户端发送
    //需要子线程
    fun funTCPClientSend(msg: String) {
        Log.e("RESULT", "send ${msg}")
        if (msg.isNotEmpty() && tcpClientConnectStatus) {
            //这里要注意，只要曾经连接过，isConnected便一直返回true，无论现在是否正在连接
            if (tcpClient.isConnected) {
                try {
                    tcpClientOutputStream = tcpClient.getOutputStream()
                    val printWriter =
                        PrintWriter(
                            OutputStreamWriter(
                                tcpClientOutputStream,
                                encodingFormat
                            ), true
                        )
                    printWriter.write(msg + "\n")
                    printWriter.flush()
                    Log.e("信息发送成功", msg + "\n")

                } catch (e: IOException) {
                    Log.e("信息发送失败", msg)
                    e.printStackTrace()
                    tcpClientInputStreamReader?.close()
                    tcpClientOutputStream?.close()
                    tcpClient.close()
                }
            }
        }
    }

    //客户端接收的消息
    //添加子线程
    fun funTCPClientReceive(): String {
        Log.e("RESULT", "client success receive")
        Log.e("RESULT", "tcpClientConnectStatus ${tcpClientConnectStatus.toString()}")
        Log.e("RESULT", "tcpClient.isConnected ${tcpClient.isConnected.toString()}")
        while (tcpClientConnectStatus) {
            if (tcpClient.isConnected) {
                tcpClientInputStreamReader = InputStreamReader(tcpClient.getInputStream(), "GBK")
                val bufferedReader = BufferedReader(tcpClientInputStreamReader)
                val readLine = bufferedReader.readLine()
                val message = Message()
                val bundle = Bundle()
                bundle.putString(socket.TCP_CLIENT_GET_MSG_BUNDLE, readLine)
                message.what = socket.TCP_CLIENT_GET_MSG
                message.data = bundle
                handler.sendMessage(message)
                Log.e("recv RESULT", "test")
                Log.e("recv RESULT", readLine)
                getip = readLine
            } else {
                Log.e("开启客户端接收失败", "TCPClient")
                tcpClientInputStreamReader?.close()
                tcpClientOutputStream?.close()
                tcpClient.close()
                break
            }
        }
        return getip
    }
    //这是官方推荐的方法
    val handler = object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(msg: Message) {
            when (msg.what) {
                TCP_CLIENT_GET_MSG -> {
                    val string = msg.data.getString(socket.TCP_CLIENT_GET_MSG_BUNDLE)
                    tcpClientReceiveBuffer.append(string)
                    //txt_tcp_client_receive.text = tcpClientReceiveBuffer.toString()
                }
                TCP_CLIENT_CONNECT_STATUS_MSG -> {
                    val boolean = msg.data.getBoolean(socket.TCP_CLIENT_CONNECT_STATUS_BUNDLE)
                    if (!boolean) {
                        //switch_tcp_client_status.isChecked = false
                    }
                }
            }
        }
    }

    fun get_roomname(): String {
        return roomname
    }

    fun get_socket(): Socket {
        return tcpClient
    }
    fun get_status(): Boolean {
        return tcpClientConnectStatus
    }
}