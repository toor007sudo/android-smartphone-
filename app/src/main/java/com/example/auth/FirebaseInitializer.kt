package com.example.auth

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions

object FirebaseInitializer {
    fun initialize(context: Context) {
        try {
            if (FirebaseApp.getApps(context.applicationContext).isEmpty()) {
                val options = FirebaseOptions.Builder()
                    .setApplicationId("1:123456789012:android:abcdef1234567890")
                    .setApiKey("AIzaSyDummyKey_AIStudioRun")
                    .setProjectId("example-project-aistudio")
                    .setStorageBucket("example-project-aistudio.appspot.com")
                    .build()
                FirebaseApp.initializeApp(context.applicationContext, options)
                android.util.Log.d("FirebaseInitializer", "Firebase initialized successfully with secure fallback options.")
            }
        } catch (e: Exception) {
            android.util.Log.e("FirebaseInitializer", "Firebase initialization error: ${e.message}", e)
        }
    }
}
