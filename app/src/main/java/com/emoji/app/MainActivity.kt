package com.emoji.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.emoji.app.domain.model.MediaType
import com.emoji.app.ui.components.StepIndicator
import com.emoji.app.ui.components.totalStepsForPhoto
import com.emoji.app.ui.components.totalStepsForVideo
import com.emoji.app.ui.screens.MaterialPickScreen
import com.emoji.app.ui.screens.PhotoOrderScreen
import com.emoji.app.ui.screens.PreviewExportScreen
import com.emoji.app.ui.screens.TextEditScreen
import com.emoji.app.ui.screens.VideoTrimScreen
import com.emoji.app.ui.theme.HappyEmojiTheme
import com.emoji.app.viewmodel.SharedViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HappyEmojiTheme {
                EmojiApp()
            }
        }
    }
}

@Composable
fun EmojiApp(viewModel: SharedViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    val isVideo = state.mediaType == MediaType.VIDEO
    val isPhoto = state.mediaType == MediaType.PHOTO
    val isSingle = isPhoto && state.photos.size == 1

    // Determine total steps and current step mapping
    val totalSteps = when {
        isVideo -> totalStepsForVideo()
        isPhoto && isSingle -> totalStepsForPhoto(1)
        isPhoto -> totalStepsForPhoto(state.photos.size.coerceAtLeast(2))
        else -> 4
    }

    val currentDisplayStep = when {
        state.currentStep == 0 -> 0
        isVideo -> state.currentStep.coerceIn(0, 3)
        isSingle -> state.currentStep.coerceIn(0, 2)
        else -> state.currentStep.coerceIn(0, 3)
    }

    // Route to the appropriate screen based on state
    // Wrap everything in a Column that respects system bars so bottom buttons don't overlap nav bar
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        StepIndicator(currentStep = currentDisplayStep, totalSteps = totalSteps)

        Box(modifier = Modifier.weight(1f)) {
            when {
            state.currentStep == 0 -> {
                MaterialPickScreen(
                    viewModel = viewModel,
                    onNavigateNext = {}
                )
            }

            // Video flow
            isVideo && state.currentStep == 1 -> {
                state.videoUri?.let { uri ->
                    VideoTrimScreen(
                        viewModel = viewModel,
                        videoUri = uri,
                        onNavigateNext = {},
                        onNavigateBack = {},
                    )
                }
            }
            isVideo && state.currentStep == 2 -> {
                TextEditScreen(viewModel = viewModel, onNavigateNext = {}, onNavigateBack = {})
            }
            isVideo && state.currentStep == 3 -> {
                PreviewExportScreen(viewModel = viewModel)
            }

            // Photo single flow
            isPhoto && isSingle && state.currentStep == 1 -> {
                TextEditScreen(viewModel = viewModel, onNavigateNext = {}, onNavigateBack = {})
            }
            isPhoto && isSingle && state.currentStep == 2 -> {
                PreviewExportScreen(viewModel = viewModel)
            }

            // Photo multi flow
            isPhoto && !isSingle && state.currentStep == 1 -> {
                PhotoOrderScreen(viewModel = viewModel, onNavigateNext = {}, onNavigateBack = {})
            }
            isPhoto && !isSingle && state.currentStep == 2 -> {
                TextEditScreen(viewModel = viewModel, onNavigateNext = {}, onNavigateBack = {})
            }
            isPhoto && !isSingle && state.currentStep == 3 -> {
                PreviewExportScreen(viewModel = viewModel)
            }
        }
        } // end Box
    }
}
