package com.example.game

data class Car(
    var x: Float,
    var y: Float,
    val w: Float = 50f,
    val h: Float = 80f,
    val colorHex: Long = 0xFFF43F5E, // Default red enemy
    val lightColorHex: Long = 0xFFBDE8FF,
    val styleVariant: Int = 0
) {
    fun intersects(other: Car, padding: Float = 4f): Boolean {
        return (x + padding) < (other.x + other.w - padding) &&
                (x + w - padding) > (other.x + padding) &&
                (y + padding) < (other.y + other.h - padding) &&
                (y + h - padding) > (other.y + padding)
    }
}
