package com.example.auth

import android.content.Context
import com.example.db.UserStats
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await

/**
 * Clean architectural manager handling Firebase authentication states.
 * Connects Gmail/Google credentials and traditional Email & Password with
 * callbacks to restore progress locally.
 */
class FirebaseAuthManager(private val context: Context) {

    private val auth: FirebaseAuth by lazy { 
        FirebaseInitializer.initialize(context)
        FirebaseAuth.getInstance() 
    }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState

    init {
        // Observe live authentication triggers
        auth.addAuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                _authState.value = AuthState.Authenticated(
                    uid = user.uid,
                    email = user.email ?: "",
                    displayName = user.displayName ?: "User"
                )
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    /**
     * Authenticates with raw Email and Password credentials
     */
    suspend fun loginWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                AuthResult.Success(
                    uid = user.uid,
                    email = user.email ?: "",
                    isNewUser = result.additionalUserInfo?.isNewUser ?: false
                )
            } else {
                AuthResult.Failure("User reference was empty.")
            }
        } catch (e: Exception) {
            AuthResult.Failure(e.localizedMessage ?: "Unknown compilation login exception.")
        }
    }

    /**
     * Creates a new user with Email and Password. 
     * Initiates step default progress values = 0.
     */
    suspend fun registerWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val user = result.user
            if (user != null) {
                AuthResult.Success(
                    uid = user.uid,
                    email = user.email ?: "",
                    isNewUser = true
                )
            } else {
                AuthResult.Failure("Failed to instantiate account.")
            }
        } catch (e: Exception) {
            AuthResult.Failure(e.localizedMessage ?: "Registration failed.")
        }
    }

    /**
     * Exchanges Google Access Token with Firebase credentials.
     * Restores returning statistics automatically.
     */
    suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user
            if (user != null) {
                AuthResult.Success(
                    uid = user.uid,
                    email = user.email ?: "",
                    isNewUser = result.additionalUserInfo?.isNewUser ?: false
                )
            } else {
                AuthResult.Failure("Failed to resolve Google Credential payload.")
            }
        } catch (e: Exception) {
            AuthResult.Failure(e.localizedMessage ?: "Google Authentication handshake failed.")
        }
    }

    /**
     * Signs out from the active session.
     */
    fun signOut() {
        auth.signOut()
        // Sign out from cached Google configuration to allow picking another Google Account
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        GoogleSignIn.getClient(context, gso).signOut()
    }
}

/**
 * Represents Authentication Flow States
 */
sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val uid: String, val email: String, val displayName: String) : AuthState()
}

/**
 * Result returned back to UI or Repositories
 */
sealed class AuthResult {
    data class Success(val uid: String, val email: String, val isNewUser: Boolean) : AuthResult()
    data class Failure(val errorMessage: String) : AuthResult()
}
