package com.example.studentkab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.studentkab.data.DatabaseProvider
import com.example.studentkab.ui.theme.StudentkabTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StudentkabTheme {
                var databaseStatus by remember { mutableStateOf("Проверка локальной базы данных...") }
                var disciplineCount by remember { mutableStateOf(0) }
                var gradeCount by remember { mutableStateOf(0) }

                LaunchedEffect(Unit) {
                    try {
                        val database = DatabaseProvider.getDatabase(applicationContext)
                        val counts = withContext(Dispatchers.IO) {
                            DatabaseProvider.insertTestDataIfNeeded(database)
                            database.disciplineDao().getAll().size to
                                database.gradeDao().getAll().size
                        }
                        disciplineCount = counts.first
                        gradeCount = counts.second
                        databaseStatus = "База Room работает. Тестовые записи загружены."
                    } catch (exception: Exception) {
                        databaseStatus = "Ошибка работы с базой данных: ${exception.localizedMessage}"
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Личный кабинет успеваемости студента",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            text = "Спринт 2. Модель данных и локальное хранение",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(text = databaseStatus)
                        Text(text = "Дисциплин в базе: $disciplineCount")
                        Text(text = "Оценок в базе: $gradeCount")
                        Text(
                            text = "Это технический экран проверки Room. Интерфейс журнала и расчёты будут реализованы в следующих спринтах.",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}
