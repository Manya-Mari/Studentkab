package com.example.studentkab

/**
 * Определяет, можно ли достичь целевого среднего одной следующей оценкой.
 * Если одной оценки недостаточно, рассчитывает минимальное число будущих
 * оценок при условии, что каждая из них будет равна 5.
 */
object TargetAverageCalculator {

    fun calculate(targetAverage: Double, grades: List<Int>): TargetAverageResult {
        if (!targetAverage.isFinite() || targetAverage !in 2.0..5.0) {
            return TargetAverageResult.InvalidTarget
        }
        if (grades.any { it !in 2..5 }) {
            return TargetAverageResult.InvalidGrades
        }

        val count = grades.size
        val sum = grades.sum()
        val requiredNextGrade = targetAverage * (count + 1) - sum

        if (requiredNextGrade <= 5.0) {
            val roundedUpGrade = kotlin.math.ceil(requiredNextGrade).toInt().coerceAtLeast(2)
            return TargetAverageResult.OneGradeEnough(roundedUpGrade)
        }

        // При целевом среднем 5 и уже имеющихся оценках ниже 5
        // достичь ровно 5 конечным количеством оценок невозможно.
        if (targetAverage == 5.0) {
            return TargetAverageResult.Impossible
        }

        // Минимальное k, при котором (sum + 5*k) / (count + k) >= target.
        val needed = (targetAverage * count - sum) / (5.0 - targetAverage)
        val roundedUpCount = kotlin.math.ceil(needed - 1e-10).toInt().coerceAtLeast(2)

        return TargetAverageResult.MoreThanOneGrade(roundedUpCount)
    }
}

sealed class TargetAverageResult {
    data class OneGradeEnough(val requiredGrade: Int) : TargetAverageResult()
    data class MoreThanOneGrade(val requiredCount: Int) : TargetAverageResult()
    object Impossible : TargetAverageResult()
    object InvalidTarget : TargetAverageResult()
    object InvalidGrades : TargetAverageResult()
}
