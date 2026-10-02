package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.game.Car
import com.example.game.GameState

@Composable
fun GameCanvas(
    gameState: GameState,
    onSteerToX: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val virtualWidth = GameState.CANVAS_WIDTH
    val virtualHeight = GameState.CANVAS_HEIGHT

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(virtualWidth / virtualHeight)
            .shadow(elevation = 12.dp, shape = RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF101827)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .testTag("racing_canvas")
                .pointerInput(gameState.isGameOver) {
                    if (!gameState.isGameOver) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val scaleX = virtualWidth / size.width
                            val touchCanvasX = change.position.x * scaleX
                            onSteerToX(touchCanvasX)
                        }
                    }
                }
                .pointerInput(gameState.isGameOver) {
                    if (!gameState.isGameOver) {
                        detectTapGestures { offset ->
                            val scaleX = virtualWidth / size.width
                            val touchCanvasX = offset.x * scaleX
                            onSteerToX(touchCanvasX)
                        }
                    }
                }
        ) {
            val scaleX = size.width / virtualWidth
            val scaleY = size.height / virtualHeight

            // Helper to transform virtual coords to actual pixels
            fun vx(x: Float) = x * scaleX
            fun vy(y: Float) = y * scaleY
            fun vw(w: Float) = w * scaleX
            fun vh(h: Float) = h * scaleY

            // 1. Draw grass background on left and right borders (#27783b)
            drawRect(
                color = Color(0xFF27783B),
                topLeft = Offset(0f, 0f),
                size = Size(size.width, size.height)
            )

            // 2. Draw road asphalt (#383838)
            drawRect(
                color = Color(0xFF333333),
                topLeft = Offset(vx(30f), 0f),
                size = Size(vw(300f), size.height)
            )

            // Red/White curb edges along the sides
            val curbSegmentHeight = 40f
            val curbOffset = gameState.roadOffset % curbSegmentHeight
            var curbY = -curbSegmentHeight + curbOffset
            var curbToggle = ((gameState.frame * gameState.speed) / curbSegmentHeight).toInt() % 2 == 0

            while (curbY < virtualHeight + curbSegmentHeight) {
                val curbColor = if (curbToggle) Color(0xFFE11D48) else Color.White
                // Left curb
                drawRect(
                    color = curbColor,
                    topLeft = Offset(vx(25f), vy(curbY)),
                    size = Size(vw(5f), vh(curbSegmentHeight))
                )
                // Right curb
                drawRect(
                    color = curbColor,
                    topLeft = Offset(vx(330f), vy(curbY)),
                    size = Size(vw(5f), vh(curbSegmentHeight))
                )
                curbY += curbSegmentHeight
                curbToggle = !curbToggle
            }

            // 3. White border lines
            drawRect(
                color = Color.White,
                topLeft = Offset(vx(30f), 0f),
                size = Size(vw(4f), size.height)
            )
            drawRect(
                color = Color.White,
                topLeft = Offset(vx(326f), 0f),
                size = Size(vw(4f), size.height)
            )

            // 4. Road dashed center lines
            val dashH = 55f
            val spacing = 100f
            val startY = -80f + (gameState.roadOffset % spacing)
            var currentDashY = startY

            while (currentDashY < virtualHeight + 80f) {
                drawRoundRect(
                    color = Color.White,
                    topLeft = Offset(vx(176f), vy(currentDashY)),
                    size = Size(vw(8f), vh(dashH)),
                    cornerRadius = CornerRadius(vw(2f), vh(2f))
                )
                currentDashY += spacing
            }

            // 5. Draw Headlight Beams for player car
            if (!gameState.isGameOver) {
                val p = gameState.player
                val beamPath = Path().apply {
                    moveTo(vx(p.x + 10f), vy(p.y))
                    lineTo(vx(p.x - 20f), vy(p.y - 120f))
                    lineTo(vx(p.x + 70f), vy(p.y - 120f))
                    lineTo(vx(p.x + 40f), vy(p.y))
                    close()
                }
                drawPath(
                    path = beamPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0x00FEF08A),
                            Color(0x33FEF08A)
                        ),
                        startY = vy(p.y - 120f),
                        endY = vy(p.y)
                    )
                )
            }

            // 6. Draw enemy cars
            for (enemy in gameState.enemies) {
                drawCarGraphic(
                    car = enemy,
                    isPlayer = false,
                    scaleX = scaleX,
                    scaleY = scaleY
                )
            }

            // 7. Draw player car
            drawCarGraphic(
                car = gameState.player,
                isPlayer = true,
                scaleX = scaleX,
                scaleY = scaleY
            )

            // 8. Draw explosion particles
            for (particle in gameState.particles) {
                drawCircle(
                    color = Color(particle.colorHex).copy(alpha = particle.alpha.coerceIn(0f, 1f)),
                    radius = vw(particle.size),
                    center = Offset(vx(particle.x), vy(particle.y))
                )
            }

            // 9. Game Over overlay
            if (gameState.isGameOver) {
                drawRect(
                    color = Color(0xBF000000),
                    topLeft = Offset(0f, 0f),
                    size = Size(size.width, size.height)
                )
            }
        }
    }
}

