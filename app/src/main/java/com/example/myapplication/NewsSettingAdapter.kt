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
 * This class is an adapater for the recycler view in NewsSettingActivity
 * @param newsList A list of NewsData data item
 * @return An adapter for the recycler view in NewsSettingActivity
 */
class NewsSettingAdapter(private val newsList: MutableList<String>): RecyclerView.Adapter<NewsSettingAdapter.NewsSettingViewHolder>(){

    // Firebase database reference
    private val database = Firebase.database.reference

    // List of following news
    private var followList = arrayListOf<String>()

    // Authentication for getting the current user
    private var mAuth = FirebaseAuth.getInstance()

    /**
     * This class is the view holder for every item in Setting aka NewsSettingActivity
     * @param itemView the view holder
     * @return A view holder
     */
    class NewsSettingViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val relativeLayout: RelativeLayout = itemView.findViewById(R.id.following_news_relativeLayout)
        val textView: TextView = itemView.findViewById(R.id.following_news_source) // Title
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsSettingViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.following_news_layout,
            parent, false)

        readDatabase()

        return NewsSettingViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: NewsSettingViewHolder, position: Int) {
        val currentItem = newsList[position]

        holder.textView.text = currentItem
    }

    override fun getItemCount(): Int {
        return newsList.size
    }

    /**
     * This method gets the news source in a given position
     * @param i index of the list
     */
    fun getNewsSourceAt(i: Int): String{
        return newsList[i]
    }

    /**
     * This methods removes an element inside the arraylist
     * @param position index of the list
     */
    fun removeAt(position: Int) {
        newsList.removeAt(position)
        notifyItemRemoved(position)
    }

    /**
     * This methods restore the item after moving
     * @param item the news source name
     * @param position index of the list
     */
    fun restoreItem(item: String, position: Int){
        newsList.add(position, item)
        notifyItemRemoved(position)
    }

    /**
     * This method gets data for following news from firebase database
     */
    private fun readDatabase(){
        val user = mAuth.currentUser

        val hRef = database.child("users").child(user!!.uid).child("History")

        val dRef = database.child("users").child(user!!.uid).child("Follow_list")

        dRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                followList.clear()
                for(data in snapshot.children){
                    followList.add(data.getValue<String>().toString())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.i("Data", "cannot get data")
            }
        })

    }

}