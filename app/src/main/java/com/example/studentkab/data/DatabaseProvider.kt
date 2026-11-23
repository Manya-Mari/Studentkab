package com.example.studentkab.data

import android.content.Context
import androidx.room.Room

object DatabaseProvider {
    @Volatile
    private var INSTANCE: StudentDatabase? = null

    private const val PREFS_NAME = "studentkab_prefs"
    private const val KEY_SEEDED = "seeded_v1"

    fun getDatabase(context: Context): StudentDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                StudentDatabase::class.java,
                "studentkab_database"
            ).build().also { INSTANCE = it }
        }
    }

    /**
     * Одноразовый сидинг: выполняется только при самом первом запуске
     * после установки/очистки данных. Флаг хранится в SharedPreferences,
     * поэтому удалённые пользователем записи не возвращаются при перезапуске.
     */
    suspend fun insertTestDataIfNeeded(context: Context, database: StudentDatabase) {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (prefs.getBoolean(KEY_SEEDED, false)) return

        // Если в базе уже что-то есть — просто ставим флаг и выходим.
        if (database.disciplineDao().getAll().isNotEmpty()) {
            prefs.edit().putBoolean(KEY_SEEDED, true).apply()
            return
        }

        // Только «Основы программирования» — «Базы данных» больше не создаём
        val programmingId = database.disciplineDao().insert(
            Discipline(name = "Основы программирования", teacherName = "Иванова А. С.")
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
                )
            )
        )

        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }
}