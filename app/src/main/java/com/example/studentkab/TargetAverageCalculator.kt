package com.example.studentkab

/**
 * Расчёт оценки, необходимой для достижения целевого среднего балла
 * после получения одной следующей оценки.
 */
object TargetAverageCalculator {

    fun calculate(targetAverage: Double, grades: List<Int>): TargetAverageResult {
        if (!targetAverage.isFinite() || targetAverage !in 2.0..5.0) {
            return TargetAverageResult.InvalidTarget
        }
        if (grades.any { it !in 2..5 }) {
            return TargetAverageResult.InvalidGrades
        }

        val required = targetAverage * (grades.size + 1) - grades.sum()
        if (required < 2.0 || required > 5.0) {
            return TargetAverageResult.OutOfRange(required)
        }

        val nearestInteger = kotlin.math.round(required).toInt()
        if (kotlin.math.abs(required - nearestInteger) > 1e-9) {
            return TargetAverageResult.NonInteger(required)
        }

        return TargetAverageResult.Success(nearestInteger)
    }
}

sealed class TargetAverageResult {
    data class Success(val requiredGrade: Int) : TargetAverageResult()
    data class OutOfRange(val requiredGrade: Double) : TargetAverageResult()
    data class NonInteger(val requiredGrade: Double) : TargetAverageResult()
    object InvalidTarget : TargetAverageResult()
    object InvalidGrades : TargetAverageResult()
}
