package com.vfdeginformatica.mysuperapp.presentation.screen.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.Screen
import com.vfdeginformatica.mysuperapp.presentation.screen.settings.contract.SettingsEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.settings.contract.SettingsEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _effect = MutableSharedFlow<SettingsEffect>()
    val effect: SharedFlow<SettingsEffect> = _effect.asSharedFlow()

    fun onEvent(event: SettingsEvent) {
        when (event) {
            SettingsEvent.OnNotificationsClick -> viewModelScope.launch {
                _effect.emit(SettingsEffect.Navigate(Screen.NotificationsScreen.route))
            }
        }
    }
}
