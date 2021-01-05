package com.example.myapplication

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

/**
 * This class is an adapater for the recycler view in HistoryFragment
 * @param newsList A list of NewsData data item
 * @return An adapter for the recycler view in HistoryFragment
 */
class HistoryNewsAdapter(private val newsList: List<NewsData>): RecyclerView.Adapter<HistoryNewsAdapter.HistoryNewsViewHolder>(){

    // Firebase database reference
    private val database = Firebase.database.reference

    // List of all following news to store back to the firebase database
    private var nameList : List<String> = listOf()

    // List of all news that read before
    private var historyList = arrayListOf<String>()

    // Authentication for getting the current user
    private var mAuth = FirebaseAuth.getInstance()

    /**
     * This class is the view holder for every item in HistoryFragment
     * @param itemView the view holder
     * @return A view holder
     */
    class HistoryNewsViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val relativeLayout: RelativeLayout = itemView.findViewById(R.id.history_relativeLayout)
        val textView1: TextView = itemView.findViewById(R.id.history_text_view_1) // Title
        val textView2: TextView = itemView.findViewById(R.id.history_text_view_2) // Description
        val textView3: TextView = itemView.findViewById(R.id.history_text_view_3) // Source
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryNewsViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.history_news_layout,
            parent, false)

        return HistoryNewsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: HistoryNewsViewHolder, position: Int) {
        val currentItem = newsList[position]

        // Adding the data to each component in the view
        holder.textView1.text = currentItem.title
        holder.textView3.text = currentItem.sourceName

        holder.relativeLayout.setOnClickListener {
            // Passing data to a new activity and starts it
            val user = mAuth.currentUser
            val context = holder.relativeLayout.context
            val intent = Intent(holder.relativeLayout.context, DetailNewsActivity::class.java)
            intent.putExtra("imageResource", currentItem.imageResource.toString())
            intent.putExtra("title", currentItem.title)
            intent.putExtra("description", currentItem.description)
            intent.putExtra("sourceName", currentItem.sourceName)
            context.startActivity(intent)
        }
    }

    /**
     * This method gets the number of item in this adapter
     */
    override fun getItemCount(): Int {
        return newsList.size
    }

}