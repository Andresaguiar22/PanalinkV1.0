package com.example.core.error

sealed class AppException(
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause) {
    class NetworkException(message: String = "Error de conexión a red", cause: Throwable? = null) : AppException(message, cause)
    class AuthException(message: String = "Error de autenticación", cause: Throwable? = null) : AppException(message, cause)
    class DatabaseException(message: String = "Error en base de datos local", cause: Throwable? = null) : AppException(message, cause)
    class ServerException(message: String = "Error en servidor remoto", cause: Throwable? = null) : AppException(message, cause)
    class UnknownException(message: String = "Ha ocurrido un error inesperado", cause: Throwable? = null) : AppException(message, cause)
}
