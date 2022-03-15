package com.locker.callingapp.repository.cloud

sealed class CloudResult<T> {
    class Success<T>(val value: T) : CloudResult<T>()
    class Failure<T>(val error: Throwable? = null, val message: String = error?.message ?: "Unexpected error occurred.") : CloudResult<T>()
}

suspend fun <T, R> CloudResult<T>.thenMap(other: suspend (T) -> CloudResult<R>): CloudResult<R> {
    if (this is CloudResult.Failure) {
        return CloudResult.Failure(this.error)
    }

    val thisValue = (this as CloudResult.Success).value
    val otherResult = other(thisValue)
    if (otherResult is CloudResult.Failure) {
        return CloudResult.Failure(otherResult.error)
    }

    return otherResult
}

suspend fun <T, R> CloudResult<T>.thenContinue(other: suspend (T) -> CloudResult<R>): CloudResult<T> {
    if (this is CloudResult.Failure) {
        return CloudResult.Failure(this.error)
    }

    val thisValue = (this as CloudResult.Success).value
    val otherResult = other(thisValue)
    if (otherResult is CloudResult.Failure) {
        return CloudResult.Failure(otherResult.error)
    }

    return this
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