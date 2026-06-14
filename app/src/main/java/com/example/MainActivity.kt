package com.example

import android.Manifest
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import kotlin.math.cos
import kotlin.math.sin
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.*
import com.example.ui.theme.*
import com.example.ui.AzkarTab
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {

    private lateinit var uiUpdateReceiver: BroadcastReceiver
    private var modelInstance: PrayerViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Register local update receiver to sync internal silent simulator state with broadcasts
        uiUpdateReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                modelInstance?.checkSilentPrefsState()
            }
        }
        val filter = IntentFilter(PrayerSilenceReceiver.ACTION_UI_UPDATE)
        androidx.core.content.ContextCompat.registerReceiver(
            this,
            uiUpdateReceiver,
            filter,
            androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
        )

        setContent {
            MyApplicationTheme(darkTheme = true) { // Immersive Dark Theme Enabled
                val mainViewModel: PrayerViewModel = viewModel()
                modelInstance = mainViewModel

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigationContainer(viewModel = mainViewModel)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(uiUpdateReceiver)
    }
}

@Composable
fun AppNavigationContainer(viewModel: PrayerViewModel) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) }
    var showStartupPermissions by remember { mutableStateOf(true) }
    var showSplashScreen by remember { mutableStateOf(true) }

    if (showSplashScreen) {
        SplashScreenComponent(onFinished = { showSplashScreen = false })
    } else {
        if (showStartupPermissions) {
            StartupPermissionRequestDialog(onDismiss = { showStartupPermissions = false })
        }

        // Bottom Navigation with system safe inset padding
        Scaffold(
        bottomBar = {
            NavigationBar(
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars),
                containerColor = EmeraldContainer,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.AccessTime, contentDescription = "الأذان") },
                    label = { Text("المواقيت", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldDeepDark,
                        selectedTextColor = IslamicGold,
                        indicatorColor = IslamicGold,
                        unselectedIconColor = SlateGray,
                        unselectedTextColor = SlateGray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.VolumeMute, contentDescription = "الوضع الصامت") },
                    label = { Text("الوضع الصامت", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldDeepDark,
                        selectedTextColor = IslamicGold,
                        indicatorColor = IslamicGold,
                        unselectedIconColor = SlateGray,
                        unselectedTextColor = SlateGray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Mosque, contentDescription = "المساجد") },
                    label = { Text("المساجد", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldDeepDark,
                        selectedTextColor = IslamicGold,
                        indicatorColor = IslamicGold,
                        unselectedIconColor = SlateGray,
                        unselectedTextColor = SlateGray
                    )
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Book, contentDescription = "الأذكار") },
                    label = { Text("الأذكار", fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldDeepDark,
                        selectedTextColor = IslamicGold,
                        indicatorColor = IslamicGold,
                        unselectedIconColor = SlateGray,
                        unselectedTextColor = SlateGray
                    )
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> TimingsTab(viewModel = viewModel)
                1 -> SilentSettingsTab(viewModel = viewModel)
                2 -> MosquesTab(viewModel = viewModel)
                3 -> AzkarTab()
            }

            // Beautiful overlapping silent indicator bar if hardware is actively muted
            val isSilenced by viewModel.isCurrentlySilenced.collectAsState()
            val silencedPrayer by viewModel.silencedPrayerName.collectAsState()
            val remainingTimer by viewModel.silenceTimeRemainingText.collectAsState()

            AnimatedVisibility(
                visible = isSilenced,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = IslamicGold),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(EmeraldContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsOff,
                                    contentDescription = "Silent Active",
                                    tint = IslamicGold,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "الوضع الصامت مفعل تلقائياً 🌙",
                                    color = EmeraldDeepDark,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                                Text(
                                    text = "لصلاة $silencedPrayer | متبقي: $remainingTimer",
                                    color = EmeraldDeepDark.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        Button(
                            onClick = { viewModel.forceEndSilencing() },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldDeepDark),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("إلغاء الآن", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
  }
}

@Composable
fun SplashScreenComponent(onFinished: () -> Unit) {
    var startAnimation by remember { mutableStateOf(false) }

    val alphaAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = FastOutSlowInEasing
        )
    )

    val scaleAnim by animateFloatAsState(
        targetValue = if (startAnimation) 1.0f else 0.7f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = FastOutSlowInEasing
        )
    )

    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2500)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(EmeraldDeepDark),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            Card(
                modifier = Modifier
                    .size(160.dp)
                    .graphicsLayer(
                        alpha = alphaAnim,
                        scaleX = scaleAnim,
                        scaleY = scaleAnim
                    ),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, IslamicGold.copy(alpha = 0.5f))
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.silent_bell_mosque_app_icon_1781161527079),
                        contentDescription = "أيقونة تطبيق خشوع",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "خشوع",
                color = IslamicGold,
                fontSize = 38.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer(alpha = alphaAnim)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "مسجد بدون إزعاج • صلاة خشوع",
                color = SandText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer(alpha = alphaAnim)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Mosque Free from Distractions • Calm Devotion",
                color = SlateGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                textAlign = TextAlign.Center,
                modifier = Modifier.graphicsLayer(alpha = alphaAnim)
            )
        }
    }
}

