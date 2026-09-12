package com.example

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.VectorConverter
import androidx.compose.ui.unit.Dp
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import android.animation.ValueAnimator
import android.view.WindowManager
import android.view.animation.OvershootInterpolator
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun CapsuleUI() {
    val context = LocalContext.current
    val state by CapsuleStateManager.currentState.collectAsState()
    val mediaInfo by CapsuleStateManager.mediaInfo.collectAsState()
    val notificationInfo by CapsuleStateManager.notificationInfo.collectAsState()
    val batteryInfo by CapsuleStateManager.batteryInfo.collectAsState()
    val isSplitEnabled by CapsulePreferencesRepository.isSplitIslandEnabled(context).collectAsState(initial = true)

    val xOffset by CapsuleStateManager.capsuleXOffset.collectAsState()
    val yOffset by CapsuleStateManager.capsuleYOffset.collectAsState()

    val showSplitPill = mediaInfo.isPlaying && (state == CapsuleState.CHARGING_EVENT || state == CapsuleState.NOTIFICATION_POPUP) && isSplitEnabled

    Box(
        contentAlignment = Alignment.TopCenter
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Top
        ) {
            MainIsland(
                state = state, 
                mediaInfo = mediaInfo, 
                notificationInfo = notificationInfo, 
                batteryInfo = batteryInfo
            )

            AnimatedVisibility(visible = showSplitPill) {
                SecondaryIsland(mediaInfo = mediaInfo)
            }
        }
    }
}

@Composable
fun MainIsland(
    state: CapsuleState,
    mediaInfo: MediaInfo,
    notificationInfo: NotificationInfo?,
    batteryInfo: BatteryInfo
) {
    val context = LocalContext.current
    val baseWidth by CapsuleStateManager.baseWidth.collectAsState()
    val baseHeight by CapsuleStateManager.baseHeight.collectAsState()

    val idleWidth = baseWidth.dp
    val idleHeight = baseHeight.dp

    val targetWidth = when (state) {
        CapsuleState.IDLE -> idleWidth
        CapsuleState.MEDIA_PLAYING -> idleWidth + 80.dp
        CapsuleState.NOTIFICATION_POPUP -> idleWidth + 240.dp
        CapsuleState.CHARGING_EVENT -> idleWidth + 120.dp
        CapsuleState.EXPANDED_MEDIA -> idleWidth + 240.dp
        CapsuleState.EXPANDED_NOTIFICATION -> idleWidth + 240.dp
    }

    val targetHeight = when (state) {
        CapsuleState.IDLE -> idleHeight
        CapsuleState.MEDIA_PLAYING -> idleHeight
        CapsuleState.NOTIFICATION_POPUP -> idleHeight + 50.dp
        CapsuleState.CHARGING_EVENT -> idleHeight + 10.dp
        CapsuleState.EXPANDED_MEDIA -> idleHeight + 130.dp
        CapsuleState.EXPANDED_NOTIFICATION -> idleHeight + 80.dp
    }

    val density = LocalDensity.current
    val view = LocalView.current
    
    val idleCornerRadius by CapsuleStateManager.capsuleCornerRadius.collectAsState()

    val cornerRadius = when (state) {
        CapsuleState.EXPANDED_MEDIA -> 40.dp
        CapsuleState.EXPANDED_NOTIFICATION -> 40.dp
        CapsuleState.NOTIFICATION_POPUP -> 40.dp
        else -> idleCornerRadius.dp
    }

    val animatedCorner = remember { Animatable(cornerRadius, Dp.VectorConverter) }

    DisposableEffect(targetWidth, targetHeight) {
        val windowManager = context.getSystemService(android.content.Context.WINDOW_SERVICE) as WindowManager
        val params = view.layoutParams as? WindowManager.LayoutParams
        if (params != null) {
            val pxTargetWidth = with(density) { targetWidth.toPx() }.toInt()
            val pxTargetHeight = with(density) { targetHeight.toPx() }.toInt()
            
            val startWidth = if (params.width > 0) params.width else pxTargetWidth
            val startHeight = if (params.height > 0) params.height else pxTargetHeight

            val animator = ValueAnimator.ofFloat(0f, 1f).apply {
                duration = 400
                interpolator = OvershootInterpolator(0.8f)
                addUpdateListener { anim ->
                    val fraction = anim.animatedFraction
                    params.width = (startWidth + (pxTargetWidth - startWidth) * fraction).toInt()
                    params.height = (startHeight + (pxTargetHeight - startHeight) * fraction).toInt()
                    windowManager.updateViewLayout(view, params)
                }
                start()
            }

            onDispose {
                animator.cancel()
            }
        } else {
            onDispose { }
        }
    }

    LaunchedEffect(cornerRadius) {
        animatedCorner.animateTo(
            targetValue = cornerRadius,
            animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f)
        )
    }

    val dominantColor = mediaInfo.dominantColor?.let { Color(it) } ?: Color.DarkGray
    val outlineColor by animateColorAsState(
        targetValue = when (state) {
            CapsuleState.CHARGING_EVENT -> Color.Green.copy(alpha = 0.5f)
            CapsuleState.EXPANDED_MEDIA -> dominantColor.copy(alpha = 0.5f)
            CapsuleState.EXPANDED_NOTIFICATION -> Color.White.copy(alpha = 0.2f)
            CapsuleState.NOTIFICATION_POPUP -> Color.White.copy(alpha = 0.2f)
            else -> Color.Transparent
        },
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    val interactionSource = remember { MutableInteractionSource() }
    val bgColor by animateColorAsState(
        targetValue = Color.Black.copy(alpha = 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .shadow(
                elevation = if (state == CapsuleState.IDLE) 0.dp else 24.dp,
                shape = RoundedCornerShape(animatedCorner.value),
                ambientColor = outlineColor,
                spotColor = outlineColor
            )
            .background(bgColor, RoundedCornerShape(animatedCorner.value))
            .clip(RoundedCornerShape(animatedCorner.value))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    when (state) {
                        CapsuleState.IDLE, CapsuleState.MEDIA_PLAYING, CapsuleState.NOTIFICATION_POPUP, CapsuleState.CHARGING_EVENT -> {
                            CapsuleStateManager.expand()
                        }
                        CapsuleState.EXPANDED_MEDIA -> {
                            mediaInfo.packageName?.let { pkg ->
                                try {
                                    val intent = context.packageManager.getLaunchIntentForPackage(pkg)
                                    intent?.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
                                    if (intent != null) context.startActivity(intent)
                                } catch (e: Exception) { e.printStackTrace() }
                            }
                            CapsuleStateManager.setState(CapsuleState.IDLE)
                        }
                        CapsuleState.EXPANDED_NOTIFICATION -> {
                            try {
                                notificationInfo?.contentIntent?.send()
                            } catch (e: Exception) { e.printStackTrace() }
                            CapsuleStateManager.setState(CapsuleState.IDLE)
                        }
                        else -> {}
                    }
                }
            )
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount ->
                    if (dragAmount.y < -20) {
                        CapsuleStateManager.setState(CapsuleState.IDLE)
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (state != CapsuleState.IDLE) {
            when (state) {
                CapsuleState.CHARGING_EVENT -> ChargingAnimation(batteryInfo)
                CapsuleState.MEDIA_PLAYING -> MediaPlayingMini(mediaInfo)
                CapsuleState.NOTIFICATION_POPUP -> notificationInfo?.let { NotificationAlert(it) }
                CapsuleState.EXPANDED_MEDIA -> MediaExpandedCard(mediaInfo)
                CapsuleState.EXPANDED_NOTIFICATION -> notificationInfo?.let { NotificationExpandedCard(it) }
                else -> {}
            }
        }
    }
}

