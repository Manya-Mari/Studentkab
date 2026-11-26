package com.example.studentkab.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Сущность «Студент».
 *
 * @property id       Первичный ключ, генерируется автоматически.
 * @property name     Полное имя студента.
 * @property login    Уникальный логин для входа.
 * @property password Пароль (хранится в открытом виде для учебного проекта).
 */
@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val login: String = "",
    val password: String = ""
)
