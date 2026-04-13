package com.example.xtreamplayer.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.xtreamplayer.UpdateUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow

@HiltViewModel
class UpdateViewModel @Inject constructor() : ViewModel() {
    val updateUiState = MutableStateFlow(UpdateUiState())
    val updateCheckJob = MutableStateFlow<Job?>(null)
    val startupUpdateCheckEnabled = MutableStateFlow<Boolean?>(null)
    val startupUpdateCheckHandled = MutableStateFlow(false)
}