private fun DrawScope.drawCarGraphic(
    car: Car,
    isPlayer: Boolean,
    scaleX: Float,
    scaleY: Float
) {
    fun vx(x: Float) = x * scaleX
    fun vy(y: Float) = y * scaleY
    fun vw(w: Float) = w * scaleX
    fun vh(h: Float) = h * scaleY

    val x = car.x
    val y = car.y
    val carColor = Color(car.colorHex)
    val wheelColor = Color(0xFF111111)
    val windowColor = Color(0xFFBDE8FF)

    // 4 Wheels:
    // Left-front (x-5, y+12, w=7, h=20)
    drawRoundRect(
        color = wheelColor,
        topLeft = Offset(vx(x - 5f), vy(y + 12f)),
        size = Size(vw(7f), vh(20f)),
        cornerRadius = CornerRadius(vw(2f), vh(2f))
    )
    // Right-front (x+48, y+12, w=7, h=20)
    drawRoundRect(
        color = wheelColor,
        topLeft = Offset(vx(x + 48f), vy(y + 12f)),
        size = Size(vw(7f), vh(20f)),
        cornerRadius = CornerRadius(vw(2f), vh(2f))
    )
    // Left-rear (x-5, y+50, w=7, h=20)
    drawRoundRect(
        color = wheelColor,
        topLeft = Offset(vx(x - 5f), vy(y + 50f)),
        size = Size(vw(7f), vh(20f)),
        cornerRadius = CornerRadius(vw(2f), vh(2f))
    )
    // Right-rear (x+48, y+50, w=7, h=20)
    drawRoundRect(
        color = wheelColor,
        topLeft = Offset(vx(x + 48f), vy(y + 50f)),
        size = Size(vw(7f), vh(20f)),
        cornerRadius = CornerRadius(vw(2f), vh(2f))
    )

    // Car Body (50x80) with rounded corners
    drawRoundRect(
        color = carColor,
        topLeft = Offset(vx(x), vy(y)),
        size = Size(vw(50f), vh(80f)),
        cornerRadius = CornerRadius(vw(8f), vh(8f))
    )

    // Racing stripe down the center
    val stripeColor = if (isPlayer) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.35f)
    drawRect(
        color = stripeColor,
        topLeft = Offset(vx(x + 22f), vy(y)),
        size = Size(vw(6f), vh(80f))
    )

    // Front windshield (x+8, y+12, w=34, h=20)
    drawRoundRect(
        color = windowColor,
        topLeft = Offset(vx(x + 8f), vy(y + 12f)),
        size = Size(vw(34f), vh(20f)),
        cornerRadius = CornerRadius(vw(4f), vh(4f))
    )

    // Rear window (x+8, y+47, w=34, h=15)
    drawRoundRect(
        color = windowColor,
        topLeft = Offset(vx(x + 8f), vy(y + 47f)),
        size = Size(vw(34f), vh(15f)),
        cornerRadius = CornerRadius(vw(3f), vh(3f))
    )

    // Roof section accent
    val roofColor = carColor.copy(alpha = 0.85f)
    drawRect(
        color = roofColor,
        topLeft = Offset(vx(x + 8f), vy(y + 32f)),
        size = Size(vw(34f), vh(15f))
    )

    // Headlights (at the front: y for player is top, y for enemy is facing downward)
    if (isPlayer) {
        // Front headlights (top)
        drawCircle(
            color = Color(0xFFFEF08A),
            radius = vw(3.5f),
            center = Offset(vx(x + 8f), vy(y + 3f))
        )
        drawCircle(
            color = Color(0xFFFEF08A),
            radius = vw(3.5f),
            center = Offset(vx(x + 42f), vy(y + 3f))
        )
        // Rear taillights (bottom)
        drawCircle(
            color = Color(0xFFEF4444),
            radius = vw(3f),
            center = Offset(vx(x + 7f), vy(y + 77f))
        )
        drawCircle(
            color = Color(0xFFEF4444),
            radius = vw(3f),
            center = Offset(vx(x + 43f), vy(y + 77f))
        )
    } else {
        // Enemy car headlights (facing down)
        drawCircle(
            color = Color(0xFFFEF08A),
            radius = vw(3.5f),
            center = Offset(vx(x + 8f), vy(y + 77f))
        )
        drawCircle(
            color = Color(0xFFFEF08A),
            radius = vw(3.5f),
            center = Offset(vx(x + 42f), vy(y + 77f))
        )
        // Enemy taillights (top)
        drawCircle(
            color = Color(0xFFEF4444),
            radius = vw(3f),
            center = Offset(vx(x + 7f), vy(y + 3f))
        )
        drawCircle(
            color = Color(0xFFEF4444),
            radius = vw(3f),
            center = Offset(vx(x + 43f), vy(y + 3f))
        )
    }
}
