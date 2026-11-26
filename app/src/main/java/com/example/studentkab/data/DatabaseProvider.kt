package com.example.studentkab.data

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Синглтон-провайдер для доступа к [StudentDatabase].
 */
object DatabaseProvider {

    @Volatile
    private var INSTANCE: StudentDatabase? = null

    private const val PREFS_NAME = "studentkab_prefs"
    private const val KEY_SEEDED = "seeded_v4"

    /** Миграция 2 → 3: добавляем login и password в таблицу students. */
    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE `students` ADD COLUMN `login` TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE `students` ADD COLUMN `password` TEXT NOT NULL DEFAULT ''")
        }
    }

    /**
     * Миграция с версии 1 → 2: добавляем таблицу студентов и поле studentId в disciplines.
     */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(db: SupportSQLiteDatabase) {
            // 1. Создаём таблицу студентов
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `students` " +
                "(`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL)"
            )

            // 2. Создаём временную таблицу дисциплин с полной новой схемой
            // (SQLite не поддерживает добавление FK через ALTER TABLE,
            // поэтому пересоздаём таблицу вручную)
            db.execSQL(
                "CREATE TABLE `disciplines_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                "`name` TEXT NOT NULL, " +
                "`teacherName` TEXT, " +
                "`studentId` INTEGER NOT NULL DEFAULT 1, " +
                "FOREIGN KEY(`studentId`) REFERENCES `students`(`id`) ON DELETE CASCADE)"
            )

            // 3. Переносим данные из старой таблицы
            db.execSQL(
                "INSERT INTO `disciplines_new` (`id`, `name`, `teacherName`, `studentId`) " +
                "SELECT `id`, `name`, `teacherName`, 1 FROM `disciplines`"
            )

            // 4. Удаляем старую таблицу
            db.execSQL("DROP TABLE `disciplines`")

            // 5. Переименовываем новую
            db.execSQL("ALTER TABLE `disciplines_new` RENAME TO `disciplines`")

            // 6. Создаём индекс (Room требует его наличие)
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_disciplines_studentId` " +
                "ON `disciplines` (`studentId`)"
            )
        }
    }

    fun getDatabase(context: Context): StudentDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                StudentDatabase::class.java,
                "studentkab_database"
            )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                .build()
                .also { INSTANCE = it }
        }
    }

    /**
     * Одноразовый сидинг: создаёт 3 тестовых студентов, каждый со своими
     * дисциплинами и оценками.
     */
    suspend fun insertTestDataIfNeeded(context: Context, database: StudentDatabase) {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        if (prefs.getBoolean(KEY_SEEDED, false)) return
        // ── Студент 1: Алексей Петров ──────────────────────────────────────
        val student1Id = database.studentDao().insert(
            Student(name = "Алексей Петров", login = "petrov", password = "pass123")
        )

        val mathId = database.disciplineDao().insert(
            Discipline(name = "Математический анализ", teacherName = "Соколов В. И.", studentId = student1Id)
        )
        val physicsId = database.disciplineDao().insert(
            Discipline(name = "Физика", teacherName = "Новиков А. Р.", studentId = student1Id)
        )
        val progId = database.disciplineDao().insert(
            Discipline(name = "Программирование", teacherName = "Иванова А. С.", studentId = student1Id)
        )

        database.gradeDao().insertAll(listOf(
            Grade(disciplineId = mathId, value = 5, date = "2026-09-10", workType = "Практическая", comment = "Отлично!"),
            Grade(disciplineId = mathId, value = 4, date = "2026-10-01", workType = "Контрольная"),
            Grade(disciplineId = mathId, value = 3, date = "2026-11-05", workType = "Лабораторная", comment = "Слабо"),
            Grade(disciplineId = physicsId, value = 4, date = "2026-09-20", workType = "Практическая"),
            Grade(disciplineId = physicsId, value = 5, date = "2026-10-15", workType = "Лабораторная"),
            Grade(disciplineId = progId, value = 5, date = "2026-09-15", workType = "Лабораторная"),
            Grade(disciplineId = progId, value = 5, date = "2026-11-01", workType = "Контрольная", comment = "Лучший результат в группе")
        ))

        // ── Студент 2: Мария Сидорова ──────────────────────────────────────
        val student2Id = database.studentDao().insert(
            Student(name = "Мария Сидорова", login = "sidorova", password = "pass123")
        )

        val histId = database.disciplineDao().insert(
            Discipline(name = "История", teacherName = "Белова О. Н.", studentId = student2Id)
        )
        val engId = database.disciplineDao().insert(
            Discipline(name = "Английский язык", teacherName = "Козлова М. А.", studentId = student2Id)
        )
        val econId = database.disciplineDao().insert(
            Discipline(name = "Экономика", teacherName = "Федоров Д. Л.", studentId = student2Id)
        )

        database.gradeDao().insertAll(listOf(
            Grade(disciplineId = histId, value = 5, date = "2026-09-12", workType = "Контрольная"),
            Grade(disciplineId = histId, value = 5, date = "2026-10-20", workType = "Практическая", comment = "Превосходно"),
            Grade(disciplineId = engId, value = 3, date = "2026-09-25", workType = "Контрольная"),
            Grade(disciplineId = engId, value = 4, date = "2026-10-30", workType = "Практическая"),
            Grade(disciplineId = econId, value = 4, date = "2026-09-18", workType = "Лабораторная"),
            Grade(disciplineId = econId, value = 3, date = "2026-11-10", workType = "Контрольная", comment = "Нужно подтянуть")
        ))

        // ── Студент 3: Дмитрий Козлов ──────────────────────────────────────
        val student3Id = database.studentDao().insert(
            Student(name = "Дмитрий Козлов", login = "kozlov", password = "pass123")
        )

        val chemId = database.disciplineDao().insert(
            Discipline(name = "Химия", teacherName = "Морозова Г. С.", studentId = student3Id)
        )
        val bioId = database.disciplineDao().insert(
            Discipline(name = "Биология", teacherName = "Захаров П. В.", studentId = student3Id)
        )
        val statId = database.disciplineDao().insert(
            Discipline(name = "Статистика", teacherName = "Волков И. О.", studentId = student3Id)
        )

        database.gradeDao().insertAll(listOf(
            Grade(disciplineId = chemId, value = 2, date = "2026-09-08", workType = "Лабораторная", comment = "Не сдана"),
            Grade(disciplineId = chemId, value = 3, date = "2026-10-10", workType = "Контрольная"),
            Grade(disciplineId = bioId, value = 4, date = "2026-09-22", workType = "Практическая"),
            Grade(disciplineId = bioId, value = 5, date = "2026-10-28", workType = "Лабораторная", comment = "Прогресс налицо"),
            Grade(disciplineId = statId, value = 4, date = "2026-09-30", workType = "Практическая"),
            Grade(disciplineId = statId, value = 4, date = "2026-11-15", workType = "Контрольная")
        ))

        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }
}
