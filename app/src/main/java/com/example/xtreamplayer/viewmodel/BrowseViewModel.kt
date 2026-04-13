package com.example.xtreamplayer.viewmodel

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.example.xtreamplayer.Section
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class BrowseViewModel @Inject constructor() : ViewModel() {
    val selectedSection = mutableStateOf(Section.ALL)
    val navExpanded = mutableStateOf(true)
    val showManageLists = mutableStateOf(false)
    val showAppearance = mutableStateOf(false)
    val showLocalFilesGuest = mutableStateOf(false)
    val moveFocusToNav = mutableStateOf(false)
    val focusToContentTrigger = mutableIntStateOf(0)
    val cacheClearNonce = mutableIntStateOf(0)
    val focusAppearanceOnSettingsReturn = mutableStateOf(false)
    val focusManageListsOnSettingsReturn = mutableStateOf(false)
    val wasShowingAppearance = mutableStateOf(false)
    val wasShowingManageLists = mutableStateOf(false)
}
