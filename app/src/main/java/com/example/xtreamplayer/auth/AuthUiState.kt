package com.example.xtreamplayer.auth

data class AuthUiState(
    val isSignedIn: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val activeConfig: AuthConfig? = null
)
