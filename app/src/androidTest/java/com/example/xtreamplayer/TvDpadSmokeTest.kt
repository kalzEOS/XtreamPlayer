package com.example.xtreamplayer

import android.view.KeyEvent
import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TvDpadSmokeTest {

    @Test
    fun dpadTraversalDoesNotCloseActivity() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val root = activity.findViewById<View>(android.R.id.content)
                val keys =
                    listOf(
                        KeyEvent.KEYCODE_DPAD_DOWN,
                        KeyEvent.KEYCODE_DPAD_DOWN,
                        KeyEvent.KEYCODE_DPAD_RIGHT,
                        KeyEvent.KEYCODE_DPAD_LEFT,
                        KeyEvent.KEYCODE_DPAD_UP,
                        KeyEvent.KEYCODE_DPAD_CENTER
                    )
                keys.forEach { keyCode ->
                    root.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
                    root.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
                }
                assertFalse(activity.isFinishing)
            }
        }
    }
}
