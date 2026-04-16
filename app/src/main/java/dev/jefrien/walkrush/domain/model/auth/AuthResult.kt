package dev.jefrien.walkrush.domain.model.auth

/**
 * Result wrapper for authentication operations
 */
sealed class AuthResult<out T> {
    data class Success<T>(val data: T) : AuthResult<T>()
    data class Error(val exception: AuthException) : AuthResult<Nothing>()
    object Loading : AuthResult<Nothing>()
}

/**
 * Custom exceptions for auth errors
 */
sealed class AuthException(message: String) : Exception(message) {
    class InvalidCredentials(message: String = "Email o contraseña incorrectos") : AuthException(message)
    class UserNotFound(message: String = "Usuario no encontrado") : AuthException(message)
    class EmailAlreadyExists(message: String = "El email ya está registrado") : AuthException(message)
    class WeakPassword(message: String = "Contraseña débil") : AuthException(message)
    class NetworkError(message: String = "Error de conexión") : AuthException(message)
    class UnknownError(message: String) : AuthException(message)
}