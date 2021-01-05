package com.example.myapplication

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import java.util.*

/**
 * This Activity is where it will display a news in detail
 */
class DetailNewsActivity : AppCompatActivity() {

    // Authentication for getting the current user
    private var mAuth = FirebaseAuth.getInstance()

    // Firebase database reference
    private val database = Firebase.database.reference

    // List of all following news to store back to the firebase database
    private var nameList : List<String> = listOf()

    // List of all following news from the firebase database
    private var followList = arrayListOf<String>()

    // List of all news that read before
    private var historyList = arrayListOf<String>()

    // Boolean on whether text to speech is playing
    private var textPlaying = false

    // An ArrayList of all article
    lateinit var newsList: ArrayList<NewsData>

    // The Text to Speech object for voicing
    lateinit var tts: TextToSpeech

    // Article's image in url text
    var imageResource: String = ""
    // Article's title
    var title: String = ""
    // Article's description
    var description: String = ""
    // Article's source name
    var sourceName: String = ""
    // Article's source Id
    var sourceID: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail_news)

        // Get data from firebase database
        readDatabase()

        // Getting all components in the view
        val imageView: ImageView = findViewById(R.id.detail_image_view) // Image
        val textView1: TextView = findViewById(R.id.detail_text_view_1) // Title
        val textView2: TextView = findViewById(R.id.detail_text_view_2) // Description
        val textView3: TextView = findViewById(R.id.detail_text_view_3) // Source
        val button: ImageButton = findViewById(R.id.backButton) // Back Button
        val followButton: Button = findViewById(R.id.detail_follow_button) // Follow Button
        val playButton: ImageButton = findViewById(R.id.detail_play_button) // Play Button

        // Pass data from another activity
        imageResource = intent.getStringExtra("imageResource").toString()
        title = intent.getStringExtra("title").toString()
        description = intent.getStringExtra("description").toString()
        sourceName = intent.getStringExtra("sourceName").toString()
        sourceID = intent.getStringExtra("sourceID").toString()

        // Sets the image and text of the article
        Picasso.get().load(imageResource).into(imageView)
        textView1.setText(title)
        textView2.setText(description)
        textView3.setText(sourceName)

        // Condition for the follow button
        if(sourceID in followList || sourceName in followList){
            followButton.text = "Following"
            followButton.setBackgroundColor(Color.GRAY)
            followButton.setTextSize(8f)
        }

        // Back button handle
        button.setOnClickListener {
            //val intent = Intent(this, MainActivity::class.java)
            //startActivity(intent)
            super.onBackPressed()
            finish()
        }

        // Follow button handle
        followButton.setOnClickListener{
            followButton.text = "Following"
            followButton.setBackgroundColor(Color.GRAY)
            followButton.setTextSize(8f)
            Log.d("Follow", sourceName)

            val user = mAuth.currentUser

            addToDatabase(user!!.uid, sourceID)
        }

        // Handler when user tap the name of the news source
        textView3.setOnClickListener {
            val intent = Intent(this, NewsSourceActivity::class.java)
            intent.putExtra("sourceName", sourceName)
            Log.d("SourceName", sourceName)
            intent.putExtra("sourceID", sourceID)
            Log.d("SourceID", sourceID)
            this.startActivity(intent)
            finish()
        }

        // Initialising the text to speech tool
        tts = TextToSpeech(this, object: TextToSpeech.OnInitListener{
            override fun onInit(status: Int) {
                if(status == TextToSpeech.SUCCESS){
                    var intResult = tts.setLanguage(Locale.ENGLISH)

                    if(intResult == TextToSpeech.LANG_MISSING_DATA || intResult == TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.d("TextToSpeech", "Language not supported")
                    } else {
                        playButton.isEnabled = true
                    }

                    // Tracking the progress of the speech and make changes according
                tts.setOnUtteranceProgressListener(object: UtteranceProgressListener(){
                    override fun onStart(utteranceId: String?) {
                        Log.i("TextToSpeech","On Start");
                    }

                    override fun onDone(utteranceId: String?) {
                        Log.i("TextToSpeech","On Done");
                        textPlaying = false
                        playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24)
                    }

                    override fun onError(utteranceId: String?) {
                        Log.i("TextToSpeech","On Error");
                    }

                })

                } else {
                    Log.d("TextToSpeech", "Task failed successfully")
                }
            }
        })

        // Play button handle
        playButton.setOnClickListener {
            if(!textPlaying){
                textPlaying = true
                speak()
                playButton.setImageResource(R.drawable.ic_baseline_pause_24)
            } else {
                textPlaying = false
                tts.stop()
                playButton.setImageResource(R.drawable.ic_baseline_play_arrow_24)
            }
        }

    }

    /**
     * This method calls the text to speech object and read the text
     */
    fun speak(){
        val readTitle = title + " from " + sourceName
        val text = readTitle + "\n" + description
        val pitch = .9f
        val speed = .9f

        tts.setPitch(pitch)
        tts.setSpeechRate(speed)

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "com.google.android.tts")
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

    /**
     * This method destory everything when the app is closed, including the
     * text to speech object itself
     */
    override fun onDestroy() {
        if(tts != null){
            tts.stop()
            tts.shutdown()
        }

        super.onDestroy()
    }
}