package com.example.xtreamplayer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.xtreamplayer.di.RootScreenEntryPoint
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RootScreen()
        }
    }

    override fun onDestroy() {
        if (isFinishing) {
            EntryPointAccessors.fromApplication(
                applicationContext,
                RootScreenEntryPoint::class.java
            ).playbackEngine().release()
        }
        super.onDestroy()
    }
}
