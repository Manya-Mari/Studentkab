package com.example.studentkab.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Главная точка входа в локальную базу данных Room.
 *
 * Класс объявлен абстрактным — Room генерирует конкретную реализацию
 * во время компиляции с помощью аннотации [@Database].
 *
 * Параметры базы данных:
 * - [entities]      — список сущностей (таблиц): [Discipline] и [Grade].
 * - [version]       — текущая версия схемы. При изменении схемы нужно увеличить
 *                     номер и добавить [androidx.room.migration.Migration].
 * - [exportSchema]  — `false` означает, что Room не сохраняет JSON-файл схемы.
 *                     Установите `true` и настройте путь, если хотите отслеживать
 *                     историю миграций в системе контроля версий.
 */
@Database(
    entities = [Discipline::class, Grade::class],
    version = 1,
    exportSchema = false
)
abstract class StudentDatabase : RoomDatabase() {

    /** Возвращает DAO для операций с таблицей дисциплин. */
    abstract fun disciplineDao(): DisciplineDao

    /** Возвращает DAO для операций с таблицей оценок. */
    abstract fun gradeDao(): GradeDao
}
