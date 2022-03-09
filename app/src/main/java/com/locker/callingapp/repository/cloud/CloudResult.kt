package com.locker.callingapp.repository.cloud

sealed class CloudResult<T> {
    class Success<T>(val value: T) : CloudResult<T>()
    class Failure<T>(val error: Throwable?) : CloudResult<T>()
}

fun CloudResult<Boolean>.isSuccessful() = this is CloudResult.Success && this.value

inline fun <reified T> CloudResult<T>.toBooleanResult(): CloudResult<Boolean> = when (T::class.java) {
    Boolean::class.java -> when (this) {
        is CloudResult.Failure -> CloudResult.Failure(error)
        is CloudResult.Success -> CloudResult.Success(value as Boolean)
    }
    else -> {
        when (this) {
            is CloudResult.Failure -> CloudResult.Failure(error)
            is CloudResult.Success -> CloudResult.Success(true)
        }
    }
}