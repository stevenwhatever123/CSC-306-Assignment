package com.example.myapplication

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.viewpager.widget.ViewPager
import com.example.myapplication.ui.main.SectionsPagerAdapter
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import java.util.*

class MainActivity : AppCompatActivity() {

    // Authentication for getting the current user
    private var mAuth = FirebaseAuth.getInstance()

    // An ArrayList of all article
    lateinit var newsList: ArrayList<NewsData>

    // Boolean on whether text to speech is playing
    private var textPlaying = false

    // The Text to Speech object for voicing
    lateinit var tts: TextToSpeech

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        // Attaching fragment to MainActivity
        val sectionsPagerAdapter = SectionsPagerAdapter(this, supportFragmentManager)
        val viewPager: ViewPager = findViewById(R.id.view_pager)
        viewPager.adapter = sectionsPagerAdapter
        // Set the default tab
        viewPager.setCurrentItem(1)
        val tabs: TabLayout = findViewById(R.id.tabs)
        tabs.setupWithViewPager(viewPager)

        // Adding the app bar to this activity
        val mToolbar = findViewById<View>(R.id.toolbar) as androidx.appcompat.widget.Toolbar
        setSupportActionBar(mToolbar)

        val ab = supportActionBar
        ab!!.setDisplayHomeAsUpEnabled(false)

        // A little message for successful login
        val contextView = findViewById<CoordinatorLayout>(R.id.CoordinatorLayout)
        Snackbar.make(contextView, R.string.Log_in_success, Snackbar.LENGTH_SHORT)
                .setAction("DISMISS"){
                    Log.e("TAG", "Done");
                }
                .show()

        newsList = arrayListOf()

        // Initialising the text to speech object
        tts = TextToSpeech(this, object: TextToSpeech.OnInitListener{
            override fun onInit(status: Int) {
                if(status == TextToSpeech.SUCCESS){
                    var intResult = tts.setLanguage(Locale.ENGLISH)

                    if(intResult == TextToSpeech.LANG_MISSING_DATA || intResult == TextToSpeech.LANG_NOT_SUPPORTED){
                        Log.d("TextToSpeech", "Language not supported")
                    }
                    tts.setOnUtteranceProgressListener(object: UtteranceProgressListener(){
                        override fun onStart(utteranceId: String?) {
                            Log.i("TextToSpeech","On Start");
                        }

                        override fun onDone(utteranceId: String?) {
                            Log.i("TextToSpeech","On Done");
                            textPlaying = false
                            //(R.id.menu_play_button).setIcon(R.drawable.ic_baseline_play_arrow_24)
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

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.main_menu, menu)

        // Adding the search view to this activity
        val manager = getSystemService(Context.SEARCH_SERVICE) as SearchManager
        val searchItem = menu?.findItem(R.id.app_bar_search)
        val searchView = searchItem?.actionView as SearchView
        searchView.setSearchableInfo(manager.getSearchableInfo(componentName))

        // Handler when the user try to use the search view
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                //searchView.setQuery("", false)
                searchItem.collapseActionView()
                if (query != null) {
                    moveToSearchResult(query)
                } else {
                    val contextView = findViewById<CoordinatorLayout>(R.id.CoordinatorLayout)
                    contextView.hideKeyboard()
                    Snackbar.make(contextView, R.string.EmptyQuery, Snackbar.LENGTH_SHORT)
                        .setAction("UNDO") {
                            Log.e("TAG", "Done");
                        }
                        .show()
                }
                searchView.setQuery("", false)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }

        })

        return true
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // Handle selection for different item
        return when (item.itemId) {
            R.id.signOut -> {
                mAuth.signOut()
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.follow_setting -> {
                val intent = Intent(this, NewsSettingActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.menu_play_button ->{
                // Changing the icon between playing the text and pause
                if(!textPlaying){
                    textPlaying = true
                    speak()
                    item.setIcon(R.drawable.ic_baseline_pause_24)
                } else {
                    textPlaying = false
                    tts.stop()
                    item.setIcon(R.drawable.ic_baseline_play_arrow_24)
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * This methods reads all the newsList and convert it into text
     * and pass it to text to speech object to read aloud.
     * Currently, due to limit of size of the string, it could
     * only read 10 news at once
     */
    fun speak(){
        Log.d("Speak", "Speaking")
        newsList = NewsListGlobal.getNewsList()

        var text: String = ""

        Log.d("SpeakSize", newsList.size.toString())

        for(i in 0 until 10){
            val readTitle = newsList[i].title + " from " + newsList[i].source_name
            Log.d("SpeakTitle", readTitle)
            text += readTitle + "\n" + newsList[i].description + "\n"
        }
        val pitch = .9f
        val speed = .9f

        tts.setPitch(pitch)
        tts.setSpeechRate(speed)

        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "com.google.android.tts")
    }

    /**
     * This methods pass the search query to a new result activity
     * @param query the query itself
     */
    fun moveToSearchResult(query: String){
        val intent = Intent(this, ResultActivity::class.java)
        intent.putExtra("query", query)
        startActivity(intent)
    }

    /**
     * This method hides the keyboard
     */
    fun View.hideKeyboard() {
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(windowToken, 0)
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