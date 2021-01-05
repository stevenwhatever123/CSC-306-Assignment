package com.example.myapplication

import android.os.Bundle
import android.os.SystemClock.sleep
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.dfl.newsapi.NewsApiRepository
import com.dfl.newsapi.enums.Category
import com.dfl.newsapi.enums.Country
import com.dfl.newsapi.model.ArticleDto
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.schedulers.Schedulers
import java.net.URL
import kotlin.collections.ArrayList

/**
 * A simple Hot subclass.
 * It displays all the news that is currently top headlines
 */
class HotFragment : Fragment() {

    private val compositeDisposable = CompositeDisposable()

    // List of all article object
    private var articleList = ArrayList<ArticleDto>()

    // An ArrayList of all article
    private var newsList = ArrayList<NewsData>()

    // Firebase database path for history
    private val db = Firebase.database

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Generate a list full of headline news
        articleList = getNews()

        // Wait for the news to return
        sleep(1000)

        Log.d("articleList", articleList.size.toString())

        // Generate every single item
        for(i in 0 until articleList.size){
            Log.d("Article", articleList[i].title)
            generateItem(articleList[i])
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)

        // Add all the items to the recycler view

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)

        Log.d("Newslist", newsList.size.toString())

        recyclerView.adapter = HotNewsAdapter(newsList)
        recyclerView.layoutManager = LinearLayoutManager(this.context)
        recyclerView.setHasFixedSize(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater!!.inflate(R.layout.fragment_hot, container, false)
        return view
    }

    /**
     * This method gets all the article using the News-Api-Kotlin
     */
    private fun getNews(): ArrayList<ArticleDto>{

        // ArrayList temp for returning the result
        val finalList = ArrayList<ArticleDto>()

        val newsApiRepository = NewsApiRepository(APIKey.getKey())

        compositeDisposable.add(newsApiRepository.getTopHeadlines(category = Category.GENERAL, country = Country.US, pageSize = 20, page = 1)
                .subscribeOn(Schedulers.io())
                .toFlowable()
                .flatMapIterable { articles -> articles.articles }
                .subscribe({ article -> finalList.add(article)},
                        { t -> Log.d("getTopHeadlines error", t.message.toString())}))

        return finalList
    }

    /**
     * This method generates the item from a given article
     */
    private fun generateItem(article: ArticleDto) {
        val title = article.title

        val description: String
        if(article.description == null){
            description = ""
        } else {
            description = article.description
        }

        val sourceID: String

        if(article.source.id == null){
            sourceID = article.source.name
        } else {
            sourceID = article.source.id
        }

        val sourceName = article.source.name

        val author: String

        if(article.author == null){
            author = article.source.name
        } else {
            author = article.author
        }

        val imageURL: URL

        if(article.urlToImage == null){
            imageURL = URL("http://java2s.com/style/logo.png") // Blank Image
        } else {
            imageURL = URL(article.urlToImage)
        }

        // Create an item using NewsData data class
        val item = NewsData(imageURL, title, description, author, sourceID, sourceName)
        Log.d("Title", title)
        newsList.add(item)
        Log.d("ListSize", newsList.size.toString())
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment Hot.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
            HotFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }

}