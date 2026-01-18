package com.example.xtreamplayer.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.xtreamplayer.api.XtreamApi
import com.example.xtreamplayer.settings.SettingsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: AuthRepository,
    private val api: XtreamApi
) : ViewModel() {
    private val _uiState = kotlinx.coroutines.flow.MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState

    val savedConfig: StateFlow<AuthConfig?> = repository.authConfig.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    fun tryAutoSignIn(settings: SettingsState) {
        if (_uiState.value.isSignedIn || _uiState.value.isLoading) return
        if (!settings.autoSignIn || !settings.rememberLogin) return
        val config = savedConfig.value ?: return
        signInWithConfig(config, rememberLogin = true)
    }

    fun signIn(
        listName: String,
        baseUrl: String,
        username: String,
        password: String,
        rememberLogin: Boolean
    ) {
        val config = AuthConfig(
            listName = listName.trim(),
            baseUrl = baseUrl.trim(),
            username = username.trim(),
            password = password
        )
        signInWithConfig(config, rememberLogin)
    }

    fun signOut() {
        viewModelScope.launch {
            repository.clear()
            _uiState.value = AuthUiState()
        }
    }

    fun signOut(keepSaved: Boolean) {
        viewModelScope.launch {
            if (!keepSaved) {
                repository.clear()
            }
            _uiState.value = AuthUiState()
        }
    }

    fun enterEditMode() {
        _uiState.value = AuthUiState()
    }

    private fun signInWithConfig(config: AuthConfig, rememberLogin: Boolean) {
        if (_uiState.value.isLoading) return
        _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
        viewModelScope.launch {
            val result = api.authenticate(config)
            result.onSuccess {
                if (rememberLogin) {
                    repository.save(config)
                } else {
                    repository.clear()
                }
                _uiState.value = AuthUiState(
                    isSignedIn = true,
                    isLoading = false,
                    errorMessage = null,
                    activeConfig = config
                )
            }.onFailure { error ->
                Timber.e(error, "Authentication failed for user: ${config.username}")
                _uiState.value = _uiState.value.copy(
                    isSignedIn = false,
                    isLoading = false,
                    errorMessage = error.message ?: "Login failed",
                    activeConfig = null
                )
            }
        }
    }
}
