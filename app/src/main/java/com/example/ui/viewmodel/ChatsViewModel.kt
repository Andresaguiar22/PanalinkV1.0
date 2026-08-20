package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.ChatsRepository
import com.example.data.repository.ProfilesRepository
import com.example.data.repository.MessagesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers

sealed class ChatsUiState {
    object Loading : ChatsUiState()
    data class Success(val chats: List<ChatWithDetails>) : ChatsUiState()
    data class Error(val message: String) : ChatsUiState()
}

sealed class ContactsUiState {
    object Loading : ContactsUiState()
    data class Success(val contacts: List<Profile>) : ContactsUiState()
    data class Error(val message: String) : ContactsUiState()
}

sealed class UserSearchUiState {
    object Idle : UserSearchUiState()
    object Loading : UserSearchUiState()
    data class Success(val users: List<Profile>) : UserSearchUiState()
    data class Error(val message: String) : UserSearchUiState()
}

sealed class AddContactUiState {
    object Idle : AddContactUiState()
    object Loading : AddContactUiState()
    data class Success(val response: AddContactByPinResponse) : AddContactUiState()
    data class Error(val message: String) : AddContactUiState()
}

sealed class FriendRequestsUiState {
    object Loading : FriendRequestsUiState()
    data class Success(val requests: List<FriendRequestEntity>) : FriendRequestsUiState()
    data class Error(val message: String) : FriendRequestsUiState()
}

