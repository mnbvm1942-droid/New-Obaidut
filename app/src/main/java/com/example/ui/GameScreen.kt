package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.game.GameViewModel

@Composable
fun GameScreen(
    viewModel: GameViewModel,
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsStateWithLifecycle()
    val topScores by viewModel.topScores.collectAsStateWithLifecycle()
    var showLeaderboard by remember { mutableStateOf(false) }
    var soundEnabled by remember { mutableStateOf(viewModel.soundEffects.isSoundEnabled) }

    Surface(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF101827)),
        color = Color(0xFF101827)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 1. Top Header Row: Title and action buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.game_title),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.testTag("app_header_title")
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Sound Toggle
                    IconButton(
                        onClick = {
                            val newVal = !soundEnabled
                            soundEnabled = newVal
                            viewModel.soundEffects.isSoundEnabled = newVal
                        },
                        modifier = Modifier.size(36.dp).testTag("sound_toggle_button")
                    ) {
                        Icon(
                            imageVector = if (soundEnabled) Icons.Default.VolumeUp else Icons.Default.VolumeOff,
                            contentDescription = "Sound Toggle",
                            tint = if (soundEnabled) Color(0xFF38BDF8) else Color(0xFF64748B)
                        )
                    }

                    // Leaderboard Button
                    IconButton(
                        onClick = { showLeaderboard = true },
                        modifier = Modifier.size(36.dp).testTag("leaderboard_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = "Leaderboard",
                            tint = Color(0xFFFACC15)
                        )
                    }

                    // Pause / Resume Button
                    IconButton(
                        onClick = { viewModel.togglePause() },
                        modifier = Modifier.size(36.dp).testTag("pause_resume_button")
                    ) {
                        Icon(
                            imageVector = if (gameState.isPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                            contentDescription = if (gameState.isPaused) "Resume" else "Pause",
                            tint = Color.White
                        )
                    }
                }
            }

            // 2. Score and High Score Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.testTag("current_score_box")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.score_label, gameState.score),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF1E293B),
                    modifier = Modifier.testTag("high_score_box")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = stringResource(R.string.high_score_label, gameState.highScore),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFFACC15)
                        )
                    }
                }
            }

            // 3. Canvas in Center (with GameOver overlay)
            Box(
                modifier = Modifier
                    .weight(1f, fill = true)
                    .padding(vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                GameCanvas(
                    gameState = gameState,
                    onSteerToX = { x -> viewModel.setPlayerTargetX(x) }
                )

                if (gameState.isGameOver) {
                    GameOverOverlay(
                        score = gameState.score,
                        highScore = gameState.highScore,
                        onRestart = { viewModel.restartGame() },
                        modifier = Modifier.fillMaxSize()
                    )
                } else if (gameState.isPaused) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xCC000000),
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "⏸️ সাময়িক বিরতি (Paused)",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(20.dp)
                        )
                    }
                }
            }

            // 4. Steering Controls: ⬅️ and ➡️
            GameControls(
                onLeftPressChanged = { pressed -> viewModel.setLeftPressed(pressed) },
                onRightPressChanged = { pressed -> viewModel.setRightPressed(pressed) },
                onRestart = { viewModel.restartGame() }
            )

            // 5. Restart button: "আবার খেলুন"
            Button(
                onClick = { viewModel.restartGame() },
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(48.dp)
                    .padding(bottom = 4.dp)
                    .testTag("restart_bottom_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF16A34A),
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.restart_button),
                    fontSize = 17.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Leaderboard bottom sheet
        if (showLeaderboard) {
            LeaderboardSheet(
                scores = topScores,
                onDismiss = { showLeaderboard = false }
            )
        }
    }
}
