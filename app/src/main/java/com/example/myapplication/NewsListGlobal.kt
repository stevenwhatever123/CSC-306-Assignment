package com.example.myapplication

/**
 * This class is used to getting and setting newsList for passing data to
 * another activity without starting a new activity
 */
class NewsListGlobal {

    companion object{
        // An ArrayList of all article
        var newsList: ArrayList<NewsData> = arrayListOf()

        /**
         * This methods sets the newsList
         * @param newsList the newsList to be stored
         */
        @JvmName("setNewsList1")
        fun setNewsList(newsList: ArrayList<NewsData>){
            NewsListGlobal.newsList = newsList
        }

        /**
         * This methods gets the newsList
         * @return the newsList
         */
        @JvmName("getNewsList1")
        fun getNewsList(): ArrayList<NewsData>{
            return NewsListGlobal.newsList
        }
    }

}