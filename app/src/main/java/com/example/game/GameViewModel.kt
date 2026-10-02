package com.example.game

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.HighScoreEntity
import com.example.data.HighScoreRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

class GameViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: HighScoreRepository
    val soundEffects: SoundEffects = SoundEffects(application)

    val topScores: StateFlow<List<HighScoreEntity>>
    val topScore: StateFlow<HighScoreEntity?>

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private var leftPressed = false
    private var rightPressed = false

    private var gameLoopJob: Job? = null

    init {
        val db = AppDatabase.getDatabase(application)
        repository = HighScoreRepository(db.highScoreDao())
        topScores = repository.topScores.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )
        topScore = repository.topScore.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            null
        )

        // Observe high score from DB to keep local state updated
        viewModelScope.launch {
            repository.topScore.collect { entity ->
                val best = entity?.score ?: 0
                _gameState.update { it.copy(highScore = maxOf(it.highScore, best)) }
            }
        }

        startGameLoop()
    }

    private fun startGameLoop() {
        gameLoopJob?.cancel()
        gameLoopJob = viewModelScope.launch {
            // Target ~60 FPS (approx 16ms per frame)
            while (isActive) {
                val startTime = System.currentTimeMillis()
                updateGameTick()
                val elapsed = System.currentTimeMillis() - startTime
                val sleepTime = maxOf(2L, 16L - elapsed)
                delay(sleepTime)
            }
        }
    }

    private fun updateGameTick() {
        val state = _gameState.value
        if (state.isGameOver || state.isPaused) {
            // Update particles if any
            if (state.particles.isNotEmpty()) {
                val updatedParticles = state.particles.mapNotNull { p ->
                    p.x += p.vx
                    p.y += p.vy
                    p.alpha -= 0.03f
                    if (p.alpha > 0f) p else null
                }
                _gameState.update { it.copy(particles = updatedParticles) }
            }
            return
        }

        val frame = state.frame + 1
        var playerX = state.player.x

        if (leftPressed) {
            playerX -= 5.5f
        }
        if (rightPressed) {
            playerX += 5.5f
        }
        playerX = playerX.coerceIn(GameState.PLAYER_MIN_X, GameState.PLAYER_MAX_X)

        val speed = 4f + minOf(state.score / 500f, 5f)
        val roadOffset = (state.roadOffset + speed) % 100f

        val updatedEnemies = state.enemies.toMutableList()

        // Spawn new enemy car every 65 frames
        if (frame % 65L == 0L) {
            val laneIndex = Random.nextInt(GameState.LANES.size)
            val laneX = GameState.LANES[laneIndex]
            val colorIndex = Random.nextInt(GameState.ENEMY_COLORS.size)
            val enemyColor = GameState.ENEMY_COLORS[colorIndex]

            // Check if there's already an enemy at the very top of that lane
            val tooClose = updatedEnemies.any { it.y < 120f && kotlin.math.abs(it.x - laneX) < 30f }
            if (!tooClose) {
                updatedEnemies.add(
                    Car(
                        x = laneX,
                        y = -90f,
                        w = 50f,
                        h = 80f,
                        colorHex = enemyColor,
                        styleVariant = laneIndex
                    )
                )
            }
        }

        // Move enemies down
        var collisionOccurred = false
        val remainingEnemies = mutableListOf<Car>()

        val playerCar = state.player.copy(x = playerX)

        for (enemy in updatedEnemies) {
            enemy.y += speed
            if (enemy.intersects(playerCar)) {
                collisionOccurred = true
            }
            if (enemy.y < GameState.CANVAS_HEIGHT + 100f) {
                remainingEnemies.add(enemy)
            }
        }

        val newScore = state.score + 1
        val newHighScore = maxOf(state.highScore, newScore)

        if (collisionOccurred) {
            soundEffects.playCrash()
            val particles = createExplosion(playerCar.x + 25f, playerCar.y + 40f)

            viewModelScope.launch {
                repository.recordScore(newScore)
            }

            _gameState.update {
                it.copy(
                    player = playerCar,
                    enemies = remainingEnemies,
                    particles = particles,
                    score = newScore,
                    speed = speed,
                    roadOffset = roadOffset,
                    frame = frame,
                    highScore = newHighScore,
                    isGameOver = true
                )
            }
        } else {
            _gameState.update {
                it.copy(
                    player = playerCar,
                    enemies = remainingEnemies,
                    score = newScore,
                    speed = speed,
                    roadOffset = roadOffset,
                    frame = frame,
                    highScore = newHighScore
                )
            }
        }
    }

    private fun createExplosion(cx: Float, cy: Float): List<Particle> {
        val list = mutableListOf<Particle>()
        val colors = longArrayOf(0xFFFFE066, 0xFFFF5722, 0xFFFF0055, 0xFF38BDF8, 0xFFFFFFFF)
        for (i in 0 until 35) {
            val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
            val speed = Random.nextFloat() * 7f + 2f
            list.add(
                Particle(
                    x = cx,
                    y = cy,
                    vx = kotlin.math.cos(angle) * speed,
                    vy = kotlin.math.sin(angle) * speed,
                    size = Random.nextFloat() * 8f + 4f,
                    alpha = 1.0f,
                    colorHex = colors[Random.nextInt(colors.size)]
                )
            )
        }
        return list
    }

    fun setLeftPressed(pressed: Boolean) {
        leftPressed = pressed
    }

    fun setRightPressed(pressed: Boolean) {
        rightPressed = pressed
    }

    fun setPlayerTargetX(touchCanvasX: Float) {
        val targetX = (touchCanvasX - 25f).coerceIn(GameState.PLAYER_MIN_X, GameState.PLAYER_MAX_X)
        _gameState.update { it.copy(player = it.player.copy(x = targetX)) }
    }

    fun togglePause() {
        _gameState.update { it.copy(isPaused = !it.isPaused) }
    }

    fun restartGame() {
        leftPressed = false
        rightPressed = false
        _gameState.update { current ->
            GameState(
                player = Car(x = 155f, y = 490f, w = 50f, h = 80f, colorHex = 0xFF38BDF8),
                enemies = emptyList(),
                particles = emptyList(),
                score = 0,
                speed = 4f,
                isGameOver = false,
                isPaused = false,
                roadOffset = 0f,
                frame = 0L,
                highScore = current.highScore
            )
        }
        soundEffects.playEngineRev()
    }
}
