package com.vfdeginformatica.mysuperapp.presentation.screen.mural_comments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vfdeginformatica.mysuperapp.common.Resource
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.DeleteMuralCommentUseCase
import com.vfdeginformatica.mysuperapp.domain.use_case.qrcode.GetMuralCommentsUseCase
import com.vfdeginformatica.mysuperapp.presentation.screen.mural_comments.contract.MuralCommentsEffect
import com.vfdeginformatica.mysuperapp.presentation.screen.mural_comments.contract.MuralCommentsEvent
import com.vfdeginformatica.mysuperapp.presentation.screen.mural_comments.contract.MuralCommentsUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MuralCommentsViewModel @Inject constructor(
    private val getMuralCommentsUseCase: GetMuralCommentsUseCase,
    private val deleteMuralCommentUseCase: DeleteMuralCommentUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MuralCommentsUiState())
    val uiState: StateFlow<MuralCommentsUiState> = _uiState.asStateFlow()

    private val _effect = MutableSharedFlow<MuralCommentsEffect>()
    val effect: SharedFlow<MuralCommentsEffect> = _effect.asSharedFlow()

    fun onEvent(event: MuralCommentsEvent) {
        when (event) {
            is MuralCommentsEvent.OnLoadComments -> loadComments(event.qrCodeId)
            is MuralCommentsEvent.OnDeleteComment -> deleteComment(event.qrCodeId, event.commentId)
        }
    }

    private fun loadComments(qrCodeId: String) {
        viewModelScope.launch {
            getMuralCommentsUseCase(qrCodeId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                    is Resource.Success -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        comments = resource.data ?: emptyList(),
                        errorMessage = ""
                    )

                    is Resource.Error -> _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        errorMessage = resource.message ?: "Erro ao carregar comentários"
                    )
                }
            }
        }
    }

    private fun deleteComment(qrCodeId: String, commentId: String) {
        viewModelScope.launch {
            deleteMuralCommentUseCase(qrCodeId, commentId).collect { resource ->
                when (resource) {
                    is Resource.Loading -> _uiState.value = _uiState.value.copy(isLoading = true)
                    is Resource.Success -> {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            comments = _uiState.value.comments.filter { it.id != commentId }
                        )
                        sendEffect(MuralCommentsEffect.ShowToast("Comentário excluído com sucesso!"))
                    }

                    is Resource.Error -> {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                        sendEffect(
                            MuralCommentsEffect.ShowToast(
                                resource.message ?: "Erro ao excluir comentário"
                            )
                        )
                    }
                }
            }
        }
    }

    private fun sendEffect(effect: MuralCommentsEffect) {
        viewModelScope.launch { _effect.emit(effect) }
    }
}

