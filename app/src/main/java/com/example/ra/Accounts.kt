package com.example.ra

internal enum class AccountRole(val label: String) {
    RENTER("Renter"), OWNER("Owner")
}

internal data class AccountUser(
    val id: String,
    val name: String,
    val email: String,
    val role: AccountRole
)

internal interface AccountAuth {
    val account: AccountUser?
    val busy: Boolean
    val error: String?
    val notice: String?
    fun clearMessages()
    fun login(role: AccountRole, email: String, password: String)
    fun register(role: AccountRole, name: String, email: String, password: String)
    fun resetPassword(email: String)
    fun signOut()
}

internal object AccountValidation {
    const val MIN_PASSWORD_LENGTH = 10
    fun email(value: String): Boolean =
        Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$").matches(value.trim())

    fun registration(name: String, email: String, password: String, confirmation: String): String? = when {
        name.trim().length !in 2..80 -> "Enter your full name (2–80 characters)."
        !email(email) -> "Enter a valid email address."
        password.length < MIN_PASSWORD_LENGTH -> "Use a password with at least $MIN_PASSWORD_LENGTH characters."
        password.length > 128 -> "Use a password with no more than 128 characters."
        password != confirmation -> "The passwords don't match."
        else -> null
    }
}

internal object AccountAccess {
    fun canManage(user: AccountUser?, ownerId: String): Boolean =
        user != null && user.role == AccountRole.OWNER && ownerId.isNotBlank() && user.id == ownerId

    fun canRent(user: AccountUser?): Boolean = user?.role == AccountRole.RENTER

    fun canViewBooking(user: AccountUser?, ownerId: String, renterId: String): Boolean =
        user != null && if (user.role == AccountRole.OWNER) canManage(user, ownerId)
        else renterId.isNotBlank() && user.id == renterId
}
