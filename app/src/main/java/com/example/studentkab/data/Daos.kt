package com.example.studentkab.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

/**
 * DAO (Data Access Object) для работы с таблицей дисциплин.
 *
 * Все методы помечены `suspend` и должны вызываться из корутины
 * (например, через [kotlinx.coroutines.withContext] с [kotlinx.coroutines.Dispatchers.IO]).
 */
@Dao
interface DisciplineDao {

    /**
     * Вставляет новую дисциплину.
     * При конфликте первичного ключа запись заменяется.
     *
     * @return Автоматически присвоенный идентификатор новой записи.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(discipline: Discipline): Long

    /**
     * Возвращает все дисциплины, отсортированные по имени (A → Z / А → Я).
     */
    @Query("SELECT * FROM disciplines ORDER BY name")
    suspend fun getAll(): List<Discipline>

    /**
     * Возвращает дисциплину по её идентификатору или `null`, если запись не найдена.
     *
     * @param id Первичный ключ дисциплины.
     */
    @Query("SELECT * FROM disciplines WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Discipline?

    /**
     * Обновляет существующую дисциплину (сопоставление по первичному ключу).
     */
    @Update
    suspend fun update(discipline: Discipline)

    /**
     * Удаляет дисциплину. Благодаря каскаду, определённому во внешнем ключе
     * [Grade.disciplineId], все связанные оценки также будут удалены.
     */
    @Delete
    suspend fun delete(discipline: Discipline)
}

/**
 * DAO для работы с таблицей оценок.
 *
 * Все методы помечены `suspend` и должны вызываться из корутины.
 */
@Dao
interface GradeDao {

    /**
     * Вставляет одну оценку.
     * При конфликте первичного ключа запись заменяется.
     *
     * @return Автоматически присвоенный идентификатор новой записи.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(grade: Grade): Long

    /**
     * Вставляет список оценок за одну транзакцию.
     * Используется при первичном заполнении базы тестовыми данными.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(grades: List<Grade>)

    /**
     * Возвращает все оценки из базы, отсортированные сначала по убыванию даты,
     * затем по убыванию идентификатора (новые записи — первыми).
     */
    @Query("SELECT * FROM grades ORDER BY date DESC, id DESC")
    suspend fun getAll(): List<Grade>

    /**
     * Возвращает все оценки по указанной дисциплине, отсортированные по дате и ID.
     *
     * @param disciplineId Идентификатор дисциплины-владельца.
     */
    @Query("SELECT * FROM grades WHERE disciplineId = :disciplineId ORDER BY date DESC, id DESC")
    suspend fun getByDiscipline(disciplineId: Long): List<Grade>

    /**
     * Возвращает оценку по её идентификатору или `null`, если запись не найдена.
     *
     * @param id Первичный ключ оценки.
     */
    @Query("SELECT * FROM grades WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Grade?

    /**
     * Обновляет существующую оценку (сопоставление по первичному ключу).
     */
    @Update
    suspend fun update(grade: Grade)

    /**
     * Удаляет одну оценку по объекту.
     */
    @Delete
    suspend fun delete(grade: Grade)

    /**
     * Удаляет все оценки, принадлежащие указанной дисциплине.
     * Вызывается вручную там, где каскадное удаление не срабатывает.
     *
     * @param disciplineId Идентификатор дисциплины.
     */
    @Query("DELETE FROM grades WHERE disciplineId = :disciplineId")
    suspend fun deleteByDiscipline(disciplineId: Long)
}
