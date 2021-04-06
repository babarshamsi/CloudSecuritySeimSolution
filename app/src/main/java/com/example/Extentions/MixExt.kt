package com.example.Extentions


fun String?.default() : String = this ?: ""

fun Boolean?.default() : Boolean = this ?: false