package com.example.studentkab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetAverageCalculatorTest {

    @Test
    fun rejectsTargetBelowRange() {
        assertTrue(TargetAverageCalculator.calculate(1.9, listOf(4)) is TargetAverageResult.InvalidTarget)
    }

    @Test
    fun rejectsTargetAboveRange() {
        assertTrue(TargetAverageCalculator.calculate(5.1, listOf(4)) is TargetAverageResult.InvalidTarget)
    }

    @Test
    fun rejectsNanTarget() {
        assertTrue(TargetAverageCalculator.calculate(Double.NaN, listOf(4)) is TargetAverageResult.InvalidTarget)
    }

    @Test
    fun emptyGradeListNeedsOneGradeEqualToTargetRoundedUp() {
        assertEquals(
            TargetAverageResult.OneGradeEnough(5),
            TargetAverageCalculator.calculate(4.5, emptyList())
        )
    }

    @Test
    fun oneGradeIsEnoughWhenRequiredGradeIsInteger() {
        assertEquals(
            TargetAverageResult.OneGradeEnough(5),
            TargetAverageCalculator.calculate(4.5, listOf(4))
        )
    }

    @Test
    fun roundsRequiredNextGradeUp() {
        assertEquals(
            TargetAverageResult.OneGradeEnough(5),
            TargetAverageCalculator.calculate(4.5, listOf(5, 4))
        )
    }

    @Test
    fun reportsTwoRequiredGradesWhenOneIsNotEnough() {
        assertEquals(
            TargetAverageResult.MoreThanOneGrade(2),
            TargetAverageCalculator.calculate(4.5, listOf(4, 4))
        )
    }

    @Test
    fun roundsRequiredNumberOfGradesUp() {
        assertEquals(
            TargetAverageResult.MoreThanOneGrade(3),
            TargetAverageCalculator.calculate(4.5, listOf(3, 3, 3))
        )
    }

    @Test
    fun targetFiveWithExistingLowerGradeIsImpossible() {
        assertTrue(TargetAverageCalculator.calculate(5.0, listOf(4, 5)) is TargetAverageResult.Impossible)
    }

    @Test
    fun targetFiveWithNoGradesNeedsOneFive() {
        assertEquals(
            TargetAverageResult.OneGradeEnough(5),
            TargetAverageCalculator.calculate(5.0, emptyList())
        )
    }

    @Test
    fun rejectsInvalidExistingGrades() {
        assertTrue(TargetAverageCalculator.calculate(4.0, listOf(3, 6)) is TargetAverageResult.InvalidGrades)
    }
}
