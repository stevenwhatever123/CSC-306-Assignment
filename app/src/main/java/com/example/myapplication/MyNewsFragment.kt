package com.example.myapplication

import android.os.Bundle
import android.os.SystemClock.sleep
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
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
import org.w3c.dom.Text
import java.net.URL
import kotlin.collections.ArrayList

/**
 * A simple MyNews subclass.
 * It displays all the news that the user has followed
 */
class MyNewsFragment : Fragment() {

    private val compositeDisposable = CompositeDisposable()

    // Authentication for getting the current user
    private var mAuth = FirebaseAuth.getInstance()

    // Getting the current user
    private val user = mAuth.currentUser

    // Firebase database path for follow list
    private val database = Firebase.database.reference.child("users").child(user!!.uid).child("Follow_list")

    // List of all following news from the firebase database
    private var followList = arrayListOf<String>()

    // List of all article object
    private var articleList = ArrayList<ArticleDto>()

    // An ArrayList of all news data
    private var newsList = ArrayList<NewsData>()

    // An ArrayList that is used as a temp for preventing items from historyList
    private var currentList = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        readDatabase(object : FirebaseCallBack {
            override fun onCallBack(list: ArrayList<String>) {
                followList = list

                // Loop through all the list and get the article and add it to articleList
                // If an item has already been seen before in currentList, we ignore it instead
                for (i in 0 until followList.size) {
                    if(followList[i] !in currentList){
                        Log.d("FollowName", followList[i])
                        val articleTemp = getNews(followList[i])
                        sleep(500)
                        Log.d("ArticleTemp", articleTemp.size.toString())
                        for (j in 0 until articleTemp.size) {
                            articleList.add(articleTemp[j])
                        }
                    }
                }

                // Generate item for recycle view
                for (i in 0 until articleList.size) {
                    Log.d("MyArticle", articleList[i].title)
                    generateItem(articleList[i])
                }

                // Shuffle the newsList in random order
                newsList.shuffle();

                // Create an copy of the followList
                currentList = followList.clone() as ArrayList<String>

                // Add all items to the MyNewsAdapter
                val recyclerView = view!!.findViewById<RecyclerView>(R.id.recycler_view_my_news)
                recyclerView.adapter = HotNewsAdapter(newsList)
                recyclerView.layoutManager = LinearLayoutManager(view!!.context)
                recyclerView.setHasFixedSize(true)

                NewsListGlobal.setNewsList(newsList)

                // Set the textView for loading to not visible to user
                try{
                    val textView: TextView = view!!.findViewById(R.id.textView4)
                    textView.isVisible = false
                } catch (e: Exception){
                    Log.d("Null", "no textView")
                }
            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_my_news)

        recyclerView.adapter = HotNewsAdapter(newsList)
        recyclerView.layoutManager = LinearLayoutManager(this.context)
        recyclerView.setHasFixedSize(true)
    }

    override fun onCreateView(
            inflater: LayoutInflater, container: ViewGroup?,
            savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_my_news, container, false)
        return view
    }

    /**
     * This method gets data for following news from firebase database
     */
    private fun readDatabase(firebaseCallback: FirebaseCallBack){
        var followList = arrayListOf<String>()
        Log.d("Database", "Read database")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                followList.clear()
                for (data in snapshot.children) {
                    val dataTemp = data.getValue<String>().toString()
                    if (dataTemp !in followList) {
                        followList.add(dataTemp)
                    }
                    //followList.add(data.getValue<String>().toString())
                }
                firebaseCallback.onCallBack(followList)
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }

    /**
     * This method gets all the article using the News-Api-Kotlin
     */
    private fun getNews(source: String): ArrayList<ArticleDto>{

        // ArrayList temp for returning the result
        val finalList = ArrayList<ArticleDto>()

        val newsApiRepository = NewsApiRepository(APIKey.getKey())

        Log.d("GenerateNews", source)

        compositeDisposable.add(newsApiRepository.getEverything(sources = source, pageSize = 10, page = 1)
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

        val sourceID: String

        if(article.source.id == null){
            sourceID = article.source.name
        } else {
            sourceID = article.source.id
        }

        val sourceName = article.source.name

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
         * @return A new instance of fragment MyNewsFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
            MyNewsFragment().apply {
                arguments = Bundle().apply {

                }
            }
    }

    override fun onDestroy() {
        super.onDestroy()
        compositeDisposable.clear()
    }
}