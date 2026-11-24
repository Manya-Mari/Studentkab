package com.example.studentkab

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
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

    private var disciplines: List<Discipline> = emptyList()
    private var grades: List<Grade> = emptyList()
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

        window.statusBarColor = getColor(R.color.screen_background)
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR

        disciplinesContainer = findViewById(R.id.disciplinesContainer)
        gradesContainer = findViewById(R.id.gradesContainer)
        disciplineCount = findViewById(R.id.disciplineCount)
        selectedDisciplineName = findViewById(R.id.selectedDisciplineName)
        emptyDisciplines = findViewById(R.id.emptyDisciplines)
        emptyGrades = findViewById(R.id.emptyGrades)
        errorMessage = findViewById(R.id.errorMessage)

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

        grades = selectedDiscipline?.let { discipline ->
            withContext(Dispatchers.IO) {
                database.gradeDao().getByDiscipline(discipline.id)
            }
        } ?: emptyList()

        renderDisciplines()
        renderGrades()
    }

    private suspend fun refreshGrades() {
        val current = selectedDiscipline
        grades = if (current == null) {
            emptyList()
        } else {
            withContext(Dispatchers.IO) {
                database.gradeDao().getByDiscipline(current.id)
            }
        }
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

            card.setBackgroundResource(
                if (selectedDiscipline?.id == discipline.id) {
                    R.drawable.bg_discipline_selected
                } else {
                    R.drawable.bg_discipline_card
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
        selectedDisciplineName.text = current?.name ?: "Сначала добавьте дисциплину"
        emptyGrades.visibility =
            if (current != null && grades.isEmpty()) View.VISIBLE else View.GONE

        val averageView = findViewById<TextView>(R.id.averageGrade)
        val weightedAverageView = findViewById<TextView>(R.id.weightedAverageGrade)
        if (grades.isEmpty()) {
            averageView.text = "Обычный средний балл: —"
            weightedAverageView.text = "Взвешенный средний балл: —"
        } else {
            val average = grades.map { it.value }.average()
            val totalWeight = grades.sumOf { weightForWorkType(it.workType) }
            val weightedAverage = grades.sumOf {
                it.value * weightForWorkType(it.workType)
            } / totalWeight
            averageView.text = "Обычный средний балл: ${formatAverage(average)}"
            weightedAverageView.text =
                "Взвешенный средний балл: ${formatAverage(weightedAverage)}"
        }

        grades.forEach { grade ->
            val card = LayoutInflater.from(this)
                .inflate(R.layout.item_grade, gradesContainer, false)

            card.findViewById<TextView>(R.id.gradeValue).text = "Оценка: ${grade.value}"
            card.findViewById<TextView>(R.id.gradeDate).text = "Дата: ${grade.date}"
            card.findViewById<TextView>(R.id.gradeWeight).text =
                "Вес работы: ${formatAverage(weightForWorkType(grade.workType))}"
            card.findViewById<TextView>(R.id.gradeWorkType).text = "Тип работы: ${grade.workType}"

            val comment = card.findViewById<TextView>(R.id.gradeComment)
            if (grade.comment.isBlank()) {
                comment.visibility = View.GONE
            } else {
                comment.visibility = View.VISIBLE
                comment.text = "Комментарий: ${grade.comment}"
            }

            card.findViewById<Button>(R.id.editGradeButton).setOnClickListener {
                showGradeDialog(grade)
            }
            card.findViewById<Button>(R.id.deleteGradeButton).setOnClickListener {
                confirmDeleteGrade(grade)
            }

            gradesContainer.addView(card)
        }
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
        val workTypeInput = dialogView.findViewById<EditText>(R.id.inputWorkType)
        val commentInput = dialogView.findViewById<EditText>(R.id.inputGradeComment)

        valueInput.setText(initial?.value?.toString().orEmpty())
        dateInput.setText(initial?.date.orEmpty())
        workTypeInput.setText(initial?.workType.orEmpty())
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
                val workType = workTypeInput.text.toString().trim()
                val comment = commentInput.text.toString().trim()
                val value = valueText.toIntOrNull()

                when {
                    value == null || value !in 2..5 ->
                        valueInput.error = "Оценка должна быть целым числом от 2 до 5"
                    workType.isBlank() ->
                        workTypeInput.error = "Заполните тип работы"
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

    private fun formatAverage(value: Double): String =
        String.format(Locale("ru", "RU"), "%.2f", value)

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
