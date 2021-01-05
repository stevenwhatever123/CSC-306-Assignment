package com.example.myapplication

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

/**
 * This class is an adapater for the recycler view in ResultFragment
 * @param newsList A list of NewsData data item
 * @return An adapter for the recycler view in ResultFragment
 */
class ResultNewsAdapter(private val newsList: List<NewsData>): RecyclerView.Adapter<ResultNewsAdapter.ResultNewsViewHolder>(){

    // Firebase database reference
    private val database = Firebase.database.reference

    // List of all following news to store back to the firebase database
    private var nameList : List<String> = listOf()

    // List of all news that read before
    private var historyList = arrayListOf<String>()

    // Authentication for getting the current user
    private var mAuth = FirebaseAuth.getInstance()

    // Maximum number of news that can be displayed in the HistoryFragment
    private val historySizeMax = 5

    /**
     * This class is the view holder for every item in ResultActivity
     * @param itemView the view holder
     * @return A view holder
     */
    class ResultNewsViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val relativeLayout: RelativeLayout = itemView.findViewById(R.id.result_relativeLayout)
        val textView1: TextView = itemView.findViewById(R.id.result_text_view_1) // Title
        val textView3: TextView = itemView.findViewById(R.id.result_text_view_3) // Source
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ResultNewsViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.result_news_layout,
                parent, false)

        readDatabase()

        return ResultNewsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ResultNewsViewHolder, position: Int) {
        val currentItem = newsList[position]

        holder.textView1.text = currentItem.title
        holder.textView3.text = currentItem.sourceName

        // Handler when the layout is clicked and passes data to another activity
        holder.relativeLayout.setOnClickListener {
            val user = mAuth.currentUser

            addToHistory(user!!.uid, currentItem.title)

            val context = holder.relativeLayout.context
            val intent = Intent(holder.relativeLayout.context, DetailNewsActivity::class.java)
            intent.putExtra("imageResource", currentItem.imageResource.toString())
            intent.putExtra("title", currentItem.title)
            intent.putExtra("description", currentItem.description)
            intent.putExtra("sourceName", currentItem.sourceName)
            intent.putExtra("sourceID", currentItem.sourceID)
            context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return newsList.size
    }

    /**
     * This method gets data for following news from firebase database
     */
    private fun readDatabase(){
        val user = mAuth.currentUser

        val hRef = database.child("users").child(user!!.uid).child("History")

        hRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                historyList.clear()
                for(data in snapshot.children){
                    historyList.add(data.getValue<String>().toString())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.i("History", "cannot get data")
            }
        })

    }

    /**
     * This method adds the news to the firebase database history
     *
     * @param Uid the user id of the current user
     * @param source the title of the news
     */
    private fun addToHistory(Uid: String, title:String){
        val user = User(Uid)

        //val name_array = source.split(",")

        var temp: List<String>

        if(historyList.size >= historySizeMax){
            val splitTemp = historyList.takeLast(4).toList()
            temp = splitTemp + title
        } else {
            temp = historyList + title
        }

        database.child("users").child(Uid).child("History").setValue(temp)

    }
}