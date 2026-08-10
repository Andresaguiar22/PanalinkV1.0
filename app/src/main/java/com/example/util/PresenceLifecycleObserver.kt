package com.example.util

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.example.data.repository.PresenceRepository
import com.example.data.repository.UserPresenceStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object PresenceLifecycleObserver : DefaultLifecycleObserver {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var awayTimerJob: Job? = null

    override fun onStart(owner: LifecycleOwner) {
        awayTimerJob?.cancel()
        awayTimerJob = null
        PresenceRepository.updateMyStatus(UserPresenceStatus.ONLINE)
    }

    override fun onStop(owner: LifecycleOwner) {
        awayTimerJob?.cancel()
        awayTimerJob = scope.launch {
            delay(5_000L) // 5 seconds in background updates status to AWAY
            PresenceRepository.updateMyStatus(UserPresenceStatus.AWAY)
        }
    }
}
