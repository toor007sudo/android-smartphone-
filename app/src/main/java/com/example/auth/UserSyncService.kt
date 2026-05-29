package com.example.auth

import android.content.Context
import androidx.room.Room
import com.example.db.AppDatabase
import com.example.db.UserStats
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Handles communication between localized SQLite Room Database and active remote
 * Firestore databases to back up/restore student statistics under high latency.
 */
class UserSyncService(private val context: Context) {

    // Access local database
    private val localDb: AppDatabase by lazy {
        Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "neet_jee_prep_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    private fun getFirestore(): FirebaseFirestore {
        FirebaseInitializer.initialize(context)
        return FirebaseFirestore.getInstance()
    }

    /**
     * Called on successful signup or sign-in state triggers.
     * Ensures perfect transition of state variables based on user status.
     *
     * @param uid Firebase Authenticated Unique UID
     * @param isNewUser Indicates first time registration or returning user login
     * @param fallbackUserName Name derived from provider profiles (e.g. Google profile)
     */
    suspend fun synchronizeUserStats(
        uid: String, 
        isNewUser: Boolean, 
        fallbackUserName: String
    ) = withContext(Dispatchers.IO) {
        
        val userStatsDao = localDb.userStatsDao()

        if (isNewUser) {
            // Rule 1: On first signup -> Initialize starting progress = 0
            val zeroProgressStats = UserStats(
                id = 1,
                userName = fallbackUserName,
                streakDays = 0,
                totalXp = 0,
                overallAccuracy = 0,
                currentTarTopic = "Select your target stream",
                todayTargetDonePercent = 0
            )
            
            // Save inside local Room cache
            userStatsDao.insertUserStats(zeroProgressStats)

            // Sync with Firestore Cloud Backup
            uploadStatsToFirestoreCloud(uid, zeroProgressStats)

        } else {
            // Rule 2: On returning login -> Restore all previous cloud statistics
            val restoredCloudStats = fetchStatsFromFirestoreCloud(uid) ?: UserStats(
                id = 1,
                userName = fallbackUserName,
                streakDays = 1, // Minimum default fallback for active session
                totalXp = 50,
                overallAccuracy = 80,
                currentTarTopic = "Select your target stream",
                todayTargetDonePercent = 10
            )

            // Overwrite local Room db representation to restore account state
            userStatsDao.insertUserStats(restoredCloudStats.copy(id = 1))
        }
    }

    /**
     * Back up local student results to Firestore cloud Document Store.
     */
    suspend fun backupLocalStatsToCloud(uid: String) = withContext(Dispatchers.IO) {
        val currentLocalStats = localDb.userStatsDao().getUserStatsDirect()
        if (currentLocalStats != null) {
            uploadStatsToFirestoreCloud(uid, currentLocalStats)
        }
    }

    /**
     * Abstracted Firestore API integration calls.
     * Restores/uploads clean Map schemas cleanly representing NEET/JEE status.
     */
    private suspend fun uploadStatsToFirestoreCloud(uid: String, stats: UserStats) = withContext(Dispatchers.IO) {
        try {
            val db = getFirestore()
            val localTests = localDb.mockTestDao().getAllTestsDirect()
            val testsMap = localTests.associate { it.title to it.accuracyPercent }

            val data = hashMapOf(
                "userName" to stats.userName,
                "streakDays" to stats.streakDays,
                "totalXp" to stats.totalXp,
                "overallAccuracy" to stats.overallAccuracy,
                "currentTarTopic" to stats.currentTarTopic,
                "todayTargetDonePercent" to stats.todayTargetDonePercent,
                "profilePicUrl" to stats.profilePicUrl,
                "lastSyncTimestamp" to System.currentTimeMillis(),
                "mockTests" to testsMap
            )
            db.collection("users").document(uid).set(data).await()
            android.util.Log.d("UserSyncService", "Upstream Cloud Sync committed to 'users/$uid' with mock test results: $testsMap")
        } catch (e: Exception) {
            android.util.Log.w("UserSyncService", "Error writing stats to Firestore, skipping: ${e.message}")
        }
    }

    /**
     * Pulls previously stored achievements and progress values from user document.
     */
    private suspend fun fetchStatsFromFirestoreCloud(uid: String): UserStats? = withContext(Dispatchers.IO) {
        try {
            val db = getFirestore()
            val document = db.collection("users").document(uid).get().await()
            if (document.exists()) {
                // Restore completed mock test accuracies if present
                val cloudTests = document.get("mockTests") as? Map<String, Any>
                if (cloudTests != null) {
                    val localTests = localDb.mockTestDao().getAllTestsDirect()
                    for (test in localTests) {
                        val cloudAccuracy = cloudTests[test.title]
                        if (cloudAccuracy != null) {
                            val accuracyInt = when (cloudAccuracy) {
                                is Number -> cloudAccuracy.toInt()
                                is String -> cloudAccuracy.toIntOrNull() ?: test.accuracyPercent
                                else -> test.accuracyPercent
                            }
                            localDb.mockTestDao().insertTest(test.copy(accuracyPercent = accuracyInt))
                        }
                    }
                }

                UserStats(
                    id = 1,
                    userName = document.getString("userName") ?: "Student",
                    streakDays = document.get("streakDays")?.let { (it as? Number)?.toInt() } ?: 0,
                    totalXp = document.get("totalXp")?.let { (it as? Number)?.toInt() } ?: 0,
                    overallAccuracy = document.get("overallAccuracy")?.let { (it as? Number)?.toInt() } ?: 0,
                    currentTarTopic = document.getString("currentTarTopic") ?: "Select your target stream",
                    todayTargetDonePercent = document.get("todayTargetDonePercent")?.let { (it as? Number)?.toInt() } ?: 0,
                    profilePicUrl = document.getString("profilePicUrl")
                )
            } else {
                null
            }
        } catch (e: Exception) {
            android.util.Log.w("UserSyncService", "Error reading stats from Firestore, using fallback default: ${e.message}")
            null
        }
    }

    /**
     * Uploads profile picture file to Firebase Storage and maps its accessible download url.
     */
    suspend fun uploadProfilePicture(uid: String, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            FirebaseInitializer.initialize(context)
            val storageRef = FirebaseStorage.getInstance().reference.child("profile_pics/$uid.jpg")
            
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val bytes = inputStream.readBytes()
            inputStream.close()
            
            storageRef.putBytes(bytes).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()
            
            val userStatsDao = localDb.userStatsDao()
            val currentStats = userStatsDao.getUserStatsDirect()
            if (currentStats != null) {
                val updatedStats = currentStats.copy(profilePicUrl = downloadUrl)
                userStatsDao.insertUserStats(updatedStats)
                uploadStatsToFirestoreCloud(uid, updatedStats)
            }
            
            downloadUrl
        } catch (e: Exception) {
            android.util.Log.e("UserSyncService", "Error uploading profile image: ${e.message}", e)
            null
        }
    }
}
