package com.example.ui.viewmodel

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import com.example.db.*
import com.example.network.GeminiHelper
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import com.example.auth.FirebaseAuthManager
import com.example.auth.UserSyncService
import com.example.auth.AuthState
import com.example.auth.AuthResult

class AppViewModel(application: Application) : AndroidViewModel(application) {

    // Database Reference
    private val db: AppDatabase by lazy {
        Room.databaseBuilder(
            application,
            AppDatabase::class.java,
            "neet_jee_prep_db"
        )
        .fallbackToDestructiveMigration()
        .build()
    }

    // Auth and User Sync references
    val firebaseAuthManager = FirebaseAuthManager(application)
    val userSyncService = UserSyncService(application)
    val authState: StateFlow<AuthState> = firebaseAuthManager.authState

    // Streams & Flows
    val userStats: StateFlow<UserStats> = db.userStatsDao().getUserStatsFlow()
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserStats())

    val studyTasks: StateFlow<List<StudyTask>> = db.studyTaskDao().getStudyTasksFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val mockTests: StateFlow<List<MockTest>> = db.mockTestDao().getAllTestsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val doubtThreads: StateFlow<List<DoubtThread>> = db.doubtThreadDao().getDoubtsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val badges: StateFlow<List<Badge>> = db.badgeDao().getAllBadgesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ----------------------------------------------------
    // Step-Based Test Flow Creation State (Step 1 -> 2 -> 3)
    // ----------------------------------------------------
    var testFlowStep by mutableStateOf(1) // Current step (1, 2, or 3)
    var selectedStream by mutableStateOf("JEE") // "JEE" or "NEET"
    var selectedExamType by mutableStateOf("JEE Main") // "JEE Main", "JEE Advanced", "NEET UG"
    var selectedSubjects by mutableStateOf(setOf("Physics", "Chemistry", "Maths"))
    var selectedTestType by mutableStateOf("Full Syllabus Test") // "Full Syllabus Test", "Chapter Test", "Topic Test", "Previous Year Test"
    var selectedDifficulty by mutableStateOf("Medium") // "Easy", "Medium", "Hard"
    var testListFilter by mutableStateOf("All") // "All", "Recommended", "Trending", "High Accuracy"

    // Active AI Solver State
    var aiDoubtResponse by mutableStateOf<String?>(null)
    var isDoubtLoading by mutableStateOf(false)

    // Formula sheet search & generated advice state
    var formulaBookSearchQuery by mutableStateOf("")
    var aiStudyPlannerText by mutableStateOf<String?>(null)
    var isPlannerLoading by mutableStateOf(false)

    // Current screen navigation state (inside Main screen structure)
    // "dashboard", "test_stats", "new_test", "learn", "community", "ai_solver"
    var currentScreen by mutableStateOf("dashboard")

    init {
        // Pre-populate with realistic starting states to match the beautiful reference design
        viewModelScope.launch {
            // Stats
            val existingStats = db.userStatsDao().getUserStatsDirect()
            if (existingStats == null) {
                db.userStatsDao().insertUserStats(
                    UserStats(
                        userName = "Arjun",
                        streakDays = 27,
                        totalXp = 1420,
                        overallAccuracy = 89,
                        currentTarTopic = "Physics: Rotational Motion",
                        todayTargetDonePercent = 68
                    )
                )

                // Starter plan tasks
                db.studyTaskDao().insertTask(StudyTask(subject = "Physics", topic = "Rotational Motion", timeRange = "9:00 AM - 11:00 AM", isCompleted = true))
                db.studyTaskDao().insertTask(StudyTask(subject = "Chemistry", topic = "Chemical Bonding", timeRange = "11:30 AM - 1:00 PM", isCompleted = true))
                db.studyTaskDao().insertTask(StudyTask(subject = "Maths", topic = "Vector 3D", timeRange = "2:30 PM - 4:00 PM", isCompleted = false))
                db.studyTaskDao().insertTask(StudyTask(subject = "Biology", topic = "Human Physiology", timeRange = "7:00 PM - 8:30 PM", isCompleted = false))

                // Prepopulate 12 realistic Mock Tests for JEE and NEET
                val tests = listOf(
                    MockTest(
                        title = "Full Syllabus Test 01", questionsCount = 180, maxMarks = 540, durationMins = 180,
                        accuracyPercent = 92, studentsCountString = "12.5K", stream = "JEE", subjectSummary = "Physics, Chemistry, Maths",
                        examSubtype = "JEE Main", testCategory = "Full syllabus", difficulty = "Medium", isRecommended = true
                    ),
                    MockTest(
                        title = "Full Syllabus Test 02", questionsCount = 180, maxMarks = 540, durationMins = 180,
                        accuracyPercent = 89, studentsCountString = "9.8K", stream = "JEE", subjectSummary = "Physics, Chemistry, Maths",
                        examSubtype = "JEE Main", testCategory = "Full syllabus", difficulty = "Medium", isTrending = true
                    ),
                    MockTest(
                        title = "Full Syllabus Test 03", questionsCount = 180, maxMarks = 540, durationMins = 180,
                        accuracyPercent = 94, studentsCountString = "8.2K", stream = "JEE", subjectSummary = "Physics, Chemistry, Maths",
                        examSubtype = "JEE Advanced", testCategory = "Full syllabus", difficulty = "Hard", isHighAccuracy = true
                    ),
                    MockTest(
                        title = "Full Syllabus Test 04", questionsCount = 180, maxMarks = 540, durationMins = 180,
                        accuracyPercent = 87, studentsCountString = "6.4K", stream = "JEE", subjectSummary = "Physics, Chemistry, Maths",
                        examSubtype = "JEE Main", testCategory = "Full syllabus", difficulty = "Medium"
                    ),
                    MockTest(
                        title = "Physics: Kinematics Booster", questionsCount = 30, maxMarks = 120, durationMins = 45,
                        accuracyPercent = 91, studentsCountString = "4.2K", stream = "JEE", subjectSummary = "Physics",
                        examSubtype = "JEE Main", testCategory = "Chapter", difficulty = "Easy", isRecommended = true
                    ),
                    MockTest(
                        title = "Chemistry: Organic Alcohols", questionsCount = 40, maxMarks = 160, durationMins = 60,
                        accuracyPercent = 85, studentsCountString = "5.1K", stream = "JEE", subjectSummary = "Chemistry",
                        examSubtype = "JEE Advanced", testCategory = "Chapter", difficulty = "Hard"
                    ),
                    MockTest(
                        title = "NEET Biology Sectional 01", questionsCount = 90, maxMarks = 360, durationMins = 90,
                        accuracyPercent = 93, studentsCountString = "15.4K", stream = "NEET", subjectSummary = "Biology",
                        examSubtype = "NEET UG", testCategory = "Topic", difficulty = "Medium", isRecommended = true, isTrending = true
                    ),
                    MockTest(
                        title = "NEET Chemistry PYQ 2024", questionsCount = 50, maxMarks = 200, durationMins = 50,
                        accuracyPercent = 88, studentsCountString = "11.2K", stream = "NEET", subjectSummary = "Chemistry",
                        examSubtype = "NEET UG", testCategory = "PYQ", difficulty = "Medium", isHighAccuracy = true
                    )
                )
                for (test in tests) {
                    db.mockTestDao().insertTest(test)
                }

                // Community Doubt threads
                db.doubtThreadDao().insertDoubt(
                    DoubtThread(
                        query = "Why is the bond angle in NH3 less than in CH4 although both are sp3 hybridized?",
                        aiResponse = "Both NH3 and CH4 have sp3 hybridization. However, NH3 contains one nitrogen lone pair, while CH4 has only bond pairs. According to VSEPR theory, lone pair-bond pair repulsion is stronger than bond pair-bond pair repulsion. This squeezes the H-N-H bond angle down to 107° compared to the symmetric 109.5° tetrahedral angle in CH4.",
                        isCommunityPost = true,
                        userName = "Rohan Sharma",
                        subject = "Chemistry",
                        likesCount = 24,
                        repliesCount = 3
                    )
                )
                db.doubtThreadDao().insertDoubt(
                    DoubtThread(
                        query = "How do we calculate the moment of inertia of a hollow sphere about its tangent?",
                        aiResponse = "First, recall that the moment of inertia of a hollow sphere about its diameter is I_cm = (2/3) * M * R^2. Using the Parallel Axis Theorem: I_tangent = I_cm + M * d^2. Here, the distance d from the center to the tangent is R. Thus, I_tangent = (2/3) * M * R^2 + M * R^2 = (5/3) * M * R^2.",
                        isCommunityPost = true,
                        userName = "Kriti Patel",
                        subject = "Physics",
                        likesCount = 18,
                        repliesCount = 2
                    )
                )

                // Badges
                db.badgeDao().insertBadge(Badge("Perfect Streaker", "Maintain a streak above 25 days.", "🔥", true, "Unlocked"))
                db.badgeDao().insertBadge(Badge("Mock Hero", "Unlock by scoring above 90% accuracy in a mock test.", "🏆", false))
                db.badgeDao().insertBadge(Badge("AI Scholar", "Ask 3 doubts using the AI Doubt Solver.", "🤖", false))
                db.badgeDao().insertBadge(Badge("Forum Star", "Earn 10 likes on a community doubt post.", "💬", false))
            }
        }

        // Live local-to-cloud auto-backups whenever any changes are done to stats
        viewModelScope.launch {
            userStats.collect { stats ->
                val auth = firebaseAuthManager.authState.value
                if (auth is AuthState.Authenticated) {
                    userSyncService.backupLocalStatsToCloud(auth.uid)
                }
            }
        }
    }

    // ----------------------------------------------------
    // User Authentication Methods
    // ----------------------------------------------------
    fun register(email: String, password: String, userName: String, onResult: (AuthResult) -> Unit) {
        viewModelScope.launch {
            val result = firebaseAuthManager.registerWithEmail(email, password)
            if (result is AuthResult.Success) {
                // Initialize stats to 0 (First signup)
                userSyncService.synchronizeUserStats(result.uid, isNewUser = true, userName)
            }
            onResult(result)
        }
    }

    fun login(email: String, password: String, onResult: (AuthResult) -> Unit) {
        viewModelScope.launch {
            val result = firebaseAuthManager.loginWithEmail(email, password)
            if (result is AuthResult.Success) {
                // Restore stats from Firestore (Returning login)
                userSyncService.synchronizeUserStats(result.uid, isNewUser = false, email.substringBefore("@"))
            }
            onResult(result)
        }
    }

    fun handleGoogleSignIn(idToken: String, onResult: (AuthResult) -> Unit) {
        viewModelScope.launch {
            val result = firebaseAuthManager.signInWithGoogle(idToken)
            if (result is AuthResult.Success) {
                // Restore if old user, zero progress if new user (Standard first signup vs returning logic)
                userSyncService.synchronizeUserStats(result.uid, result.isNewUser, result.email.substringBefore("@"))
            }
            onResult(result)
        }
    }

    fun logout() {
        viewModelScope.launch {
            firebaseAuthManager.signOut()
            // Reset to default guest on-logout view settings (does not touch previous cloud stats, simply acts as active guest stats representation)
            db.userStatsDao().insertUserStats(
                UserStats(
                    id = 1,
                    userName = "Guest Student",
                    streakDays = 0,
                    totalXp = 0,
                    overallAccuracy = 0,
                    currentTarTopic = "Select your target stream",
                    todayTargetDonePercent = 0
                )
            )
        }
    }

    fun uploadProfileImage(uri: android.net.Uri, onCompleted: (String?) -> Unit) {
        val auth = firebaseAuthManager.authState.value
        if (auth is AuthState.Authenticated) {
            viewModelScope.launch {
                val url = userSyncService.uploadProfilePicture(auth.uid, uri)
                onCompleted(url)
            }
        } else {
            onCompleted(null)
        }
    }

    // Toggle a task in "Today's Plan" and recalculate daily completion percentage
    fun toggleTaskCompletion(task: StudyTask) {
        viewModelScope.launch {
            val updatedTask = task.copy(isCompleted = !task.isCompleted)
            db.studyTaskDao().updateTask(updatedTask)

            // Recalculate percent
            studyTasks.take(1).collect { currentList ->
                val list = currentList.map { if (it.id == task.id) updatedTask else it }
                val completed = list.count { it.isCompleted }
                val total = list.size
                val newPercent = if (total > 0) (completed * 100) / total else 0

                val currentStats = userStats.value
                db.userStatsDao().updateUserStats(
                    currentStats.copy(todayTargetDonePercent = newPercent)
                )
            }
        }
    }

    // Submit a community question
    fun submitCommunityQuestion(text: String, subject: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            db.doubtThreadDao().insertDoubt(
                DoubtThread(
                    query = text,
                    aiResponse = null,
                    isCommunityPost = true,
                    userName = "Arjun",
                    subject = subject,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    // AI doubt solver logic (text only or text plus simulation image placeholder)
    fun askAiDoubt(question: String) {
        if (question.isBlank()) return
        isDoubtLoading = true
        aiDoubtResponse = null
        viewModelScope.launch {
            // Crafting an educational tutor persona prompt
            val prompt = """
                You are high-performance JEE and NEET prep assistant. Provide a highly precise, step-by-step
                academic solution for the following user question. Keep your formatting well-structured
                with headings and mathematical relations if needed, clear enough for a 11/12th standard student.
                
                Question: $question
            """.trimIndent()

            val answer = GeminiHelper.askGemini(prompt)
            aiDoubtResponse = answer
            isDoubtLoading = false

            // Capture in local DB history!
            db.doubtThreadDao().insertDoubt(
                DoubtThread(
                    query = question,
                    aiResponse = answer,
                    isCommunityPost = false,
                    userName = "Arjun",
                    subject = "AI Doubt Solver",
                    timestamp = System.currentTimeMillis()
                )
            )

            // Update badge stats for AI Scholar
            db.badgeDao().updateBadge(
                Badge("AI Scholar", "Ask 3 doubts using the AI Doubt Solver.", "🤖", true, "Unlocked")
            )
        }
    }

    // Ask Gemini for a community response directly
    fun triggerAiCommunityReply(doubt: DoubtThread) {
        viewModelScope.launch {
            val prompt = "Provide a brief correct textbook solution (max 4 lines) for: ${doubt.query}"
            val reply = GeminiHelper.askGemini(prompt)
            db.doubtThreadDao().updateDoubt(
                doubt.copy(
                    aiResponse = reply,
                    repliesCount = doubt.repliesCount + 1
                )
            )
        }
    }

    // Generate smart personalized study tips using artificial intelligence
    fun requestSmartStudyTips() {
        isPlannerLoading = true
        aiStudyPlannerText = null
        viewModelScope.launch {
            val stats = userStats.value
            val prompt = """
                Based on the current student stats:
                - Overall Accuracy: ${stats.overallAccuracy}%
                - Study Streak: ${stats.streakDays} Days
                - Target Topic: ${stats.currentTarTopic}
                - Mode: JEE & NEET prep
                
                Generate a highly energetic, 3-point study advice and week target.
                Keep it concise and motivating (max 120 words total).
            """.trimIndent()

            val adv = GeminiHelper.askGemini(prompt)
            aiStudyPlannerText = adv
            isPlannerLoading = false
        }
    }

    // Simulate completing a test - gives users interactive rewards
    fun simulateTestCompletion(test: MockTest, scoreAccuracyPercent: Int) {
        viewModelScope.launch {
            // Update overall accuracy & reward XP points
            val stats = userStats.value
            val xpGain = if (scoreAccuracyPercent > 90) 100 else 50
            val totalXpNew = stats.totalXp + xpGain
            val newStreak = stats.streakDays + 1
            val reCalcAccuracy = (stats.overallAccuracy * 4 + scoreAccuracyPercent) / 5

            // Also save user's custom score accuracy back under SQLite mock_tests 
            db.mockTestDao().insertTest(
                test.copy(accuracyPercent = scoreAccuracyPercent)
            )

            db.userStatsDao().updateUserStats(
                stats.copy(
                    streakDays = newStreak,
                    totalXp = totalXpNew,
                    overallAccuracy = reCalcAccuracy
                )
            )

            // Dynamic badges checks
            if (scoreAccuracyPercent >= 90) {
                db.badgeDao().updateBadge(
                    Badge("Mock Hero", "Unlock by scoring above 90% accuracy in a mock test.", "🏆", true, "Unlocked")
                )
            }

            // Record this completed test as a community post as well
            db.doubtThreadDao().insertDoubt(
                DoubtThread(
                    query = "Hooray! Just completed ${test.title} with $scoreAccuracyPercent% accuracy and earned +$xpGain XP!",
                    aiResponse = "[🏆 SYSTEM RECORD] ${stats.userName} achieved ${scoreAccuracyPercent}% on ${test.examSubtype} - ${test.testCategory} exam (${test.difficulty} level). Awesome!",
                    isCommunityPost = true,
                    userName = stats.userName,
                    subject = "Achievements",
                    likesCount = 5,
                    repliesCount = 0
                )
            )
        }
    }
}
