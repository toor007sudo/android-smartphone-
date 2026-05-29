package com.example.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ----------------------------------------------------
// 1. Entities
// ----------------------------------------------------

@Entity(tableName = "user_stats")
data class UserStats(
    @PrimaryKey val id: Int = 1,
    val userName: String = "Arjun",
    val streakDays: Int = 27,
    val totalXp: Int = 1420,
    val overallAccuracy: Int = 89,
    val currentTarTopic: String = "Physics: Rotational Motion",
    val todayTargetDonePercent: Int = 68,
    val profilePicUrl: String? = null
)

@Entity(tableName = "study_tasks")
data class StudyTask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subject: String, // e.g. "Physics", "Chemistry", "Maths", "Biology"
    val topic: String,   // e.g. "Rotational Motion"
    val timeRange: String, // e.g. "9:00 AM - 11:00 AM"
    val isCompleted: Boolean = false
)

@Entity(tableName = "mock_tests")
data class MockTest(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val questionsCount: Int,
    val maxMarks: Int,
    val durationMins: Int,
    val accuracyPercent: Int,
    val studentsCountString: String, // e.g. "12.5K"
    val stream: String, // "JEE" or "NEET"
    val subjectSummary: String, // e.g. "Physics, Chemistry, Maths"
    val examSubtype: String, // "JEE Main", "JEE Advanced", "NEET UG"
    val testCategory: String, // "Full syllabus", "Chapter", "Topic", "PYQ"
    val difficulty: String, // "Easy", "Medium", "Hard"
    val isRecommended: Boolean = false,
    val isTrending: Boolean = false,
    val isHighAccuracy: Boolean = false
)

@Entity(tableName = "doubt_threads")
data class DoubtThread(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val query: String,
    val aiResponse: String?,
    val isCommunityPost: Boolean = false,
    val userName: String = "Arjun",
    val subject: String = "General",
    val likesCount: Int = 0,
    val repliesCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "badges")
data class Badge(
    @PrimaryKey val name: String,
    val description: String,
    val iconEmoji: String,
    val isUnlocked: Boolean = false,
    val dateUnlocked: String = ""
)

// ----------------------------------------------------
// 2. DAOs
// ----------------------------------------------------

@Dao
interface UserStatsDao {
    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    fun getUserStatsFlow(): Flow<UserStats?>

    @Query("SELECT * FROM user_stats WHERE id = 1 LIMIT 1")
    suspend fun getUserStatsDirect(): UserStats?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserStats(stats: UserStats)

    @Update
    suspend fun updateUserStats(stats: UserStats)
}

@Dao
interface StudyTaskDao {
    @Query("SELECT * FROM study_tasks ORDER BY id ASC")
    fun getStudyTasksFlow(): Flow<List<StudyTask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: StudyTask)

    @Update
    suspend fun updateTask(task: StudyTask)

    @Query("DELETE FROM study_tasks")
    suspend fun clearTasks()
}

@Dao
interface MockTestDao {
    @Query("SELECT * FROM mock_tests")
    fun getAllTestsFlow(): Flow<List<MockTest>>

    @Query("SELECT * FROM mock_tests")
    suspend fun getAllTestsDirect(): List<MockTest>

    @Query("SELECT * FROM mock_tests WHERE stream = :stream")
    fun getTestsByStream(stream: String): Flow<List<MockTest>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTest(test: MockTest)

    @Query("DELETE FROM mock_tests")
    suspend fun clearTests()
}

@Dao
interface DoubtThreadDao {
    @Query("SELECT * FROM doubt_threads ORDER BY timestamp DESC")
    fun getDoubtsFlow(): Flow<List<DoubtThread>>

    @Query("SELECT * FROM doubt_threads WHERE isCommunityPost = 1 ORDER BY timestamp DESC")
    fun getCommunityPostsFlow(): Flow<List<DoubtThread>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDoubt(doubt: DoubtThread)

    @Update
    suspend fun updateDoubt(doubt: DoubtThread)
}

@Dao
interface BadgeDao {
    @Query("SELECT * FROM badges")
    fun getAllBadgesFlow(): Flow<List<Badge>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBadge(badge: Badge)

    @Update
    suspend fun updateBadge(badge: Badge)
}

// ----------------------------------------------------
// 3. AppDatabase
// ----------------------------------------------------

@Database(
    entities = [UserStats::class, StudyTask::class, MockTest::class, DoubtThread::class, Badge::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userStatsDao(): UserStatsDao
    abstract fun studyTaskDao(): StudyTaskDao
    abstract fun mockTestDao(): MockTestDao
    abstract fun doubtThreadDao(): DoubtThreadDao
    abstract fun badgeDao(): BadgeDao
}
