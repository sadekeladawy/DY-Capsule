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
import android.view.WindowManager
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
    val baseWidth by CapsuleStateManager.baseWidth.collectAsState()
    val baseHeight by CapsuleStateManager.baseHeight.collectAsState()

    val idleWidth = baseWidth.dp.coerceAtLeast(24.dp)
    val idleHeight = baseHeight.dp.coerceAtLeast(24.dp)
    
    val idleCornerRadius by CapsuleStateManager.capsuleCornerRadius.collectAsState()
    val cornerRadius = when (state) {
        CapsuleState.EXPANDED_MEDIA -> 40.dp
        CapsuleState.EXPANDED_NOTIFICATION -> 40.dp
        CapsuleState.NOTIFICATION_POPUP -> 40.dp
        else -> idleCornerRadius.dp
    }
    
    val animatedCorner = remember { Animatable(cornerRadius, Dp.VectorConverter) }
    
    LaunchedEffect(cornerRadius) {
        animatedCorner.animateTo(cornerRadius, animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f))
    }

    val bgColor by animateColorAsState(if (state == CapsuleState.CHARGING_EVENT) Color(0xFF111111) else Color.Black)
    val outlineColor = if (state == CapsuleState.CHARGING_EVENT) Color.Green.copy(alpha = 0.5f) else Color.Transparent

    val interactionSource = remember { MutableInteractionSource() }

    val stateModifier = if (state == CapsuleState.IDLE) {
        Modifier.size(idleWidth, idleHeight)
    } else if (state == CapsuleState.MEDIA_PLAYING || state == CapsuleState.CHARGING_EVENT) {
        Modifier.width(idleWidth + if(state == CapsuleState.CHARGING_EVENT) 120.dp else 80.dp).wrapContentHeight()
    } else {
        Modifier.widthIn(max = 380.dp).wrapContentSize()
    }

    Box(
        modifier = Modifier
            .then(stateModifier)
            .animateContentSize(animationSpec = spring(dampingRatio = 0.65f, stiffness = 300f))
            .shadow(
                elevation = if (state == CapsuleState.IDLE) 0.dp else 24.dp,
                shape = RoundedCornerShape(animatedCorner.value.coerceAtLeast(0.dp)),
                ambientColor = outlineColor,
                spotColor = outlineColor
            )
            .background(bgColor, RoundedCornerShape(animatedCorner.value.coerceAtLeast(0.dp)))
            .clip(RoundedCornerShape(animatedCorner.value.coerceAtLeast(0.dp)))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (state == CapsuleState.IDLE) {
                        if (mediaInfo.isPlaying) {
                            CapsuleStateManager.setState(CapsuleState.EXPANDED_MEDIA)
                        } else if (notificationInfo != null) {
                            CapsuleStateManager.setState(CapsuleState.EXPANDED_NOTIFICATION)
                        }
                    } else {
                        CapsuleStateManager.setState(CapsuleState.IDLE)
                    }
                }
            )
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    CapsuleStateManager.setXOffset(CapsuleStateManager.capsuleXOffset.value + dragAmount.x.toInt())
                    CapsuleStateManager.setYOffset(CapsuleStateManager.capsuleYOffset.value + dragAmount.y.toInt())
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Crossfade(targetState = state, animationSpec = spring(stiffness = 300f)) { targetState ->
            when (targetState) {
                CapsuleState.IDLE -> Box(modifier = Modifier.fillMaxSize())
                CapsuleState.MEDIA_PLAYING -> MediaPlayingMini(mediaInfo)
                CapsuleState.NOTIFICATION_POPUP -> notificationInfo?.let { NotificationAlert(it) }
                CapsuleState.CHARGING_EVENT -> ChargingAnimation(batteryInfo)
                CapsuleState.EXPANDED_MEDIA -> MediaExpandedCard(mediaInfo)
                CapsuleState.EXPANDED_NOTIFICATION -> notificationInfo?.let { NotificationExpandedCard(it) }
            }
        }
    }
}

@Composable
fun SecondaryIsland(mediaInfo: MediaInfo) {
    val baseHeight by CapsuleStateManager.baseHeight.collectAsState()
    val size = baseHeight.dp.coerceAtLeast(24.dp)
    
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
        }
    }
}

@Composable
fun ChargingAnimation(batteryInfo: BatteryInfo) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 16.dp, vertical = 8.dp),
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
            .padding(horizontal = 12.dp, vertical = 8.dp),
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
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { MediaControllerManager.previous() }) {
                Icon(Icons.Filled.FastRewind, contentDescription = "Previous", tint = Color.White)
            }
            IconButton(
                onClick = {
                    MediaControllerManager.playPause()
                },
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
                inactiveTrackColor = Color.DarkGray
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun EqualizerWave(isPlaying: Boolean, color: Color) {
    val barCount = 4
    val heights = remember { mutableStateListOf<Float>().apply { for(i in 0 until barCount) add(0.2f) } }
    
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
