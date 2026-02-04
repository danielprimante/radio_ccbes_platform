package com.radio.ccbes.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radio.ccbes.data.repository.AboutConfig
import com.radio.ccbes.data.repository.ConfigRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AboutUsViewModel : ViewModel() {
    private val configRepository = ConfigRepository()

    private val _aboutConfig = MutableStateFlow(AboutConfig())
    val aboutConfig: StateFlow<AboutConfig> = _aboutConfig.asStateFlow()

    init {
        loadConfig()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            _aboutConfig.value = configRepository.getAboutConfig()
        }
    }
}
