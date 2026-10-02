package com.example.game

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var size: Float,
    var alpha: Float,
    val colorHex: Long
)

data class GameState(
    val player: Car = Car(
        x = 155f,
        y = 490f,
        w = 50f,
        h = 80f,
        colorHex = 0xFF38BDF8 // #38bdf8 player cyan
    ),
    val enemies: List<Car> = emptyList(),
    val particles: List<Particle> = emptyList(),
    val score: Int = 0,
    val speed: Float = 4f,
    val isGameOver: Boolean = false,
    val isPaused: Boolean = false,
    val roadOffset: Float = 0f,
    val frame: Long = 0L,
    val highScore: Int = 0
) {
    companion object {
        const val CANVAS_WIDTH = 360f
        const val CANVAS_HEIGHT = 600f
        const val ROAD_LEFT = 30f
        const val ROAD_RIGHT = 330f
        const val PLAYER_MIN_X = 40f
        const val PLAYER_MAX_X = 270f
        val LANES = floatArrayOf(55f, 155f, 255f)
        val ENEMY_COLORS = longArrayOf(
            0xFFF43F5E, // Rose red
            0xFFF59E0B, // Amber/Orange
            0xFFA855F7, // Purple
            0xFF10B981, // Emerald green
            0xFFE11D48  // Crimson
        )
    }
}
