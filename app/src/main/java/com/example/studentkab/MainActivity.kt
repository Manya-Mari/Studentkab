package com.example.studentkab

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Spinner
import android.widget.ArrayAdapter
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.studentkab.data.DatabaseProvider
import com.example.studentkab.data.Discipline
import com.example.studentkab.data.Grade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val database by lazy { DatabaseProvider.getDatabase(applicationContext) }

    private lateinit var disciplinesContainer: LinearLayout
    private lateinit var gradesContainer: LinearLayout
    private lateinit var disciplineCount: TextView
    private lateinit var selectedDisciplineName: TextView
    private lateinit var emptyDisciplines: TextView
    private lateinit var emptyGrades: TextView
    private lateinit var errorMessage: TextView
    private lateinit var workTypeFilter: Spinner
    private lateinit var sortGrades: Spinner
    private lateinit var dateFromFilter: EditText
    private lateinit var dateToFilter: EditText
    private lateinit var filterMessage: TextView
    private lateinit var globalAverage: TextView
    private lateinit var disciplineAverage: TextView
    private lateinit var progressDynamics: TextView
    private lateinit var targetAverageInput: EditText
    private lateinit var targetAverageResult: TextView

    private var disciplines: List<Discipline> = emptyList()
    private var grades: List<Grade> = emptyList()
    private var allGrades: List<Grade> = emptyList()
    private var selectedDiscipline: Discipline? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        val root = findViewById<View>(R.id.mainScroll)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, systemBars.bottom)
            insets
        }

        // Подавляем предупреждение об устаревшем методе, так как для старых версий Android он работает корректно
        @Suppress("DEPRECATION")
        window.statusBarColor = getColor(R.color.screen_background)

        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true

        disciplinesContainer = findViewById(R.id.disciplinesContainer)
        gradesContainer = findViewById(R.id.gradesContainer)
        disciplineCount = findViewById(R.id.disciplineCount)
        selectedDisciplineName = findViewById(R.id.selectedDisciplineName)
        emptyDisciplines = findViewById(R.id.emptyDisciplines)
        emptyGrades = findViewById(R.id.emptyGrades)
        errorMessage = findViewById(R.id.errorMessage)
        workTypeFilter = findViewById(R.id.workTypeFilter)
        sortGrades = findViewById(R.id.sortGrades)
        dateFromFilter = findViewById(R.id.dateFromFilter)
        dateToFilter = findViewById(R.id.dateToFilter)
        filterMessage = findViewById(R.id.filterMessage)
        globalAverage = findViewById(R.id.globalAverage)
        disciplineAverage = findViewById(R.id.disciplineAverage)
        progressDynamics = findViewById(R.id.progressDynamics)
        targetAverageInput = findViewById(R.id.targetAverageInput)
        targetAverageResult = findViewById(R.id.targetAverageResult)
        findViewById<Button>(R.id.calculateTargetAverageButton).setOnClickListener {
            calculateTargetAverage()
        }

        val workTypeAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            listOf("Все типы работ", "Практическая", "Лабораторная", "Контрольная")
        ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item) }
        workTypeFilter.adapter = workTypeAdapter

        val sortAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            listOf("Сначала новые", "Сначала старые", "По возрастанию оценки", "По убыванию оценки")
        ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item) }
        sortGrades.adapter = sortAdapter

        findViewById<Button>(R.id.applyFiltersButton).setOnClickListener { renderGrades() }
        workTypeFilter.onItemSelectedListener = simpleSelectionListener { renderGrades() }
        sortGrades.onItemSelectedListener = simpleSelectionListener { renderGrades() }

        findViewById<Button>(R.id.addDisciplineButton).setOnClickListener {
            showDisciplineDialog(null)
        }

        lifecycleScope.launch {
            withContext(Dispatchers.IO) {
                DatabaseProvider.insertTestDataIfNeeded(applicationContext, database)
            }
            refreshData()
        }
    }

    private suspend fun refreshData() {
        val loaded = withContext(Dispatchers.IO) {
            database.disciplineDao().getAll()
        }
        disciplines = loaded
        selectedDiscipline = selectedDiscipline?.let { selected ->
            loaded.find { it.id == selected.id }
        } ?: loaded.firstOrNull()

        allGrades = withContext(Dispatchers.IO) { database.gradeDao().getAll() }
        grades = selectedDiscipline?.let { discipline -> allGrades.filter { it.disciplineId == discipline.id } } ?: emptyList()

        renderDisciplines()
        renderGrades()
    }

    private suspend fun refreshGrades() {
        val current = selectedDiscipline
        allGrades = withContext(Dispatchers.IO) { database.gradeDao().getAll() }
        grades = if (current == null) emptyList() else allGrades.filter { it.disciplineId == current.id }
        renderDisciplines()
        renderGrades()
    }

    private fun renderDisciplines() {
        disciplinesContainer.removeAllViews()
        disciplineCount.text = disciplines.size.toString()
        emptyDisciplines.visibility = if (disciplines.isEmpty()) View.VISIBLE else View.GONE

        disciplines.forEach { discipline ->
            val card = LayoutInflater.from(this)
                .inflate(R.layout.item_discipline, disciplinesContainer, false)

            card.findViewById<TextView>(R.id.disciplineName).text = discipline.name
            card.findViewById<TextView>(R.id.teacherName).text =
                discipline.teacherName?.takeIf { it.isNotBlank() } ?: "Преподаватель не указан"

            val disciplineGrades = allGrades.filter { it.disciplineId == discipline.id }
            val averageLabel = card.findViewById<TextView>(R.id.disciplineAverageValue)
            if (disciplineGrades.isEmpty()) {
                averageLabel.text = "Средний балл: пока нет оценок"
                averageLabel.setTextColor(getColor(R.color.text_secondary))
            } else {
                val average = disciplineGrades.map { it.value }.average()
                averageLabel.text = if (average < LOW_AVERAGE_THRESHOLD) {
                    "Низкий средний балл: ${formatAverage(average)}"
                } else {
                    "Средний балл: ${formatAverage(average)}"
                }
                averageLabel.setTextColor(
                    getColor(if (average < LOW_AVERAGE_THRESHOLD) R.color.action_delete else R.color.text_primary)
                )
            }

            // НОВЫЕ НАЗВАНИЯ РЕСУРСОВ
            card.setBackgroundResource(
                if (selectedDiscipline?.id == discipline.id) {
                    R.drawable.card_discipline_selected
                } else {
                    R.drawable.card_discipline_bg
                }
            )

            card.setOnClickListener {
                selectedDiscipline = discipline
                lifecycleScope.launch { refreshGrades() }
            }

            card.findViewById<Button>(R.id.editDisciplineButton).setOnClickListener {
                showDisciplineDialog(discipline)
            }
            card.findViewById<Button>(R.id.deleteDisciplineButton).setOnClickListener {
                confirmDeleteDiscipline(discipline)
            }
            card.findViewById<Button>(R.id.addGradeButton).setOnClickListener {
                selectedDiscipline = discipline
                renderDisciplines()
                showGradeDialog(null)
            }

            disciplinesContainer.addView(card)
        }
    }

    private fun renderGrades() {
        gradesContainer.removeAllViews()
        val current = selectedDiscipline
        selectedDisciplineName.text = current?.name ?: "Все дисциплины"
        val averageView = findViewById<TextView>(R.id.averageGrade)
        globalAverage.text = if (allGrades.isEmpty()) {
            "Общий средний балл по всем записям: — (оценок пока нет)"
        } else {
            "Общий средний балл по всем записям: ${formatAverage(allGrades.map { it.value }.average())}"
        }
        val disciplineLines = disciplines.mapNotNull { discipline ->
            val items = allGrades.filter { it.disciplineId == discipline.id }
            if (items.isEmpty()) null else "${discipline.name}: ${formatAverage(items.map { it.value }.average())}"
        }
        disciplineAverage.text = if (disciplineLines.isEmpty()) "Средние баллы по дисциплинам: пока нет оценок."
        else "Средние баллы по дисциплинам:\n" + disciplineLines.joinToString("\n")

        val from = dateFromFilter.text.toString().trim()
        val to = dateToFilter.text.toString().trim()
        val validFrom = from.isBlank() || validateDate(from) == null
        val validTo = to.isBlank() || validateDate(to) == null
        if (!validFrom || !validTo || (from.isNotBlank() && to.isNotBlank() && from > to)) {
            averageView.text = "Средний балл по выбранному фильтру: —"
            findViewById<TextView>(R.id.weightedAverageGrade).text = "Взвешенный средний балл: —"
            filterMessage.visibility = View.VISIBLE
            filterMessage.text = "Проверьте период: даты должны быть в формате ГГГГ-ММ-ДД, начало не позже конца."
            return
        }
        filterMessage.visibility = View.GONE

        var visibleGrades = grades.filter { grade ->
            (from.isBlank() || grade.date >= from) && (to.isBlank() || grade.date <= to) &&
                    matchesWorkType(grade.workType, workTypeFilter.selectedItemPosition)
        }
        visibleGrades = when (sortGrades.selectedItemPosition) {
            1 -> visibleGrades.sortedWith(compareBy<Grade> { it.date }.thenBy { it.id })
            2 -> visibleGrades.sortedBy { it.value }
            3 -> visibleGrades.sortedByDescending { it.value }
            else -> visibleGrades.sortedWith(compareByDescending<Grade> { it.date }.thenByDescending { it.id })
        }

        if (visibleGrades.isEmpty()) {
            averageView.text = "Средний балл по выбранному фильтру: —"
            findViewById<TextView>(R.id.weightedAverageGrade).text = "Взвешенный средний балл: —"
            emptyGrades.text = if (grades.isEmpty()) "По этой дисциплине оценок пока нет." else "За выбранный период и тип работы оценок нет."
            emptyGrades.visibility = if (current != null || allGrades.isNotEmpty()) View.VISIBLE else View.GONE
        } else {
            emptyGrades.visibility = View.GONE
            averageView.text = "Средний балл по выбранному фильтру: ${formatAverage(visibleGrades.map { it.value }.average())}"
            val totalWeight = visibleGrades.sumOf { weightForWorkType(it.workType) }
            val weightedAverage = visibleGrades.sumOf { it.value * weightForWorkType(it.workType) } / totalWeight
            findViewById<TextView>(R.id.weightedAverageGrade).text = "Взвешенный средний балл: ${formatAverage(weightedAverage)}"
        }

        val monthly = visibleGrades.groupBy { it.date.take(7) }.toSortedMap()
        progressDynamics.text = if (monthly.isEmpty()) "Динамика по месяцам: пока недостаточно данных."
        else "Динамика по месяцам (среднее арифметическое):\n" +
                monthly.entries.joinToString("\n") { (month, monthGrades) ->
                    "$month — ${formatAverage(monthGrades.map { it.value }.average())} (${monthGrades.size} оцен.)"
                }

        visibleGrades.forEach { grade ->
            val card = LayoutInflater.from(this).inflate(R.layout.item_grade, gradesContainer, false)
            card.findViewById<TextView>(R.id.gradeValue).text = "Оценка: ${grade.value}"
            card.findViewById<TextView>(R.id.gradeDate).text = "Дата: ${grade.date}"
            card.findViewById<TextView>(R.id.gradeWeight).text = "Вес работы: ${formatAverage(weightForWorkType(grade.workType))}"
            card.findViewById<TextView>(R.id.gradeWorkType).text = "Тип работы: ${grade.workType}"
            val comment = card.findViewById<TextView>(R.id.gradeComment)
            if (grade.comment.isBlank()) comment.visibility = View.GONE else {
                comment.visibility = View.VISIBLE
                comment.text = "Комментарий: ${grade.comment}"
            }
            card.findViewById<Button>(R.id.editGradeButton).setOnClickListener { showGradeDialog(grade) }
            card.findViewById<Button>(R.id.deleteGradeButton).setOnClickListener { confirmDeleteGrade(grade) }
            gradesContainer.addView(card)
        }
    }


    private fun calculateTargetAverage() {
        val targetText = targetAverageInput.text.toString().trim().replace(',', '.')
        val target = targetText.toDoubleOrNull()
        if (target == null || !target.isFinite() || target !in 2.0..5.0) {
            targetAverageInput.error = "Введите целевой средний балл от 2 до 5"
            targetAverageResult.text = "Укажите корректное целевое значение."
            return
        }

        val discipline = selectedDiscipline
        if (discipline == null) {
            targetAverageResult.text = "Сначала выберите дисциплину."
            return
        }

        val currentGrades = allGrades
            .filter { it.disciplineId == discipline.id }
            .map { it.value }

        when (val result = TargetAverageCalculator.calculate(target, currentGrades)) {
            is TargetAverageResult.Success -> {
                val currentAverage = if (currentGrades.isEmpty()) 0.0 else currentGrades.average()
                targetAverageResult.text =
                    "Дисциплина: ${discipline.name}\n" +
                    "Сейчас: ${formatAverage(currentAverage)} (оценок: ${currentGrades.size})\n" +
                    "Ваша цель: ${formatAverage(target)}\n\n" +
                    "Нужная следующая оценка: ${result.requiredGrade}.\n" +
                    "Если получите её, средний балл достигнет цели."
            }
            is TargetAverageResult.OutOfRange -> {
                val count = currentGrades.size
                val currentSum = currentGrades.sum()
                val currentAverage = if (count == 0) 0.0 else currentGrades.average()
                val bestPossibleAverage = (currentSum + 5.0) / (count + 1)
                val explanation = when {
                    result.requiredGrade > 5.0 ->
                        "Нужно получить оценку ${formatRequiredGrade(result.requiredGrade)}, но максимальная оценка — 5."
                    else ->
                        "Нужно получить оценку ${formatRequiredGrade(result.requiredGrade)}, но минимальная оценка — 2."
                }
                targetAverageResult.text =
                    "ОДНОЙ ОЦЕНКОЙ ЦЕЛЬ НЕ ДОСТИЧЬ\n\n" +
                    "Дисциплина: ${discipline.name}\n" +
                    "Сейчас: ${formatAverage(currentAverage)} (оценок: $count)\n" +
                    "Желаемый средний: ${formatAverage(target)}\n\n" +
                    "$explanation\n" +
                    "Даже если следующая оценка будет 5, средний станет ${formatAverage(bestPossibleAverage)}.\n\n" +
                    "Что делать: снизить целевой балл или рассчитывать на несколько будущих оценок."
            }
            is TargetAverageResult.NonInteger -> {
                val currentAverage = if (currentGrades.isEmpty()) 0.0 else currentGrades.average()
                val lower = kotlin.math.floor(result.requiredGrade).toInt()
                val upper = kotlin.math.ceil(result.requiredGrade).toInt()
                targetAverageResult.text =
                    "ТОЧНО ПОПАСТЬ В ЦЕЛЬ ОДНОЙ ОЦЕНКОЙ НЕЛЬЗЯ\n\n" +
                    "Сейчас: ${formatAverage(currentAverage)} (оценок: ${currentGrades.size})\n" +
                    "Желаемый средний: ${formatAverage(target)}\n" +
                    "Расчётная оценка: ${formatAverage(result.requiredGrade)}\n\n" +
                    "Оценки выставляются целыми числами. Если получить $lower, средний будет ниже цели; если $upper — выше."
            }
            is TargetAverageResult.InvalidTarget -> {
                targetAverageResult.text = "Целевой средний балл должен быть от 2 до 5."
            }
            is TargetAverageResult.InvalidGrades -> {
                targetAverageResult.text = "В сохранённых оценках обнаружены некорректные значения. Проверьте данные дисциплины."
            }
        }
    }

    private fun matchesWorkType(workType: String, selectedPosition: Int): Boolean {
        val normalized = workType.trim().lowercase(Locale.ROOT)
        return when (selectedPosition) {
            1 -> "практичес" in normalized
            2 -> "лаборатор" in normalized
            3 -> "контрольн" in normalized
            else -> true
        }
    }

    private fun simpleSelectionListener(onSelected: () -> Unit) =
        object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) { onSelected() }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

    private fun showDisciplineDialog(initial: Discipline?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_discipline, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.inputDisciplineName)
        val teacherInput = dialogView.findViewById<EditText>(R.id.inputTeacherName)

        nameInput.setText(initial?.name.orEmpty())
        teacherInput.setText(initial?.teacherName.orEmpty())

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (initial == null) "Новая дисциплина" else "Изменить дисциплину")
            .setView(dialogView)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = nameInput.text.toString().trim()
                val teacher = teacherInput.text.toString().trim()

                when {
                    name.isEmpty() -> nameInput.error = "Введите название дисциплины"
                    name.length < 2 -> nameInput.error = "Минимум 2 символа"
                    name.length > 100 -> nameInput.error = "Максимум 100 символов"
                    else -> {
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                if (initial == null) {
                                    database.disciplineDao().insert(
                                        Discipline(name = name, teacherName = teacher.ifBlank { null })
                                    )
                                } else {
                                    database.disciplineDao().update(
                                        initial.copy(name = name, teacherName = teacher.ifBlank { null })
                                    )
                                }
                            }
                            dialog.dismiss()
                            clearError()
                            refreshData()
                        }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun showGradeDialog(initial: Grade?) {
        if (selectedDiscipline == null) {
            showError("Сначала добавьте или выберите дисциплину.")
            return
        }

        val dialogView = layoutInflater.inflate(R.layout.dialog_grade, null)
        val valueInput = dialogView.findViewById<EditText>(R.id.inputGradeValue)
        val dateInput = dialogView.findViewById<EditText>(R.id.inputGradeDate)
        val workTypeInput = dialogView.findViewById<Spinner>(R.id.inputWorkType)
        val workTypes = listOf("Практическая", "Лабораторная", "Контрольная")
        workTypeInput.adapter = ArrayAdapter(this, R.layout.spinner_item, workTypes).also {
            it.setDropDownViewResource(R.layout.spinner_dropdown_item)
        }
        val commentInput = dialogView.findViewById<EditText>(R.id.inputGradeComment)

        valueInput.setText(initial?.value?.toString().orEmpty())
        dateInput.setText(initial?.date.orEmpty())
        workTypeInput.setSelection(
            workTypes.indexOfFirst {
                initial?.workType?.startsWith(it, ignoreCase = true) == true
            }.takeIf { it >= 0 } ?: 0
        )
        commentInput.setText(initial?.comment.orEmpty())

        val dialog = AlertDialog.Builder(this)
            .setTitle(if (initial == null) "Новая оценка" else "Изменить оценку")
            .setView(dialogView)
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Сохранить", null)
            .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val valueText = valueInput.text.toString().trim()
                val date = dateInput.text.toString().trim()
                val workType = workTypeInput.selectedItem?.toString().orEmpty()
                val comment = commentInput.text.toString().trim()
                val value = valueText.toIntOrNull()

                when {
                    value == null || value !in 2..5 ->
                        valueInput.error = "Оценка должна быть целым числом от 2 до 5"

                    validateDate(date) != null ->
                        dateInput.error = validateDate(date)

                    else -> {
                        val discipline = selectedDiscipline ?: return@setOnClickListener
                        lifecycleScope.launch {
                            withContext(Dispatchers.IO) {
                                if (initial == null) {
                                    database.gradeDao().insert(
                                        Grade(
                                            disciplineId = discipline.id,
                                            value = value,
                                            date = date,
                                            workType = workType,
                                            comment = comment
                                        )
                                    )
                                } else {
                                    database.gradeDao().update(
                                        initial.copy(
                                            value = value,
                                            date = date,
                                            workType = workType,
                                            comment = comment
                                        )
                                    )
                                }
                            }
                            dialog.dismiss()
                            clearError()
                            refreshData()
                        }
                    }
                }
            }
        }
        dialog.show()
    }

    private fun confirmDeleteDiscipline(discipline: Discipline) {
        AlertDialog.Builder(this)
            .setTitle("Удалить дисциплину?")
            .setMessage("«${discipline.name}» и все связанные с ней оценки будут удалены.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        database.disciplineDao().delete(discipline)
                    }
                    if (selectedDiscipline?.id == discipline.id) {
                        selectedDiscipline = null
                    }
                    refreshData()
                }
            }
            .show()
    }

    private fun confirmDeleteGrade(grade: Grade) {
        AlertDialog.Builder(this)
            .setTitle("Удалить оценку?")
            .setMessage("Запись с оценкой ${grade.value} будет удалена.")
            .setNegativeButton("Отмена", null)
            .setPositiveButton("Удалить") { _, _ ->
                lifecycleScope.launch {
                    withContext(Dispatchers.IO) {
                        database.gradeDao().delete(grade)
                    }
                    refreshData()
                }
            }
            .show()
    }

    private fun weightForWorkType(workType: String): Double {
        val normalizedType = workType.trim().lowercase(Locale.ROOT)
        return when {
            "практичес" in normalizedType -> 1.0
            "лаборатор" in normalizedType -> 1.5
            "контрольн" in normalizedType -> 2.0
            else -> 1.0
        }
    }

    companion object {
        private const val LOW_AVERAGE_THRESHOLD = 3.5
    }

    private fun formatRequiredGrade(value: Double): String =
        if (kotlin.math.abs(value - kotlin.math.round(value)) < 1e-9) {
            kotlin.math.round(value).toInt().toString()
        } else {
            formatAverage(value)
        }

    private fun formatAverage(value: Double): String =
        String.format(Locale.forLanguageTag("ru-RU"), "%.2f", value)

    private fun validateDate(input: String): String? {
        if (!Regex("""^\d{4}-\d{2}-\d{2}$""").matches(input)) {
            return "Введите дату в формате ГГГГ-ММ-ДД"
        }
        val parts = input.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        if (month !in 1..12) return "Месяц должен быть от 01 до 12"

        val leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
        val daysInMonth = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            else -> if (leap) 29 else 28
        }
        if (day !in 1..daysInMonth) return "Некорректный день для выбранного месяца"
        return null
    }

    private fun showError(message: String) {
        errorMessage.text = message
        errorMessage.visibility = View.VISIBLE
    }

    private fun clearError() {
        errorMessage.text = ""
        errorMessage.visibility = View.GONE
    }
}