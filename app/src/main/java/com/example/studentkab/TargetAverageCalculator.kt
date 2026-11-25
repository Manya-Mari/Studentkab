package com.example.studentkab

/**
 * Калькулятор для расчёта количества оценок, необходимых для достижения
 * целевого среднего балла по дисциплине.
 *
 * Алгоритм:
 * 1. Вычисляет, какую оценку нужно получить следующей, чтобы средний балл
 *    достиг [targetAverage]: `requiredGrade = targetAverage * (n+1) - sum`.
 * 2. Если требуемая оценка ≤ 5 — возвращает [TargetAverageResult.OneGradeEnough].
 * 3. Если целевой балл ровно 5 — математически невозможно достичь его конечным
 *    числом оценок при наличии хотя бы одной < 5; возвращает [TargetAverageResult.Impossible].
 * 4. Иначе — находит минимальное число будущих пятёрок по формуле:
 *    `k ≥ (target * n - sum) / (5 - target)`; возвращает [TargetAverageResult.MoreThanOneGrade].
 */
object TargetAverageCalculator {

    /**
     * Вычисляет результат достижения [targetAverage] при текущем наборе оценок [grades].
     *
     * @param targetAverage Желаемый средний балл (от 2.0 до 5.0 включительно).
     * @param grades        Список текущих оценок по дисциплине (каждая от 2 до 5).
     * @return Один из подтипов [TargetAverageResult], описывающий, что нужно сделать.
     */
    fun calculate(targetAverage: Double, grades: List<Int>): TargetAverageResult {
        // Проверяем корректность целевого среднего балла.
        if (!targetAverage.isFinite() || targetAverage !in 2.0..5.0) {
            return TargetAverageResult.InvalidTarget
        }
        // Проверяем, что все переданные оценки находятся в допустимом диапазоне.
        if (grades.any { it !in 2..5 }) {
            return TargetAverageResult.InvalidGrades
        }

        val count = grades.size       // текущее количество оценок
        val sum = grades.sum()        // сумма всех текущих оценок

        // Вычисляем, какой балл нужно получить следующим, чтобы достичь цели.
        // Формула: (sum + x) / (count + 1) = target  =>  x = target*(count+1) - sum
        val requiredNextGrade = targetAverage * (count + 1) - sum

        if (requiredNextGrade <= 5.0) {
            // Одной следующей оценки достаточно; округляем вверх, но не меньше 2.
            val roundedUpGrade = kotlin.math.ceil(requiredNextGrade).toInt().coerceAtLeast(2)
            return TargetAverageResult.OneGradeEnough(roundedUpGrade)
        }

        // При целевом среднем ровно 5.0 и наличии оценок ниже 5 математически
        // невозможно достичь его конечным числом оценок — среднее лишь стремится к 5.
        if (targetAverage == 5.0) {
            return TargetAverageResult.Impossible
        }

        // Находим минимальное k (количество будущих пятёрок), при котором:
        // (sum + 5*k) / (count + k) >= target
        // Решение: k >= (target*count - sum) / (5 - target)
        val needed = (targetAverage * count - sum) / (5.0 - targetAverage)
        // Небольшое смещение 1e-10 компенсирует погрешность вещественной арифметики.
        val roundedUpCount = kotlin.math.ceil(needed - 1e-10).toInt().coerceAtLeast(2)

        return TargetAverageResult.MoreThanOneGrade(roundedUpCount)
    }
}

/**
 * Запечатанная иерархия результатов расчёта целевого среднего балла.
 *
 * Используется в [TargetAverageCalculator.calculate] и обрабатывается
 * в `MainActivity.calculateTargetAverage()` через `when`-выражение.
 */
sealed class TargetAverageResult {

    /**
     * Одна следующая оценка решает задачу.
     *
     * @property requiredGrade Минимальный балл, который нужно получить (2–5).
     */
    data class OneGradeEnough(val requiredGrade: Int) : TargetAverageResult()

    /**
     * Одной оценки недостаточно; указывает минимальное количество пятёрок,
     * которые нужно получить подряд.
     *
     * @property requiredCount Минимальное число оценок «5», необходимых для достижения цели.
     */
    data class MoreThanOneGrade(val requiredCount: Int) : TargetAverageResult()

    /**
     * Достичь целевого среднего балла невозможно конечным числом оценок
     * (возникает, когда цель равна 5.0, а в истории уже есть оценки ниже 5).
     */
    object Impossible : TargetAverageResult()

    /**
     * Переданный целевой балл находится вне допустимого диапазона [2.0, 5.0].
     */
    object InvalidTarget : TargetAverageResult()

    /**
     * В переданном списке оценок обнаружены значения вне диапазона [2, 5].
     */
    object InvalidGrades : TargetAverageResult()
}
