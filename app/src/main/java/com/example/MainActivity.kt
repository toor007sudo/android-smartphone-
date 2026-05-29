package com.example

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.auth.*
import com.example.db.DoubtThread
import com.example.db.MockTest
import com.example.db.StudyTask
import com.example.db.UserStats
import com.example.ui.theme.*
import com.example.ui.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseInitializer.initialize(applicationContext)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppHost()
            }
        }
    }
}

@Composable
fun MainAppHost() {
    val viewModel: AppViewModel = viewModel()
    val userStats by viewModel.userStats.collectAsState()
    val context = LocalContext.current

    // Observe test completion callback simulation to match state persistence
    var showActiveTestDialog by remember { mutableStateOf<MockTest?>(null) }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CosmicBackground),
        containerColor = CosmicBackground,
        bottomBar = {
            CustomBottomNavigation(
                activeScreen = viewModel.currentScreen,
                onScreenSelected = { screen ->
                    viewModel.currentScreen = screen
                    if (screen == "new_test") {
                        // Reset test creator to step 1
                        viewModel.testFlowStep = 1
                    }
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Screen Switcher
            when (viewModel.currentScreen) {
                "dashboard" -> DashboardScreen(
                    viewModel = viewModel,
                    userStats = userStats,
                    onStartTestDirect = { test ->
                        showActiveTestDialog = test
                    },
                    onQuickAccessTool = { toolName ->
                        when (toolName) {
                            "AI Doubt Solver" -> viewModel.currentScreen = "ai_solver"
                            "Mock Tests" -> {
                                viewModel.currentScreen = "new_test"
                                viewModel.testFlowStep = 1
                            }
                            "PYQ" -> {
                                viewModel.currentScreen = "new_test"
                                viewModel.testFlowStep = 1
                                viewModel.selectedTestType = "Previous Year Test"
                            }
                            "Revision" -> viewModel.currentScreen = "learn"
                            "Flashcards" -> {
                                Toast.makeText(context, "Flashcards feature loaded! Generating cards...", Toast.LENGTH_SHORT).show()
                            }
                            "Formula Book" -> viewModel.currentScreen = "learn"
                            "Chapter Roadmap" -> {
                                viewModel.currentScreen = "learn"
                                viewModel.requestSmartStudyTips()
                            }
                            "Study Battle" -> {
                                Toast.makeText(context, "Entering Battle Arena! Competing matching ranks...", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
                "test_stats" -> TestStatsScreen(
                    viewModel = viewModel,
                    userStats = userStats,
                    onRetakeTest = { test ->
                        showActiveTestDialog = test
                    }
                )
                "new_test" -> NewTestWizardScreen(
                    viewModel = viewModel,
                    onBeginTest = { test ->
                        showActiveTestDialog = test
                    }
                )
                "learn" -> LearnHubScreen(
                    viewModel = viewModel,
                    userStats = userStats
                )
                "community" -> CommunityScreen(
                    viewModel = viewModel
                )
                "ai_solver" -> AiDoubtSolverScreen(
                    viewModel = viewModel
                )
            }

            // Interactive Simulated Practice Exam Dialogue
            showActiveTestDialog?.let { testObj ->
                SimulatedTestRunnerDialog(
                    test = testObj,
                    onDismiss = { showActiveTestDialog = null },
                    onSubmitResult = { calculatedAccuracy ->
                        viewModel.simulateTestCompletion(testObj, calculatedAccuracy)
                        showActiveTestDialog = null
                        Toast.makeText(context, "Mock Test Submitted! accuracy reported: ${calculatedAccuracy}%", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENT: Custom Navigation Bar matching raw reference aesthetics EXACTLY
// -------------------------------------------------------------------------
@Composable
fun CustomBottomNavigation(
    activeScreen: String,
    onScreenSelected: (String) -> Unit
) {
    // Elegant neon capsule backdrop
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(24.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp), ambientColor = NeonPurple, spotColor = NeonIndigo)
            .border(
                1.dp,
                Brush.linearGradient(listOf(Color(0x3F94A3B8), Color(0x1F8B5CF6))),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ),
        color = CosmicSurface,
        shadowElevation = 18.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            NavBarItem(
                label = "Home",
                emoji = "🏠",
                isActive = activeScreen == "dashboard",
                onClick = { onScreenSelected("dashboard") },
                testTag = "nav_home"
            )

            NavBarItem(
                label = "Tests",
                emoji = "📊",
                isActive = activeScreen == "test_stats",
                onClick = { onScreenSelected("test_stats") },
                testTag = "nav_test_stats"
            )

            // Elevated Center Button: custom "✨" OR "New Test" indicator
            Box(
                modifier = Modifier
                    .offset(y = (-14).dp)
                    .size(62.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            listOf(
                                NeonPurple,
                                IntensePurple
                            )
                        )
                    )
                    .clickable { onScreenSelected("new_test") }
                    .border(4.dp, CosmicBackground, CircleShape)
                    .shadow(12.dp, CircleShape)
                    .testTag("nav_new_test"),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✨",
                    fontSize = 26.sp,
                    color = Color.White
                )
            }

            NavBarItem(
                label = "Learn",
                emoji = "🏆",
                isActive = activeScreen == "learn",
                onClick = { onScreenSelected("learn") },
                testTag = "nav_learn"
            )

            NavBarItem(
                label = "Community",
                emoji = "👤",
                isActive = activeScreen == "community",
                onClick = { onScreenSelected("community") },
                testTag = "nav_community"
            )
        }
    }
}

@Composable
fun RowScope.NavBarItem(
    label: String,
    emoji: String,
    isActive: Boolean,
    onClick: () -> Unit,
    testTag: String
) {
    val activeColor by animateColorAsState(if (isActive) NeonPurple else TextMuted, label = "ColorAnimation")

    Column(
        modifier = Modifier
            .weight(1f)
            .clickable { onClick() }
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = emoji,
            fontSize = if (isActive) 24.sp else 20.sp,
            modifier = Modifier.padding(bottom = 2.dp)
        )
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = activeColor
        )
    }
}

// -------------------------------------------------------------------------
// SCREEN 1: Home Dashboard View with Motivational Banners & Streaks
// -------------------------------------------------------------------------
@Composable
fun DashboardScreen(
    viewModel: AppViewModel,
    userStats: UserStats,
    onStartTestDirect: (MockTest) -> Unit,
    onQuickAccessTool: (String) -> Unit
) {
    val tasks by viewModel.studyTasks.collectAsState()
    var showProfileDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        // App Header Section
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "WELCOME BACK",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.5.sp
                    )
                    Text(
                        text = "Hello, ${userStats.userName}! 👋",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.currentScreen = "ai_solver" },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.05f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Brush.linearGradient(listOf(NeonPurple, NeonIndigo)))
                            .padding(2.dp)
                            .clickable { showProfileDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                                .background(CosmicSurface),
                            contentAlignment = Alignment.Center
                        ) {
                            if (!userStats.profilePicUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = userStats.profilePicUrl,
                                    contentDescription = "Profile Picture",
                                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Text(
                                    text = "👨‍🎓",
                                    fontSize = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        // Animated Motivational quote Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                Color(0x3F4F46E5), // Purple-900/40
                                Color(0x1F1A1235)  // Indigo-900/40
                            )
                        )
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(20.dp))
                    .padding(14.dp)
            ) {
                Text(
                    text = "\"Focus on progress, not perfection. Every small step brings you closer to your NEET & JEE goals. Unlock your strengths dynamically!\"",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Light
                )
            }
        }

        // Study Streak Core Card (Aesthetic exact replica of reference with Ring)
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(CosmicSurface)
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(24.dp))
                    .padding(18.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left Part
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Study Streak ",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                            Text(
                                text = "🔥",
                                fontSize = 16.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${userStats.streakDays} Days",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "You're on fire! Keep it up. 🚀",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeonPurple
                        )
                    }

                    // Right Part: Dynamic Canvas Progress Ring (representing todayTargetDonePercent)
                    Box(
                        modifier = Modifier.size(90.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Under layer base circle
                            drawCircle(
                                color = Color.White.copy(alpha = 0.05f),
                                radius = size.minDimension / 2.2f,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // Progress Arc
                            drawArc(
                                brush = Brush.linearGradient(listOf(NeonPurple, NeonIndigo)),
                                startAngle = -90f,
                                sweepAngle = (userStats.todayTargetDonePercent / 100f) * 360f,
                                useCenter = false,
                                style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round),
                                size = size / 1.1f,
                                topLeft = Offset(
                                    (size.width - size.width / 1.1f) / 2,
                                    (size.height - size.height / 1.1f) / 2
                                )
                            )
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${userStats.todayTargetDonePercent}%",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Text(
                                text = "Done",
                                fontSize = 9.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        // Section: Today's study tasks list
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Today's Plan",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "View Plan",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonPurple,
                    modifier = Modifier.clickable { viewModel.currentScreen = "learn" }
                )
            }
        }

        items(tasks) { task ->
            TodayPlanTaskItem(task = task, onCheckedChange = { viewModel.toggleTaskCompletion(task) })
        }

        // Section: Quick Access Grid Options (Exactly styled cards)
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Quick Access",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Edit",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        item {
            val quickAccessList = listOf(
                QuickAccessItem("AI Doubt Solver", "🤖", NeonBlue),
                QuickAccessItem("Mock Tests", "📝", NeonPurple),
                QuickAccessItem("PYQ", "📁", Color(0xFFF59E0B)),
                QuickAccessItem("Revision", "📑", NeonEmerald),
                QuickAccessItem("Flashcards", "🗂️", NeonRose),
                QuickAccessItem("Formula Book", "📊", Color(0xFF047857)),
                QuickAccessItem("Chapter Roadmap", "🗺️", IntensePurple),
                QuickAccessItem("Study Battle", "🏆", NeonOrange)
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(quickAccessList) { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .clickable { onQuickAccessTool(item.name) }
                            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
                            .testTag("quick_tool_${item.name.replace(" ", "_")}"),
                        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(item.colorBg.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = item.emoji, fontSize = 20.sp)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = item.name,
                                fontSize = 9.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        ProfileDialog(
            viewModel = viewModel,
            userStats = userStats,
            onDismiss = { showProfileDialog = false }
        )
    }
}

// Support Structures
data class QuickAccessItem(val name: String, val emoji: String, val colorBg: Color)

@Composable
fun TodayPlanTaskItem(task: StudyTask, onCheckedChange: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            .clickable { onCheckedChange() },
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Circular Check Box / Play Button Indicator combo
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            if (task.isCompleted) NeonEmerald.copy(alpha = 0.2f)
                            else NeonPurple.copy(alpha = 0.1f)
                        )
                        .clickable { onCheckedChange() },
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Task Done",
                            tint = NeonEmerald,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Active task",
                            tint = NeonPurple,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = task.subject,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = task.topic,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Schedule hours column
            Text(
                text = task.timeRange,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
                textAlign = TextAlign.End
            )
        }
    }
}