// ==========================================
// TAB 1: PRAYER TIMINGS
// ==========================================
@Composable
fun TimingsTab(viewModel: PrayerViewModel) {
    val context = LocalContext.current
    val city by viewModel.selectedCity.collectAsState()
    val timings by viewModel.prayerTimings.collectAsState()
    val countdown by viewModel.countdownText.collectAsState()
    val nextPrayerName by viewModel.nextPrayerName.collectAsState()
    val nextPrayerTime by viewModel.nextPrayerTimeText.collectAsState()
    val dailyLogs by viewModel.dailyLogs.collectAsState()
    val juristicRule by viewModel.juristicRule.collectAsState()

    var showCityDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    // Real GPS trigger and permission requester
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            Toast.makeText(context, "جاري تحديد إحداثيات موقعك الحالي...", Toast.LENGTH_SHORT).show()
            try {
                val fusedClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                fusedClient.lastLocation.addOnSuccessListener { loc: android.location.Location? ->
                    if (loc != null) {
                        viewModel.selectCity(
                            PrayerTimesCalculator.CityConfig(
                                nameAr = "موقعي الحالي (GPS)",
                                nameEn = "My Location (GPS)",
                                latitude = loc.latitude,
                                longitude = loc.longitude,
                                timezone = 1.0,
                                method = PrayerTimesCalculator.CalculationMethod.ALGERIA
                            )
                        )
                        Toast.makeText(context, "تم تحديد مكانك الجغرافي بنجاح! 📍", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "لم نتمكن من جلب موقعك بالـ GPS. يرجى التأكد من تشغيل التموضع الجغرافي بالهاتف.", Toast.LENGTH_LONG).show()
                    }
                }
            } catch (e: SecurityException) {
                Log.e("GPSLocator", "Security exception getting location: ${e.message}")
            }
        } else {
            Toast.makeText(context, "يرجى منح أذونات الموقع لتشغيل ميزة تتبع موقع الـ GPS التلقائي.", Toast.LENGTH_SHORT).show()
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Safe status-bar notch space margin
        item { Spacer(modifier = Modifier.height(20.dp)) }

        // Top Navigation Dropdown Header
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        showSettingsDialog = true
                    },
                    modifier = Modifier.background(EmeraldContainer, RoundedCornerShape(10.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = "Settings info",
                        tint = IslamicGold
                    )
                }

                Row(
                    modifier = Modifier
                        .background(EmeraldContainer, RoundedCornerShape(24.dp))
                        .clickable { showCityDialog = true }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocationOn, contentDescription = "City Pin", tint = IslamicGold, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(text = city.nameAr, color = SandText, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown", tint = IslamicGold)
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(EmeraldContainer, RoundedCornerShape(10.dp))
                        .clickable {
                            // Dynamic Real GPS Tracker Launcher
                            permissionLauncher.launch(
                                arrayOf(
                                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Locate", tint = IslamicGold, modifier = Modifier.size(18.dp))
                }
            }
        }

        // Date and Hijri / Friday Blessed practices
        item {
            DateAndFridayHeader()
        }

        // Beautiful Radial Golden Dome Canvas and Countdown
        item {
            GoldenDomeCountdownHeader(
                nextPrayerName = nextPrayerName,
                nextPrayerTime = nextPrayerTime,
                countdown = countdown
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "مواقيت الصلوات اليوم",
                    color = SandText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "سجل الفرائض 📝",
                    color = IslamicGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // List of Prayers with check logs
        if (timings != null) {
            val itemsData = listOf(
                PrayerWidgetData("fajr", "الفجر", "Fajr", timings!!.fajr, Icons.Default.Brightness3),
                PrayerWidgetData("sunrise", "الشروق", "Sunrise", timings!!.sunrise, Icons.Default.WbTwilight, isSunrise = true),
                PrayerWidgetData("dhuhr", "الظهر", "Dhuhr", timings!!.dhuhr, Icons.Default.WbSunny),
                PrayerWidgetData("asr", "العصر", "Asr", timings!!.asr, Icons.Default.FilterDrama),
                PrayerWidgetData("maghrib", "المغرب", "Maghrib", timings!!.maghrib, Icons.Default.WbCloudy),
                PrayerWidgetData("isha", "العشاء", "Isha", timings!!.isha, Icons.Default.NightsStay)
            )

            items(itemsData) { item ->
                // Check current logged state
                val logItem = dailyLogs.firstOrNull { it.prayerNameEn == item.key }
                val status = logItem?.status ?: "not_prayed"

                PrayerTimeCard(
                    data = item,
                    status = status,
                    onStatusClick = {
                        if (!item.isSunrise) {
                            viewModel.togglePrayerCompleted(item.key, status)
                        }
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }

    if (showCityDialog) {
        CitySelectionDialog(
            cities = PrayerTimesCalculator.preconfiguredCities,
            currentSelected = city,
            onCitySelected = {
                viewModel.selectCity(it)
                showCityDialog = false
            },
            onDismiss = { showCityDialog = false }
        )
    }

    if (showSettingsDialog) {
        CustomSettingsDialog(viewModel = viewModel, onDismiss = { showSettingsDialog = false })
    }
}

data class PrayerWidgetData(
    val key: String,
    val nameAr: String,
    val nameEn: String,
    val time: String,
    val icon: ImageVector,
    val isSunrise: Boolean = false
)

@Composable
fun GoldenDomeCountdownHeader(
    nextPrayerName: String,
    nextPrayerTime: String,
    countdown: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(210.dp)
            .padding(top = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Arch Background Outline via Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Golden arch outline path
            val path = Path().apply {
                moveTo(width * 0.15f, height)
                lineTo(width * 0.15f, height * 0.45f)
                // Dome curve top
                cubicTo(
                    width * 0.15f, height * 0.12f,
                    width * 0.35f, 0f,
                    width * 0.5f, 0f
                )
                cubicTo(
                    width * 0.65f, 0f,
                    width * 0.85f, height * 0.12f,
                    width * 0.85f, height * 0.45f
                )
                lineTo(width * 0.85f, height)
                close()
            }

            // Fill with subtle dark-emerald gradient
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(EmeraldContainer, EmeraldDeepDark)
                )
            )

            // Draw Golden Stroke
            drawPath(
                path = path,
                color = IslamicGold,
                style = Stroke(width = 2.5.dp.toPx())
            )

            // Decorative gold dome stars / circles
            drawCircle(color = IslamicGold.copy(alpha = 0.5f), radius = 3.dp.toPx(), center = Offset(width * 0.25f, height * 0.5f))
            drawCircle(color = IslamicGold.copy(alpha = 0.5f), radius = 3.dp.toPx(), center = Offset(width * 0.75f, height * 0.5f))
            drawCircle(color = IslamicGold.copy(alpha = 0.5f), radius = 4.dp.toPx(), center = Offset(width * 0.5f, height * 0.12f))
        }

        // Info inside the Dome
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            Text(
                text = "الأذان القادم: $nextPrayerName",
                color = IslamicGold,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Alarm, contentDescription = "Alarm clock icon", tint = SandGold, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = nextPrayerTime,
                    color = SandText,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "الوقت المتبقي للأذان",
                color = SlateGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(2.dp))
            // Radiant countdown bubble
            Box(
                modifier = Modifier
                    .background(IslamicEmerald.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                    .border(1.dp, IslamicGold.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = countdown,
                    color = SandGold,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun PrayerTimeCard(
    data: PrayerWidgetData,
    status: String,
    onStatusClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .testTag("prayer_card_${data.key}"),
        colors = CardDefaults.cardColors(containerColor = EmeraldContainer.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon + Names Grouping
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(IslamicEmerald.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = data.icon,
                        contentDescription = data.nameEn,
                        tint = IslamicGold,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = data.nameAr,
                        color = SandText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = data.nameEn,
                        color = SlateGray,
                        fontSize = 11.sp
                    )
                }
            }

            // Time and Log Cycle button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = data.time,
                    color = SandGold,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 16.dp)
                )

                if (data.isSunrise) {
                    // Sunrise cannot be logged as a completed prayer
                    Box(modifier = Modifier.size(36.dp))
                } else {
                    // Interactive Check Log State Cycle Button
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                when (status) {
                                    "prayed" -> IslamicEmerald
                                    "prayed_in_mosque" -> IslamicGold
                                    else -> Color.White.copy(alpha = 0.05f)
                                }
                            )
                            .clickable { onStatusClick() },
                        contentAlignment = Alignment.Center
                    ) {
                        when (status) {
                            "prayed" -> Icon(
                                Icons.Default.Check,
                                contentDescription = "Prayed",
                                tint = EmeraldDeepDark,
                                modifier = Modifier.size(18.dp)
                            )
                            "prayed_in_mosque" -> Icon(
                                Icons.Default.Mosque,
                                contentDescription = "Prayed in Mosque",
                                tint = EmeraldDeepDark,
                                modifier = Modifier.size(18.dp)
                            )
                            else -> Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .border(1.5.dp, SlateGray, CircleShape)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CitySelectionDialog(
    cities: List<PrayerTimesCalculator.CityConfig>,
    currentSelected: PrayerTimesCalculator.CityConfig,
    onCitySelected: (PrayerTimesCalculator.CityConfig) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableStateOf(0) } // 0 = ولايات الجزائر (58), 1 = مدن وعواصم أخرى

    // Separate Algeria list and other capitals
    val algerianWilayas = remember(cities) {
        cities.filter { it.method == PrayerTimesCalculator.CalculationMethod.ALGERIA }
    }
    
    val otherCapitals = remember(cities) {
        cities.filter { it.method != PrayerTimesCalculator.CalculationMethod.ALGERIA }
    }

    val currentList = if (selectedTab == 0) algerianWilayas else otherCapitals

    // Apply Filter/Search
    val filteredList = remember(currentList, searchQuery) {
        if (searchQuery.isBlank()) {
            currentList
        } else {
            currentList.filter {
                it.nameAr.contains(searchQuery, ignoreCase = true) ||
                it.nameEn.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EmeraldContainer,
        title = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    "تحديد الموقع الجغرافي 📍",
                    color = SandText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    "وزارة الشؤون الدينية والأوقاف الجزائرية",
                    color = IslamicGold,
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Right,
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
            ) {
                // Category Tabs Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .background(EmeraldDeepDark, RoundedCornerShape(12.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 0) IslamicEmerald else Color.Transparent)
                            .clickable { selectedTab = 0 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "ولايات الجزائر (58)",
                            color = if (selectedTab == 0) SandText else SlateGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedTab == 1) IslamicEmerald else Color.Transparent)
                            .clickable { selectedTab = 1 }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "مدن وعواصم أخرى",
                            color = if (selectedTab == 1) SandText else SlateGray,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }

                // Beautiful and clean Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text(
                            text = if (selectedTab == 0) "ابحث باسم الولاية أو الرقم..." else "ابحث عن مدينة أخرى...",
                            color = SlateGray.copy(alpha = 0.7f),
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Right
                        )
                    },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        color = SandText,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Right
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = IslamicGold
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SandText,
                        unfocusedTextColor = SandText,
                        focusedBorderColor = IslamicGold,
                        unfocusedBorderColor = IslamicGold.copy(alpha = 0.3f),
                        focusedContainerColor = EmeraldDeepDark,
                        unfocusedContainerColor = EmeraldDeepDark,
                        cursorColor = IslamicGold
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                // List of filtered cities/Provinces
                if (filteredList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "لم يتم العثور على نتائج 🔍",
                                color = SlateGray,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        items(filteredList) { city ->
                            val isFav = city.nameEn == currentSelected.nameEn
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onCitySelected(city) },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isFav) IslamicEmerald.copy(alpha = 0.12f) else Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 1.dp,
                                    color = if (isFav) IslamicGold.copy(alpha = 0.5f) else Color.Transparent
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Right section: Select state name
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.End,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(
                                            text = city.nameAr,
                                            color = if (isFav) IslamicGold else SandText,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            textAlign = TextAlign.Right,
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                    
                                    // Left section: Check or info indicator
                                    Spacer(modifier = Modifier.width(12.dp))
                                    if (isFav) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = IslamicGold,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Text("إغلاق", color = IslamicGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    )
}

// ==========================================
// TAB 2: SILENT SETTINGS & AUTO-MUTE CONTROL
// ==========================================
@Composable
fun SilentSettingsTab(viewModel: PrayerViewModel) {
    val context = LocalContext.current
    val dbSettings by viewModel.dbSettings.collectAsState()
    var isGlobalMuteEnabled by remember { mutableStateOf(true) }
    var slideTestValue by remember { mutableFloatStateOf(15f) }

    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    var hasDndPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.isNotificationPolicyAccessGranted
            } else {
                true
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(20.dp)) }

        // Settings Header Title
        item {
            Text(
                text = "إعدادات صامت وقت الصلاة 🌙",
                color = SandText,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        }

        // "Do Not Disturb" System Permission Check card
        if (!hasDndPermission) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .border(1.5.dp, IslamicGold.copy(alpha = 0.6f), RoundedCornerShape(16.dp)),
                    colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Permission warning",
                            tint = IslamicGold,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "صلاحيات الوصول للهاتف مطلوبة!",
                            color = SandText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "يتطلب كتم رنين الهاتف تلقائياً منح التطبيق صلاحية 'الوصول إلى عدم الإزعاج' وإلا سيقتصر التنبيه صامتاً داخل التطبيق فقط.",
                            color = SlateGray,
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                    val intent = Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                    context.startActivity(intent)
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGold),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("منح الوصول الآن 🔑", color = EmeraldDeepDark, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // Global Auto-Silent Switch Option
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                shape = RoundedCornerShape(14.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Switch(
                        checked = isGlobalMuteEnabled,
                        onCheckedChange = { isGlobalMuteEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = EmeraldDeepDark,
                            checkedTrackColor = IslamicGold,
                            uncheckedThumbColor = SlateGray,
                            uncheckedTrackColor = EmeraldDeepDark
                        )
                    )
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "كتم الهاتف التلقائي أثناء الصلاة",
                            color = SandText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "تبديل الميزة في كافة مواقيت الصلاة",
                            color = SlateGray,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        }

        // Title for Prayer Specific Mute settings
        item {
            Text(
                text = "تخصيص مدة الصمت لكل صلاة",
                color = SandGold,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                textAlign = TextAlign.Right,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp)
            )
        }

        // 5 Prayers Specific sliders from Room DB
        items(dbSettings) { setting ->
            PrayerSilenceConfigItem(
                setting = setting,
                onUpdate = { updatedSetting ->
                    viewModel.updatePrayerSetting(updatedSetting)
                }
            )
        }

        // Interactive "TEST MODE" card
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
                    .border(1.dp, IslamicGold.copy(alpha = 0.3f), RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Test alarm",
                            tint = IslamicGold
                        )
                        Text(
                            text = "اختبار جودة كتم الصوت والإلغاء 🔔",
                            color = SandText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "يمكنك اختبار ميزة كتم الصوت الفورية لعدة ثوانٍ للتأكد من المزامنة وعمل التنبيهات بالشكل السليم فور انتهاء مدة الصلاة.",
                        color = SlateGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right,
                        lineHeight = 16.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "${slideTestValue.toInt()} ثانية",
                            color = IslamicGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Slider(
                            value = slideTestValue,
                            onValueChange = { slideTestValue = it },
                            valueRange = 5f..60f,
                            steps = 11,
                            modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                            colors = SliderDefaults.colors(
                                thumbColor = IslamicGold,
                                activeTrackColor = IslamicGold,
                                inactiveTrackColor = EmeraldDeepDark
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            viewModel.triggerDemoSilentAlarm("التجريبية", slideTestValue.toInt())
                            Toast.makeText(context, "تم تفعيل كتم الصوت التجريبي لمدة ${slideTestValue.toInt()} ثانية!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGold),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "ابدأ تجربة كتم الصوت الآن ✨",
                            color = EmeraldDeepDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
fun PrayerSilenceConfigItem(
    setting: PrayerSetting,
    onUpdate: (PrayerSetting) -> Unit
) {
    val arabicNames = mapOf(
        "fajr" to "الفجر",
        "dhuhr" to "الظهر",
        "asr" to "العصر",
        "maghrib" to "المغرب",
        "isha" to "العشاء"
    )
    val nameAr = arabicNames[setting.prayerNameEn] ?: setting.prayerNameEn

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldContainer.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Settings enable switch
                Switch(
                    checked = setting.isAutoSilent,
                    onCheckedChange = { onUpdate(setting.copy(isAutoSilent = it)) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = EmeraldDeepDark,
                        checkedTrackColor = IslamicGold,
                        uncheckedThumbColor = SlateGray,
                        uncheckedTrackColor = EmeraldDeepDark
                    )
                )

                // Title info
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "صلاة $nameAr",
                        color = SandText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "سيكتم الصوت لمدة ${setting.silenceDurationMinutes} دقائق",
                        color = SlateGray,
                        fontSize = 11.sp
                    )
                }
            }

            if (setting.isAutoSilent) {
                Spacer(modifier = Modifier.height(8.dp))
                // Duration stepper or quick adjust row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                val currentMin = setting.silenceDurationMinutes
                                if (currentMin > 5) {
                                    onUpdate(setting.copy(silenceDurationMinutes = currentMin - 5))
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(EmeraldDeepDark, CircleShape)
                        ) {
                            Text("-", color = IslamicGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "${setting.silenceDurationMinutes} دقيقة",
                            color = SandGold,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        IconButton(
                            onClick = {
                                val currentMin = setting.silenceDurationMinutes
                                if (currentMin < 60) {
                                    onUpdate(setting.copy(silenceDurationMinutes = currentMin + 5))
                                }
                            },
                            modifier = Modifier
                                .size(32.dp)
                                .background(EmeraldDeepDark, CircleShape)
                        ) {
                            Text("+", color = IslamicGold, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                    }

                    // Alert check toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onUpdate(setting.copy(isAlertEnabled = !setting.isAlertEnabled)) }
                    ) {
                        Text("رنين التنبيه بالنهاية", color = SlateGray, fontSize = 11.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Checkbox(
                            checked = setting.isAlertEnabled,
                            onCheckedChange = { onUpdate(setting.copy(isAlertEnabled = it)) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = IslamicGold,
                                uncheckedColor = SlateGray
                            ),
                            modifier = Modifier.scale(0.8f)
                        )
                    }
                }
            }
        }
    }
}

fun Modifier.scale(scale: Float): Modifier = this.then(
    Modifier.rotate(0f) // dummy to chain safely or scaling
)

// ==========================================
// TAB 3: MOSQUE LOCATOR
// ==========================================
@Composable
fun MosquesTab(viewModel: PrayerViewModel) {
    val context = LocalContext.current
    val nearbyMosques by viewModel.nearbyMosques.collectAsState()
    val lat by viewModel.currentLatitude.collectAsState()
    val lng by viewModel.currentLongitude.collectAsState()
    val city by viewModel.selectedCity.collectAsState()

    var hasLocPermission by remember { mutableStateOf(false) }
    var isRefreshing by remember { mutableStateOf(false) }

    // Auto-check location permission on launch
    LaunchedEffect(Unit) {
        val fineGranted = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarseGranted = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (fineGranted || coarseGranted) {
            hasLocPermission = true
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        viewModel.updateCoordinates(loc.latitude, loc.longitude)
                    }
                }
            } catch (e: SecurityException) {
                Log.e("MosqueLocator", "No location permission granted on start: ${e.message}")
            }
        }
    }

    // Android Location Permission seeker
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            hasLocPermission = true
            isRefreshing = true
            try {
                val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
                    if (loc != null) {
                        viewModel.updateCoordinates(loc.latitude, loc.longitude)
                    }
                    isRefreshing = false
                }.addOnFailureListener {
                    isRefreshing = false
                }
            } catch (e: SecurityException) {
                Log.e("MosqueLocator", "No location permission granted: ${e.message}")
                isRefreshing = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(20.dp)) }

        // Sleek visual card header with status and interactive refresh button
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                shape = RoundedCornerShape(16.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGold.copy(alpha = 0.15f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Update Action Button
                    if (isRefreshing) {
                        CircularProgressIndicator(
                            color = IslamicGold,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (hasLocPermission) {
                        IconButton(
                            onClick = {
                                isRefreshing = true
                                try {
                                    val fusedClient = LocationServices.getFusedLocationProviderClient(context)
                                    fusedClient.lastLocation.addOnSuccessListener { loc: Location? ->
                                        if (loc != null) {
                                            viewModel.updateCoordinates(loc.latitude, loc.longitude)
                                        }
                                        isRefreshing = false
                                    }.addOnFailureListener {
                                        isRefreshing = false
                                    }
                                } catch (e: SecurityException) {
                                    isRefreshing = false
                                }
                            },
                            modifier = Modifier
                                .background(IslamicGold.copy(alpha = 0.15f), CircleShape)
                                .size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "تحديث الموقع والبحث عن مساجد قريبة",
                                tint = IslamicGold,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        // Small placeholder to balance spacer
                        Spacer(modifier = Modifier.size(36.dp))
                    }

                    // Status and Info
                    Column(
                        horizontalAlignment = Alignment.End,
                        modifier = Modifier.weight(1f).padding(horizontal = 12.dp)
                    ) {
                        Text(
                            text = "تحديد المساجد القريبة 🕌",
                            color = SandText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (hasLocPermission) "موقعك الجغرافي نشط وحي 🛰️" else "يعرض مساجد مقترحة في ${city.nameAr}",
                            color = if (hasLocPermission) IslamicGold else SlateGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Live Interactive Map display showing user + nearby mosques
        item {
            MosqueMapView(latitude = lat, longitude = lng, mosques = nearbyMosques)
        }

        // GPS Activation Guide banner
        if (!hasLocPermission) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = IslamicEmerald.copy(alpha = 0.15f)),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, IslamicEmerald.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "اعثر على المساجد من حولك تلقائياً!",
                            color = SandText,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "يرجى تفعيل الـ GPS لنتمكن من جلب وعرض أقرب المساجد الحقيقية لإحداثياتك الحالية بدقة وعرض المسافة والاتجاه الفعلي.",
                            color = SlateGray,
                            fontSize = 11.sp,
                            textAlign = TextAlign.Right,
                            lineHeight = 15.sp
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = IslamicGold),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text("تحديد موقعي التلقائي 📍", color = EmeraldDeepDark, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "المسافة والاتجاه",
                    color = SlateGray,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = if (hasLocPermission) "مساجد بجوارك حالياً (بيانات حية)" else "مساجد مقترحة في ${city.nameAr}",
                    color = SandText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        // Dynamic listing of mosques
        if (nearbyMosques.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.CloudQueue, contentDescription = "Empty", tint = SlateGray, modifier = Modifier.size(40.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("لا توجد نتائج مساجد حالياً... جاري البحث والتحميل.", color = SlateGray, fontSize = 12.sp)
                }
            }
        } else {
            items(nearbyMosques) { mosque ->
                MosqueItemCard(mosque = mosque)
            }
        }

        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

val SuiteGoldAlpha = Color(0xFFD4AF37).copy(alpha = 0.15f)

fun Modifier.fillDarkRadarContainer() = this.then(
    Modifier.fillMaxWidth().padding(top = 8.dp)
)

@Composable
fun MosqueItemCard(mosque: LocationHelper.Mosque) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp),
        colors = CardDefaults.cardColors(containerColor = EmeraldContainer.copy(alpha = 0.8f)),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Get navigation action button
            IconButton(
                onClick = {
                    val mapUri = "geo:${mosque.latitude},${mosque.longitude}?q=" + Uri.encode(mosque.nameAr)
                    val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse(mapUri)).apply {
                        setPackage("com.google.android.apps.maps")
                    }
                    try {
                        context.startActivity(mapIntent)
                    } catch (e: Exception) {
                        // Spring fallback standard map web launcher
                        val webMapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/maps/search/?api=1&query=${mosque.latitude},${mosque.longitude}"))
                        context.startActivity(webMapIntent)
                    }
                },
                modifier = Modifier
                    .background(IslamicGold, CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Directions,
                    contentDescription = "Get Directions in Map Launcher Tool",
                    tint = EmeraldDeepDark,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Right: Distance and mosque description texts
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f).padding(end = 12.dp)
            ) {
                Text(
                    text = mosque.nameAr,
                    color = SandText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    text = mosque.addressAr,
                    color = SlateGray,
                    fontSize = 11.sp,
                    textAlign = TextAlign.Right
                )
                
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = mosque.getCompassDirection(),
                        color = IslamicGold,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = "Bearing direction compass item pointer symbol",
                        tint = SlateGray,
                        modifier = Modifier
                            .size(10.dp)
                            .rotate(mosque.bearingDegrees)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "تبعد: ${mosque.getFormattedDistance()}",
                        color = SandGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun MosqueMapView(latitude: Double, longitude: Double, mosques: List<com.example.data.LocationHelper.Mosque>) {
    val htmlContent = remember(latitude, longitude, mosques) {
        val markersJS = mosques.map { mosque ->
            """
            L.marker([${mosque.latitude}, ${mosque.longitude}], {icon: mosqueIcon})
                .addTo(map)
                .bindPopup("<b>${mosque.nameAr.replace("'", "\\'").replace("\"", "\\\"")}</b><br>${mosque.addressAr.replace("'", "\\'").replace("\"", "\\\"")}<br>📌 تبعد ${mosque.getFormattedDistance()}");
            """.trimIndent()
        }.joinToString("\n")

        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="utf-8" />
            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no" />
            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css" />
            <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
            <style>
                html, body, #map {
                    width: 100%;
                    height: 100%;
                    margin: 0;
                    padding: 0;
                    background: #FAF5EA;
                }
                .leaflet-popup-content-wrapper {
                    background: #FFFDFC !important;
                    color: #1B2616 !important;
                    border-radius: 12px !important;
                    border: 1px solid #4A5D3F !important;
                    font-family: sans-serif;
                    text-align: right;
                    direction: rtl;
                }
                .leaflet-popup-tip {
                    background: #FFFDFC !important;
                }
            </style>
        </head>
        <body>
            <div id="map"></div>
            <script>
                var map = L.map('map', {
                    zoomControl: false,
                    attributionControl: false
                }).setView([$latitude, $longitude], 14);

                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                    maxZoom: 19
                }).addTo(map);

                // Custom icon for user
                var userIcon = L.divIcon({
                    className: 'user-marker',
                    html: '<div style="background-color: #007AFF; width: 14px; height: 14px; border-radius: 50%; border: 3px solid white; box-shadow: 0 0 6px rgba(0,0,0,0.4);"></div>',
                    iconSize: [20, 20],
                    iconAnchor: [10, 10]
                });

                // Custom icon for mosques
                var mosqueIcon = L.divIcon({
                    className: 'mosque-marker',
                    html: '<div style="background-color: #4A5D3F; width: 16px; height: 16px; border-radius: 50%; border: 2.5px solid #FAF5EA; box-shadow: 0 0 6px rgba(0,0,0,0.4); display: flex; align-items: center; justify-content: center; font-size: 10px;">🕌</div>',
                    iconSize: [24, 24],
                    iconAnchor: [12, 12]
                });

                L.marker([$latitude, $longitude], {icon: userIcon})
                    .addTo(map)
                    .bindPopup("<b>أنت هنا 🧭</b><br>موقعك الحالي")
                    .openPopup();

                $markersJS
            </script>
        </body>
        </html>
        """.trimIndent()
    }

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)) {
        Text(
            text = "خريطة المساجد الحية المتكاملة 🗺️",
            color = SandText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .border(1.5.dp, IslamicGold.copy(alpha = 0.4f), RoundedCornerShape(16.dp)),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldDeepDark)
        ) {
            androidx.compose.ui.viewinterop.AndroidView(
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        webViewClient = android.webkit.WebViewClient()
                        settings.apply {
                            javaScriptEnabled = true
                            domStorageEnabled = true
                            databaseEnabled = true
                            cacheMode = android.webkit.WebSettings.LOAD_DEFAULT
                            loadWithOverviewMode = true
                            useWideViewPort = true
                        }
                        loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
                    }
                },
                update = { webView ->
                    webView.loadDataWithBaseURL("https://openstreetmap.org", htmlContent, "text/html", "UTF-8", null)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun StartupPermissionRequestDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val notificationManager = remember { context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager }
    
    // Check states dynamically
    var hasLocationPermission by remember {
        mutableStateOf(
            androidx.core.content.ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    
    var hasDndPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                notificationManager.isNotificationPolicyAccessGranted
            } else {
                true
            }
        )
    }

    val powerManager = remember { context.getSystemService(Context.POWER_SERVICE) as android.os.PowerManager }
    var isBatteryOptimizedIgnored by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                powerManager.isIgnoringBatteryOptimizations(context.packageName)
            } else {
                true
            }
        )
    }

    // Dynamic state listener that updates when returning to the app from Android Settings
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                hasLocationPermission = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                hasDndPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    notificationManager.isNotificationPolicyAccessGranted
                } else {
                    true
                }
                
                isBatteryOptimizedIgnored = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    powerManager.isIgnoringBatteryOptimizations(context.packageName)
                } else {
                    true
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Permission launcher for Location
    val locationLauncher = rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[android.Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[android.Manifest.permission.ACCESS_COARSE_LOCATION] ?: false
        if (fineGranted || coarseGranted) {
            hasLocationPermission = true
            android.widget.Toast.makeText(context, "تم تفعيل الوصول إلى الموقع بنجاح 📍", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { onDismiss() }
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(2.dp, IslamicGold, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldContainer)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Mosque icon badge
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(IslamicGold.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Mosque,
                        contentDescription = "التحقق من الأذونات",
                        tint = IslamicGold,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "الأذونات والصلاحيات المطلوبة 📝",
                    color = SandText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "يرجى منح التطبيق الصلاحيات التالية ليعمل بأعلى كفاءة ودقة في الخلفية لتنبيهك وإجراء كتم الصوت التلقائي:",
                    color = SandText.copy(alpha = 0.8f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(20.dp))

                // 1. Location permission row
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasLocationPermission) EmeraldContainer else EmeraldDeepDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "تحديد الموقع الجغرافي 📍",
                                color = SandText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "لحساب مواقيت الصلاة الصحيحة حسب مكانك والبحث عن المساجد القريبة.",
                                color = SandText.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (hasLocationPermission) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Granted",
                                tint = IslamicGold,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Button(
                                onClick = {
                                    locationLauncher.launch(
                                        arrayOf(
                                            android.Manifest.permission.ACCESS_FINE_LOCATION,
                                            android.Manifest.permission.ACCESS_COARSE_LOCATION
                                        )
                                    )
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IslamicGold),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("سماح", color = EmeraldDeepDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 2. DND silent policy permission row
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (hasDndPermission) EmeraldContainer else EmeraldDeepDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "وصول لوضع صامت (DND) 🌙",
                                color = SandText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "لكتم صوت الهاتف تلقائياً أثناء الصلاة وإرجاعه للوضع الطبيعي بعد إنهائها.",
                                color = SandText.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (hasDndPermission) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Granted",
                                tint = IslamicGold,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        try {
                                            val intent = Intent(android.provider.Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                            context.startActivity(intent)
                                            android.widget.Toast.makeText(context, "الرجاء تفعيل وصول التطبيق والعودة 🕌", android.widget.Toast.LENGTH_LONG).show()
                                        } catch (e: Exception) {
                                            android.widget.Toast.makeText(context, "فشل فتح الإعدادات تلقائياً", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IslamicGold),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("تفعيل", color = EmeraldDeepDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 3. Battery Optimization Exempt Row
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isBatteryOptimizedIgnored) EmeraldContainer else EmeraldDeepDark
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "العمل بالخلفية دون قيود ⚡",
                                color = SandText,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "لتجاوز قيود توفير الطاقة والبطارية لضمان دقة كتم الصوت وتشغيل التنبيهات حتى عند إغلاق التطبيق ومسحه من الذاكرة الحية.",
                                color = SandText.copy(alpha = 0.7f),
                                fontSize = 10.sp,
                                lineHeight = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isBatteryOptimizedIgnored) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Granted",
                                tint = IslamicGold,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Button(
                                onClick = {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                        try {
                                            val intent = Intent(android.provider.Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                                data = android.net.Uri.parse("package:${context.packageName}")
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            try {
                                                val intent = Intent(android.provider.Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                                context.startActivity(intent)
                                            } catch (ex: Exception) {
                                                android.widget.Toast.makeText(context, "فشل فتح الإعدادات تلقائياً", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = IslamicGold),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("سماح", color = EmeraldDeepDark, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, IslamicGold.copy(alpha = 0.5f), RoundedCornerShape(12.dp)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "دخول للتطبيق ✨",
                        color = IslamicGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun CustomSettingsDialog(
    viewModel: com.example.data.PrayerViewModel,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val juristicRule by viewModel.juristicRule.collectAsState()
    
    val sharedPrefs = remember { context.getSharedPreferences("silent_pray_prefs", Context.MODE_PRIVATE) }
    
    var isAdhanEnabled by remember {
        mutableStateOf(sharedPrefs.getBoolean("prefs_adhan_enabled", true))
    }
    var isFridayNoMuteEnabled by remember {
        mutableStateOf(sharedPrefs.getBoolean("prefs_friday_no_mute", true))
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(2.dp, IslamicGold, RoundedCornerShape(24.dp)),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = EmeraldContainer)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                item {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(IslamicGold.copy(alpha = 0.15f), androidx.compose.foundation.shape.CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "الإعدادات",
                            tint = IslamicGold,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "خيارات الإعدادات المخصصة ⚙️",
                        color = SandText,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // 1. Juristic Rule Option
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "المذهب الفقهي (العصر)",
                                    color = SandText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = if (juristicRule == com.example.data.PrayerTimesCalculator.JuristicRule.HANAFI) "المذهب الحنفي" else "الجمهور (مالكي، شافعي، حنبلي)",
                                    color = SandText.copy(alpha = 0.7f),
                                    fontSize = 11.sp
                                )
                            }
                            Switch(
                                checked = juristicRule == com.example.data.PrayerTimesCalculator.JuristicRule.HANAFI,
                                onCheckedChange = { isHanafi ->
                                    val newRule = if (isHanafi) {
                                        com.example.data.PrayerTimesCalculator.JuristicRule.HANAFI
                                    } else {
                                        com.example.data.PrayerTimesCalculator.JuristicRule.STANDARD
                                    }
                                    viewModel.updateJuristicRule(newRule)
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = IslamicGold,
                                    checkedTrackColor = EmeraldDeepDark,
                                    uncheckedThumbColor = SlateGray,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                    }
                }

                // 2. Adhan Switch Option
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "تفعيل أذان مواقيت الصلاة 🕌",
                                    color = SandText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "تنبيه صوتي اختياري عند دخول وقت الأذان",
                                    color = SandText.copy(alpha = 0.7f),
                                    fontSize = 10.sp
                                )
                            }
                            Switch(
                                checked = isAdhanEnabled,
                                onCheckedChange = {
                                    isAdhanEnabled = it
                                    sharedPrefs.edit().putBoolean("prefs_adhan_enabled", it).apply()
                                    viewModel.scheduleAllPrayerAlarms() // Refreshes alarms
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = IslamicGold,
                                    checkedTrackColor = EmeraldDeepDark,
                                    uncheckedThumbColor = SlateGray,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                    }
                }

                // 3. Friday No Mute Option
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "منع كتم الصوت صلاة الجمعة 🕋",
                                    color = SandText,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "إيقاف الصامت لتمكين سماع خطبة الجمعة بالتفصيل",
                                    color = SandText.copy(alpha = 0.7f),
                                    fontSize = 10.sp
                                )
                            }
                            Switch(
                                checked = isFridayNoMuteEnabled,
                                onCheckedChange = {
                                    isFridayNoMuteEnabled = it
                                    sharedPrefs.edit().putBoolean("prefs_friday_no_mute", it).apply()
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = IslamicGold,
                                    checkedTrackColor = EmeraldDeepDark,
                                    uncheckedThumbColor = SlateGray,
                                    uncheckedTrackColor = Color.DarkGray
                                )
                            )
                        }
                    }
                }

                // 4. About Us section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                         Divider(color = IslamicGold.copy(alpha = 0.3f), thickness = 1.dp)
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "من نحن ℹ️",
                        color = IslamicGold,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = EmeraldContainer),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, IslamicGold.copy(alpha = 0.2f))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "مجموعة \"المُسلم\" لخدمة الاسلام و المسلمين",
                                color = SandText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(10.dp))
                            
                            Text(
                                text = "يسعدنا ويشرفنا تواصلكم واقتراحاتكم القيمة لتطوير وتجويد خدماتنا الإسلامية الموجهة للمسلمين.",
                                color = SandText.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                lineHeight = 16.sp
                            )
                            
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Email,
                                    contentDescription = "Email",
                                    tint = IslamicGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                androidx.compose.foundation.text.selection.SelectionContainer {
                                    Text(
                                        text = "boubchirabdelilah@gmail.com",
                                        color = SandText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = "Phone",
                                    tint = IslamicGold,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                androidx.compose.foundation.text.selection.SelectionContainer {
                                    Text(
                                        text = "00213673697554",
                                        color = SandText,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }

                // 5. Close button
                item {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = IslamicGold),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "تم وحفظ الإعدادات 👍",
                            color = EmeraldDeepDark,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// HIJRI DATE & BLESSED JUMU'AH SECTION
// ==========================================

fun getArabicFriendlyHijriDate(): String {
    val cal = Calendar.getInstance()
    var year = cal.get(Calendar.YEAR)
    var month = cal.get(Calendar.MONTH) + 1
    val day = cal.get(Calendar.DAY_OF_MONTH)
    
    // Julian Day calculation
    if (month < 3) {
        year -= 1
        month += 12
    }
    val a = (year / 100).toInt()
    val b = (a / 4).toInt()
    val c = 2 - a + b
    val e = (365.25 * (year + 4716)).toInt()
    val f = (30.6001 * (month + 1)).toInt()
    val jd = c + day + e + f - 1524
    
    // Convert Julian Day to Hijri
    val l = jd - 1948440 + 10632
    val n = ((l - 1) / 10631).toInt()
    val l2 = l - 10631 * n + 354
    val j = (((10985 - l2) / 5316).toInt()) * ((50 + l2) / 2307).toInt() + ((l2 / 2137).toInt()) * ((4713 - l2) / 1151).toInt()
    val l3 = l2 - ((30 - j) / 15).toInt() * ((17719 * j) / 50).toInt() - (j / 16).toInt() * ((17719 * j) / 50).toInt() + 29
    val mHijri = ((24 * l3) / 709).toInt()
    val dHijri = l3 - ((709 * mHijri) / 24).toInt()
    val yHijri = 30 * n + j - 30
    
    val hijriMonths = arrayOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الآخر", 
        "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", 
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )
    val monthName = if (mHijri in 1..12) hijriMonths[mHijri - 1] else "ذو الحجة"
    return "$dHijri $monthName $yHijri هـ"
}

fun getArabicGregorianDate(): String {
    val cal = Calendar.getInstance()
    val gregFormatter = SimpleDateFormat("EEEE، d MMMM yyyy", Locale("ar"))
    return gregFormatter.format(cal.time)
}

fun isTodayFriday(): Boolean {
    val cal = Calendar.getInstance()
    return cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
}

@Composable
fun DateAndFridayHeader() {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }
    val isFriday = remember { isTodayFriday() }
    val gregDate = remember { getArabicGregorianDate() }
    val hijriDate = remember { getArabicFriendlyHijriDate() }
    
    var isExpanded by remember { mutableStateOf(false) }
    
    val weekOfYear = remember { calendar.get(Calendar.WEEK_OF_YEAR) }
    val year = remember { calendar.get(Calendar.YEAR) }
    val prefs = remember { context.getSharedPreferences("friday_sunnah_prefs", Context.MODE_PRIVATE) }
    
    val sunnahItems = listOf(
        "الغسل والتطيب والتعطر 🧼",
        "لبس أحسن الثياب والتأنق 👕",
        "السواك وتقليم الأظافر 🦷",
        "التبكير والذهاب مبكراً للمسجد 🕌",
        "قراءة سورة الكهف المباركة 📖",
        "الإكثار من الصلاة على النبي ﷺ 📿",
        "تحري الدعاء وساعة الاستجابة 🤲"
    )
    
    // Check states
    val checkStates = remember(weekOfYear, year) {
        sunnahItems.indices.map { index ->
            mutableStateOf(prefs.getBoolean("sunnah_${index}_${weekOfYear}_${year}", false))
        }
    }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isFriday) {
                EmeraldContainer
            } else {
                EmeraldContainer.copy(alpha = 0.6f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(
            width = if (isFriday) 1.5.dp else 1.dp,
            color = if (isFriday) IslamicGold else IslamicGold.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Golden Crescent / Friday Badge on the left
                if (isFriday) {
                    Box(
                        modifier = Modifier
                            .background(IslamicGold.copy(alpha = 0.15f), CircleShape)
                            .clickable { isExpanded = !isExpanded }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = "عرض سنن الجمعة",
                                tint = IslamicGold,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "أعمال وسنن الجمعة ✨",
                                color = IslamicGold,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    // Small Hijri aesthetic icon
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Hijri Moon Star",
                        tint = IslamicGold.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Date texts aligned to the right
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = hijriDate,
                        color = SandGold,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        textAlign = TextAlign.Right
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = gregDate,
                        color = SlateGray,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Right
                    )
                }
            }

            // Beautiful status for Friday
            if (isFriday) {
                Spacer(modifier = Modifier.height(8.dp))
                androidx.compose.material3.HorizontalDivider(
                    color = IslamicGold.copy(alpha = 0.15f),
                    thickness = 1.dp
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "جمعة مباركة! يوم عظيم يستحب فيه التقرب بالطاعات وذكر الله والسنن النبوية.",
                        color = SandText.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Right,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(
                        imageVector = Icons.Default.Mosque,
                        contentDescription = "جمعة مباركة",
                        tint = IslamicGold,
                        modifier = Modifier.size(16.dp)
                    )
                }

                // Expandable Sunnah list if isFriday is true and clicked
                if (isExpanded) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "جدول سنن وآداب يوم الجمعة 📝",
                        color = IslamicGold,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    
                    sunnahItems.forEachIndexed { index, sunnahTitle ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val newVal = !checkStates[index].value
                                    checkStates[index].value = newVal
                                    prefs.edit().putBoolean("sunnah_${index}_${weekOfYear}_${year}", newVal).apply()
                                }
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = sunnahTitle,
                                color = if (checkStates[index].value) SlateGray else SandText,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Right,
                                style = androidx.compose.ui.text.TextStyle(
                                    textDecoration = if (checkStates[index].value) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            androidx.compose.material3.Checkbox(
                                checked = checkStates[index].value,
                                onCheckedChange = { newVal ->
                                    checkStates[index].value = newVal
                                    prefs.edit().putBoolean("sunnah_${index}_${weekOfYear}_${year}", newVal).apply()
                                },
                                colors = androidx.compose.material3.CheckboxDefaults.colors(
                                    checkedColor = IslamicGold,
                                    uncheckedColor = SlateGray,
                                    checkmarkColor = EmeraldDeepDark
                                ),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                } else {
                    // Small invitation to expand
                    Text(
                        text = "اضغط للمشاهدة والمتابعة لقائمة سنن الجمعة 📥",
                        color = IslamicGold.copy(alpha = 0.8f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { isExpanded = true }
                            .padding(top = 4.dp)
                    )
                }
            }
        }
    }
}
