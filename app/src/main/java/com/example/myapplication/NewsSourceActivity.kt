package com.example.myapplication

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock.sleep
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dfl.newsapi.NewsApiRepository
import com.dfl.newsapi.model.ArticleDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.net.URL

/**
 * This class is responsible for display every news from a given news source
 */
class NewsSourceActivity : AppCompatActivity() {

    // Name and id for the news source
    var sourceName = ""
    var sourceID = ""

    // Authentication for getting the current user
    private var mAuth = FirebaseAuth.getInstance()

    // Firebase database reference
    private val database = Firebase.database.reference

    // List of all following news to store back to the firebase database
    private var nameList : List<String> = listOf()

    // List of all following news from the firebase database
    private var followList = arrayListOf<String>()

    private val compositeDisposable = CompositeDisposable()

    // An ArrayList of all news data
    private var newsList = ArrayList<NewsData>()

    // List of all article object
    private var articleList = ArrayList<ArticleDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_source)

        val button: ImageButton = findViewById(R.id.news_source_backButton) // Back Button

        // Back button handler
        button.setOnClickListener {
            super.onBackPressed()
            finish()
        }

        // Get data from another activity
        sourceName = intent.getStringExtra("sourceName").toString()
        sourceID = intent.getStringExtra("sourceID").toString()

        val textViewForSource: TextView = findViewById(R.id.news_source_text_view)
        textViewForSource.setText(sourceName)

        var articleTemp = ArrayList<ArticleDto>()

        // If the sourceID is empty or null, use source name instead
        if(sourceID == null || sourceID.equals("") || sourceID.contains(" ")){
            Log.d("ByName", sourceName)
            articleTemp = getNewsByName(sourceName)
        } else {
            Log.d("ByID", sourceID)
            articleTemp = getNewsByID(sourceID)
        }
        // Wait for the data to come back
        sleep(500)

        for (j in 0 until articleTemp.size) {
            articleList.add(articleTemp[j])
        }

        // Generate item for recycle view
        for (i in 0 until articleList.size) {
            Log.d("MyArticle", articleList[i].title)
            generateItem(articleList[i])
        }

        val recyclerView = findViewById<RecyclerView>(R.id.news_source_recycler_view)

        recyclerView.adapter = ResultNewsAdapter(newsList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)


        val followButton: Button = findViewById(R.id.news_source_follow_button)

        //Handler for follow button
        followButton.setOnClickListener {
            addToDatabase(sourceID)
        }

        if(sourceID in followList || sourceName in followList){
            followButton.text = "Following"
            followButton.setBackgroundColor(Color.GRAY)
            followButton.setTextSize(8f)
        }


    }

    /**
     * This method gets the news by souce id using News-Api-Kotlin
     * @param source source id
     */
    private fun getNewsByID(source: String): ArrayList<ArticleDto>{

        val finalList = ArrayList<ArticleDto>()

        val newsApiRepository = NewsApiRepository(APIKey.getKey())

        Log.d("NewsGenerateNews", source)

        compositeDisposable.add(newsApiRepository.getEverything(sources = source, pageSize = 10, page = 1)
            .subscribeOn(Schedulers.io())
            .toFlowable()
            .flatMapIterable { articles -> articles.articles }
            .subscribe({ article -> finalList.add(article) },
                { t -> Log.d("getTopHeadlines error", t.message.toString()) }))

        return finalList
    }

    /**
     * This method gets the news by souce name using News-Api-Kotlin
     * @param source source name
     */
    private fun getNewsByName(source: String): ArrayList<ArticleDto>{

        val finalList = ArrayList<ArticleDto>()

        val newsApiRepository = NewsApiRepository(APIKey.getKey())

        Log.d("GenerateNews", source)

        compositeDisposable.add(newsApiRepository.getEverything(q = source, pageSize = 10, page = 1)
            .subscribeOn(Schedulers.io())
            .toFlowable()
            .flatMapIterable { articles -> articles.articles }
            .subscribe({ article -> finalList.add(article) },
                { t -> Log.d("getTopHeadlines error", t.message.toString()) }))

        return finalList
    }

    /**
     * This method generates the item from a given article
     */
    private fun generateItem(article: ArticleDto) {
        val title = article.title
        val description = article.description

        val source_id: String

        if(article.source.id == null){
            source_id = article.source.name
        } else {
            source_id = article.source.id
        }

        val source_name = article.source.name

        val author: String

        if(article.author == null){
            author = "null"
        } else {
            author = article.author
        }

        val imageURL: URL

        if(article.urlToImage == null){
            imageURL = URL("http://java2s.com/style/logo.png") // Blank Image
        } else {
            imageURL = URL(article.urlToImage)
        }
        val item = NewsData(imageURL, title, description, author, source_id, source_name)
        Log.d("Title", title)
        newsList.add(item)
        Log.d("ListSize", newsList.size.toString())
    }

    /**
     * This method adds the news source to the firebase database
     *
     * @param Uid the user id of the current user
     * @param source the source id of the news source
     */
    private fun addToDatabase(source:String){
        val user = mAuth.currentUser

        val name_array = source.split(",")

        nameList = followList + name_array.toList()

        database.child("users").child(user!!.uid).child("Follow_list").setValue(nameList)
    }
}