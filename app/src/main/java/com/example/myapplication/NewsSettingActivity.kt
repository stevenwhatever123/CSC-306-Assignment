package com.example.myapplication

import android.app.AlarmManager
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import java.util.*

/**
 * This class is responsible for the news setting pages
 */
class NewsSettingActivity : AppCompatActivity() {

    // Authentication for getting the current user
    private var mAuth = FirebaseAuth.getInstance()

    // Getting the current user
    private val user = mAuth.currentUser

    // Firebase database reference
    private val database = Firebase.database.reference

    // List of all following news from the firebase database
    private var followList = arrayListOf<String>()

    // List of all following news to store back to the firebase database
    private var nameList : List<String> = listOf()

    // Boolean to check if the save button is pressed
    private var pressedSave = false

    // Alarm Manager objectr
    private var mAlarmManager : AlarmManager? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_setting)

        val recyclerView = findViewById<RecyclerView>(R.id.setting_recycler_view)

        readDatabase(object : FirebaseCallBack {
            override fun onCallBack(list: ArrayList<String>) {
                Log.d("ReadDataBase", "You Know")
                followList = list

                recyclerView.adapter = NewsSettingAdapter(followList)
                recyclerView.layoutManager = LinearLayoutManager(this@NewsSettingActivity)
                recyclerView.setHasFixedSize(true)
            }
        })

        // Swipe Handler for deleting a item from the recyclerview and follow list
        val swipeHandler = object : SwipeToDeleteCallback(this) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val adapter = recyclerView.adapter as NewsSettingAdapter
                val position = viewHolder.adapterPosition
                val item = adapter.getNewsSourceAt(position)

                adapter.removeAt(position)

                Snackbar.make(
                    recyclerView, // The ID of your recycler_view
                    "Removed " + item,
                    Snackbar.LENGTH_LONG
                ).apply {
                    // a reset if the user wants to undo it
                    setAction("UNDO") {
                        adapter.restoreItem(item, position)
                    }
                    setActionTextColor(Color.YELLOW)
                }.show()
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(recyclerView)

        // Save button handler which updates the firebase database and refreshes the MainActivity
        val saveButton: Button = findViewById(R.id.setting_save_button)
        saveButton.setOnClickListener {
            pressedSave = true
            val adapter = recyclerView.adapter as NewsSettingAdapter
            updateDatabase(adapter)
            val message = "Perference Saved"
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }

        // Back Button handler
        val backButton: ImageButton = findViewById(R.id.setting_backButton)
        backButton.setOnClickListener {
            // If the user make changes to the follow list, refresh the MainActivity
            // Otherwise, just simply return
            if(pressedSave){
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            } else {
                super.onBackPressed()
                finish()
            }
        }

        // Edit Text for time
        val fromDateText: EditText = findViewById(R.id.editText1)
        fromDateText.setInputType(InputType.TYPE_NULL)
        fromDateText.setOnClickListener {
            val cldr: Calendar = Calendar.getInstance()
            val hour: Int = cldr.get(Calendar.HOUR_OF_DAY)
            val minutes: Int = cldr.get(Calendar.MINUTE)
            // Time Picker Dialog
            var picker = TimePickerDialog(
                this,
                { tp, sHour, sMinute ->
                    fromDateText.setText(
                        checkDigit(sHour) + ":" + checkDigit(
                            sMinute
                        )
                    )
                }, hour, minutes, true
            )
            picker.show()
        }

        // Edit Text for time
        val toDateText: EditText = findViewById(R.id.editText2)
        toDateText.setInputType(InputType.TYPE_NULL)
        toDateText.setOnClickListener {
            val cldr: Calendar = Calendar.getInstance()
            val hour: Int = cldr.get(Calendar.HOUR_OF_DAY)
            val minutes: Int = cldr.get(Calendar.MINUTE)
            // Time Picker Dialog
            var picker = TimePickerDialog(
                this,
                { tp, sHour, sMinute ->
                    toDateText.setText(
                        checkDigit(sHour) + ":" + checkDigit(
                            sMinute
                        )
                    )
                }, hour, minutes, true
            )
            picker.show()
        }

        // Button for saving both dates
        val getDateButton: Button = findViewById(R.id.get_date_button)
        getDateButton.setOnClickListener {
            if(fromDateText.text != null && toDateText.text != null){
                var fromTemp = fromDateText.text
                var fromDateHour = fromDateText.text.split(":").first().toInt()

                var toTemp =  toDateText.text
                var toDateHour: Int
                var toDateHourTemp = toDateText.text.split(":").first()
                if(fromDateHour > 12){
                    toDateHour = toDateHourTemp.toInt() + 12
                } else {
                    toDateHour = toDateHourTemp.toInt()
                }

                var alarmList =  arrayListOf<Calendar>()

                // Generate different alarms between the time
                for(i in fromDateHour until toDateHour){
                    val alarmFor = Calendar.getInstance()
                    alarmFor.set(Calendar.HOUR_OF_DAY, i)
                    alarmFor.set(Calendar.MINUTE, 0)
                    alarmFor.set(Calendar.SECOND, 0)

                    alarmList.add(alarmFor)
                }

                val intentArray = ArrayList<PendingIntent>()
                mAlarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                // Creating different alarm for pushing notification
                for(i in fromDateHour until toDateHour){
                    val mIntent = Intent(this, MyReceiver::class.java)

                    val mPendingIntent = PendingIntent.getBroadcast(
                        this,
                        i,
                        mIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    //mAlarmManager = this.getSystemService(Context.ALARM_SERVICE) as AlarmManager

                    mAlarmManager!!.setRepeating(
                        AlarmManager.RTC_WAKEUP, System.currentTimeMillis(),
                        1000 * 60 * 120, mPendingIntent
                    )
                }

                getDateButton.text = "Date Saved"
                getDateButton.setBackgroundColor(Color.GRAY)
                getDateButton.setTextSize(8f)

            } else {
                Toast.makeText(this, "Invalid Time", Toast.LENGTH_SHORT).show()
            }
        }

    }

    /**
     * This method checks the digit of the time and add leading zero according
     */
    fun checkDigit(number: Int): String{
        if(number <= 9){
            return "0" + number.toString()
        } else {
            return number.toString()
        }
    }

    /**
     * This method updates the follow list in firebase database
     */
    private fun updateDatabase(adapter: NewsSettingAdapter){
        val user = mAuth.currentUser

        var nameList : List<String> = listOf()

        for(i in 0 until adapter.itemCount){
            nameList += listOf(adapter.getNewsSourceAt(i))
        }

        //nameList = followList + name_array.toList()

        database.child("users").child(user!!.uid).child("Follow_list").setValue(nameList)
    }

    /**
     * This method gets data for following news from firebase database
     */
    private fun readDatabase(firebaseCallback: FirebaseCallBack){
        val databaseForReading = Firebase.database.reference.child("users").child(user!!.uid).child(
            "Follow_list"
        )
        var followList = arrayListOf<String>()
        Log.d("Database", "Read database")
        databaseForReading.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                followList.clear()
                for (data in snapshot.children) {
                    val dataTemp = data.getValue<String>().toString()
                    if (dataTemp !in followList) {
                        followList.add(dataTemp)
                    }
                }

                firebaseCallback.onCallBack(followList)
            }

            override fun onCancelled(error: DatabaseError) {

            }
        })
    }


}