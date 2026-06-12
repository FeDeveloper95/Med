@file:OptIn(ExperimentalTextApi::class, ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package com.fedeveloper95.med

import android.Manifest
import android.app.AppOpsManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.Watch
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SegmentedListItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.fedeveloper95.med.elements.MainActivity.Tabs.You.AllergiesBottomSheet
import com.fedeveloper95.med.elements.MainActivity.Tabs.You.BloodTypeBottomSheet
import com.fedeveloper95.med.elements.MainActivity.Tabs.You.DateOfBirthBottomSheet
import com.fedeveloper95.med.elements.MainActivity.Tabs.You.HeightBottomSheet
import com.fedeveloper95.med.elements.MainActivity.Tabs.You.ProfileBottomSheet
import com.fedeveloper95.med.elements.MainActivity.Tabs.You.WeightBottomSheet
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import com.fedeveloper95.med.ui.theme.MedTheme
import kotlinx.coroutines.launch

data class OnboardingPageInfo(
    val content: @Composable () -> Unit
)

class WelcomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        val prefs = getSharedPreferences("med_settings", MODE_PRIVATE)
        val isFirstRun = prefs.getBoolean("is_first_run", true)
        val forceShow = intent.getBooleanExtra("FORCE_SHOW", false)

        val currentVersionName = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: "1.0"
        } catch (e: Exception) {
            "1.0"
        }
        val lastWelcomeVersion = prefs.getString("last_welcome_version", null)
        val isUpdated = lastWelcomeVersion != currentVersionName

        if (!isFirstRun && !forceShow && !isUpdated) {
            finishOnboarding()
            return
        }

        enableEdgeToEdge()
        setContent {
            MedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WelcomePagerScreen(onFinished = {
                        prefs.edit().putBoolean("is_first_run", false).apply()
                        prefs.edit().putString("last_welcome_version", currentVersionName).apply()
                        if (forceShow) {
                            finish()
                        } else {
                            finishOnboarding()
                        }
                    })
                }
            }
        }
    }

    private fun finishOnboarding() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun WelcomePagerScreen(onFinished: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val prefs = remember { context.getSharedPreferences("med_settings", Context.MODE_PRIVATE) }
    val commonAnimSpec = tween<Float>(durationMillis = 200, easing = FastOutSlowInEasing)

    val customWelcomeFontFamily = FontFamily(
        Font(
            resId = R.font.sans_flex,
            variationSettings = FontVariation.Settings(
                FontVariation.slant(-9f),
                FontVariation.width(111f),
                FontVariation.weight(333),
                FontVariation.Setting("GRAD", 100f),
                FontVariation.Setting("ROND", 100f)
            )
        )
    )

    val thinHeaderStyle = TextStyle(
        fontFamily = customWelcomeFontFamily,
        fontSize = 48.sp
    )

    var hasNotificationPermission by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            } else true
        )
    }

    var canInstallPackages by remember {
        mutableStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.packageManager.canRequestPackageInstalls()
            } else true
        )
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    hasNotificationPermission = ContextCompat.checkSelfPermission(
                        context,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    canInstallPackages = context.packageManager.canRequestPackageInstalls()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasNotificationPermission = isGranted
            if (isGranted && Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val notificationManager =
                    context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                if (!notificationManager.canUseFullScreenIntent()) {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            }
        }
    )

    val installParamsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            canInstallPackages = context.packageManager.canRequestPackageInstalls()
        }
    }

    var dob by remember { mutableStateOf(prefs.getString("profile_dob", "") ?: "") }
    var height by remember { mutableStateOf(prefs.getString("profile_height", "") ?: "") }
    var weight by remember { mutableStateOf(prefs.getString("profile_weight", "") ?: "") }
    var bloodType by remember { mutableStateOf(prefs.getString("profile_blood_type", "") ?: "") }
    var allergies by remember { mutableStateOf(prefs.getString("profile_allergies", "") ?: "") }
    var imageUriStr by remember { mutableStateOf(prefs.getString("profile_image_uri", "") ?: "") }
    val currentPhotoUri = if (imageUriStr.isNotEmpty()) Uri.parse(imageUriStr) else null

    var showProfileSheet by remember { mutableStateOf(false) }
    var showDobSheet by remember { mutableStateOf(false) }
    var showHeightSheet by remember { mutableStateOf(false) }
    var showWeightSheet by remember { mutableStateOf(false) }
    var showBloodTypeSheet by remember { mutableStateOf(false) }
    var showAllergiesSheet by remember { mutableStateOf(false) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                imageUriStr = uri.toString()
                prefs.edit().putString("profile_image_uri", imageUriStr).apply()
            }
        }
    )

    val pages = listOf(
        OnboardingPageInfo(
            content = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(80.dp))
                    Text(
                        text = stringResource(R.string.welcome_to),
                        style = thinHeaderStyle,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.app_name),
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 56.sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RotatingShapeContainer(
                            modifier = Modifier.size(280.dp)
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        Text(
                            text = stringResource(R.string.welcome_preparing_subtitle),
                            fontFamily = GoogleSansFlex,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.weight(1.2f))
                }
            }
        ),
        OnboardingPageInfo(
            content = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(80.dp))
                    Text(
                        text = stringResource(R.string.perm_required),
                        style = thinHeaderStyle,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.perm_permissions),
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 56.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.perm_intro_text),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = GoogleSansFlex
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                    ) {
                        WelcomeSegmentedItem(
                            icon = Icons.Rounded.Notifications,
                            iconColor = Color(0xFFffaee4),
                            iconTint = Color(0xFF8d0053),
                            title = stringResource(R.string.perm_notif_title),
                            description = stringResource(R.string.perm_notif_desc),
                            index = 0, count = 2,
                            onClick = {
                                if (hasNotificationPermission) {
                                    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                    }
                                    context.startActivity(intent)
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                }
                            },
                            trailingContent = {
                                Switch(
                                    checked = hasNotificationPermission,
                                    onCheckedChange = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        if (hasNotificationPermission) {
                                            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            }
                                            context.startActivity(intent)
                                        } else {
                                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                            }
                                        }
                                    },
                                    thumbContent = {
                                        Icon(
                                            imageVector = if (hasNotificationPermission) Icons.Rounded.Check else Icons.Rounded.Close,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        )

                        WelcomeSegmentedItem(
                            icon = Icons.Rounded.SystemUpdate,
                            iconColor = Color(0xFFffb683),
                            iconTint = Color(0xFF753403),
                            title = stringResource(R.string.perm_install_title),
                            description = stringResource(R.string.perm_install_desc),
                            index = 1, count = 2,
                            onClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    val intent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    installParamsLauncher.launch(intent)
                                }
                            },
                            trailingContent = {
                                Icon(
                                    imageVector = if (canInstallPackages) Icons.Rounded.Check else Icons.Rounded.ChevronRight,
                                    contentDescription = null,
                                    tint = if (canInstallPackages) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        )
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        ),
        OnboardingPageInfo(
            content = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(80.dp))
                    Text(
                        text = stringResource(R.string.personal_data_title),
                        style = thinHeaderStyle,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(R.string.personal_data_header),
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Bold,
                        fontSize = 48.sp,
                        color = MaterialTheme.colorScheme.primary,
                        lineHeight = 56.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.personal_data_intro),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = GoogleSansFlex
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                    ) {
                        WelcomeSegmentedItem(
                            icon = Icons.Rounded.Person,
                            iconColor = MaterialTheme.colorScheme.primaryContainer,
                            iconTint = MaterialTheme.colorScheme.onPrimaryContainer,
                            title = stringResource(R.string.profile_picture),
                            description = if (imageUriStr.isNotEmpty()) stringResource(R.string.image_set) else stringResource(R.string.tap_to_set),
                            index = 0, count = 6,
                            onClick = { showProfileSheet = true },
                            trailingContent = { Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )

                        WelcomeSegmentedItem(
                            icon = Icons.Rounded.CalendarMonth,
                            iconColor = Color(0xFFffb683),
                            iconTint = Color(0xFF753403),
                            title = stringResource(R.string.dob_title),
                            description = dob.ifEmpty { stringResource(R.string.tap_to_set) },
                            index = 1, count = 6,
                            onClick = { showDobSheet = true },
                            trailingContent = { Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )

                        WelcomeSegmentedItem(
                            icon = Icons.Rounded.Height,
                            iconColor = Color(0xFF83C5FF),
                            iconTint = Color(0xFF034B75),
                            title = stringResource(R.string.height_title),
                            description = height.ifEmpty { stringResource(R.string.tap_to_set) },
                            index = 2, count = 6,
                            onClick = { showHeightSheet = true },
                            trailingContent = { Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )

                        WelcomeSegmentedItem(
                            icon = Icons.Rounded.FitnessCenter,
                            iconColor = Color(0xFFA5FF83),
                            iconTint = Color(0xFF1E7503),
                            title = stringResource(R.string.weight_title),
                            description = weight.ifEmpty { stringResource(R.string.tap_to_set) },
                            index = 3, count = 6,
                            onClick = { showWeightSheet = true },
                            trailingContent = { Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )

                        WelcomeSegmentedItem(
                            icon = Icons.Rounded.WaterDrop,
                            iconColor = Color(0xFFffb4ab),
                            iconTint = Color(0xFF690005),
                            title = stringResource(R.string.blood_type_title),
                            description = bloodType.ifEmpty { stringResource(R.string.tap_to_set) },
                            index = 4, count = 6,
                            onClick = { showBloodTypeSheet = true },
                            trailingContent = { Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )

                        WelcomeSegmentedItem(
                            icon = Icons.Rounded.Warning,
                            iconColor = Color(0xFFfcbd00),
                            iconTint = Color(0xFF6d3a01),
                            title = stringResource(R.string.allergies_title),
                            description = allergies.ifEmpty { stringResource(R.string.tap_to_set) },
                            index = 5, count = 6,
                            onClick = { showAllergiesSheet = true },
                            trailingContent = { Icon(Icons.Rounded.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
                        )

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        ),
        OnboardingPageInfo(
            content = {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.Start
                ) {
                    Spacer(modifier = Modifier.height(80.dp))
                    Column(modifier = Modifier.padding(bottom = 16.dp)) {
                        Text(
                            text = stringResource(R.string.feat_discover),
                            style = thinHeaderStyle,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = stringResource(R.string.feat_features),
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Bold,
                            fontSize = 48.sp,
                            color = MaterialTheme.colorScheme.primary,
                            lineHeight = 56.sp
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = stringResource(R.string.feat_intro),
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = GoogleSansFlex
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Box(modifier = Modifier.weight(1f)) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(ListItemDefaults.SegmentedGap)
                        ) {
                            WelcomeSegmentedItem(
                                icon = ImageVector.vectorResource(R.drawable.ic_quick_actions),
                                iconColor = Color(0xFFffaee4),
                                iconTint = Color(0xFF8d0053),
                                title = stringResource(R.string.feat_presets_title),
                                description = stringResource(R.string.feat_presets_desc),
                                index = 0, count = 6
                            )

                            WelcomeSegmentedItem(
                                icon = Icons.Rounded.Alarm,
                                iconColor = Color(0xFF80da88),
                                iconTint = Color(0xFF00522c),
                                title = stringResource(R.string.feat_reminders_title),
                                description = stringResource(R.string.feat_reminders_desc),
                                index = 1, count = 6
                            )

                            WelcomeSegmentedItem(
                                icon = Icons.Rounded.CheckCircle,
                                iconColor = Color(0xFFffb683),
                                iconTint = Color(0xFF753403),
                                title = stringResource(R.string.feat_tracking_title),
                                description = stringResource(R.string.feat_tracking_desc),
                                index = 2, count = 6
                            )

                            WelcomeSegmentedItem(
                                icon = Icons.Rounded.Watch,
                                iconColor = Color(0xFF67D4FF),
                                iconTint = Color(0xFF004E5D),
                                title = stringResource(R.string.feat_wearos_title),
                                description = stringResource(R.string.feat_wearos_desc),
                                index = 3, count = 6
                            )

                            WelcomeSegmentedItem(
                                icon = Icons.Rounded.BarChart,
                                iconColor = Color(0xFFfcbd00),
                                iconTint = Color(0xFF6d3a01),
                                title = stringResource(R.string.feat_tabs_title),
                                description = stringResource(R.string.feat_tabs_desc),
                                index = 4, count = 6
                            )

                            WelcomeSegmentedItem(
                                icon = Icons.Rounded.SystemUpdate,
                                iconColor = Color(0xFFC7C7C7),
                                iconTint = Color(0xFF2C2C2C),
                                title = stringResource(R.string.feat_update_title),
                                description = stringResource(R.string.feat_update_desc),
                                index = 5, count = 6
                            )

                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val isFirstPage = pagerState.currentPage == 0
    val isLastPage = pagerState.currentPage == pages.size - 1

    BackHandler(enabled = !isFirstPage) {
        scope.launch {
            pagerState.animateScrollToPage(
                pagerState.currentPage - 1,
                animationSpec = commonAnimSpec
            )
        }
    }

    val backInteractionSource = remember { MutableInteractionSource() }
    val nextInteractionSource = remember { MutableInteractionSource() }

    val isBackPressed by backInteractionSource.collectIsPressedAsState()
    val isNextPressed by nextInteractionSource.collectIsPressedAsState()

    val targetBackWeight = when {
        isFirstPage -> 0.0001f
        isBackPressed -> 1.125f
        isNextPressed -> 0.875f
        else -> 1f
    }

    val targetNextWeight = when {
        isNextPressed -> 1.125f
        isBackPressed -> 0.875f
        else -> 1f
    }

    val backButtonWeight by animateFloatAsState(
        targetValue = targetBackWeight,
        animationSpec = commonAnimSpec,
        label = "backWeight"
    )

    val nextButtonWeight by animateFloatAsState(
        targetValue = targetNextWeight,
        animationSpec = commonAnimSpec,
        label = "nextWeight"
    )

    val spacerWeight by animateFloatAsState(
        targetValue = if (isFirstPage) 0.0001f else 0.05f,
        animationSpec = commonAnimSpec,
        label = "spacerWeight"
    )

    val alphaBack by animateFloatAsState(
        targetValue = if (isFirstPage) 0f else 1f,
        animationSpec = commonAnimSpec,
        label = "backAlpha"
    )

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 700.dp)
                .fillMaxSize()
                .padding(24.dp)
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = false,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { index ->
                OnboardingPageItem(page = pages[index])
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
                    .height(64.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(backButtonWeight)
                        .fillMaxHeight()
                        .alpha(alphaBack)
                ) {
                    WelcomeExpressiveButton(
                        text = stringResource(R.string.back),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (!isFirstPage) {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        pagerState.currentPage - 1,
                                        animationSpec = commonAnimSpec
                                    )
                                }
                            }
                        },
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxSize(),
                        isOutlined = true,
                        interactionSource = backInteractionSource
                    )
                }

                Spacer(modifier = Modifier.weight(spacerWeight))

                Box(
                    modifier = Modifier
                        .weight(nextButtonWeight)
                        .fillMaxHeight()
                ) {
                    WelcomeExpressiveButton(
                        text = if (isLastPage) stringResource(R.string.get_started) else stringResource(R.string.next),
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (isLastPage) {
                                onFinished()
                            } else {
                                scope.launch {
                                    pagerState.animateScrollToPage(
                                        pagerState.currentPage + 1,
                                        animationSpec = commonAnimSpec
                                    )
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.fillMaxSize(),
                        interactionSource = nextInteractionSource
                    )
                }
            }
        }
    }

    if (showProfileSheet) {
        ProfileBottomSheet(
            onDismiss = { showProfileSheet = false },
            onRemove = {
                imageUriStr = ""
                prefs.edit().putString("profile_image_uri", "").apply()
            },
            onChange = {
                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            currentPhotoUri = currentPhotoUri
        )
    }

    if (showDobSheet) {
        DateOfBirthBottomSheet(
            onDismiss = { showDobSheet = false },
            onSave = { newValue ->
                dob = newValue
                prefs.edit().putString("profile_dob", newValue).apply()
            },
            currentValue = dob
        )
    }

    if (showHeightSheet) {
        HeightBottomSheet(
            onDismiss = { showHeightSheet = false },
            onSave = { newValue ->
                height = newValue
                prefs.edit().putString("profile_height", newValue).apply()
            },
            currentValue = height
        )
    }

    if (showWeightSheet) {
        WeightBottomSheet(
            onDismiss = { showWeightSheet = false },
            onSave = { newValue ->
                weight = newValue
                prefs.edit().putString("profile_weight", newValue).apply()
            },
            currentValue = weight
        )
    }

    if (showBloodTypeSheet) {
        BloodTypeBottomSheet(
            onDismiss = { showBloodTypeSheet = false },
            onSave = { newValue ->
                bloodType = newValue
                prefs.edit().putString("profile_blood_type", newValue).apply()
            },
            currentValue = bloodType
        )
    }

    if (showAllergiesSheet) {
        AllergiesBottomSheet(
            onDismiss = { showAllergiesSheet = false },
            onSave = { newValue ->
                allergies = newValue
                prefs.edit().putString("profile_allergies", newValue).apply()
            },
            currentValue = allergies
        )
    }
}

@Composable
fun WelcomeSegmentedItem(
    icon: ImageVector,
    iconColor: Color,
    iconTint: Color,
    title: String,
    description: String,
    index: Int,
    count: Int,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable (() -> Unit)? = null
) {
    val haptic = LocalHapticFeedback.current
    SegmentedListItem(
        selected = false,
        onClick = {
            if (onClick != null) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
        },
        colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        shapes = ListItemDefaults.segmentedShapes(index = index, count = count),
        content = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(iconColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.titleMedium
                    )
                    if (description.isNotEmpty()) {
                        Text(
                            text = description,
                            fontFamily = GoogleSansFlex,
                            fontWeight = FontWeight.Normal,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (trailingContent != null) {
                    Spacer(modifier = Modifier.width(16.dp))
                    trailingContent()
                }
            }
        }
    )
}

@Composable
fun RotatingShapeContainer(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "shapeRotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val backgroundColor = MaterialTheme.colorScheme.background

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_ten_sided_cookie),
            contentDescription = null,
            tint = primaryColor,
            modifier = Modifier
                .fillMaxSize()
                .rotate(rotation)
        )
        Icon(
            painter = painterResource(R.drawable.ic_launcher_monochrome),
            contentDescription = null,
            modifier = Modifier.size(250.dp),
            tint = backgroundColor
        )
    }
}

@Composable
fun OnboardingPageItem(page: OnboardingPageInfo) {
    page.content()
}

@Composable
private fun WelcomeExpressiveButton(
    text: String,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    isOutlined: Boolean = false,
    interactionSource: MutableInteractionSource
) {
    val isPressed by interactionSource.collectIsPressedAsState()
    val cornerPercent by animateIntAsState(
        targetValue = if (isPressed) 15 else 50,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "btnMorph"
    )

    if (isOutlined) {
        OutlinedButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(cornerPercent),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = contentColor
            ),
            border = BorderStroke(1.dp, contentColor.copy(alpha = 0.5f)),
            interactionSource = interactionSource,
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
            }
        }
    } else {
        Button(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(cornerPercent),
            colors = ButtonDefaults.buttonColors(
                containerColor = containerColor,
                contentColor = contentColor
            ),
            interactionSource = interactionSource,
            contentPadding = PaddingValues(0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = text,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1
                )
            }
        }
    }
}

fun checkUsageStatsPermission(context: Context): Boolean {
    val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
    val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    } else {
        appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
    }
    return mode == AppOpsManager.MODE_ALLOWED
}