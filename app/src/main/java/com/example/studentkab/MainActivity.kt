package com.example.studentkab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.studentkab.data.DatabaseProvider
import com.example.studentkab.data.Discipline
import com.example.studentkab.data.Grade
import com.example.studentkab.data.StudentDatabase
import com.example.studentkab.ui.theme.StudentkabTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            StudentkabTheme {
                val database = remember { DatabaseProvider.getDatabase(applicationContext) }
                val scope = rememberCoroutineScope()
                var disciplines by remember { mutableStateOf(emptyList<Discipline>()) }
                var grades by remember { mutableStateOf(emptyList<Grade>()) }
                var selectedDiscipline by remember { mutableStateOf<Discipline?>(null) }
                var showDisciplineDialog by remember { mutableStateOf(false) }
                var editingDiscipline by remember { mutableStateOf<Discipline?>(null) }
                var showGradeDialog by remember { mutableStateOf(false) }
                var editingGrade by remember { mutableStateOf<Grade?>(null) }
                var deletingDiscipline by remember { mutableStateOf<Discipline?>(null) }
                var deletingGrade by remember { mutableStateOf<Grade?>(null) }
                var errorMessage by remember { mutableStateOf<String?>(null) }

                suspend fun refresh() {
                    val result = withContext(Dispatchers.IO) {
                        database.disciplineDao().getAll()
                    }
                    disciplines = result
                    selectedDiscipline = selectedDiscipline?.let { current ->
                        result.find { it.id == current.id }
                    }
                    grades = selectedDiscipline?.let { discipline ->
                        withContext(Dispatchers.IO) {
                            database.gradeDao().getByDiscipline(discipline.id)
                        }
                    } ?: emptyList()
                }

                LaunchedEffect(Unit) {
                    withContext(Dispatchers.IO) {
                        DatabaseProvider.insertTestDataIfNeeded(database)
                    }
                    refresh()
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Личный кабинет успеваемости",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "Спринт 2 · Дисциплины и оценки",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Дисциплины", style = MaterialTheme.typography.titleLarge)
                            Button(onClick = {
                                editingDiscipline = null
                                showDisciplineDialog = true
                            }) {
                                Text("Добавить")
                            }
                        }

                        if (disciplines.isEmpty()) {
                            Text("Дисциплин пока нет. Добавьте первую дисциплину.")
                        }

                        disciplines.forEach { discipline ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedDiscipline = discipline
                                        scope.launch { refresh() }
                                    },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedDiscipline?.id == discipline.id)
                                        MaterialTheme.colorScheme.secondaryContainer
                                    else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(discipline.name, style = MaterialTheme.typography.titleMedium)
                                    Text(discipline.teacherName?.takeIf { it.isNotBlank() } ?: "Преподаватель не указан")
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        TextButton(onClick = {
                                            editingDiscipline = discipline
                                            showDisciplineDialog = true
                                        }) { Text("Изменить") }
                                        TextButton(onClick = {
                                            deletingDiscipline = discipline
                                        }) { Text("Удалить") }
                                        Button(onClick = {
                                            selectedDiscipline = discipline
                                            editingGrade = null
                                            errorMessage = null
                                            showGradeDialog = true
                                        }) { Text("Добавить оценку") }
                                    }
                                }
                            }
                        }

                        selectedDiscipline?.let { discipline ->
                            Spacer(Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Оценки: ${discipline.name}", style = MaterialTheme.typography.titleLarge)
                                Button(onClick = {
                                    editingGrade = null
                                    showGradeDialog = true
                                }) { Text("Добавить") }
                            }

                            if (grades.isEmpty()) {
                                Text("Оценок пока нет.")
                            }

                            grades.forEach { grade ->
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Оценка: ${grade.value}", style = MaterialTheme.typography.titleMedium)
                                        Text("Дата: ${grade.date}")
                                        Text("Тип работы: ${grade.workType}")
                                        if (grade.comment.isNotBlank()) Text("Комментарий: ${grade.comment}")
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            TextButton(onClick = {
                                                editingGrade = grade
                                                showGradeDialog = true
                                            }) { Text("Изменить") }
                                            TextButton(onClick = {
                                                deletingGrade = grade
                                            }) { Text("Удалить") }
                                        }
                                    }
                                }
                            }
                        }

                        errorMessage?.let {
                            Text(it, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }

                deletingDiscipline?.let { discipline ->
                    AlertDialog(
                        onDismissRequest = { deletingDiscipline = null },
                        title = { Text("Удалить дисциплину?") },
                        text = { Text("«${discipline.name}» и все связанные с ней оценки будут удалены.") },
                        confirmButton = {
                            TextButton(onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        database.disciplineDao().delete(discipline)
                                    }
                                    if (selectedDiscipline?.id == discipline.id) {
                                        selectedDiscipline = null
                                    }
                                    deletingDiscipline = null
                                    refresh()
                                }
                            }) { Text("Удалить") }
                        },
                        dismissButton = {
                            TextButton(onClick = { deletingDiscipline = null }) { Text("Отмена") }
                        }
                    )
                }

                deletingGrade?.let { grade ->
                    AlertDialog(
                        onDismissRequest = { deletingGrade = null },
                        title = { Text("Удалить оценку?") },
                        text = { Text("Запись с оценкой ${grade.value} будет удалена.") },
                        confirmButton = {
                            TextButton(onClick = {
                                scope.launch {
                                    withContext(Dispatchers.IO) {
                                        database.gradeDao().delete(grade)
                                    }
                                    deletingGrade = null
                                    refresh()
                                }
                            }) { Text("Удалить") }
                        },
                        dismissButton = {
                            TextButton(onClick = { deletingGrade = null }) { Text("Отмена") }
                        }
                    )
                }

                if (showDisciplineDialog) {
                    DisciplineDialog(
                        initial = editingDiscipline,
                        onDismiss = { showDisciplineDialog = false },
                        onSave = { name, teacher ->
                            scope.launch {
                                if (name.isBlank()) {
                                    errorMessage = "Введите название дисциплины."
                                    return@launch
                                }
                                withContext(Dispatchers.IO) {
                                    val current = editingDiscipline
                                    if (current == null) {
                                        database.disciplineDao().insert(
                                            Discipline(name = name.trim(), teacherName = teacher.trim().ifBlank { null })
                                        )
                                    } else {
                                        database.disciplineDao().update(
                                            current.copy(name = name.trim(), teacherName = teacher.trim().ifBlank { null })
                                        )
                                    }
                                }
                                errorMessage = null
                                showDisciplineDialog = false
                                refresh()
                            }
                        }
                    )
                }

                if (showGradeDialog && selectedDiscipline != null) {
                    GradeDialog(
                        initial = editingGrade,
                        onDismiss = { showGradeDialog = false },
                        onSave = { value, date, workType, comment ->
                            scope.launch {
                                val numericValue = value.toIntOrNull()
                                if (numericValue == null || numericValue !in 2..5) {
                                    errorMessage = "Оценка должна быть целым числом от 2 до 5."
                                    return@launch
                                }
                                if (date.isBlank() || workType.isBlank()) {
                                    errorMessage = "Заполните дату и тип работы."
                                    return@launch
                                }
                                withContext(Dispatchers.IO) {
                                    val current = editingGrade
                                    if (current == null) {
                                        database.gradeDao().insert(
                                            Grade(
                                                disciplineId = selectedDiscipline!!.id,
                                                value = numericValue,
                                                date = date.trim(),
                                                workType = workType.trim(),
                                                comment = comment.trim()
                                            )
                                        )
                                    } else {
                                        database.gradeDao().update(
                                            current.copy(
                                                value = numericValue,
                                                date = date.trim(),
                                                workType = workType.trim(),
                                                comment = comment.trim()
                                            )
                                        )
                                    }
                                }
                                errorMessage = null
                                showGradeDialog = false
                                refresh()
                            }
                        }
                    )
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun DisciplineDialog(
    initial: Discipline?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember(initial) { mutableStateOf(initial?.name.orEmpty()) }
    var teacher by remember(initial) { mutableStateOf(initial?.teacherName.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Новая дисциплина" else "Изменить дисциплину") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Название") }, singleLine = true)
                OutlinedTextField(teacher, { teacher = it }, label = { Text("Преподаватель") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(name, teacher) }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@androidx.compose.runtime.Composable
private fun GradeDialog(
    initial: Grade?,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String) -> Unit
) {
    var value by remember(initial) { mutableStateOf(initial?.value?.toString().orEmpty()) }
    var date by remember(initial) { mutableStateOf(initial?.date.orEmpty()) }
    var workType by remember(initial) { mutableStateOf(initial?.workType.orEmpty()) }
    var comment by remember(initial) { mutableStateOf(initial?.comment.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Новая оценка" else "Изменить оценку") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(value, { value = it }, label = { Text("Оценка (2–5)") }, singleLine = true)
                OutlinedTextField(date, { date = it }, label = { Text("Дата") }, singleLine = true)
                OutlinedTextField(workType, { workType = it }, label = { Text("Тип работы") }, singleLine = true)
                OutlinedTextField(comment, { comment = it }, label = { Text("Комментарий") })
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(value, date, workType, comment) }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}
