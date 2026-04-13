package com.example.xtreamplayer.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.xtreamplayer.UpdateUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.Job

@HiltViewModel
class UpdateViewModel @Inject constructor() : ViewModel() {
    val updateUiState = mutableStateOf(UpdateUiState())
    val updateCheckJob = mutableStateOf<Job?>(null)
    val startupUpdateCheckEnabled = mutableStateOf<Boolean?>(null)
    val startupUpdateCheckHandled = mutableStateOf(false)
}
