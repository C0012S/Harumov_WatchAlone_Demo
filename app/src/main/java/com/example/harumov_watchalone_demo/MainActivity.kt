package com.example.harumov_watchalone_demo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button

class MainActivity : AppCompatActivity() {

    lateinit var btnRecord : Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var id = "allonsy-sh"

        // 감상하기 버튼
        btnRecord = findViewById(R.id.btnRecord)
        btnRecord.setOnClickListener { // 감상하기 버튼 클릭 시 영화 검색 페이지로 이동
            val intent = Intent(this, SearchActivity::class.java) // 영화 검색 페이지
            intent.putExtra("user_id", id)
            startActivity(intent)
        }
    }
}