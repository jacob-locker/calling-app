package com.locker.callingapp

import android.content.Context
import android.widget.Toast

val Any.TAG: String get() = this::class.java.simpleName

fun Context.showToast(message: String, length: Int = Toast.LENGTH_LONG) =
    Toast.makeText(this, message, length).show()