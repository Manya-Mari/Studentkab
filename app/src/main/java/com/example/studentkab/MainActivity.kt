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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
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

                /**
                 * Загружает дисциплины. Если [selectedDiscipline] == null,
                 * автоматически выбирает первую — чтобы секция «Оценки»
                 * сразу отрисовывалась на экране.
                 */
                suspend fun loadDisciplines() {
                    val result = withContext(Dispatchers.IO) {
                        database.disciplineDao().getAll()
                    }
                    disciplines = result

                    if (selectedDiscipline == null) {
                        selectedDiscipline = result.firstOrNull()
                    } else {
                        // Обновить выбранную дисциплину по id (могла измениться)
                        selectedDiscipline = result.find { it.id == selectedDiscipline?.id }
                    }
                }

                /**
                 * Загружает оценки для конкретной дисциплины.
                 */
                suspend fun loadGrades(disciplineId: Long?) {
                    grades = if (disciplineId == null) {
                        emptyList()
                    } else {
                        withContext(Dispatchers.IO) {
                            database.gradeDao().getByDiscipline(disciplineId)
                        }
                    }
                }

                suspend fun refresh() {
                    loadDisciplines()
                    loadGrades(selectedDiscipline?.id)
                }

                // 1. Первичная загрузка: показываем то, что есть в БД
                LaunchedEffect(Unit) {
                    refresh()
                    // 2. Одноразовый сидинг
                    withContext(Dispatchers.IO) {
                        DatabaseProvider.insertTestDataIfNeeded(applicationContext, database)
                    }
                    // 3. Повторная загрузка после сидинга
                    refresh()
                }

                // 4. Реакция на смену выбранной дисциплины: подгружаем её оценки.
                //    Сработает и при автовыборе первой дисциплины после сидинга.
                LaunchedEffect(selectedDiscipline?.id) {
                    loadGrades(selectedDiscipline?.id)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "Личный кабинет",
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Text(
                            "Успеваемость студента",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Дисциплины", style = MaterialTheme.typography.titleLarge)
                            Button(
                                modifier = Modifier.fillMaxWidth(),
                                onClick = {
                                    editingDiscipline = null
                                    showDisciplineDialog = true
                                }
                            ) {
                                Text("Добавить дисциплину")
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
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Button(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = {
                                                editingDiscipline = discipline
                                                showDisciplineDialog = true
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.secondary,
                                                contentColor = MaterialTheme.colorScheme.onSecondary
                                            )
                                        ) { Text("Изменить дисциплину") }

                                        Button(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = { deletingDiscipline = discipline },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = MaterialTheme.colorScheme.error,
                                                contentColor = MaterialTheme.colorScheme.onError
                                            )
                                        ) { Text("Удалить дисциплину") }

                                        Button(
                                            modifier = Modifier.fillMaxWidth(),
                                            onClick = {
                                                selectedDiscipline = discipline
                                                editingGrade = null
                                                errorMessage = null
                                                showGradeDialog = true
                                            }
                                        ) { Text("Добавить оценку") }
                                    }
                                }
                            }
                        }

                        selectedDiscipline?.let { discipline ->
                            Spacer(Modifier.height(4.dp))
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Оценки",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    discipline.name,
                                    style = MaterialTheme.typography.titleMedium
                                )
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
                                            TextButton(
                                                onClick = {
                                                    editingGrade = grade
                                                    showGradeDialog = true
                                                },
                                                colors = ButtonDefaults.textButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.secondary
                                                )
                                            ) { Text("Изменить") }

                                            TextButton(
                                                onClick = { deletingGrade = grade },
                                                colors = ButtonDefaults.textButtonColors(
                                                    contentColor = MaterialTheme.colorScheme.error
                                                )
                                            ) { Text("Удалить") }
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
                            TextButton(
                                onClick = {
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
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) { Text("Удалить") }
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
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        withContext(Dispatchers.IO) {
                                            database.gradeDao().delete(grade)
                                        }
                                        deletingGrade = null
                                        refresh()
                                    }
                                },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = MaterialTheme.colorScheme.error
                                )
                            ) { Text("Удалить") }
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
                                val numericValue = value.trim().toInt()
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
    var localError by remember(initial) { mutableStateOf<String?>(null) }

    val nameError = localError?.takeIf { it.contains("Название", ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Новая дисциплина" else "Изменить дисциплину") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; localError = null },
                    label = { Text("Название") },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = {
                        Text(
                            text = nameError ?: "Обязательное поле. Например: «Основы программирования»",
                            color = if (nameError != null)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                OutlinedTextField(
                    value = teacher,
                    onValueChange = { teacher = it; localError = null },
                    label = { Text("Преподаватель") },
                    singleLine = true,
                    supportingText = {
                        Text("Необязательное поле. Например: «Иванова А. С.»")
                    }
                )

                localError?.let { message ->
                    if (message.isNotBlank() && nameError == null) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val trimmedName = name.trim()
                if (trimmedName.isEmpty()) {
                    localError = "Название дисциплины обязательно. Введите хотя бы одно слово."
                    return@TextButton
                }
                if (trimmedName.length < 2) {
                    localError = "Название слишком короткое — введите минимум 2 символа."
                    return@TextButton
                }
                if (trimmedName.length > 100) {
                    localError = "Название слишком длинное — максимум 100 символов."
                    return@TextButton
                }
                onSave(name, teacher)
            }) { Text("Сохранить") }
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

    var localError by remember(initial) { mutableStateOf<String?>(null) }

    val dateError = localError?.takeIf { it.contains("дат", ignoreCase = true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Новая оценка" else "Изменить оценку") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = value,
                    onValueChange = { value = it; localError = null },
                    label = { Text("Оценка (2–5)") },
                    singleLine = true,
                    isError = localError?.contains("Оценка", ignoreCase = true) == true
                )

                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it; localError = null },
                    label = { Text("Дата") },
                    singleLine = true,
                    isError = dateError != null,
                    supportingText = {
                        Text(
                            text = dateError
                                ?: "Формат: ГГГГ-ММ-ДД, например 2026-11-15",
                            color = if (dateError != null)
                                MaterialTheme.colorScheme.error
                            else
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                OutlinedTextField(
                    value = workType,
                    onValueChange = { workType = it; localError = null },
                    label = { Text("Тип работы") },
                    singleLine = true,
                    isError = localError?.contains("Тип работы", ignoreCase = true) == true
                )

                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Комментарий") }
                )

                localError?.let { message ->
                    if (message.isNotBlank()) {
                        Text(
                            text = message,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val numericValue = value.trim().toIntOrNull()
                if (numericValue == null || numericValue !in 2..5) {
                    localError = "Оценка должна быть целым числом от 2 до 5."
                    return@TextButton
                }
                if (workType.isBlank()) {
                    localError = "Заполните тип работы (например, «Практическая работа»)."
                    return@TextButton
                }
                val dateProblem = validateDate(date)
                if (dateProblem != null) {
                    localError = dateProblem
                    return@TextButton
                }
                onSave(value, date, workType, comment)
            }) { Text("Сохранить") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

private fun validateDate(input: String): String? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) {
        return "Введите дату в формате ГГГГ-ММ-ДД, например 2026-11-15."
    }
    val regex = Regex("""^\d{4}-\d{2}-\d{2}$""")
    if (!regex.matches(trimmed)) {
        return "Неверный формат даты. Нужно ГГГГ-ММ-ДД, например 2026-11-15."
    }
    val (yearStr, monthStr, dayStr) = trimmed.split("-")
    val year = yearStr.toInt()
    val month = monthStr.toInt()
    val day = dayStr.toInt()

    if (month !in 1..12) {
        return "Месяц должен быть от 01 до 12. Пример: 2026-11-15."
    }
    val isLeap = (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0)
    val daysInMonth = when (month) {
        1, 3, 5, 7, 8, 10, 12 -> 31
        4, 6, 9, 11 -> 30
        2 -> if (isLeap) 29 else 28
        else -> 0
    }
    if (day !in 1..daysInMonth) {
        return "В месяце $monthStr не может быть дня $dayStr. Пример корректной даты: 2026-11-15."
    }
    return null
}