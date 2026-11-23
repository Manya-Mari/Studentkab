package com.example.studentkab.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface DisciplineDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(discipline: Discipline): Long

    @Query("SELECT * FROM disciplines ORDER BY name")
    suspend fun getAll(): List<Discipline>

    @Query("SELECT * FROM disciplines WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Discipline?

    @Update
    suspend fun update(discipline: Discipline)

    @Delete
    suspend fun delete(discipline: Discipline)
}

@Dao
interface GradeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(grade: Grade): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(grades: List<Grade>)

    @Query("SELECT * FROM grades ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<Grade>

    @Query("SELECT * FROM grades WHERE disciplineId = :disciplineId ORDER BY date DESC, id DESC")
    suspend fun getByDiscipline(disciplineId: Long): List<Grade>

    @Query("SELECT * FROM grades WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Grade?

    @Update
    suspend fun update(grade: Grade)

    @Delete
    suspend fun delete(grade: Grade)

    @Query("DELETE FROM grades WHERE disciplineId = :disciplineId")
    suspend fun deleteByDiscipline(disciplineId: Long)
}
