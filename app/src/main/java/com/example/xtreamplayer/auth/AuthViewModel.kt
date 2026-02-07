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

    private val _savedConfigLoaded = kotlinx.coroutines.flow.MutableStateFlow(false)
    val savedConfigLoaded: StateFlow<Boolean> = _savedConfigLoaded

    val savedConfig: StateFlow<AuthConfig?> = repository.authConfig.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    init {
        viewModelScope.launch {
            repository.authConfig.collect {
                _savedConfigLoaded.value = true
            }
        }
    }

    fun tryAutoSignIn(settings: SettingsState) {
        val state = _uiState.value
        if (state.isSignedIn ||
            state.isLoading ||
            state.isEditingList ||
            state.autoSignInSuppressed
        ) {
            return
        }
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
        _uiState.value = _uiState.value.copy(autoSignInSuppressed = false)
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
            _uiState.value = AuthUiState(autoSignInSuppressed = true)
        }
    }

    fun signOut(keepSaved: Boolean) {
        viewModelScope.launch {
            if (!keepSaved) {
                repository.clear()
            }
            _uiState.value = AuthUiState(autoSignInSuppressed = true)
        }
    }

    fun enterEditMode() {
        val currentConfig = _uiState.value.activeConfig
        _uiState.value = AuthUiState(
            activeConfig = currentConfig,
            isEditingList = true,
            autoSignInSuppressed = true
        )
    }

    private fun signInWithConfig(config: AuthConfig, rememberLogin: Boolean) {
        if (_uiState.value.isLoading) return
        _uiState.value =
            _uiState.value.copy(
                isLoading = true,
                errorMessage = null,
                autoSignInSuppressed = false
            )
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
                    activeConfig = config,
                    autoSignInSuppressed = false
                )
            }.onFailure { error ->
                Timber.e(error, "Authentication failed")
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
