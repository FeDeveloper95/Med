@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class, androidx.compose.ui.text.ExperimentalTextApi::class)
package com.fedeveloper95.med.elements.MainActivity.Tabs

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.Height
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.fedeveloper95.med.R
import com.fedeveloper95.med.elements.MainActivity.Tabs.You.AllergiesBottomSheet
import com.fedeveloper95.med.elements.MainActivity.Tabs.You.BloodTypeBottomSheet
import com.fedeveloper95.med.elements.MainActivity.Tabs.You.DateOfBirthBottomSheet
import com.fedeveloper95.med.elements.MainActivity.Tabs.You.HeightBottomSheet
import com.fedeveloper95.med.elements.MainActivity.Tabs.You.ProfileBottomSheet
import com.fedeveloper95.med.elements.MainActivity.Tabs.You.WeightBottomSheet
import com.fedeveloper95.med.ui.theme.GoogleSansFlex
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class CookieShape(val rotationDegrees: Float = 0f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cx = size.width / 2f
        val cy = size.height / 2f
        val r = java.lang.Math.min(cx, cy)
        val rotationRads = java.lang.Math.toRadians(rotationDegrees.toDouble())

        val points = 120
        for (i in 0..points) {
            val baseAngle = (i * 2 * java.lang.Math.PI / points) - (java.lang.Math.PI / 2)
            val bump = 0.06
            val currentR = r * (1 - bump * java.lang.Math.pow(java.lang.Math.sin(6 * baseAngle), 2.0))
            val angle = baseAngle + rotationRads
            val px = cx + currentR * java.lang.Math.cos(angle)
            val py = cy + currentR * java.lang.Math.sin(angle)

            if (i == 0) path.moveTo(px.toFloat(), py.toFloat())
            else path.lineTo(px.toFloat(), py.toFloat())
        }
        path.close()
        return Outline.Generic(path)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YouTab() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("med_settings", Context.MODE_PRIVATE) }
    var profilePhotoUri by remember {
        mutableStateOf(prefs.getString("profile_photo_uri", null)?.let { Uri.parse(it) })
    }

    var dob by remember { mutableStateOf(prefs.getString("profile_dob", "") ?: "") }
    var bloodType by remember { mutableStateOf(prefs.getString("profile_blood_type", "") ?: "") }
    var height by remember { mutableStateOf(prefs.getString("profile_height", "") ?: "") }
    var weight by remember { mutableStateOf(prefs.getString("profile_weight", "") ?: "") }
    var allergies by remember { mutableStateOf(prefs.getString("profile_allergies", "") ?: "") }

    var showProfileSheet by remember { mutableStateOf(false) }
    var showDobSheet by remember { mutableStateOf(false) }
    var showBloodTypeSheet by remember { mutableStateOf(false) }
    var showHeightSheet by remember { mutableStateOf(false) }
    var showWeightSheet by remember { mutableStateOf(false) }
    var showAllergiesSheet by remember { mutableStateOf(false) }

    val rotationAnim = remember { Animatable(0f) }

    val configuration = LocalConfiguration.current
    val isExpanded = configuration.screenWidthDp > 600 || configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    LaunchedEffect(Unit) {
        rotationAnim.animateTo(
            targetValue = 360f,
            animationSpec = tween(durationMillis = 1500, easing = FastOutSlowInEasing)
        )
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            if (uri != null) {
                try {
                    context.contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) { }
                prefs.edit().putString("profile_photo_uri", uri.toString()).apply()
                profilePhotoUri = uri
            }
        }
    )

    if (showProfileSheet) {
        ProfileBottomSheet(
            onDismiss = { showProfileSheet = false },
            onRemove = {
                prefs.edit().remove("profile_photo_uri").apply()
                profilePhotoUri = null
            },
            onChange = {
                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            currentPhotoUri = profilePhotoUri
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

    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    val onRefresh: () -> Unit = {
        isRefreshing = true
        scope.launch {
            delay(1000)
            isRefreshing = false
        }
    }

    val profilePictureContent = @Composable {
        Box(
            modifier = Modifier
                .size(if (isExpanded) 200.dp else 120.dp)
                .clip(CookieShape(rotationAnim.value))
                .background(MaterialTheme.colorScheme.primaryContainer)
                .clickable {
                    if (profilePhotoUri == null) {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    } else {
                        showProfileSheet = true
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (profilePhotoUri != null) {
                AsyncImage(
                    model = profilePhotoUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(if (isExpanded) 100.dp else 64.dp)
                )
            }
        }
    }

    val cardsContent = @Composable {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(R.string.medical_info_header),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, bottom = 8.dp)
            )
            ProfileInfoCard(
                icon = Icons.Rounded.CalendarMonth,
                title = stringResource(R.string.dob_title),
                subtitle = dob.ifEmpty { stringResource(R.string.not_set) },
                containerColor = Color(0xFFffb683),
                iconColor = Color(0xFF753403),
                shape = RoundedCornerShape(
                    topStart = 20.dp,
                    topEnd = 20.dp,
                    bottomStart = 4.dp,
                    bottomEnd = 4.dp
                ),
                onClick = { showDobSheet = true }
            )
            Spacer(modifier = Modifier.height(2.dp))
            ProfileInfoCard(
                icon = Icons.Rounded.WaterDrop,
                title = stringResource(R.string.blood_type_title),
                subtitle = bloodType.ifEmpty { stringResource(R.string.not_set) },
                containerColor = Color(0xFFffb4ab),
                iconColor = Color(0xFF690005),
                shape = RoundedCornerShape(4.dp),
                onClick = { showBloodTypeSheet = true }
            )
            Spacer(modifier = Modifier.height(2.dp))
            ProfileInfoCard(
                icon = Icons.Rounded.Height,
                title = stringResource(R.string.height_title),
                subtitle = height.ifEmpty { stringResource(R.string.not_set) },
                containerColor = Color(0xFFA1C9FF),
                iconColor = Color(0xFF04409F),
                shape = RoundedCornerShape(4.dp),
                onClick = { showHeightSheet = true }
            )
            Spacer(modifier = Modifier.height(2.dp))
            ProfileInfoCard(
                icon = Icons.Rounded.FitnessCenter,
                title = stringResource(R.string.weight_title),
                subtitle = weight.ifEmpty { stringResource(R.string.not_set) },
                containerColor = Color(0xFF80da88),
                iconColor = Color(0xFF00522c),
                shape = RoundedCornerShape(4.dp),
                onClick = { showWeightSheet = true }
            )
            Spacer(modifier = Modifier.height(2.dp))
            ProfileInfoCard(
                icon = Icons.Rounded.Warning,
                title = stringResource(R.string.allergies_title),
                subtitle = allergies.ifEmpty { stringResource(R.string.not_set) },
                containerColor = Color(0xFFfcbd00),
                iconColor = Color(0xFF6d3a01),
                shape = RoundedCornerShape(
                    topStart = 4.dp,
                    topEnd = 4.dp,
                    bottomStart = 20.dp,
                    bottomEnd = 20.dp
                ),
                onClick = { showAllergiesSheet = true }
            )
        }
    }

    val privacyContent = @Composable {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.privacy_info),
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = GoogleSansFlex
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    PullToRefreshBox(
        state = pullRefreshState,
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize(),
        indicator = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                PullToRefreshDefaults.LoadingIndicator(
                    state = pullRefreshState,
                    isRefreshing = isRefreshing
                )
            }
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item {
                    if (isExpanded) {
                        Spacer(modifier = Modifier.height(32.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .widthIn(max = 1000.dp),
                            horizontalArrangement = Arrangement.spacedBy(32.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                profilePictureContent()
                            }
                            Column(
                                modifier = Modifier.weight(1.5f)
                            ) {
                                cardsContent()
                                Spacer(modifier = Modifier.height(32.dp))
                                privacyContent()
                                Spacer(modifier = Modifier.height(100.dp))
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier.widthIn(max = 700.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Spacer(modifier = Modifier.height(32.dp))
                            profilePictureContent()
                            Spacer(modifier = Modifier.height(32.dp))
                            cardsContent()
                            Spacer(modifier = Modifier.height(32.dp))
                            privacyContent()
                            Spacer(modifier = Modifier.height(100.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileInfoCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    containerColor: Color,
    iconColor: Color,
    shape: Shape,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressProgress by animateFloatAsState(
        targetValue = if (isPressed) 1f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "anim_shape"
    )

    val animatedShape = remember(shape, pressProgress) {
        if (shape is RoundedCornerShape) {
            object : Shape {
                override fun createOutline(
                    size: Size,
                    layoutDirection: LayoutDirection,
                    density: Density
                ): Outline {
                    val targetPx = with(density) { 20.dp.toPx() }
                    fun lerp(start: Float, stop: Float, fraction: Float) =
                        (1 - fraction) * start + fraction * stop

                    val ts = lerp(shape.topStart.toPx(size, density), targetPx, pressProgress)
                    val te = lerp(shape.topEnd.toPx(size, density), targetPx, pressProgress)
                    val bs = lerp(shape.bottomStart.toPx(size, density), targetPx, pressProgress)
                    val be = lerp(shape.bottomEnd.toPx(size, density), targetPx, pressProgress)

                    return Outline.Rounded(
                        androidx.compose.ui.geometry.RoundRect(
                            rect = androidx.compose.ui.geometry.Rect(
                                0f,
                                0f,
                                size.width,
                                size.height
                            ),
                            topLeft = androidx.compose.ui.geometry.CornerRadius(ts),
                            topRight = androidx.compose.ui.geometry.CornerRadius(te),
                            bottomRight = androidx.compose.ui.geometry.CornerRadius(be),
                            bottomLeft = androidx.compose.ui.geometry.CornerRadius(bs)
                        )
                    )
                }
            }
        } else shape
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(animatedShape),
        shape = animatedShape,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        ListItem(
            headlineContent = {
                Text(
                    text = title,
                    fontFamily = GoogleSansFlex,
                    fontWeight = FontWeight.Normal,
                    style = MaterialTheme.typography.titleMedium
                )
            },
            supportingContent = {
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        fontFamily = GoogleSansFlex,
                        fontWeight = FontWeight.Normal,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            leadingContent = {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            modifier = Modifier
                .clickable(
                    interactionSource = interactionSource,
                    indication = LocalIndication.current,
                    onClick = onClick
                )
                .padding(vertical = 4.dp),
            colors = ListItemDefaults.colors(
                containerColor = Color.Transparent
            )
        )
    }
}