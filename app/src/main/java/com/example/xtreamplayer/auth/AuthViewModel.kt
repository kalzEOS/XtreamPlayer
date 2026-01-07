package com.example.xtreamplayer.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.xtreamplayer.api.XtreamApi
import com.example.xtreamplayer.settings.SettingsState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(
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

class AuthViewModelFactory(
    private val context: Context,
    private val api: XtreamApi
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            val repository = AuthRepository(context)
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(repository, api) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
