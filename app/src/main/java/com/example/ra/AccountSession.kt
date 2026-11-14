package com.example.ra

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Authentication is handled by Firebase; a session is exposed only after its role is loaded. */
internal class AccountSession(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val profiles: com.google.firebase.database.DatabaseReference =
        FirebaseDatabase.getInstance().getReference("accountProfiles")
) : ViewModel(), AccountAuth {
    override var account by mutableStateOf<AccountUser?>(null)
        private set
    override var busy by mutableStateOf(true)
        private set
    override var error by mutableStateOf<String?>(null)
        private set
    override var notice by mutableStateOf<String?>(null)
        private set

    init {
        viewModelScope.launch {
            try {
                withTimeout(30_000) {
                    auth.currentUser?.let { user ->
                        user.reload().awaitAccountTask()
                        account = loadProfile(user)
                    }
                }
            } catch (failure: Exception) {
                auth.signOut()
                account = null
                if (failure is CancellationException && failure !is TimeoutCancellationException) throw failure
                error = friendlyError(failure)
            } finally {
                busy = false
            }
        }
    }

    override fun clearMessages() { error = null; notice = null }

    private fun perform(action: suspend () -> Unit) {
        if (busy) return
        clearMessages()
        busy = true
        viewModelScope.launch {
            try {
                withTimeout(30_000) { action() }
            } catch (failure: Exception) {
                account = null
                auth.signOut()
                if (failure is CancellationException && failure !is TimeoutCancellationException) throw failure
                error = friendlyError(failure)
            } finally {
                busy = false
            }
        }
    }

    override fun login(role: AccountRole, email: String, password: String) {
        if (!AccountValidation.email(email) || password.isEmpty()) {
            error = "Enter your email address and password."
            return
        }
        perform {
            val user = auth.signInWithEmailAndPassword(email.trim(), password).awaitAccountTask().user
                ?: throw AccountProblem("Couldn't sign in. Please try again.")
            val loaded = loadProfile(user)
            if (loaded.role != role) throw AccountProblem(
                "This is a ${loaded.role.label.lowercase()} account. Choose ${loaded.role.label.lowercase()} login.")
            account = loaded
        }
    }

    override fun register(role: AccountRole, name: String, email: String, password: String) {
        val validation = AccountValidation.registration(name, email, password, password)
        if (validation != null) { error = validation; return }
        perform {
            val user = try {
                auth.createUserWithEmailAndPassword(email.trim(), password).awaitAccountTask().user
            } catch (_: FirebaseAuthUserCollisionException) {
                // Recover an interrupted signup only after proving the same account's credentials.
                val existing = auth.signInWithEmailAndPassword(email.trim(), password).awaitAccountTask().user
                    ?: throw AccountProblem("Couldn't complete registration. Please try again.")
                if (profiles.child(existing.uid).get().awaitAccountTask().exists())
                    throw AccountProblem("An account already exists with these details. Please log in.")
                existing
            } ?: throw AccountProblem("Couldn't create your account. Please try again.")
            val profile = mapOf("name" to name.trim(), "role" to role.name)
            profiles.child(user.uid).setValue(profile).awaitAccountTask()
            account = loadProfile(user)
        }
    }

    override fun resetPassword(email: String) {
        if (!AccountValidation.email(email)) { error = "Enter a valid email address."; return }
        perform {
            try {
                auth.sendPasswordResetEmail(email.trim()).awaitAccountTask()
            } catch (_: FirebaseAuthInvalidUserException) {
                // Keep the response the same whether or not the email is registered.
            }
            notice = "If an account exists for this email, a password reset email has been sent."
        }
    }

    override fun signOut() {
        if (busy) return
        account = null
        auth.signOut()
        clearMessages()
    }

    private suspend fun loadProfile(user: FirebaseUser): AccountUser {
        val snapshot = profiles.child(user.uid).get().awaitAccountTask()
        if (!snapshot.exists()) throw AccountProblem(
            "Account setup wasn't completed. Choose Create account and use the same email and password to finish.")
        return snapshot.toAccount(user)
    }

    private fun DataSnapshot.toAccount(user: FirebaseUser): AccountUser {
        val name = child("name").getValue(String::class.java)
            ?: throw AccountProblem("Your account profile couldn't be loaded. Please contact support.")
        val role = child("role").getValue(String::class.java)?.let { value ->
            AccountRole.entries.firstOrNull { it.name == value }
        } ?: throw AccountProblem("Your account type couldn't be loaded. Please contact support.")
        return AccountUser(user.uid, name, user.email.orEmpty(), role)
    }

    private fun friendlyError(failure: Exception): String = when (failure) {
        is AccountProblem -> failure.message.orEmpty()
        is TimeoutCancellationException -> "The connection took too long. Check your internet connection and try again."
        is FirebaseNetworkException -> "Couldn't connect. Check your internet connection and try again."
        is FirebaseTooManyRequestsException -> "Too many attempts. Please wait a little before trying again."
        is FirebaseAuthWeakPasswordException -> "Use a password with at least ${AccountValidation.MIN_PASSWORD_LENGTH} characters."
        is FirebaseAuthInvalidCredentialsException, is FirebaseAuthInvalidUserException -> "The email or password is incorrect."
        is FirebaseAuthUserCollisionException -> "Couldn't create this account. Try logging in or resetting your password."
        else -> "Couldn't complete your request. Please try again."
    }

    private class AccountProblem(message: String) : Exception(message)
}

private suspend fun <T> Task<T>.awaitAccountTask(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
    addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
    addOnCanceledListener { continuation.cancel() }
}