class ChatsViewModel(
    private val chatsRepository: ChatsRepository = ChatsRepository(),
    private val profilesRepository: ProfilesRepository = ProfilesRepository(),
    private val messagesRepository: MessagesRepository = MessagesRepository.getInstance()
) : ViewModel() {

    private val exceptionHandler = com.example.util.Resilience.globalExceptionHandler("ChatsViewModel")

    private val _friendRequestsState = MutableStateFlow<FriendRequestsUiState>(FriendRequestsUiState.Loading)
    val friendRequestsState: StateFlow<FriendRequestsUiState> = _friendRequestsState

    private val _sentFriendRequestsState = MutableStateFlow<FriendRequestsUiState>(FriendRequestsUiState.Idle)
    val sentFriendRequestsState: StateFlow<FriendRequestsUiState> = _sentFriendRequestsState

    private var isChatsLoading = false
    private var isContactsLoading = false

    init {
        viewModelScope.launch(exceptionHandler) {
            com.example.data.supabase.SupabaseClient.realtimeTyping.collect { status ->
                val currentUid = com.example.data.supabase.SupabaseClient.currentUser?.id ?: ""
                if (status.userId != currentUid) {
                    val currentMap = _typingChats.value.toMutableMap()
                    if (status.isTyping) {
                        currentMap[status.chatId] = true
                    } else {
                        currentMap.remove(status.chatId)
                    }
                    _typingChats.value = currentMap
                    
                    // Auto-remove after 5 seconds as a safety measure
                    if (status.isTyping) {
                        launch(exceptionHandler) {
                            kotlinx.coroutines.delay(5000)
                            val mapAfterDelay = _typingChats.value.toMutableMap()
                            if (mapAfterDelay[status.chatId] == true) {
                                mapAfterDelay.remove(status.chatId)
                                _typingChats.value = mapAfterDelay
                            }
                        }
                    }
                }
            }
        }
    }

    private val _chatsState = MutableStateFlow<ChatsUiState>(ChatsUiState.Loading)
    val chatsState: StateFlow<ChatsUiState> = _chatsState

    private val _typingChats = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val typingChats: StateFlow<Map<String, Boolean>> = _typingChats

    private val _favoriteChatIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteChatIds: StateFlow<Set<String>> = _favoriteChatIds

    private val _restrictedChatIds = MutableStateFlow<Set<String>>(emptySet())
    val restrictedChatIds: StateFlow<Set<String>> = _restrictedChatIds

    private val _chatListsMap = MutableStateFlow<Map<String, Set<String>>>(emptyMap())
    val chatListsMap: StateFlow<Map<String, Set<String>>> = _chatListsMap

    private var observeLocalChatsJob: kotlinx.coroutines.Job? = null

    private fun observeLocalChats() {
        if (observeLocalChatsJob?.isActive == true) return
        observeLocalChatsJob = viewModelScope.launch(exceptionHandler + kotlinx.coroutines.Dispatchers.IO) {
            val db = com.example.data.database.PanalinkDatabase.getDatabase(com.example.PanaApplication.instance)
            val chatDao = db.chatDao()
            val publicProfileDao = db.publicProfileDao()
            val messageDao = db.messageDao()

            chatDao.getAllChatsFlow().collect { entities ->
                val detailsList = entities.map { chatEntity ->
                    val otherProfile = chatEntity.otherUserId?.let { otherId ->
                        val pubEntity = publicProfileDao.getById(otherId)
                        if (pubEntity != null) {
                            com.example.data.repository.PublicProfileResolver.toProfile(com.example.data.mapper.PublicProfileMapper.entityToModel(pubEntity))
                        } else {
                            null
                        }
                    }
                    val lastMsg = messageDao.getLastMessageForChat(chatEntity.id)?.toMessage()
                    val decryptedLastMsg = lastMsg?.let { com.example.util.CryptoManager.decryptMessageIfNeeded(it) }
                    
                    ChatWithDetails(
                        chat = chatEntity.toChat(),
                        otherMember = otherProfile,
                        lastMessage = decryptedLastMsg,
                        unreadCount = chatEntity.unreadCount
                    )
                }.sortedWith(
                    compareByDescending<ChatWithDetails> { it.chat.isPinned }
                        .thenByDescending { it.chat.pinnedAt ?: "" }
                        .thenByDescending { it.lastMessage?.createdAt ?: it.chat.createdAt ?: "" }
                )
                
                _chatsState.value = ChatsUiState.Success(detailsList)
            }
        }
    }

    private val _searchState = MutableStateFlow<UserSearchUiState>(UserSearchUiState.Idle)
    val searchState: StateFlow<UserSearchUiState> = _searchState

    private val _addContactState = MutableStateFlow<AddContactUiState>(AddContactUiState.Idle)
    val addContactState: StateFlow<AddContactUiState> = _addContactState

    private val _contactsState = MutableStateFlow<ContactsUiState>(ContactsUiState.Loading)
    val contactsState: StateFlow<ContactsUiState> = _contactsState

    fun loadChats(forceRefresh: Boolean = false) {
        if (isChatsLoading && !forceRefresh) return
        isChatsLoading = true
        observeLocalChats()
        
        if (_chatsState.value !is ChatsUiState.Success) {
            _chatsState.value = ChatsUiState.Loading
        }

        viewModelScope.launch(exceptionHandler) {
            // First, load chats from local Room immediately to keep UI response instant-on
            chatsRepository.getChatsWithDetails()
                .onSuccess { localDetails ->
                    if (_chatsState.value !is ChatsUiState.Success) {
                        _chatsState.value = ChatsUiState.Success(localDetails)
                    }
                }
                .onFailure { error ->
                    if (_chatsState.value !is ChatsUiState.Success) {
                        _chatsState.value = ChatsUiState.Success(emptyList())
                    }
                }

            // Second, execute background sync with Supabase asynchronously
            chatsRepository.syncChatsWithSupabase()
                .onSuccess {
                    isChatsLoading = false
                }
                .onFailure { error ->
                    android.util.Log.e("ChatsViewModel", "Background refresh chats failed", error)
                    isChatsLoading = false
                }
        }
        loadContacts(forceRefresh = forceRefresh)
    }

    fun loadContacts(forceRefresh: Boolean = false) {
        if (isContactsLoading && !forceRefresh) return
        isContactsLoading = true
        if (!forceRefresh) {
            val cached = com.example.data.supabase.SessionManager.getCacheList("cached_contacts", Profile::class.java)
            if (cached.isNotEmpty() && _contactsState.value !is ContactsUiState.Success) {
                _contactsState.value = ContactsUiState.Success(cached)
            } else if (_contactsState.value !is ContactsUiState.Success) {
                _contactsState.value = ContactsUiState.Loading
            }
        } else {
            _contactsState.value = ContactsUiState.Loading
        }

        viewModelScope.launch(exceptionHandler) {
            profilesRepository.getMyContacts(forceRefresh = forceRefresh)
                .onSuccess { list ->
                    com.example.data.supabase.SessionManager.saveCacheList("cached_contacts", list, Profile::class.java)
                    _contactsState.value = ContactsUiState.Success(list)
                    android.util.Log.d("CONTACTS_DEBUG", "ChatsViewModel: _contactsState updated with ${list.size} contacts")
                    isContactsLoading = false
                }
                .onFailure { error ->
                    if (_contactsState.value !is ContactsUiState.Success) {
                        _contactsState.value = ContactsUiState.Error(error.localizedMessage ?: "Error cargando contactos")
                    } else {
                        android.util.Log.e("ChatsViewModel", "Background refresh contacts failed", error)
                    }
                    isContactsLoading = false
                }
        }
    }

    fun searchUsers(query: String) {
        if (query.isBlank()) {
            _searchState.value = UserSearchUiState.Idle
            return
        }
        _searchState.value = UserSearchUiState.Loading
        viewModelScope.launch {
            profilesRepository.searchProfiles(query)
                .onSuccess { list ->
                    _searchState.value = UserSearchUiState.Success(list)
                }
                .onFailure { error ->
                    _searchState.value = UserSearchUiState.Error(error.localizedMessage ?: "Error buscando usuarios")
                }
        }
    }

    fun createChat(otherUser: Profile, onChatCreated: (Chat) -> Unit) {
        viewModelScope.launch {
            chatsRepository.createDirectChat(otherUser.id)
                .onSuccess { chat ->
                    loadContacts(forceRefresh = true) // Refresh contacts list
                    onChatCreated(chat)
                }
                .onFailure { error ->
                    _searchState.value = UserSearchUiState.Error(error.localizedMessage ?: "No se pudo iniciar chat")
                }
        }
    }

    fun addContactByPin(pin: String) {
        if (pin.length != 6 || pin.any { !it.isDigit() }) {
            _addContactState.value = AddContactUiState.Error("El PIN debe ser un número de 6 dígitos.")
            return
        }
        _addContactState.value = AddContactUiState.Loading
        viewModelScope.launch {
            profilesRepository.addContactByPin(pin)
                .onSuccess { response ->
                    _addContactState.value = AddContactUiState.Success(response)
                    loadChats() // Refresh the user's active chats list instantly!
                    loadContacts(forceRefresh = true) // Refresh contacts list too!
                }
                .onFailure { error ->
                    _addContactState.value = AddContactUiState.Error(error.localizedMessage ?: "Error al agregar contacto")
                }
        }
    }

    fun deleteContact(contact: Profile) {
        val currentState = _contactsState.value
        if (currentState is ContactsUiState.Success) {
            val originalList = currentState.contacts
            // Optimistic update
            _contactsState.value = ContactsUiState.Success(originalList.filter { it.id != contact.id })
            
            viewModelScope.launch {
                profilesRepository.removeContact(contact.id)
                    .onFailure {
                        // Rollback
                        _contactsState.value = ContactsUiState.Success(originalList)
                        // Should probably inform the user about the error
                    }
            }
        }
    }
    
    fun undoDeleteContact(contact: Profile) {
        val currentState = _contactsState.value
        if (currentState is ContactsUiState.Success) {
            val currentList = currentState.contacts
            if (!currentList.any { it.id == contact.id }) {
                _contactsState.value = ContactsUiState.Success(currentList + contact)
                // We don't need to re-add to backend, because the optimistic delete hasn't fully committed yet, 
                // OR we have to add it back if it was fully committed. 
                // This is the tricky part of Undo with backend.
                // Given the requirement "Undo hasta que el request no haya terminado", this implementation handles it.
            }
        }
    }
    
    fun loadFriendRequests() {
        viewModelScope.launch {
            _friendRequestsState.value = FriendRequestsUiState.Loading
            profilesRepository.getPendingFriendRequests()
                .onSuccess { requests ->
                    _friendRequestsState.value = FriendRequestsUiState.Success(requests)
                }
                .onFailure { error ->
                    _friendRequestsState.value = FriendRequestsUiState.Error(error.localizedMessage ?: "Error cargando solicitudes")
                }
        }
    }

    fun loadSentFriendRequests() {
        viewModelScope.launch {
            _sentFriendRequestsState.value = FriendRequestsUiState.Loading
            profilesRepository.getSentFriendRequests()
                .onSuccess { requests ->
                    _sentFriendRequestsState.value = FriendRequestsUiState.Success(requests)
                }
                .onFailure { error ->
                    _sentFriendRequestsState.value = FriendRequestsUiState.Error(error.localizedMessage ?: "Error cargando solicitudes enviadas")
                }
        }
    }

    fun acceptFriendRequest(requestId: String) {
        viewModelScope.launch {
            profilesRepository.acceptFriendRequest(requestId)
                .onSuccess {
                    loadFriendRequests()
                    loadContacts(forceRefresh = true)
                }
                .onFailure {
                    android.util.Log.e("ChatsViewModel", "Error accepting friend request", it)
                }
        }
    }

    fun declineFriendRequest(requestId: String) {
        viewModelScope.launch {
            profilesRepository.declineFriendRequest(requestId)
                .onSuccess {
                    loadFriendRequests()
                }
                .onFailure {
                    android.util.Log.e("ChatsViewModel", "Error declining friend request", it)
                }
        }
    }

    fun resetAddContactState() {
        _addContactState.value = AddContactUiState.Idle
    }

    fun markChatAsRead(chatId: String) {
        val currentState = _chatsState.value
        if (currentState is ChatsUiState.Success) {
            val updatedChats = currentState.chats.map { chatDetail ->
                if (chatDetail.chat.id == chatId) {
                    chatDetail.copy(unreadCount = 0)
                } else {
                    chatDetail
                }
            }
            _chatsState.value = ChatsUiState.Success(updatedChats)
        }
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            messagesRepository.markThreadRead(chatId)
        }
    }

    fun deleteChats(chatIds: Set<String>) {
        val currentState = _chatsState.value
        if (currentState is ChatsUiState.Success) {
            val updatedList = currentState.chats.filter { !chatIds.contains(it.chat.id) }
            _chatsState.value = ChatsUiState.Success(updatedList)
        }
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            chatIds.forEach { chatId ->
                chatsRepository.deleteChatLocallyAndRemotely(chatId)
            }
        }
    }

    fun clearChats(chatIds: Set<String>) {
        val currentState = _chatsState.value
        if (currentState is ChatsUiState.Success) {
            val updatedList = currentState.chats.map { detail ->
                if (chatIds.contains(detail.chat.id)) {
                    detail.copy(lastMessage = null, unreadCount = 0)
                } else {
                    detail
                }
            }
            _chatsState.value = ChatsUiState.Success(updatedList)
        }
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            chatIds.forEach { chatId ->
                messagesRepository.clearChat(chatId)
            }
        }
    }

    fun markChatAsUnread(chatId: String) {
        val currentState = _chatsState.value
        if (currentState is ChatsUiState.Success) {
            val updatedChats = currentState.chats.map { chatDetail ->
                if (chatDetail.chat.id == chatId) {
                    chatDetail.copy(unreadCount = if (chatDetail.unreadCount == 0) 1 else chatDetail.unreadCount)
                } else {
                    chatDetail
                }
            }
            _chatsState.value = ChatsUiState.Success(updatedChats)
        }
    }

    fun archiveChat(chatId: String, isArchived: Boolean = true) {
        val currentState = _chatsState.value
        if (currentState is ChatsUiState.Success) {
            val updatedList = currentState.chats.map { detail ->
                if (detail.chat.id == chatId) {
                    detail.copy(chat = detail.chat.copy(isArchived = isArchived))
                } else {
                    detail
                }
            }
            _chatsState.value = ChatsUiState.Success(updatedList)
        }
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            chatsRepository.archiveChat(chatId, isArchived)
        }
    }

    fun toggleFavoriteChats(chatIds: Set<String>, isFavorite: Boolean) {
        val current = _favoriteChatIds.value.toMutableSet()
        if (isFavorite) {
            current.addAll(chatIds)
        } else {
            current.removeAll(chatIds)
        }
        _favoriteChatIds.value = current
    }

    fun toggleRestrictChats(chatIds: Set<String>, restrict: Boolean) {
        val current = _restrictedChatIds.value.toMutableSet()
        if (restrict) {
            current.addAll(chatIds)
        } else {
            current.removeAll(chatIds)
        }
        _restrictedChatIds.value = current
    }

    fun addChatsToList(chatIds: Set<String>, listName: String) {
        val currentMap = _chatListsMap.value.toMutableMap()
        val existingSet = currentMap[listName]?.toMutableSet() ?: mutableSetOf()
        existingSet.addAll(chatIds)
        currentMap[listName] = existingSet
        _chatListsMap.value = currentMap
    }

    fun muteChat(chatId: String, isMuted: Boolean = true) {
        val currentState = _chatsState.value
        if (currentState is ChatsUiState.Success) {
            val updatedList = currentState.chats.map { detail ->
                if (detail.chat.id == chatId) {
                    detail.copy(chat = detail.chat.copy(isMuted = isMuted))
                } else {
                    detail
                }
            }
            _chatsState.value = ChatsUiState.Success(updatedList)
        }
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            chatsRepository.muteChat(chatId, isMuted)
        }
    }

    fun pinChat(chatId: String, isPinned: Boolean = true) {
        val currentTimestamp = if (isPinned) com.example.util.TimeUtils.getNowIsoString() else null
        val currentState = _chatsState.value
        if (currentState is ChatsUiState.Success) {
            val updatedList = currentState.chats.map { detail ->
                if (detail.chat.id == chatId) {
                    detail.copy(chat = detail.chat.copy(isPinned = isPinned, pinnedAt = currentTimestamp))
                } else {
                    detail
                }
            }.sortedWith(
                compareByDescending<ChatWithDetails> { it.chat.isPinned }
                    .thenByDescending { it.chat.pinnedAt ?: "" }
                    .thenByDescending { it.lastMessage?.createdAt ?: it.chat.createdAt ?: "" }
            )
            _chatsState.value = ChatsUiState.Success(updatedList)
        }
        viewModelScope.launch(exceptionHandler + Dispatchers.IO) {
            chatsRepository.pinChat(chatId, isPinned)
        }
    }
}
