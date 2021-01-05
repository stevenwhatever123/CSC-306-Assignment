package com.example.myapplication

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock.sleep
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dfl.newsapi.NewsApiRepository
import com.dfl.newsapi.model.ArticleDto
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.net.URL

/**
 * This class is responsible for the result activity after a query in search view
 */
class ResultActivity : AppCompatActivity() {

    // The search query
    var query = ""

    private val compositeDisposable = CompositeDisposable()

    // An ArrayList of all news data
    private var newsList = ArrayList<NewsData>()

    // List of all article object
    private var articleList = ArrayList<ArticleDto>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_result)

        val button: ImageButton = findViewById(R.id.result_backButton) // Back Button

        // Back button handler
        button.setOnClickListener {
            super.onBackPressed()
            finish()
        }

        // Get data from another activity
        query = intent.getStringExtra("query").toString()
        // Get all news according to the query
        val articleTemp = getNews(query)
        // Wait for data to come back
        sleep(500)
        for (j in 0 until articleTemp.size) {
            articleList.add(articleTemp[j])
        }

        // Generate item for recycle view
        for (i in 0 until articleList.size) {
            generateItem(articleList[i])
        }

        val recyclerView = findViewById<RecyclerView>(R.id.result_recycler_view)

        recyclerView.adapter = ResultNewsAdapter(newsList)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        if(articleList.size != 0){
            try{
                val textView: TextView = findViewById(R.id.result_text_view)
                textView.isVisible = false
            } catch (e: Exception){
                Log.d("Null", "no textView")
            }
        }
    }

    /**
     * This method gets all the article using the News-Api-Kotlin
     */
    private fun getNews(source: String): ArrayList<ArticleDto>{

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
}