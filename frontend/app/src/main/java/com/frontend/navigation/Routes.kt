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

    fun walkDetail(walkId: Long) = "walk_detail/$walkId"
}