// -------------------------------------------------------------------------
// SCREEN 2 & 3: Interactive Multi-Step NEW TEST CREATOR & LISTING FLOW
// -------------------------------------------------------------------------
@Composable
fun NewTestWizardScreen(
    viewModel: AppViewModel,
    onBeginTest: (MockTest) -> Unit
) {
    val tests by viewModel.mockTests.collectAsState()

    // Filter local list of mock tests based on created configurations
    val filteredTests = remember(tests, viewModel.selectedStream, viewModel.testListFilter, viewModel.selectedTestType, viewModel.selectedDifficulty) {
        tests.filter { test ->
            val matchStream = test.stream == viewModel.selectedStream
            val matchFilter = when (viewModel.testListFilter) {
                "Recommended" -> test.isRecommended
                "Trending" -> test.isTrending
                "High Accuracy" -> test.isHighAccuracy
                else -> true
            }
            matchStream && matchFilter
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        // High-fidelity Multi Step Indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepIndicatorNode(num = 1, label = "Stream & Type", isActive = viewModel.testFlowStep >= 1, isCompleted = viewModel.testFlowStep > 1)
            Box(modifier = Modifier.weight(1f).height(2.dp).background(if (viewModel.testFlowStep > 1) NeonPurple else Color.White.copy(alpha = 0.08f)))
            StepIndicatorNode(num = 2, label = "Test Selection", isActive = viewModel.testFlowStep >= 2, isCompleted = viewModel.testFlowStep > 2)
            Box(modifier = Modifier.weight(1f).height(2.dp).background(if (viewModel.testFlowStep > 2) NeonPurple else Color.White.copy(alpha = 0.08f)))
            StepIndicatorNode(num = 3, label = "Test Settings", isActive = viewModel.testFlowStep >= 3, isCompleted = viewModel.testFlowStep > 3)
        }

        Text(
            text = "New Test Builder",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        // FLOW PANEL SWITCH
        when (viewModel.testFlowStep) {
            1 -> {
                // STEP 1 CONTENT: Streams & Subjects Selection
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Choose your stream",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }

                    // JEE & NEET Core Stream Buttons (exactly replicating reference drawings)
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StreamButtonCard(
                                label = "JEE",
                                tagline = "Engineering Entrance",
                                emoji = "⚙️",
                                isSelected = viewModel.selectedStream == "JEE",
                                onClick = {
                                    viewModel.selectedStream = "JEE"
                                    viewModel.selectedExamType = "JEE Main"
                                    viewModel.selectedSubjects = setOf("Physics", "Chemistry", "Maths")
                                },
                                modifier = Modifier.weight(1f)
                            )
                            StreamButtonCard(
                                label = "NEET",
                                tagline = "Medical Entrance",
                                emoji = "🩺",
                                isSelected = viewModel.selectedStream == "NEET",
                                onClick = {
                                    viewModel.selectedStream = "NEET"
                                    viewModel.selectedExamType = "NEET UG"
                                    viewModel.selectedSubjects = setOf("Physics", "Chemistry", "Biology")
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Text(
                            text = "Select Exam Type",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val subTypes = if (viewModel.selectedStream == "JEE") listOf("JEE Main", "JEE Advanced") else listOf("NEET UG")
                            subTypes.forEach { type ->
                                FilterChipBox(
                                    label = type,
                                    isSelected = viewModel.selectedExamType == type,
                                    onClick = { viewModel.selectedExamType = type }
                                )
                            }
                        }
                    }

                    // Choose Subjects Multi-select
                    item {
                        Text(
                            text = "Choose Subject(s)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                        Text(
                            text = "You can select multiple subjects",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        val subjectsList = if (viewModel.selectedStream == "JEE") {
                            listOf("Physics" to "⚛️", "Chemistry" to "🧪", "Maths" to "📐")
                        } else {
                            listOf("Physics" to "⚛️", "Chemistry" to "🧪", "Biology" to "🧬")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            subjectsList.forEach { (subName, subEmoji) ->
                                val isSelected = viewModel.selectedSubjects.contains(subName)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) NeonPurple.copy(alpha = 0.15f) else CosmicSurface)
                                        .border(
                                            1.dp,
                                            if (isSelected) NeonPurple else Color.White.copy(alpha = 0.06f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable {
                                            if (isSelected) {
                                                if (viewModel.selectedSubjects.size > 1) {
                                                    viewModel.selectedSubjects = viewModel.selectedSubjects - subName
                                                }
                                            } else {
                                                viewModel.selectedSubjects = viewModel.selectedSubjects + subName
                                            }
                                        }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(text = subEmoji, fontSize = 22.sp)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(text = subName, fontSize = 11.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(text = "✓", color = NeonPurple, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Continue trigger to step 2
                Button(
                    onClick = { viewModel.testFlowStep = 2 },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .testTag("continue_btn_step1"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                ) {
                    Text(text = "Continue  →", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }

            2 -> {
                // STEP 2 CONTENT: Test Type & Difficulty selection
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text(
                            text = "Select Test Type",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }

                    item {
                        val testTypes = listOf(
                            "Full Syllabus Test" to "Complete syllabus trial tests",
                            "Chapter Test" to "Topic evaluations for single units",
                            "Topic Test" to "Ultra-specific focus tests",
                            "Previous Year Test" to "Real historical papers (PYQ)"
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            testTypes.forEach { (title, subtitle) ->
                                val isSelected = viewModel.selectedTestType == title
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isSelected) NeonPurple.copy(alpha = 0.1f) else CosmicSurface)
                                        .border(
                                            1.dp,
                                            if (isSelected) NeonPurple else Color.White.copy(alpha = 0.05f),
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable { viewModel.selectedTestType = title }
                                        .padding(14.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(text = subtitle, fontSize = 11.sp, color = TextMuted)
                                        }
                                        if (isSelected) {
                                            Text(text = "✓", color = NeonPurple, fontWeight = FontWeight.Black)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "Select Difficulty",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Easy", "Medium", "Hard").forEach { diff ->
                                val isSelected = viewModel.selectedDifficulty == diff
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) NeonIndigo.copy(alpha = 0.2f) else CosmicSurface)
                                        .border(
                                            1.dp,
                                            if (isSelected) NeonIndigo else Color.White.copy(alpha = 0.05f),
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { viewModel.selectedDifficulty = diff }
                                        .padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = diff, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.testFlowStep = 1 },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(TextMuted, Color.Transparent)))
                    ) {
                        Text(text = "Back")
                    }

                    Button(
                        onClick = { viewModel.testFlowStep = 3 },
                        modifier = Modifier
                            .weight(1.5f)
                            .testTag("continue_btn_step2"),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple)
                    ) {
                        Text(text = "Continue to Settings  →", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }

            3 -> {
                // STEP 3 CONTENT: Mock Tests filter result & trigger taker (Reference Screen 3 replica)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(IntensePurple.copy(alpha = 0.1f))
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📋", fontSize = 24.sp, modifier = Modifier.padding(end = 12.dp))
                    Column {
                        Text(
                            text = "${viewModel.selectedExamType} • ${viewModel.selectedTestType}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${viewModel.selectedSubjects.joinToString()} • ${viewModel.selectedDifficulty}",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Choose Test",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )

                // Filters List: All, Recommended, Trending, High Accuracy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Recommended", "Trending", "High Accuracy").forEach { f ->
                        FilterChipBox(
                            label = f,
                            isSelected = viewModel.testListFilter == f,
                            onClick = { viewModel.testListFilter = f }
                        )
                    }
                }

                // Lists the available mock tests matching current selections
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp)
                ) {
                    if (filteredTests.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = "🔍", fontSize = 42.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = "No matching tests found in database.", color = TextSecondary, fontSize = 13.sp)
                                Text(text = "Change preferences to unlock other tests.", color = TextMuted, fontSize = 11.sp)
                            }
                        }
                    } else {
                        items(filteredTests) { mockObj ->
                            MockTestListCard(
                                testObj = mockObj,
                                onBegin = { onBeginTest(mockObj) }
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = { viewModel.testFlowStep = 2 },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextPrimary),
                    border = ButtonDefaults.outlinedButtonBorder.copy(brush = Brush.linearGradient(listOf(TextMuted, Color.Transparent)))
                ) {
                    Text(text = "Change settings")
                }
            }
        }
    }
}

// Support Node
@Composable
fun StepIndicatorNode(num: Int, label: String, isActive: Boolean, isCompleted: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted) NeonPurple
                    else if (isActive) NeonPurple.copy(alpha = 0.2f)
                    else Color.White.copy(alpha = 0.05f)
                )
                .border(
                    1.dp,
                    if (isActive || isCompleted) NeonPurple else Color.White.copy(alpha = 0.1f),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Text(text = "✓", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            } else {
                Text(
                    text = num.toString(),
                    color = if (isActive) NeonPurple else TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, fontSize = 9.sp, color = if (isActive || isCompleted) TextPrimary else TextMuted, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun StreamButtonCard(
    label: String,
    tagline: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(if (isSelected) NeonPurple.copy(alpha = 0.15f) else CosmicSurface)
            .border(
                1.3.dp,
                if (isSelected) NeonPurple else Color.White.copy(alpha = 0.05f),
                RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(text = emoji, fontSize = 28.sp)
                if (isSelected) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(NeonPurple),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "✓", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = label, fontSize = 16.sp, fontWeight = FontWeight.Black, color = TextPrimary)
            Text(text = tagline, fontSize = 10.sp, color = TextSecondary)
        }
    }
}

@Composable
fun FilterChipBox(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(30.dp))
            .background(if (isSelected) NeonPurple else CosmicSurface)
            .clickable { onClick() }
            .border(
                1.dp,
                if (isSelected) NeonPurple else Color.White.copy(alpha = 0.08f),
                RoundedCornerShape(30.dp)
            )
            .padding(vertical = 5.dp, horizontal = 12.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (isSelected) Color.White else TextSecondary
        )
    }
}

@Composable
fun MockTestListCard(
    testObj: MockTest,
    onBegin: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
            .testTag("test_card_${testObj.title.replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = CosmicSurface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(NeonIndigo.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📝", fontSize = 20.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (testObj.isRecommended) {
                        Surface(
                            color = NeonOrange.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                "Recommended",
                                fontSize = 8.sp,
                                color = NeonOrange,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (testObj.isTrending) {
                        Surface(
                            color = NeonPurple.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(4.dp),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                "Trending",
                                fontSize = 8.sp,
                                color = NeonPurple,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))
                Text(text = testObj.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${testObj.questionsCount} Questions • ${testObj.maxMarks} Marks • ${testObj.durationMins} mins",
                    fontSize = 9.sp,
                    color = TextMuted
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(text = "${testObj.accuracyPercent}%", fontSize = 14.sp, fontWeight = FontWeight.Black, color = NeonEmerald)
                Text(text = "Accuracy", fontSize = 8.sp, color = TextMuted)
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { onBegin() },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.15f), contentColor = NeonPurple),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier.height(24.dp).testTag("begin_test_btn_${testObj.id}")
                ) {
                    Text(text = "Start", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// SCREEN 4: AI DOUBT SOLVER PANEL (with prompt engineering sandbox presets)
// -------------------------------------------------------------------------
@Composable
fun AiDoubtSolverScreen(viewModel: AppViewModel) {
    var queryText by remember { mutableStateOf("") }
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "AI Doubt Solver",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )
        Text(
            text = "Powered by Google Gemini 3.5 Flash",
            fontSize = 11.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Warning prompt about safe sandbox API use
        Surface(
            color = Color(0x1F3B82F6),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().border(1.dp, NeonBlue.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
        ) {
            Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(text = "🛡️", fontSize = 18.sp, modifier = Modifier.padding(end = 8.dp))
                Column {
                    Text(text = "Sandbox Environment Warning", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = NeonBlue)
                    Text(
                        text = "Keys stored securely via Android Secrets; compile targets do not reveal credential keys.",
                        fontSize = 8.sp,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Input Box
        OutlinedTextField(
            value = queryText,
            onValueChange = { queryText = it },
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .testTag("ai_doubt_input"),
            placeholder = { Text(text = "Ask any Physics, Chemistry, Biology, or Maths doubt...", color = TextMuted, fontSize = 12.sp) },
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextSecondary,
                focusedBorderColor = NeonPurple,
                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                focusedContainerColor = CosmicSurface,
                unfocusedContainerColor = CosmicSurface
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = {
                if (queryText.isNotBlank()) {
                    viewModel.askAiDoubt(queryText)
                    focusManager.clearFocus()
                }
            })
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Preset suggestion chips
            Text(text = "Presets: ", fontSize = 10.sp, color = TextMuted)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf("Newton's 3rd Law", "De Broglie equation", "Mitosis vs Meiosis").forEach { preset ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.05f))
                            .clickable { queryText = preset }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(text = preset, fontSize = 8.sp, color = TextSecondary)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = {
                if (queryText.isNotBlank()) {
                    viewModel.askAiDoubt(queryText)
                    focusManager.clearFocus()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("ai_solve_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
            enabled = !viewModel.isDoubtLoading
        ) {
            if (viewModel.isDoubtLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Text(text = "Ask AI Assistant ✨", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Output Result Panel
        Text(
            text = "AI Explanation Output",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 6.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(CosmicSurface)
                .border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                .padding(12.dp)
        ) {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                item {
                    if (viewModel.aiDoubtResponse == null && !viewModel.isDoubtLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🤖", fontSize = 42.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Your smart AI Tutor is ready to compute step-by-step textbook explanations.",
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                color = TextMuted
                            )
                        }
                    } else if (viewModel.isDoubtLoading) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🧠 Computing response...", fontSize = 13.sp, color = NeonPurple)
                        }
                    } else {
                        viewModel.aiDoubtResponse?.let { ans ->
                            if (ans.contains("API_KEY_MISSING_NOTICE")) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "⚠️ Key Required", color = NeonOrange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ans.replace("API_KEY_MISSING_NOTICE: ", ""),
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            } else {
                                Text(
                                    text = ans,
                                    fontSize = 12.sp,
                                    color = TextPrimary,
                                    lineHeight = 18.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// SCREEN 5: LEARN HUB — Formula finder & AI Planner Recommendations
// -------------------------------------------------------------------------
@Composable
fun LearnHubScreen(viewModel: AppViewModel, userStats: UserStats) {
    val badges by viewModel.badges.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Learn Hub & Gamification",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Text(
                text = "Personalized formulas database and badging rewards",
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        // Formula Search Book
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(text = "Formula Finder", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = viewModel.formulaBookSearchQuery,
                        onValueChange = { viewModel.formulaBookSearchQuery = it },
                        modifier = Modifier.fillMaxWidth().testTag("formula_search"),
                        placeholder = { Text("Search Coulomb's law, derivatives, photosynthesis...", fontSize = 11.sp, color = TextMuted) },
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = TextMuted) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextSecondary,
                            focusedBorderColor = NeonPurple,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                            focusedContainerColor = CosmicBackground,
                            unfocusedContainerColor = CosmicBackground
                        )
                    )

                    // Dummy matching responses based on inputs
                    if (viewModel.formulaBookSearchQuery.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(CosmicBackground)
                                .padding(10.dp)
                        ) {
                            val math = viewModel.formulaBookSearchQuery.lowercase()
                            val answer = when {
                                math.contains("coulomb") -> "Coulomb's Law: F = k * (|q1 * q2|) / r^2\nWhere k = 8.99e9 N m²/C² (vacuum permittivity constant)."
                                math.contains("deriv") || math.contains("calculus") -> "Basic Derivatives:\nd/dx (x^n) = n * x^(n-1)\nd/dx (sin x) = cos x\nd/dx (e^x) = e^x."
                                math.contains("photo") -> "Photosynthesis equation:\n6CO₂ + 6H₂O + light -> C₆H₁₂O₆ + 6O₂\nOccurs inside plant chloroplasts."
                                else -> "Formula search complete: Found 1 matching formula in high yield guides.\nResult: 'Equation parameters: log(A) - log(B) = log(A/B)'"
                            }
                            Text(text = answer, fontSize = 11.sp, color = NeonPurple, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Smart dynamic study planner from Gemini API!
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(text = "Smart AI Study Planner", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Text(text = "Refining targets based on accuracy (${userStats.overallAccuracy}%)", fontSize = 10.sp, color = TextMuted)
                        }
                        Button(
                            onClick = { viewModel.requestSmartStudyTips() },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            modifier = Modifier.height(28.dp).testTag("generate_plan_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Generate", fontSize = 9.sp, color = Color.White)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CosmicBackground)
                            .padding(10.dp)
                    ) {
                        if (viewModel.isPlannerLoading) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = NeonPurple)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Analyzing weaknesses...", fontSize = 11.sp, color = TextSecondary)
                            }
                        } else if (viewModel.aiStudyPlannerText == null) {
                            Text(
                                "Click Generate to construct customized daily targets designed by Gemini.",
                                fontSize = 11.sp, color = TextMuted
                            )
                        } else {
                            Text(
                                text = viewModel.aiStudyPlannerText!!,
                                fontSize = 11.sp, color = TextPrimary, lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        }

        // Gamified Badges unlocked listing
        item {
            Text(text = "Earned Badges", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        }

        items(badges) { badge ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CosmicSurface)
                    .border(
                        1.dp,
                        if (badge.isUnlocked) NeonPurple.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (badge.isUnlocked) NeonPurple.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = badge.iconEmoji, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = badge.name, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text(text = badge.description, fontSize = 10.sp, color = TextMuted)
                }
                if (badge.isUnlocked) {
                    Text(
                        text = "UNLOCKED ✓",
                        color = NeonEmerald,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                } else {
                    Text(
                        text = "LOCKED",
                        color = TextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// SCREEN 3: TEST STATS / HISTORICAL ARCHIVE
// -------------------------------------------------------------------------
@Composable
fun TestStatsScreen(
    viewModel: AppViewModel,
    userStats: UserStats,
    onRetakeTest: (MockTest) -> Unit
) {
    val tests by viewModel.mockTests.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
    ) {
        item {
            Text(
                text = "Diagnostic Metrics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Text(
                text = "Tracking offline progress over mock challenges",
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        // Summary Graph Card Mockup
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Overall Performance Tracker", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Spacer(modifier = Modifier.height(12.dp))

                    // Minimal Bar Chart to resemble real analytical widgets
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        val points = listOf(60 to "Physics", 78 to "Chem", 92 to "Maths", 84 to "Bio")
                        points.forEach { (heightVal, label) ->
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Box(
                                    modifier = Modifier
                                        .width(28.dp)
                                        .height((heightVal).dp)
                                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                        .background(Brush.verticalGradient(listOf(NeonPurple, NeonIndigo)))
                                ) {
                                    Text(
                                        text = "$heightVal%",
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 2.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(text = label, fontSize = 9.sp, color = TextSecondary)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(text = "Past Mock Tests Attempted", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
        }

        items(tests) { test ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().border(0.5.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = test.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Text(text = "${test.examSubtype} - ${test.testCategory}", fontSize = 10.sp, color = TextSecondary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${test.accuracyPercent}% Accuracy",
                            color = NeonEmerald,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(end = 12.dp)
                        )
                        Button(
                            onClick = { onRetakeTest(test) },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple.copy(alpha = 0.15f), contentColor = NeonPurple),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                            modifier = Modifier.height(24.dp)
                        ) {
                            Text("Retake", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// SCREEN 6: COMMUNITY BOARD (With Ask AI Helper responses)
// -------------------------------------------------------------------------
@Composable
fun CommunityScreen(viewModel: AppViewModel) {
    val doubts by viewModel.doubtThreads.collectAsState()
    var discussionText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Physics") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Community boards & Doubts Forum",
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )
        Text(
            text = "Ask questions, share advice, list answers with AI backup",
            fontSize = 11.sp,
            color = TextMuted
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Fast typing form
        Card(
            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            shape = RoundedCornerShape(18.dp),
            modifier = Modifier.border(1.dp, Color.White.copy(alpha = 0.05f), RoundedCornerShape(18.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(text = "Start a New Discussion / Ask doubt", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = discussionText,
                    onValueChange = { discussionText = it },
                    modifier = Modifier.fillMaxWidth().testTag("community_post_field"),
                    placeholder = { Text("What syllabus concepts are confusing today? Ask here...", fontSize = 11.sp, color = TextMuted) },
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextSecondary,
                        focusedBorderColor = NeonPurple,
                        unfocusedBorderColor = Color.White.copy(alpha = 0.05f),
                        focusedContainerColor = CosmicBackground,
                        unfocusedContainerColor = CosmicBackground
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Category Selection Row
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("Physics", "Chemistry", "Maths", "Biology").forEach { cat ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(30.dp))
                                    .background(if (selectedCategory == cat) NeonPurple else CosmicBackground)
                                    .clickable { selectedCategory = cat }
                                    .padding(vertical = 4.dp, horizontal = 10.dp)
                            ) {
                                Text(text = cat, fontSize = 9.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    Button(
                        onClick = {
                            if (discussionText.isNotBlank()) {
                                viewModel.submitCommunityQuestion(discussionText, selectedCategory)
                                discussionText = ""
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(28.dp).testTag("post_confirm_btn"),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 2.dp)
                    ) {
                        Text("Post", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Discussion list feed
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 16.dp)
        ) {
            items(doubts.filter { it.isCommunityPost }) { doubt ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, Color.White.copy(alpha = 0.04f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = CosmicSurface),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "👤", fontSize = 16.sp, modifier = Modifier.padding(end = 6.dp))
                                Text(text = doubt.userName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            }
                            Surface(color = NeonPurple.copy(alpha = 0.15f), shape = RoundedCornerShape(4.dp)) {
                                Text(
                                    text = doubt.subject, fontSize = 8.sp, color = NeonPurple,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = doubt.query, fontSize = 12.sp, color = TextPrimary, lineHeight = 16.sp)

                        // If AI solution exists, show it inside beautiful neon panel
                        doubt.aiResponse?.let { resp ->
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(IntensePurple.copy(alpha = 0.08f))
                                    .border(0.5.dp, IntensePurple.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = "🤖 AI Tutor Response:\n$resp",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 15.sp
                                )
                            }
                        } ?: run {
                            // Button to solve dynamic post using AI
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(
                                onClick = { viewModel.triggerAiCommunityReply(doubt) },
                                colors = ButtonDefaults.buttonColors(containerColor = NeonIndigo.copy(alpha = 0.15f), contentColor = NeonIndigo),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                modifier = Modifier.height(24.dp).testTag("ai_solve_comm_${doubt.id}")
                            ) {
                                Text(text = "✨ Call AI Tutor to Solve", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Social count summaries
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "❤️ ${doubt.likesCount} student upvotes", fontSize = 9.sp, color = TextMuted)
                            Text(text = "💬 ${doubt.repliesCount} replies", fontSize = 9.sp, color = TextMuted)
                        }
                    }
                }
            }
        }
    }
}

// -------------------------------------------------------------------------
// COMPONENT: MOCK TEST SIMULATED EXAM TAKER
// Allows users to mock take 3 actual sample academic questions & records score stats!
// -------------------------------------------------------------------------
@Composable
fun SimulatedTestRunnerDialog(
    test: MockTest,
    onDismiss: () -> Unit,
    onSubmitResult: (accuracy: Int) -> Unit
) {
    var q1Selection by remember { mutableStateOf<String?>(null) }
    var q2Selection by remember { mutableStateOf<String?>(null) }
    var q3Selection by remember { mutableStateOf<String?>(null) }

    val q1Options = listOf("Option A", "Option B", "Option C", "Option D")

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, NeonPurple, RoundedCornerShape(24.dp))
                .padding(12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "📝 Practice Arena",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "Taking: ${test.title} (${test.examSubtype})",
                    fontSize = 11.sp,
                    color = NeonPurple,
                    fontWeight = FontWeight.Bold
                )

                Divider(color = Color.White.copy(alpha = 0.05f))

                LazyColumn(
                    modifier = Modifier.height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Q1
                    item {
                        Column {
                            Text(text = "Q1: If a particle travels in a circular path with constant speed, its acceleration direction is:", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            q1Options.forEach { opt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (q1Selection == opt) NeonPurple.copy(alpha = 0.15f) else CosmicBackground)
                                        .clickable { q1Selection = opt }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = q1Selection == opt, onClick = { q1Selection = opt }, colors = RadioButtonDefaults.colors(selectedColor = NeonPurple))
                                    Text(
                                        text = when(opt) {
                                            "Option A" -> "Along the velocity vector"
                                            "Option B" -> "Towards the center of the circle (Centripetal) [Correct]"
                                            "Option C" -> "Outwards from the center"
                                            else -> "None of the above"
                                        },
                                        fontSize = 10.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    // Q2
                    item {
                        Column {
                            Text(text = "Q2: Which of the following compounds has a coordinate covalent coordinate bond?", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            listOf("NH4+ [Correct]", "NaCl", "H2O", "CH4").forEach { opt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (q2Selection == opt) NeonPurple.copy(alpha = 0.15f) else CosmicBackground)
                                        .clickable { q2Selection = opt }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = q2Selection == opt, onClick = { q2Selection = opt }, colors = RadioButtonDefaults.colors(selectedColor = NeonPurple))
                                    Text(text = opt, fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                        }
                    }

                    // Q3
                    item {
                        Column {
                            Text(text = "Q3: The derivative of sin(x^2) with respect to x is:", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(6.dp))
                            listOf("2x * cos(x^2) [Correct]", "cos(2x)", "-2x * cos(x^2)", "2 * sin(x)").forEach { opt ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (q3Selection == opt) NeonPurple.copy(alpha = 0.15f) else CosmicBackground)
                                        .clickable { q3Selection = opt }
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(selected = q3Selection == opt, onClick = { q3Selection = opt }, colors = RadioButtonDefaults.colors(selectedColor = NeonPurple))
                                    Text(text = opt, fontSize = 10.sp, color = TextSecondary)
                                }
                            }
                        }
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.05f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { onDismiss() }) {
                        Text(text = "Cancel", color = TextMuted)
                    }
                    Button(
                        onClick = {
                            var correct = 0
                            if (q1Selection == "Option B") correct++
                            if (q2Selection == "NH4+ [Correct]") correct++
                            if (q3Selection == "2x * cos(x^2) [Correct]") correct++

                            val acc = when (correct) {
                                3 -> 100
                                2 -> 66
                                1 -> 33
                                else -> 0
                            }
                            onSubmitResult(acc)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                        enabled = q1Selection != null && q2Selection != null && q3Selection != null,
                        modifier = Modifier.testTag("submit_exam_dialog_btn")
                    ) {
                        Text(text = "Submit Answers ✓", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDialog(
    viewModel: AppViewModel,
    userStats: UserStats,
    onDismiss: () -> Unit
) {
    val authState by viewModel.authState.collectAsState()
    val context = LocalContext.current

    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var isSignUpMode by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    // Image upload handler state
    var isUploadingImage by remember { mutableStateOf(false) }
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            isUploadingImage = true
            viewModel.uploadProfileImage(uri) { downloadUrl ->
                isUploadingImage = false
                if (downloadUrl != null) {
                    Toast.makeText(context, "Profile picture uploaded successfully!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Upload failed. Please try again.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    Dialog(onDismissRequest = { onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = CosmicSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STUDENT PROFILE",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        letterSpacing = 1.sp
                    )
                    IconButton(onClick = { onDismiss() }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                    }
                }

                Divider(color = Color.White.copy(alpha = 0.08f))

                when (val auth = authState) {
                    is AuthState.Authenticated -> {
                        // Profile Avatar
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(Brush.linearGradient(listOf(NeonPurple, NeonIndigo)))
                                .padding(3.dp)
                                .clickable {
                                    if (!isUploadingImage) {
                                        galleryLauncher.launch("image/*")
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(CosmicBackground),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingImage) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(28.dp),
                                        color = NeonPurple,
                                        strokeWidth = 3.dp
                                    )
                                } else if (!userStats.profilePicUrl.isNullOrEmpty()) {
                                    AsyncImage(
                                        model = userStats.profilePicUrl,
                                        contentDescription = "Profile Picture",
                                        modifier = Modifier.fillMaxSize().clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Text(text = "👨‍🎓", fontSize = 48.sp)
                                }
                            }
                        }

                        Text(
                            text = if (!userStats.profilePicUrl.isNullOrEmpty()) "Tap to change photo" else "Tap to upload photo",
                            fontSize = 11.sp,
                            color = TextMuted,
                            fontWeight = FontWeight.Medium
                        )

                        Text(
                            text = userStats.userName,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )

                        Text(
                            text = auth.email,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        // Stats Summary Row
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = CosmicBackground),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.05f))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Study Streak", color = TextSecondary, fontSize = 13.sp)
                                    Text("${userStats.streakDays} Days 🔥", color = NeonOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Academic XP", color = TextSecondary, fontSize = 13.sp)
                                    Text("${userStats.totalXp} XP 🚀", color = NeonPurple, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Overall Accuracy", color = TextSecondary, fontSize = 13.sp)
                                    Text("${userStats.overallAccuracy}% 🎯", color = NeonEmerald, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = {
                                viewModel.logout()
                                Toast.makeText(context, "Signed out safely", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red.copy(alpha = 0.1f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .border(1.dp, Color.Red.copy(alpha = 0.25f), RoundedCornerShape(50.dp)),
                            shape = RoundedCornerShape(50.dp)
                        ) {
                            Text("Sign Out", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }

                    AuthState.Unauthenticated -> {
                        // Tab selections
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(CosmicBackground, RoundedCornerShape(12.dp))
                                .padding(4.dp)
                        ) {
                            Button(
                                onClick = { isSignUpMode = false },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isSignUpMode) CosmicSurface else Color.Transparent
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("Sign In", color = if (!isSignUpMode) TextPrimary else TextMuted, fontSize = 13.sp)
                            }

                            Button(
                                onClick = { isSignUpMode = true },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isSignUpMode) CosmicSurface else Color.Transparent
                                ),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(8.dp),
                                contentPadding = PaddingValues(vertical = 8.dp)
                            ) {
                                Text("Sign Up", color = if (isSignUpMode) TextPrimary else TextMuted, fontSize = 13.sp)
                            }
                        }

                        if (isSignUpMode) {
                            OutlinedTextField(
                                value = nameInput,
                                onValueChange = { nameInput = it },
                                label = { Text("Student Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonPurple,
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                    focusedLabelColor = NeonPurple,
                                    unfocusedLabelColor = TextMuted,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                )
                            )
                        }

                        OutlinedTextField(
                            value = emailInput,
                            onValueChange = { emailInput = it },
                            label = { Text("Email Address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedLabelColor = NeonPurple,
                                unfocusedLabelColor = TextMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        OutlinedTextField(
                            value = passwordInput,
                            onValueChange = { passwordInput = it },
                            label = { Text("Password") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NeonPurple,
                                unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                                focusedLabelColor = NeonPurple,
                                unfocusedLabelColor = TextMuted,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )

                        Button(
                            onClick = {
                                if (emailInput.isBlank() || passwordInput.isBlank() || (isSignUpMode && nameInput.isBlank())) {
                                    Toast.makeText(context, "Please enter all fields", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }
                                isLoading = true
                                if (isSignUpMode) {
                                    viewModel.register(emailInput, passwordInput, nameInput) { result ->
                                        isLoading = false
                                        when (result) {
                                            is com.example.auth.AuthResult.Success -> {
                                                Toast.makeText(context, "Registered successfully!", Toast.LENGTH_SHORT).show()
                                            }
                                            is com.example.auth.AuthResult.Failure -> {
                                                Toast.makeText(context, "Error: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                } else {
                                    viewModel.login(emailInput, passwordInput) { result ->
                                        isLoading = false
                                        when (result) {
                                            is com.example.auth.AuthResult.Success -> {
                                                Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                                            }
                                            is com.example.auth.AuthResult.Failure -> {
                                                Toast.makeText(context, "Error: ${result.errorMessage}", Toast.LENGTH_LONG).show()
                                            }
                                        }
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonPurple),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp),
                            shape = RoundedCornerShape(50.dp),
                            enabled = !isLoading
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                            } else {
                                Text(if (isSignUpMode) "Register & Start Study" else "Sign In", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
        }
    }
}
