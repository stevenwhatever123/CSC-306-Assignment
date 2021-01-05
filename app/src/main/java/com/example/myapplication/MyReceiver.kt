package com.example.myapplication

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.SystemClock.sleep
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
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

/**
 * This class is a receiver for pushing notification
 */
class MyReceiver : BroadcastReceiver() {

    // Channel Settings
    private val CHANNEL_ID = "channel_id_example_01"
    private val notificationID = 101

    private val compositeDisposable = CompositeDisposable()

    // Authentication for getting the current user
    private var mAuth = FirebaseAuth.getInstance()

    // Getting the current user
    private val user = mAuth.currentUser

    // Firebase database path for follow list
    private val database = Firebase.database.reference.child("users").child(user!!.uid).child("Follow_list")

    // List of all following news from the firebase database
    private var followList = arrayListOf<String>()

    // List of all news that already by read
    private var readList = arrayListOf<String>()

    // An ArrayList that is used as a temp for preventing items from historyList
    private var currentList = arrayListOf<String>()

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("Toast", "notification")
        readDatabase(object : FirebaseCallBack {
            override fun onCallBack(list: ArrayList<String>) {

                followList = list
                readList.clear()
                // Boolean to check if there is any new news
                var hasNewReport = false

                for(i in 0 until followList.size){
                    val articleTemp = getNews(followList[i])
                    sleep(500)
                    for (j in 0 until articleTemp.size) {
                        readList.add(articleTemp[j].title)
                        if(articleTemp[j] in currentList){
                            // If the article is not in the currentList, there is a new news
                            // And we set hasNewReport to true
                            hasNewReport = true
                        }
                    }
                }

                currentList = readList.clone() as ArrayList<String>

                createNotificationChannel(context)
                sendNotification(context)

            }
        })
    }

    /**
     * This method creates a notification channel
     * @param context the activity context
     */
    private fun createNotificationChannel(context: Context){
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val name = "Notification Title"
            val descriptionText = "Notification Description"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
                .apply {
                    description = descriptionText
                }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.createNotificationChannel(channel)

        }
    }

    /**
     * This method send notification to the user
     */
    private fun sendNotification(context: Context){

        // The intent to open when the user click the notification
        val intent = Intent(context, MainActivity::class.java).apply{
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, 0)

        // Building the notification bar
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_baseline_notification_important_24)
            .setContentTitle("News Update")
            .setContentText("Latest news just rolled in")
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setNotificationSilent()

        with(NotificationManagerCompat.from(context)){
            notify(notificationID, builder.build())
        }
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

        val finalList = ArrayList<ArticleDto>()

        val newsApiRepository = NewsApiRepository(APIKey.getKey())

        Log.d("GenerateNews", source)

        compositeDisposable.add(newsApiRepository.getTopHeadlines(sources = source, pageSize = 10, page = 1)
            .subscribeOn(Schedulers.io())
            .toFlowable()
            .flatMapIterable { articles -> articles.articles }
            .subscribe({ article -> finalList.add(article) },
                { t -> Log.d("getTopHeadlines error", t.message.toString()) }))

        return finalList
    }
}