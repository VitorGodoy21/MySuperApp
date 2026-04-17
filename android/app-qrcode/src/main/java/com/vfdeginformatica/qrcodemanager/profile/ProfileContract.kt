package com.vfdeginformatica.qrcodemanager.profile

data class ProfileUiState(
    val name: String? = null,
    val email: String? = null,
    val isEditing: Boolean = false,
    val editName: String = "",
    val editEmail: String = "",
    val isSaving: Boolean = false,
    val errorMessage: String = "",
    val successMessage: String = ""
)

sealed interface ProfileEvent {
    data object OnToggleEdit : ProfileEvent
    data object OnCancelEdit : ProfileEvent
    data object OnSave : ProfileEvent
    data class OnNameChanged(val name: String) : ProfileEvent
    data class OnEmailChanged(val email: String) : ProfileEvent
}

sealed interface ProfileEffect {
    data class ShowToast(val message: String) : ProfileEffect
}

