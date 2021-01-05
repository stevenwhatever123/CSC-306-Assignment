package com.example.myapplication

import android.graphics.Bitmap
import java.net.URL

/**
 * A data class for storing a particular news
 * @param imageResource image url
 * @param title title of the news
 * @param description description of the news
 * @param author author of the news
 * @param sourceID source id of the news
 * @param sourceName source name of the news
 */
data class NewsData(val imageResource: URL, val title: String, val description: String,
                    val author: String, val sourceID: String, val sourceName: String){

}