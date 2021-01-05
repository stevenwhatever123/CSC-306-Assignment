package com.example.myapplication

import android.os.Bundle
import android.os.SystemClock.sleep
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
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
 * A simple History subclass.
 * It displays all the news that the user has read before
 */
class HistoryFragment : Fragment() {

    private val compositeDisposable = CompositeDisposable()

    // Authentication for getting the current user
    private var mAuth = FirebaseAuth.getInstance()

    // Getting the current user
    private val user = mAuth.currentUser

    // Firebase database path for history
    private val database = Firebase.database.reference.child("users").child(user!!.uid).child("History")

    // List of all news that read before
    private var historyList = arrayListOf<String>()

    // List of all article object
    private var articleList = ArrayList<ArticleDto>()

    // An ArrayList of all article
    private var newsList = ArrayList<NewsData>()

    // An ArrayList that is used as a temp for preventing items from historyList
    private var currentList = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Get data from firebase database
        readDatabase(object: FirebaseCallBack{
            override fun onCallBack(list: ArrayList<String>) {

                articleList.clear()

                historyList = list

                // Loop through all the list and get the article and add it to articleList
                // If an item has already been seen before in currentList, we ignore it instead
                for (i in 0 until historyList.size) {
                    if(historyList[i] !in currentList){
                        val articleTemp = getNews(historyList[i])
                        sleep(500)
                        for (j in 0 until articleTemp.size) {
                            articleList.add(articleTemp[j])
                        }
                    }
                }

                articleList.reverse()

                // Generate item for recycle view
                for (i in 0 until articleList.size) {
                    generateItem(articleList[i])
                }

                // Check if the activity is inactive
                // If true, we do nothing
                // Otherwise, we update the recycler view
                if(view != null){
                    currentList = historyList.clone() as ArrayList<String>

                    val recyclerView = view!!.findViewById<RecyclerView>(R.id.history_recycler_view)
                    recyclerView.adapter = HistoryNewsAdapter(newsList)
                    recyclerView.layoutManager = LinearLayoutManager(view!!.context)
                    recyclerView.setHasFixedSize(true)
                } else {
                    Log.d("HistoryView", "No View")
                }

            }
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?){
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view!!.findViewById<RecyclerView>(R.id.history_recycler_view)

        recyclerView.adapter = HistoryNewsAdapter(newsList)
        recyclerView.layoutManager = LinearLayoutManager(view!!.context)
        recyclerView.setHasFixedSize(true)

        // Remove the textView for loading
        try{
            val textView: TextView = view!!.findViewById(R.id.history_text_view)
            textView.isVisible = false
        } catch (e: Exception){
            Log.d("Null", "no textView")
        }
    }

    /**
     * This method gets all the article using the News-Api-Kotlin
     */
    private fun getNews(source: String): ArrayList<ArticleDto>{

        // ArrayList temp for returning the result
        val finalList = ArrayList<ArticleDto>()

        val newsApiRepository = NewsApiRepository(APIKey.getKey())

        Log.d("GenerateNews", source)

        compositeDisposable.add(newsApiRepository.getEverything(q = source, pageSize = 1, page = 1)
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


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    /**
     * This method gets data for following news from firebase database
     * This is also a callback method so it will return a callback list
     */
    private fun readDatabase(firebaseCallback: FirebaseCallBack){
        var historyList = arrayListOf<String>()
        Log.d("Database", "Read history")
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                historyList.clear()
                for (data in snapshot.children) {
                    val dataTemp = data.getValue<String>().toString()
                    if (dataTemp !in historyList) {
                        historyList.add(dataTemp)
                    }
                }

                firebaseCallback.onCallBack(historyList)
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }


    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment HistoryFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance() =
            HistoryFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }

}