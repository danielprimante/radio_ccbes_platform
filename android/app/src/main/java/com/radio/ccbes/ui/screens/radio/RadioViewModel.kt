package com.radio.ccbes.ui.screens.radio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radio.ccbes.data.repository.ConfigRepository
import com.radio.ccbes.data.repository.RadioConfig
import com.radio.ccbes.data.repository.AboutConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class RadioViewModel : ViewModel() {
    private val configRepository = ConfigRepository()

    private val _radioConfig = MutableStateFlow(RadioConfig())
    val radioConfig: StateFlow<RadioConfig> = _radioConfig.asStateFlow()

    private val _logoUrl = MutableStateFlow("")
    val logoUrl: StateFlow<String> = _logoUrl.asStateFlow()

    init {
        loadConfig()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            _radioConfig.value = configRepository.getRadioConfig()
            val aboutConfig = configRepository.getAboutConfig()
            _logoUrl.value = aboutConfig.logoUrl
        }
    }
}
