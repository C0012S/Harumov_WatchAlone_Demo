package com.example.harumov_watchalone_demo

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class SearchActivity : AppCompatActivity(), TextWatcher {

    var recyclerView: RecyclerView? = null
    var editText: EditText? = null
    var adapter: SearchAdapter? = null

    var items = ArrayList<String>()
    var poster = arrayOf(R.drawable.about, R.drawable.gucci, R.drawable.spider)
    var title = arrayOf("About Times", "Gucci", "Spider Man3")
    var runningTime = arrayOf(50, 60, 70)

    // 현재 로그인하고 있는 사용자 아이디
    lateinit var id : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)

        id = intent.getStringExtra("user_id").toString()

        recyclerView = findViewById<View>(R.id.recylcerview) as RecyclerView
        editText = findViewById<View>(R.id.search_edt) as EditText
        editText!!.addTextChangedListener(this)

        // items 배열에 영화 제목 넣기
        for(i: Int in 0..poster.size-1) {
            items.add(title[i]) // item 배열에 영화 제목 추가
        }

        adapter = SearchAdapter(applicationContext, id, items)
        recyclerView!!.layoutManager =
            LinearLayoutManager(applicationContext, LinearLayoutManager.VERTICAL, false)
        recyclerView!!.adapter = adapter
    }

    override fun beforeTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {}
    override fun onTextChanged(charSequence: CharSequence, i: Int, i1: Int, i2: Int) {
        adapter?.getFilter()?.filter(charSequence)
    }

    override fun afterTextChanged(editable: Editable) {}
}