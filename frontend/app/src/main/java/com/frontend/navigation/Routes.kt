package com.frontend.navigation

object Routes {
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val HOME = "home"
    const val WALK = "walk"
    const val RECORD = "record"
    const val MY_INFO = "myinfo"
    const val DOG_EDIT = "dog_edit"
    const val WALK_DETAIL = "walk_detail/{walkId}"
    const val MET_DOGS = "met_dogs/{dogId}"
    const val CHAT = "chat/{chatRoomId}"

    fun walkDetail(walkId: Long) = "walk_detail/$walkId"
    const val BADGES = "badges"
    const val ADD_PLACE = "add_place"

    fun metDogs(dogId: Long) = "met_dogs/$dogId"
    fun chat(chatRoomId: Long) = "chat/$chatRoomId"
}