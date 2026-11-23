package com.example.studentkab.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Discipline::class, Grade::class],
    version = 1,
    exportSchema = false
)
abstract class StudentDatabase : RoomDatabase() {
    abstract fun disciplineDao(): DisciplineDao
    abstract fun gradeDao(): GradeDao
}
