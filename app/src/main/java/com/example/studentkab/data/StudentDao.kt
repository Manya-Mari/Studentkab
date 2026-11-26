package com.example.studentkab.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/** DAO для работы с таблицей студентов. */
@Dao
interface StudentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(student: Student): Long

    @Update
    suspend fun update(student: Student)

    @Delete
    suspend fun delete(student: Student)

    @Query("SELECT * FROM students ORDER BY name")
    suspend fun getAll(): List<Student>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Student?

    /** Ищет студента по логину и паролю. Возвращает null, если не найден. */
    @Query("SELECT * FROM students WHERE login = :login AND password = :password LIMIT 1")
    suspend fun findByCredentials(login: String, password: String): Student?
}
