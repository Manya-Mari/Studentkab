package com.example.studentkab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.studentkab.ui.theme.StudentkabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StudentkabTheme {
                StudentkabPrototype()
            }
        }
    }
}

private enum class AppScreen {
    Home, Subjects, SubjectDetails, GradeForm, Analytics, GoalCalculator
}

private data class DemoGrade(
    val value: Int,
    val date: String,
    val workType: String,
    val comment: String
)

private data class DemoDiscipline(
    val id: Int,
    val name: String,
    val teacher: String,
    val average: String,
    val grades: List<DemoGrade>
)

private val demoDisciplines = listOf(
    DemoDiscipline(
        id = 1,
        name = "Основы программирования",
        teacher = "Иванова А. С.",
        average = "4,6",
        grades = listOf(
            DemoGrade(5, "12.11.2026", "Практическая работа", "Работа выполнена"),
            DemoGrade(4, "08.11.2026", "Лабораторная работа", "Небольшие замечания"),
            DemoGrade(5, "02.11.2026", "Ответ на занятии", "")
        )
    ),
    DemoDiscipline(
        id = 2,
        name = "Базы данных",
        teacher = "Петров И. В.",
        average = "4,0",
        grades = listOf(
            DemoGrade(4, "14.11.2026", "Практическая работа", ""),
            DemoGrade(4, "06.11.2026", "Контрольная работа", "")
        )
    ),
    DemoDiscipline(
        id = 3,
        name = "Математика",
        teacher = "Смирнова Е. П.",
        average = "3,8",
        grades = listOf(
            DemoGrade(3, "13.11.2026", "Контрольная работа", "Повторить тему"),
            DemoGrade(4, "05.11.2026", "Ответ на занятии", ""),
            DemoGrade(4, "29.10.2026", "Практическая работа", "")
        )
    )
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StudentkabPrototype() {
    var screen by rememberSaveable { mutableStateOf(AppScreen.Home) }
    var selectedDisciplineId by rememberSaveable { mutableStateOf(1) }

    val selectedDiscipline = demoDisciplines.firstOrNull { it.id == selectedDisciplineId }
        ?: demoDisciplines.first()

    val title = when (screen) {
        AppScreen.Home -> "Успеваемость"
        AppScreen.Subjects -> "Мои дисциплины"
        AppScreen.SubjectDetails -> selectedDiscipline.name
        AppScreen.GradeForm -> "Новая оценка"
        AppScreen.Analytics -> "Динамика"
        AppScreen.GoalCalculator -> "Целевой балл"
    }

    val rootScreens = setOf(AppScreen.Home, AppScreen.Subjects, AppScreen.Analytics)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(title, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = "Демонстрационный прототип · Спринт 1",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                navigationIcon = {
                    if (screen !in rootScreens) {
                        Text(
                            text = "‹",
                            modifier = Modifier
                                .clickable {
                                    screen = when (screen) {
                                        AppScreen.SubjectDetails -> AppScreen.Subjects
                                        AppScreen.GradeForm -> AppScreen.SubjectDetails
                                        AppScreen.GoalCalculator -> AppScreen.Analytics
                                        else -> AppScreen.Home
                                    }
                                }
                                .padding(horizontal = 16.dp),
                            fontSize = 30.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            )
        },
        bottomBar = {
            if (screen in rootScreens) {
                NavigationBar {
                    NavigationBarItem(
                        selected = screen == AppScreen.Home,
                        onClick = { screen = AppScreen.Home },
                        icon = { Text("⌂", fontSize = 20.sp) },
                        label = { Text("Главная") }
                    )
                    NavigationBarItem(
                        selected = screen == AppScreen.Subjects,
                        onClick = { screen = AppScreen.Subjects },
                        icon = { Text("▤", fontSize = 20.sp) },
                        label = { Text("Дисциплины") }
                    )
                    NavigationBarItem(
                        selected = screen == AppScreen.Analytics,
                        onClick = { screen = AppScreen.Analytics },
                        icon = { Text("▥", fontSize = 20.sp) },
                        label = { Text("Динамика") }
                    )
                }
            }
        }
    ) { innerPadding ->
        when (screen) {
            AppScreen.Home -> HomeScreen(
                modifier = Modifier.padding(innerPadding),
                onOpenSubjects = { screen = AppScreen.Subjects },
                onOpenDiscipline = {
                    selectedDisciplineId = it
                    screen = AppScreen.SubjectDetails
                },
                onOpenAnalytics = { screen = AppScreen.Analytics }
            )
            AppScreen.Subjects -> SubjectsScreen(
                modifier = Modifier.padding(innerPadding),
                onSelect = {
                    selectedDisciplineId = it
                    screen = AppScreen.SubjectDetails
                }
            )
            AppScreen.SubjectDetails -> DisciplineScreen(
                modifier = Modifier.padding(innerPadding),
                discipline = selectedDiscipline,
                onAddGrade = { screen = AppScreen.GradeForm }
            )
            AppScreen.GradeForm -> GradeFormScreen(
                modifier = Modifier.padding(innerPadding)
            )
            AppScreen.Analytics -> AnalyticsScreen(
                modifier = Modifier.padding(innerPadding),
                onOpenGoal = { screen = AppScreen.GoalCalculator }
            )
            AppScreen.GoalCalculator -> GoalCalculatorScreen(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun HomeScreen(
    modifier: Modifier = Modifier,
    onOpenSubjects: () -> Unit,
    onOpenDiscipline: (Int) -> Unit,
    onOpenAnalytics: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Здравствуйте!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(
            "Здесь собрана основная информация о вашей учебной успеваемости.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("Общий средний балл", color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(8.dp))
                Text(
                    "4,3",
                    fontSize = 42.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text("Среднее арифметическое по всем оценкам", color = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SummaryCard("Оценок", "8", Modifier.weight(1f))
            SummaryCard("Дисциплин", "3", Modifier.weight(1f))
        }

        SectionHeading("Успеваемость по дисциплинам")
        demoDisciplines.forEach { discipline ->
            DisciplineCard(discipline = discipline) {
                onOpenDiscipline(discipline.id)
            }
        }

        OutlinedButton(
            onClick = onOpenSubjects,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Все дисциплины")
        }
        FilledTonalButton(
            onClick = onOpenAnalytics,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Посмотреть динамику")
        }
        PrototypeNote()
    }
}

@Composable
private fun SummaryCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier, shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.padding(16.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(6.dp))
            Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SubjectsScreen(
    modifier: Modifier = Modifier,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Ваши учебные дисциплины", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "Выберите дисциплину, чтобы открыть журнал оценок.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        demoDisciplines.forEach { discipline ->
            DisciplineCard(discipline = discipline) { onSelect(discipline.id) }
        }
        PrototypeNote()
    }
}

@Composable
private fun DisciplineCard(discipline: DemoDiscipline, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(discipline.name, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                Text(
                    discipline.teacher,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    discipline.grades.size.toString() + " оценок",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(discipline.average, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("средний", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun DisciplineScreen(
    modifier: Modifier = Modifier,
    discipline: DemoDiscipline,
    onAddGrade: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(shape = RoundedCornerShape(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Преподаватель", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(discipline.teacher, fontWeight = FontWeight.Medium)
                }
                Column {
                    Text("Средний балл", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(discipline.average, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
            }
        }

        SectionHeading("Журнал оценок")
        discipline.grades.forEach { grade ->
            Card(shape = RoundedCornerShape(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        grade.value.toString(),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Column(Modifier.weight(1f)) {
                        Text(grade.workType, fontWeight = FontWeight.SemiBold)
                        Text(grade.date, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                        if (grade.comment.isNotBlank()) {
                            Text(grade.comment, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        Button(onClick = onAddGrade, modifier = Modifier.fillMaxWidth()) {
            Text("Добавить оценку")
        }
        PrototypeNote()
    }
}

@Composable
private fun GradeFormScreen(modifier: Modifier = Modifier) {
    var grade by rememberSaveable { mutableStateOf("5") }
    var date by rememberSaveable { mutableStateOf("23.11.2026") }
    var workType by rememberSaveable { mutableStateOf("Практическая работа") }
    var comment by rememberSaveable { mutableStateOf("") }
    var message by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Добавление оценки", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Дисциплина: выбрана на предыдущем экране")
        OutlinedTextField(
            value = grade,
            onValueChange = { grade = it },
            label = { Text("Оценка (2–5)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = date,
            onValueChange = { date = it },
            label = { Text("Дата") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = workType,
            onValueChange = { workType = it },
            label = { Text("Тип учебной работы") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        OutlinedTextField(
            value = comment,
            onValueChange = { comment = it },
            label = { Text("Комментарий (необязательно)") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        Button(
            onClick = { message = "Макет формы согласован. Сохранение будет реализовано в следующих спринтах." },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Проверить макет формы")
        }
        if (message.isNotBlank()) {
            Text(message, color = MaterialTheme.colorScheme.primary)
        }
        PrototypeNote()
    }
}

@Composable
private fun AnalyticsScreen(
    modifier: Modifier = Modifier,
    onOpenGoal: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Динамика успеваемости", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text(
            "В этом разделе в следующих этапах будет отображаться изменение среднего балла по периодам.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        PeriodPreview("Сентябрь", "4,0")
        PeriodPreview("Октябрь", "4,2")
        PeriodPreview("Ноябрь", "4,3")
        HorizontalDivider()
        SectionHeading("Целевой результат")
        Text("Расчёт необходимой следующей оценки будет добавлен на этапе реализации калькулятора.")
        OutlinedButton(onClick = onOpenGoal, modifier = Modifier.fillMaxWidth()) {
            Text("Открыть макет калькулятора")
        }
        PrototypeNote()
    }
}

@Composable
private fun PeriodPreview(period: String, average: String) {
    Card(shape = RoundedCornerShape(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(period, fontWeight = FontWeight.Medium)
            Text(average, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun GoalCalculatorScreen(modifier: Modifier = Modifier) {
    var target by rememberSaveable { mutableStateOf("4,5") }
    var message by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Калькулятор целевого среднего балла", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Text("Укажите желаемый средний балл. Расчёт будет подключён в одном из следующих спринтов.")
        OutlinedTextField(
            value = target,
            onValueChange = { target = it },
            label = { Text("Желаемый средний балл") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
        Button(
            onClick = { message = "Макет калькулятора готов. Логика расчёта будет добавлена позднее." },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Проверить макет")
        }
        if (message.isNotBlank()) {
            Text(message, color = MaterialTheme.colorScheme.primary)
        }
        PrototypeNote()
    }
}

@Composable
private fun SectionHeading(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
}

@Composable
private fun PrototypeNote() {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        shape = RoundedCornerShape(12.dp)
    ) {
        Text(
            text = "Прототип первого спринта. Данные демонстрационные и пока не сохраняются.",
            modifier = Modifier.padding(12.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSecondaryContainer
        )
    }
}
