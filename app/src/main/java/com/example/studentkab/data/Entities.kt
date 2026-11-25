package com.example.studentkab.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Сущность «Дисциплина» — одна строка таблицы [disciplines] в базе данных.
 *
 * @property id          Первичный ключ, генерируется автоматически.
 * @property name        Название дисциплины (например, «Математический анализ»).
 * @property teacherName Имя преподавателя; может отсутствовать (null).
 */
@Entity(tableName = "disciplines")
data class Discipline(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val teacherName: String? = null
)

/**
 * Сущность «Оценка» — одна строка таблицы [grades] в базе данных.
 *
 * Связана с [Discipline] через внешний ключ [disciplineId].
 * При удалении дисциплины все связанные оценки удаляются автоматически
 * благодаря стратегии [ForeignKey.CASCADE].
 *
 * Индекс по столбцу [disciplineId] ускоряет выборку оценок по дисциплине.
 *
 * @property id           Первичный ключ, генерируется автоматически.
 * @property disciplineId Идентификатор дисциплины, к которой относится оценка.
 * @property value        Числовая оценка (от 2 до 5).
 * @property date         Дата выставления оценки в формате «ГГГГ-ММ-ДД».
 * @property workType     Тип учебной работы («Практическая», «Лабораторная», «Контрольная»).
 * @property comment      Произвольный комментарий; по умолчанию — пустая строка.
 */
@Entity(
    tableName = "grades",
    foreignKeys = [
        ForeignKey(
            entity = Discipline::class,
            parentColumns = ["id"],
            childColumns = ["disciplineId"],
            onDelete = ForeignKey.CASCADE   // при удалении дисциплины — каскадное удаление оценок
        )
    ],
    indices = [Index(value = ["disciplineId"])]  // индекс для быстрой фильтрации по дисциплине
)
data class Grade(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val disciplineId: Long,
    val value: Int,
    val date: String,
    val workType: String,
    val comment: String = ""
)
