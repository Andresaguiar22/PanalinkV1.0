package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.reels.ReelsRepository
import com.example.core.media.preload.ReelsPreloadManager

class ReelsViewModelFactory(
    private val repository: ReelsRepository,
    private val preloadManager: ReelsPreloadManager
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(
        modelClass: Class<T>
    ): T {
        if (modelClass.isAssignableFrom(ReelsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ReelsViewModel(
                repository,
                preloadManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
