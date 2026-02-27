package com.radio.ccbes.ui.screens.profile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radio.ccbes.util.StorageUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountSettingsViewModel : ViewModel() {

    private val _cacheSize = MutableStateFlow("Calculando...")
    val cacheSize: StateFlow<String> = _cacheSize.asStateFlow()

    fun loadCacheSize(context: Context) {
        viewModelScope.launch {
            _cacheSize.value = StorageUtils.getCacheSize(context)
        }
    }

    fun clearCache(context: Context) {
        viewModelScope.launch {
            StorageUtils.clearCache(context)
            _cacheSize.value = StorageUtils.getCacheSize(context)
        }
    }
}
