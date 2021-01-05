package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
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
import com.squareup.picasso.Picasso

/**
 * This class is an adapater for the recycler view in HotFragment and MyNewsFragment
 * @param newsList A list of NewsData data item
 * @return An adapter for the recycler view in HotFragment and MyNewsFragment
 */
class HotNewsAdapter(private val newsList: ArrayList<NewsData>):RecyclerView.Adapter<HotNewsAdapter.HotNewsViewHolder>(){

    // Firebase database reference
    private val database = Firebase.database.reference

    // List of all following news to store back to the firebase database
    private var nameList : List<String> = listOf()

    // List of all following news from the firebase database
    private var followList = arrayListOf<String>()

    // Authentication for getting the current user
    private var mAuth = FirebaseAuth.getInstance()

    // List of all news that read before
    private var historyList = arrayListOf<String>()

    // Maximum number of news that can displayed in the HistoryFragment
    private val historySizeMax = 5

    /**
     * This class is the view holder for every item in HotFragment and MyNewsFragment
     * @param itemView the view holder
     * @return A view holder
     */
    class HotNewsViewHolder(itemView: View):RecyclerView.ViewHolder(itemView){
        val relativeLayout: RelativeLayout = itemView.findViewById(R.id.relativeLayout)
        val imageView: ImageView = itemView.findViewById(R.id.image_view) // Image
        val textView1: TextView = itemView.findViewById(R.id.text_view_1) // Title
        val textView2: TextView = itemView.findViewById(R.id.text_view_2) // Read More
        val textView3: TextView = itemView.findViewById(R.id.text_view_3) // Source
        val button: Button = itemView.findViewById(R.id.follow_button) // Follow Button
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HotNewsViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.news_layout,
            parent, false)

        // Get data from firebase database
        readDatabase()

        return HotNewsViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: HotNewsViewHolder, position: Int) {
        val currentItem = newsList[position]

        // Adding the data to each component in the view
        Picasso.get().load(currentItem.imageResource.toString()).into(holder.imageView)
        holder.textView1.text = currentItem.title
        holder.textView3.text = currentItem.sourceName

        // Change the color and text of the follow button if it is already followed
        if(currentItem.sourceID in followList || currentItem.sourceName in followList){
            holder.button.text = "Following"
            holder.button.setBackgroundColor(Color.GRAY)
            holder.button.setTextSize(8f)
        }

        // Change the color and text of the follow button when clicked
        holder.button.setOnClickListener{
            if(currentItem.sourceID !in followList){
                holder.button.text = "Following"
                holder.button.setBackgroundColor(Color.GRAY)
                holder.button.setTextSize(8f)
                Log.d("Follow", currentItem.author)

                val user = mAuth.currentUser

                // Update the database
                addToDatabase(user!!.uid, currentItem.sourceID)
            }
        }

        // Handler of what to do when the read more is clicked
        holder.textView2.setOnClickListener(View.OnClickListener {

            val user = mAuth.currentUser
            // Adding the current item to the history in the database
            addToHistory(user!!.uid, currentItem.title)

            // Passing value to the DetailNewsActivity
            val context = holder.relativeLayout.context
            val intent = Intent(holder.relativeLayout.context, DetailNewsActivity::class.java)
            intent.putExtra("imageResource", currentItem.imageResource.toString())
            intent.putExtra("title", currentItem.title)
            intent.putExtra("description", currentItem.description)
            intent.putExtra("sourceName", currentItem.sourceName)
            intent.putExtra("sourceID", currentItem.sourceID)

            // Start activity
            context.startActivity(intent)
        })

        //Handler of what to do when the read more is clicked
        holder.textView3.setOnClickListener {
            val context = holder.relativeLayout.context
            val intent = Intent(context, NewsSourceActivity::class.java)
            intent.putExtra("sourceName", currentItem.sourceName)
            intent.putExtra("sourceID", currentItem.sourceID)
            context.startActivity(intent)
        }

    }

    /**
     * This method gets the number of item in this adapter
     */
    override fun getItemCount(): Int {
        return newsList.size
    }

    /**
     * This method adds the news source to the firebase database
     *
     * @param Uid the user id of the current user
     * @param source the source id of the news source
     */
    private fun addToDatabase(Uid: String, source:String){
        val user = User(Uid)

        val name_array = source.split(",")

        nameList = followList + name_array.toList()

        database.child("users").child(Uid).child("Follow_list").setValue(nameList)
    }

    /**
     * This method adds the news to the firebase database history
     *
     * @param Uid the user id of the current user
     * @param source the title of the news
     */
    private fun addToHistory(Uid: String, title:String){
        var temp: List<String>

        if(historyList.size >= historySizeMax){
            val splitTemp = historyList.takeLast(4).toList()
            temp = splitTemp + title
        } else {
            temp = historyList + title
        }

        database.child("users").child(Uid).child("History").setValue(temp)

    }

    /**
     * This method gets data for following news from firebase database
     */
    private fun readDatabase(){
        val user = mAuth.currentUser

        // Reference for Follow_List path
        val dRef = database.child("users").child(user!!.uid).child("Follow_list")

        dRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                followList.clear()
                // Adding all data to followList
                for(data in snapshot.children){
                    followList.add(data.getValue<String>().toString())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.i("Data", "cannot get data")
            }
        })

        // Reference for History path
        val hRef = database.child("users").child(user!!.uid).child("History")

        hRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                historyList.clear()
                // Adding all data to historyList
                for(data in snapshot.children){
                    historyList.add(data.getValue<String>().toString())
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.i("History", "cannot get data")
            }
        })

    }
}