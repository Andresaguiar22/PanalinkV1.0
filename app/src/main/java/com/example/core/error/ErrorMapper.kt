package com.example.core.error

import java.io.IOException

object ErrorMapper {
    fun map(throwable: Throwable): AppException {
        return when (throwable) {
            is AppException -> throwable
            is IOException -> AppException.NetworkException(cause = throwable)
            else -> AppException.UnknownException(message = throwable.localizedMessage ?: "Error desconocido", cause = throwable)
        }
    }
}
