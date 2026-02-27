package com.radio.ccbes.ui.screens.radio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.radio.ccbes.data.model.Program
import com.radio.ccbes.data.repository.ConfigRepository
import com.radio.ccbes.data.repository.RadioConfig
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

    // Programa activo: null = no hay programa al aire (mostrar logo/nombre por defecto)
    private val _activeProgram = MutableStateFlow<Program?>(null)
    val activeProgram: StateFlow<Program?> = _activeProgram.asStateFlow()

    init {
        loadConfig()
        observeActiveProgram()
    }

    private fun loadConfig() {
        viewModelScope.launch {
            _radioConfig.value = configRepository.getRadioConfig()

            val aboutConfig = configRepository.getAboutConfig()
            _logoUrl.value = aboutConfig.logoUrl
        }
    }

    /**
     * Escucha en tiempo real los cambios del programa al aire en Firestore.
     * Cada vez que se pone o quita un programa al aire desde la web,
     * este Flow emite el nuevo valor y la UI se actualiza automáticamente.
     * El listener se cancela automáticamente cuando el ViewModel se destruye.
     */
    private fun observeActiveProgram() {
        viewModelScope.launch {
            configRepository.observeActiveProgram().collect { program ->
                _activeProgram.value = program
            }
        }
    }

    fun refresh() {
        loadConfig()
    }
}