@Composable
fun SecondaryIsland(mediaInfo: MediaInfo) {
    val size = 30.dp
    Box(
        modifier = Modifier
            .size(size)
            .background(Color.Black, CircleShape)
            .clip(CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (mediaInfo.albumArt != null) {
            Image(
                bitmap = mediaInfo.albumArt.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            EqualizerWave(isPlaying = mediaInfo.isPlaying, color = mediaInfo.dominantColor?.let { Color(it) } ?: Color.White)
        }
    }
}

@Composable
fun ChargingAnimation(batteryInfo: BatteryInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.BatteryChargingFull,
                contentDescription = "Charging",
                tint = Color.Green,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${batteryInfo.percentage}%",
                color = Color.Green,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Icon(
            imageVector = Icons.Filled.Bolt,
            contentDescription = "Bolt",
            tint = Color.Green,
            modifier = Modifier.size(24.dp)
        )
    }
}

@Composable
fun MediaPlayingMini(mediaInfo: MediaInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            mediaInfo.albumArt?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(8.dp))
            }
        }
        EqualizerWave(isPlaying = mediaInfo.isPlaying, color = mediaInfo.dominantColor?.let { Color(it) } ?: Color.White)
    }
}

@Composable
fun NotificationAlert(info: NotificationInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val icon = info.largeIcon ?: info.appIcon
        if (icon != null) {
            Image(
                bitmap = icon.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column {
            Text(
                text = info.title,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = info.content,
                color = Color.LightGray,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun NotificationExpandedCard(info: NotificationInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val icon = info.largeIcon ?: info.appIcon
            if (icon != null) {
                Image(
                    bitmap = icon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = info.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = info.content,
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MediaExpandedCard(mediaInfo: MediaInfo) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(24.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            mediaInfo.albumArt?.let {
                Image(
                    bitmap = it.asImageBitmap(),
                    contentDescription = "Album Art",
                    modifier = Modifier
                        .size(64.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = mediaInfo.title,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = mediaInfo.artist,
                    color = Color.LightGray,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            EqualizerWave(isPlaying = mediaInfo.isPlaying, color = mediaInfo.dominantColor?.let { Color(it) } ?: Color.White)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { MediaControllerManager.previous() }) {
                Icon(Icons.Filled.FastRewind, contentDescription = "Previous", tint = Color.White)
            }
            IconButton(
                onClick = { MediaControllerManager.playPause() },
                modifier = Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.2f), CircleShape)
            ) {
                Icon(
                    if (mediaInfo.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
            IconButton(onClick = { MediaControllerManager.next() }) {
                Icon(Icons.Filled.FastForward, contentDescription = "Next", tint = Color.White)
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Slider(
            value = if (mediaInfo.duration > 0) mediaInfo.currentPosition.toFloat() / mediaInfo.duration else 0f,
            onValueChange = { progress -> 
                MediaControllerManager.seekTo((progress * mediaInfo.duration).toLong())
            },
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = mediaInfo.dominantColor?.let { Color(it) } ?: Color.White,
                inactiveTrackColor = Color.DarkGray
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun EqualizerWave(isPlaying: Boolean, color: Color) {
    val barCount = 4
    val heights = remember { mutableStateListOf(0.3f, 0.6f, 0.4f, 0.8f) }
    
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            for (i in 0 until barCount) {
                heights[i] = (3..10).random() / 10f
            }
            delay(150)
        }
        if (!isPlaying) {
            for (i in 0 until barCount) {
                heights[i] = 0.2f
            }
        }
    }

    Row(
        modifier = Modifier.height(16.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 0 until barCount) {
            val h by animateFloatAsState(targetValue = heights[i], animationSpec = spring(stiffness = Spring.StiffnessLow))
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .fillMaxHeight(h)
                    .background(color, CircleShape)
            )
        }
    }
}
