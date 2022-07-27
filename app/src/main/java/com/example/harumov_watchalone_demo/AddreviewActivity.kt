package com.example.harumov_watchalone_demo

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import kotlinx.android.synthetic.main.activity_addreview.*

class AddreviewActivity : AppCompatActivity() {

    private lateinit var myTitle: TextView
    private lateinit var myGenres: TextView
    private lateinit var myPoster: ImageView

    // 현재 로그인하고 있는 사용자 아이디, 선택한 영화 아이디
    lateinit var id : String
    lateinit var movie_title : String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_addreview)

        id = intent.getStringExtra("user_id").toString()
        movie_title = intent.getStringExtra("movie_title").toString()

        // 혼자보기 페이지에서 전달받은 인텐트 데이터 확인
        if (intent.hasExtra("user_id") && intent.hasExtra("movie_title")) {
            Log.d("AddReviewActivity", "받아온 id : $id , movie title : $movie_title")
        } else {
            Log.e("AddReviewActivity", "가져온 데이터 없음")
        }

        // 감상했던 영화 제목
        myTitle = findViewById<TextView>(R.id.title)
        myTitle.setText(movie_title)

        // 영화 장르
        myGenres = findViewById<TextView>(R.id.genres)
        myGenres.setText("영화 장르")

        // 포스터
        myPoster = findViewById<ImageView>(R.id.poster)

        if(movie_title.equals("About Times")){
            myPoster.setImageResource(R.drawable.about)
        }
        else if(movie_title.equals("Gucci")) {
            myPoster.setImageResource(R.drawable.gucci)
        }
        else if(movie_title.equals("Spider Man3")) {
            myPoster.setImageResource(R.drawable.spider)
        }

        // 별점 - 람다식을 사용하여 처리
        ratingBar.setOnRatingBarChangeListener{ ratingBar, rating, fromUser ->
            ratingBar.rating = rating
            //Toast.makeText(applicationContext, "별점: ${rating}", Toast.LENGTH_SHORT).show()
        }

        btnAdd.setOnClickListener {
            var user_comment = comment.text.toString() // EditText 입력값을 텍스트로

            var intent = Intent(applicationContext, MainActivity::class.java)
            startActivityForResult(intent, 0)
        }
    }
}