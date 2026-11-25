package com.example.studentkab.data

import android.content.Context
import androidx.room.Room

/**
 * Синглтон-провайдер для доступа к [StudentDatabase].
 *
 * Гарантирует, что во всём приложении существует ровно один экземпляр базы данных.
 * Потокобезопасность обеспечена аннотацией [@Volatile] и блоком [synchronized].
 */
object DatabaseProvider {

    /**
     * Единственный экземпляр базы данных.
     * [@Volatile] гарантирует, что запись в переменную сразу видна всем потокам.
     */
    @Volatile
    private var INSTANCE: StudentDatabase? = null

    /** Имя файла SharedPreferences, в котором хранится флаг первичного заполнения. */
    private const val PREFS_NAME = "studentkab_prefs"

    /** Ключ булевого флага «база уже была заполнена тестовыми данными». */
    private const val KEY_SEEDED = "seeded_v1"

    /**
     * Возвращает единственный экземпляр [StudentDatabase], создавая его при первом вызове.
     *
     * Использует паттерн «двойная проверка блокировки» (double-checked locking):
     * первая проверка без синхронизации — быстрый путь для уже созданного экземпляра;
     * вторая проверка внутри [synchronized] — защита от гонки при параллельном первом вызове.
     *
     * @param context Контекст приложения (не Activity, чтобы избежать утечки памяти).
     * @return Готовый к использованию экземпляр базы данных.
     */
    fun getDatabase(context: Context): StudentDatabase {
        return INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                StudentDatabase::class.java,
                "studentkab_database"      // имя файла базы данных на диске
            ).build().also { INSTANCE = it }
        }
    }

    /**
     * Одноразовый сидинг (начальное заполнение) базы данных тестовыми данными.
     *
     * Выполняется **только один раз** — при самом первом запуске приложения после
     * установки или очистки данных. Флаг [KEY_SEEDED] в [SharedPreferences] предотвращает
     * повторное добавление записей после перезапуска приложения.
     *
     * Логика:
     * 1. Если флаг уже установлен — выходим сразу (ничего не делаем).
     * 2. Если в базе уже есть дисциплины — устанавливаем флаг и выходим
     *    (пользователь мог добавить данные сам до первого сидинга).
     * 3. Иначе — добавляем одну тестовую дисциплину с двумя оценками
     *    и устанавливаем флаг.
     *
     * Метод помечен `suspend` и должен вызываться из корутины
     * с диспетчером [kotlinx.coroutines.Dispatchers.IO].
     *
     * @param context  Контекст приложения для доступа к [SharedPreferences].
     * @param database Экземпляр базы данных, куда будут добавлены записи.
     */
    suspend fun insertTestDataIfNeeded(context: Context, database: StudentDatabase) {
        val prefs = context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // Флаг уже установлен — сидинг уже был выполнен ранее.
        if (prefs.getBoolean(KEY_SEEDED, false)) return

        // Если в базе уже что-то есть — просто ставим флаг и выходим.
        if (database.disciplineDao().getAll().isNotEmpty()) {
            prefs.edit().putBoolean(KEY_SEEDED, true).apply()
            return
        }

        // Вставляем тестовую дисциплину и получаем её автоматически присвоенный ID.
        val programmingId = database.disciplineDao().insert(
            Discipline(name = "Основы программирования", teacherName = "Иванова А. С.")
        )

        // Добавляем две тестовые оценки, привязанные к созданной дисциплине.
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

        // Помечаем, что сидинг выполнен — при следующем запуске пропустим этот шаг.
        prefs.edit().putBoolean(KEY_SEEDED, true).apply()
    }
}
