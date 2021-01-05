package com.example.myapplication

/**
 * This class is a holder for notificiton index position
 */
class PositionHolder {

    private var position:Int = 0

    /**
     * This is a constructor where it stores the index of the position
     * @param position index of the position
     */
    fun PositionHolder(position: Int){
        this.position = position
    }

    /**
     * This method gets the index of the position
     * @return the index of the position
     */
    fun getPosition(): Int{
        return this.position
    }

    /**
     * This method sets the index of the postition
     * @param position index of the position
     */
    fun setPosition(position: Int){
        this.position = position
    }
}