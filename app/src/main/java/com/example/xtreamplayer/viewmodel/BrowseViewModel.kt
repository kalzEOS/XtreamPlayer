package com.example.xtreamplayer.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.xtreamplayer.Section
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor() : ViewModel() {
    val selectedSection = mutableStateOf(Section.ALL)
    val navExpanded = mutableStateOf(true)
}
