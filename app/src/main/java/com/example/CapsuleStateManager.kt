package com.example

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object CapsuleStateManager {
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private val _currentState = MutableStateFlow(CapsuleState.IDLE)
    val currentState: StateFlow<CapsuleState> = _currentState.asStateFlow()

    private val _mediaInfo = MutableStateFlow(MediaInfo())
    val mediaInfo: StateFlow<MediaInfo> = _mediaInfo.asStateFlow()

    private val _notificationInfo = MutableStateFlow<NotificationInfo?>(null)
    val notificationInfo: StateFlow<NotificationInfo?> = _notificationInfo.asStateFlow()

    private val _batteryInfo = MutableStateFlow(BatteryInfo())
    val batteryInfo: StateFlow<BatteryInfo> = _batteryInfo.asStateFlow()

    private val _capsuleXOffset = MutableStateFlow(0)
    val capsuleXOffset: StateFlow<Int> = _capsuleXOffset.asStateFlow()

    private val _capsuleYOffset = MutableStateFlow(0)
    val capsuleYOffset: StateFlow<Int> = _capsuleYOffset.asStateFlow()

    private val _baseWidth = MutableStateFlow(100)
    val baseWidth: StateFlow<Int> = _baseWidth.asStateFlow()

    private val _baseHeight = MutableStateFlow(30)
    val baseHeight: StateFlow<Int> = _baseHeight.asStateFlow()

    private var eventJob: Job? = null

    fun expand() {
        if (_currentState.value == CapsuleState.MEDIA_PLAYING || _mediaInfo.value.isPlaying) {
            setState(CapsuleState.EXPANDED_MEDIA)
        } else if (_currentState.value == CapsuleState.NOTIFICATION_POPUP || _notificationInfo.value != null) {
            setState(CapsuleState.EXPANDED_NOTIFICATION)
        }
    }

    fun setState(state: CapsuleState) {
        if (_currentState.value == CapsuleState.CHARGING_EVENT && state != CapsuleState.IDLE && state != CapsuleState.MEDIA_PLAYING) {
            if (state != CapsuleState.EXPANDED_MEDIA && state != CapsuleState.EXPANDED_NOTIFICATION) return
        }
        _currentState.value = state
        reevaluateState()
    }

    fun updateMediaInfo(info: MediaInfo) {
        _mediaInfo.value = info
        reevaluateState()
    }

    fun postNotification(info: NotificationInfo) {
        _notificationInfo.value = info
        if (_currentState.value != CapsuleState.CHARGING_EVENT && _currentState.value != CapsuleState.EXPANDED_MEDIA && _currentState.value != CapsuleState.EXPANDED_NOTIFICATION && _currentState.value != CapsuleState.CALIBRATION_MODE) {
            _currentState.value = CapsuleState.NOTIFICATION_POPUP
            eventJob?.cancel()
            eventJob = scope.launch {
                delay(4000)
                _notificationInfo.value = null
                reevaluateState()
            }
        }
    }
    
    fun clearNotification() {
        _notificationInfo.value = null
        reevaluateState()
    }

    fun updateBatteryInfo(info: BatteryInfo, connected: Boolean = false) {
        _batteryInfo.value = info
        if (connected && _currentState.value != CapsuleState.CALIBRATION_MODE) {
            _currentState.value = CapsuleState.CHARGING_EVENT
            eventJob?.cancel()
            eventJob = scope.launch {
                delay(4000)
                reevaluateState(forceClearCharging = true)
            }
        }
    }

    fun setXOffset(offset: Int) {
        _capsuleXOffset.value = offset
    }

    fun setYOffset(offset: Int) {
        _capsuleYOffset.value = offset
    }

    fun setBaseWidth(width: Int) {
        _baseWidth.value = width
    }

    fun setBaseHeight(height: Int) {
        _baseHeight.value = height
    }

    fun resetAll() {
        eventJob?.cancel()
        _notificationInfo.value = null
        _mediaInfo.value = MediaInfo()
        _batteryInfo.value = BatteryInfo()
        _currentState.value = CapsuleState.IDLE
    }
    
    private fun reevaluateState(forceClearCharging: Boolean = false) {
        if (_currentState.value == CapsuleState.CALIBRATION_MODE) {
            return
        }

        if (_currentState.value == CapsuleState.CHARGING_EVENT && !forceClearCharging) {
            return
        }
        
        if (_currentState.value == CapsuleState.EXPANDED_MEDIA || _currentState.value == CapsuleState.EXPANDED_NOTIFICATION) {
            return
        }
        
        if (_notificationInfo.value != null) {
            _currentState.value = CapsuleState.NOTIFICATION_POPUP
            return
        }
        
        if (_mediaInfo.value.isPlaying) {
            _currentState.value = CapsuleState.MEDIA_PLAYING
            return
        }
        
        _currentState.value = CapsuleState.IDLE
    }
}
