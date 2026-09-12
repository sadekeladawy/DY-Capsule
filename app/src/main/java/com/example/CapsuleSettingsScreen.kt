package com.example

import android.content.Intent
import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun CapsuleSettingsScreen(onFinish: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val xOffset by CapsuleStateManager.capsuleXOffset.collectAsState()
    val yOffset by CapsuleStateManager.capsuleYOffset.collectAsState()
    val baseWidth by CapsuleStateManager.baseWidth.collectAsState()
    val baseHeight by CapsuleStateManager.baseHeight.collectAsState()

    DisposableEffect(Unit) {
        val intent = Intent(context, CapsuleOverlayService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        
        onDispose {
            CapsuleStateManager.setState(CapsuleState.IDLE)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .padding(top = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Capsule Settings",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        CalibrationRow(
            title = "X Position",
            value = xOffset,
            range = -200..200,
            onValueChange = { 
                CapsuleStateManager.setXOffset(it)
                coroutineScope.launch { CapsulePreferencesRepository.setXOffset(context, it) }
            }
        )

        CalibrationRow(
            title = "Y Position",
            value = yOffset,
            range = 0..200,
            onValueChange = { 
                CapsuleStateManager.setYOffset(it)
                coroutineScope.launch { CapsulePreferencesRepository.setYOffset(context, it) }
            }
        )

        CalibrationRow(
            title = "Width",
            value = baseWidth,
            range = 20..300,
            onValueChange = { 
                CapsuleStateManager.setBaseWidth(it)
                coroutineScope.launch { CapsulePreferencesRepository.setScaleWidth(context, it) }
            }
        )

        CalibrationRow(
            title = "Height",
            value = baseHeight,
            range = 20..300,
            onValueChange = { 
                CapsuleStateManager.setBaseHeight(it)
                coroutineScope.launch { CapsulePreferencesRepository.setScaleHeight(context, it) }
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = {
                CapsuleStateManager.setState(CapsuleState.IDLE)
                onFinish()
            },
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Finish Setup")
        }
    }
}

@Composable
fun CalibrationRow(
    title: String,
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text("$value px", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = { onValueChange((value - 1).coerceIn(range)) }
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease")
            }
            Slider(
                value = value.toFloat(),
                onValueChange = { onValueChange(it.toInt()) },
                valueRange = range.first.toFloat()..range.last.toFloat(),
                modifier = Modifier.weight(1f)
            )
            IconButton(
                onClick = { onValueChange((value + 1).coerceIn(range)) }
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Increase")
            }
        }
    }
}
