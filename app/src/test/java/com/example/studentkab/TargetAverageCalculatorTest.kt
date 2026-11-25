package com.example.studentkab

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetAverageCalculatorTest {

    @Test
    fun targetBelowMinimumIsRejected() {
        assertTrue(TargetAverageCalculator.calculate(1.9, listOf(4)) is TargetAverageResult.InvalidTarget)
    }

    @Test
    fun targetAboveMaximumIsRejected() {
        assertTrue(TargetAverageCalculator.calculate(5.1, listOf(4)) is TargetAverageResult.InvalidTarget)
    }

    @Test
    fun nanTargetIsRejected() {
        assertTrue(TargetAverageCalculator.calculate(Double.NaN, listOf(4)) is TargetAverageResult.InvalidTarget)
    }

    @Test
    fun firstGradeCanEqualIntegerTarget() {
        assertEquals(
            TargetAverageResult.Success(4),
            TargetAverageCalculator.calculate(4.0, emptyList())
        )
    }

    @Test
    fun calculatesRequiredGradeForOneExistingGrade() {
        assertEquals(
            TargetAverageResult.Success(5),
            TargetAverageCalculator.calculate(4.5, listOf(4))
        )
    }

    @Test
    fun calculatesRequiredGradeForSeveralExistingGrades() {
        assertEquals(
            TargetAverageResult.Success(5),
            TargetAverageCalculator.calculate(4.0, listOf(3, 4, 4))
        )
    }

    @Test
    fun reportsGradeAboveMaximumAsUnreachable() {
        val result = TargetAverageCalculator.calculate(4.5, listOf(4, 4))
        assertTrue(result is TargetAverageResult.OutOfRange)
        assertEquals(5.5, (result as TargetAverageResult.OutOfRange).requiredGrade, 0.000001)
    }

    @Test
    fun reportsGradeBelowMinimumAsUnreachable() {
        val result = TargetAverageCalculator.calculate(2.0, listOf(5, 5))
        assertTrue(result is TargetAverageResult.OutOfRange)
        assertEquals(-4.0, (result as TargetAverageResult.OutOfRange).requiredGrade, 0.000001)
    }

    @Test
    fun reportsNonIntegerRequiredGrade() {
        val result = TargetAverageCalculator.calculate(4.5, listOf(5, 4))
        assertTrue(result is TargetAverageResult.NonInteger)
        assertEquals(4.5, (result as TargetAverageResult.NonInteger).requiredGrade, 0.000001)
    }

    @Test
    fun acceptsMinimumGradeBoundary() {
        assertEquals(
            TargetAverageResult.Success(2),
            TargetAverageCalculator.calculate(2.0, listOf(2, 2))
        )
    }

    @Test
    fun acceptsMaximumGradeBoundary() {
        assertEquals(
            TargetAverageResult.Success(5),
            TargetAverageCalculator.calculate(5.0, listOf(5, 5))
        )
    }

    @Test
    fun rejectsInvalidExistingGradeValues() {
        assertTrue(
            TargetAverageCalculator.calculate(4.0, listOf(3, 6)) is TargetAverageResult.InvalidGrades
        )
    }
}
