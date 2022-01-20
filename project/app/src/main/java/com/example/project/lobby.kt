package com.example.project

import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity


class lobby : AppCompatActivity()  {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lobby)

        val createroom = findViewById<Button>(R.id.CREATEROOM)
        val searchroom = findViewById<ImageButton>(R.id.SEARCHROOM)

        fun getRandomString(length: Int) : String {
            val allowedChars = "1234567890ABCDEFGHIJKLMNOPQRSTUVWXTZabcdefghiklmnopqrstuvwxyz"
            return (1..length)
                .map { allowedChars.random() }
                .joinToString("")
        }

        //創建新房間，隨機生成長度8亂數
        createroom.setOnClickListener {
            val room_name = getRandomString(8)
            val get_username = findViewById<EditText>(R.id.et_username)
            val user_name = get_username.getText().toString()
            val bundle_room= Bundle().apply {
                putString("roomname", room_name)
                putString("isleader", "is leader")
                putString("username",user_name)
            }
            //跳轉到room畫面
            val intent_room = Intent(this, room::class.java)
            intent_room.putExtra("bundle_room", bundle_room)
            startActivity(intent_room)
        }
        //搜尋房間
        searchroom.setOnClickListener {
            val get_room_name = findViewById<EditText>(R.id.et_searchroom)
            val room_name = get_room_name.getText().toString()
            val get_username = findViewById<EditText>(R.id.et_username)
            val user_name = get_username.getText().toString()
            val bundle_room= Bundle().apply {
                putString("roomname", room_name)
                putString("isleader", "not leader")
                putString("username",user_name)
            }
            //跳轉到room畫面
            val intent_room = Intent(this, room::class.java)
            intent_room.putExtra("bundle_room", bundle_room)
            startService(intent_room)
        }
    }
}