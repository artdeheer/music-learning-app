package com.yourname.musiclearning

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Piano
import androidx.compose.material.icons.filled.QueueMusic
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yourname.musiclearning.ui.theme.AppTheme
import com.yourname.musiclearning.home.*
import com.yourname.musiclearning.keyboard.KeyboardScreen // <- from the file I gave you

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                val exercises = listOf(
                    Exercise(
                        id = "keyboard",
                        title = "Piano Keyboard (3 octaves)",
                        subtitle = "Landscape-only screen with a playable keyboard",
                        icon = Icons.Filled.Piano
                    ),
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

                val nav = rememberNavController()

                NavHost(navController = nav, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            exercises = exercises,
                            onExerciseClick = { ex ->
                                when (ex.id) {
                                    "keyboard" -> nav.navigate("keyboard")
                                    // add other routes later if you like
                                }
                            }
                        )
                    }
                    composable("keyboard") {
                        KeyboardScreen() // locks to landscape while visible
                    }
                }
            }
        }
    }
}
