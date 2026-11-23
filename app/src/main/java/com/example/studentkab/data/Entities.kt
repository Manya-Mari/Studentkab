package com.example.studentkab.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "disciplines")
data class Discipline(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val teacherName: String? = null
)

@Entity(
    tableName = "grades",
    foreignKeys = [
        ForeignKey(
            entity = Discipline::class,
            parentColumns = ["id"],
            childColumns = ["disciplineId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["disciplineId"])]
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
