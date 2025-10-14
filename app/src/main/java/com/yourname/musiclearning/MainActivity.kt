package com.yourname.musiclearning

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.material3.Text
import androidx.compose.ui.tooling.preview.Preview

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.QueueMusic
import com.yourname.musiclearning.ui.theme.AppTheme
import com.yourname.musiclearning.home.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val demoExercises = listOf(
                    Exercise(
                        id = "intervals",
                        title = "Interval Training",
                        subtitle = "Learn to recognize intervals by ear",
                        icon = Icons.Filled.MusicNote
                    ),
                    Exercise(
                        id = "chords",
                        title = "Chord Ear Training",
                        subtitle = "Major, minor, diminished, augmented",
                        icon = Icons.Filled.QueueMusic
                    ),
                    Exercise(
                        id = "notes",
                        title = "Note Reading",
                        subtitle = "Treble & bass clef drills",
                        icon = Icons.Filled.Piano
                    )
                )

                HomeScreen(
                    exercises = demoExercises,
                    onExerciseClick = { ex ->
                        // TODO: navigate to the exercise screen
                        // For now you can log/Toast or later plug in Navigation Compose.
                    }
                )
            }
        }
    }
}