package com.example.studentkab.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var INSTANCE: StudentDatabase? = null

    fun getDatabase(context: Context): StudentDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                StudentDatabase::class.java,
                "studentkab_database"
            ).build().also { INSTANCE = it }
        }
    }

    suspend fun insertTestDataIfNeeded(database: StudentDatabase) {
        if (database.disciplineDao().getAll().isNotEmpty()) return

        val programmingId = database.disciplineDao().insert(
            Discipline(name = "Основы программирования", teacherName = "Иванова А. С.")
        )
        val databasesId = database.disciplineDao().insert(
            Discipline(name = "Базы данных", teacherName = "Петров И. В.")
        )

        database.gradeDao().insertAll(
            listOf(
                Grade(
                    disciplineId = programmingId,
                    value = 5,
                    date = "2026-11-12",
                    workType = "Практическая работа",
                    comment = "Тестовая запись"
                ),
                Grade(
                    disciplineId = programmingId,
                    value = 4,
                    date = "2026-11-15",
                    workType = "Лабораторная работа"
                ),
                Grade(
                    disciplineId = databasesId,
                    value = 5,
                    date = "2026-11-18",
                    workType = "Контрольная работа"
                )
            )
        )
    }
}
