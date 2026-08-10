package com.example.notification.engine.badge

import androidx.annotation.Keep
import com.example.data.database.ChatDao
import com.example.notification.engine.storage.dao.NotificationDaoV2
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@Keep
class BadgeEngineV2(
    private val notificationDao: NotificationDaoV2? = null,
    private val chatDao: ChatDao? = null,
    val calculator: BadgeCalculator = BadgeCalculator(),
    private val externalSources: List<BadgeSource> = emptyList(),
    coroutineScope: CoroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
) {

    private val _badgeState = MutableStateFlow(BadgeState())
    val badgeState: StateFlow<BadgeState> = _badgeState.asStateFlow()

    private val missedCallsFlow = MutableStateFlow(0)
    private val followRequestsFlow = MutableStateFlow(0)
    private val securityAlertsFlow = MutableStateFlow(0)
    private val unreadGroupsFlow = MutableStateFlow(0)
    private val unreadChannelsFlow = MutableStateFlow(0)

    init {
        val unreadNotificationsFlow: Flow<Int> = notificationDao?.observeUnreadCount() ?: MutableStateFlow(0)

        val unreadChatsFlow: Flow<Int> = chatDao?.getAllChatsFlow()?.map { chats ->
            chats.filter { !it.isMuted }.sumOf { it.unreadCount }
        } ?: MutableStateFlow(0)

        // Connect external BadgeSources if supplied
        externalSources.forEach { source ->
            coroutineScope.launch {
                source.observeCount().collect { count ->
                    when (source.category) {
                        BadgeCategory.CALLS -> missedCallsFlow.value = count
                        BadgeCategory.SOCIAL -> followRequestsFlow.value = count
                        BadgeCategory.SECURITY -> securityAlertsFlow.value = count
                        BadgeCategory.GROUPS -> unreadGroupsFlow.value = count
                        BadgeCategory.CHANNELS -> unreadChannelsFlow.value = count
                        else -> { /* Managed directly above */ }
                    }
                }
            }
        }

        coroutineScope.launch {
            combine(
                unreadChatsFlow,
                unreadNotificationsFlow,
                missedCallsFlow,
                followRequestsFlow,
                securityAlertsFlow,
                unreadGroupsFlow,
                unreadChannelsFlow
            ) { values: Array<Int> ->
                val rawState = BadgeState(
                    unreadChats = values[0],
                    unreadNotifications = values[1],
                    missedCalls = values[2],
                    followRequests = values[3],
                    securityAlerts = values[4],
                    unreadGroups = values[5],
                    unreadChannels = values[6]
                )
                calculator.calculateEffectiveState(rawState)
            }.collect { effectiveState ->
                _badgeState.value = effectiveState
            }
        }
    }

    fun updateManualCount(category: BadgeCategory, count: Int) {
        when (category) {
            BadgeCategory.CALLS -> missedCallsFlow.value = count
            BadgeCategory.SOCIAL -> followRequestsFlow.value = count
            BadgeCategory.SECURITY -> securityAlertsFlow.value = count
            BadgeCategory.GROUPS -> unreadGroupsFlow.value = count
            BadgeCategory.CHANNELS -> unreadChannelsFlow.value = count
            else -> {}
        }
    }

    fun getFormattedTotal(max: Int = 99): String {
        return calculator.formatBadge(_badgeState.value.total)
    }
}
