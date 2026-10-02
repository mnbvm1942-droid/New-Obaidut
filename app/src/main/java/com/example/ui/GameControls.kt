package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
fun SteeringButton(
    icon: @Composable () -> Unit,
    label: String,
    testTag: String,
    onPressChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var isPressed by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isPressed) Color(0xFF15803D) else Color(0xFF16A34A),
        shadowElevation = if (isPressed) 2.dp else 8.dp,
        modifier = modifier
            .testTag(testTag)
            .scale(if (isPressed) 0.95f else 1.0f)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown()
                    isPressed = true
                    onPressChanged(true)
                    val upOrCancel = waitForUpOrCancellation()
                    isPressed = false
                    onPressChanged(false)
                }
            }
    ) {
        Row(
            modifier = Modifier
                .padding(vertical = 14.dp, horizontal = 28.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            icon()
        }
    }
}

@Composable
fun GameControls(
    onLeftPressChanged: (Boolean) -> Unit,
    onRightPressChanged: (Boolean) -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Control Button
        SteeringButton(
            icon = {
                Text(
                    text = "⬅️",
                    fontSize = 32.sp,
                    color = Color.White
                )
            },
            label = stringResource(R.string.left_button),
            testTag = "left_button",
            onPressChanged = onLeftPressChanged,
            modifier = Modifier.weight(1f).height(64.dp)
        )

        Box(modifier = Modifier.size(24.dp))

        // Right Control Button
        SteeringButton(
            icon = {
                Text(
                    text = "➡️",
                    fontSize = 32.sp,
                    color = Color.White
                )
            },
            label = stringResource(R.string.right_button),
            testTag = "right_button",
            onPressChanged = onRightPressChanged,
            modifier = Modifier.weight(1f).height(64.dp)
        )
    }
}
