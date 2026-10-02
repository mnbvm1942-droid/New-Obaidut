package com.example.data

import kotlinx.coroutines.flow.Flow

class HighScoreRepository(private val dao: HighScoreDao) {
    val topScore: Flow<HighScoreEntity?> = dao.getTopScore()
    val topScores: Flow<List<HighScoreEntity>> = dao.getTopScores()

    suspend fun recordScore(score: Int) {
        if (score > 0) {
            dao.insertScore(HighScoreEntity(score = score))
        }
    }
}
