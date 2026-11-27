package com.example.studentkab

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.AdapterView
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

    // ── ID и имя текущего студента (передаются из LoginActivity) ──
    private var currentStudentId: Long = -1L
    private var currentStudentName: String = ""

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

    // ── Жизненный цикл

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Читаем данные студента из Intent
        currentStudentId = intent.getLongExtra(EXTRA_STUDENT_ID, -1L)
        currentStudentName = intent.getStringExtra(EXTRA_STUDENT_NAME) ?: ""

        // Если вошли без выбора студента — вернуть на экран входа
        if (currentStudentId == -1L) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // Разрешаем контенту рисоваться под системными барами (edge-to-edge).
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_main)

        // Добавляем отступы к корневому view, чтобы контент не перекрывался системными барами.
        val root = findViewById<View>(R.id.mainScroll)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, systemBars.top, view.paddingRight, systemBars.bottom)
            insets
        }

        // Подавляем предупреждение об устаревшем методе, так как для старых версий Android он работает корректно.
        @Suppress("DEPRECATION")
        window.statusBarColor = getColor(R.color.screen_background)

        // Устанавливаем тёмные иконки в строке состояния (для светлого фона).
        val windowInsetsController = WindowInsetsControllerCompat(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = true

        // Привязываем элементы интерфейса к переменным
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

        // Кнопка запуска расчёта целевого среднего балла.
        findViewById<Button>(R.id.calculateTargetAverageButton).setOnClickListener {
            calculateTargetAverage()
        }

        // ── Настройка спиннеров

        // Адаптер фильтра по типу работы.
        val workTypeAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            listOf("Все типы работ", "Практическая", "Лабораторная", "Контрольная")
        ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item) }
        workTypeFilter.adapter = workTypeAdapter

        // Адаптер сортировки оценок.
        val sortAdapter = ArrayAdapter(
            this,
            R.layout.spinner_item,
            listOf("Сначала новые", "Сначала старые", "По возрастанию оценки", "По убыванию оценки")
        ).also { it.setDropDownViewResource(R.layout.spinner_dropdown_item) }
        sortGrades.adapter = sortAdapter

        // При изменении фильтра или сортировки — сразу перерисовываем оценки.
        findViewById<Button>(R.id.applyFiltersButton).setOnClickListener { renderGrades() }
        workTypeFilter.onItemSelectedListener = simpleSelectionListener { renderGrades() }
        sortGrades.onItemSelectedListener = simpleSelectionListener { renderGrades() }

        // Кнопка открытия диалога добавления новой дисциплины.
        findViewById<Button>(R.id.addDisciplineButton).setOnClickListener {
            showDisciplineDialog(null)
        }

        // Кнопка «Выйти из аккаунта»
        findViewById<Button>(R.id.logoutButton).setOnClickListener {
            // Очищаем сохранённую сессию, чтобы при следующем запуске открылся экран входа
            getSharedPreferences(LoginActivity.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .clear()
                .apply()
            selectedDiscipline = null
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
        }

        // Показываем имя студента в заголовке
        findViewById<TextView>(R.id.studentNameHeader).text = currentStudentName

        // Загружаем данные студента
        lifecycleScope.launch { refreshData() }
    }

    // Загрузка и обновление данных
    private suspend fun refreshData() {
        val loaded = withContext(Dispatchers.IO) {
            database.disciplineDao().getAll()
                .filter { it.studentId == currentStudentId }  // только дисциплины текущего студента
        }
        disciplines = loaded

        // Сохраняем текущий выбор дисциплины по ID; если она удалена — берём первую из списка.
        selectedDiscipline = selectedDiscipline?.let { selected ->
            loaded.find { it.id == selected.id }
        } ?: loaded.firstOrNull()

        val disciplineIds = loaded.map { it.id }.toSet()
        allGrades = withContext(Dispatchers.IO) {
            database.gradeDao().getAll().filter { it.disciplineId in disciplineIds }
        }
        grades = selectedDiscipline?.let { discipline ->
            allGrades.filter { it.disciplineId == discipline.id }
        } ?: emptyList()

        renderDisciplines()
        renderGrades()
    }

    /**
     * Лёгкое обновление: перезагружает только оценки, не трогая список дисциплин.
     * Используется при смене выбранной дисциплины, чтобы избежать лишних запросов к базе.
     */
    private suspend fun refreshGrades() {
        val current = selectedDiscipline
        allGrades = withContext(Dispatchers.IO) { database.gradeDao().getAll() }
        // Оставляем только оценки дисциплин текущего студента
        val studentDisciplineIds = disciplines.map { it.id }.toSet()
        allGrades = allGrades.filter { it.disciplineId in studentDisciplineIds }
        grades = if (current == null) emptyList() else allGrades.filter { it.disciplineId == current.id }
        renderDisciplines()
        renderGrades()
    }

    //  Отрисовка UI

    /**
     * Перестраивает список карточек дисциплин в [disciplinesContainer].
     *
     * Для каждой дисциплины:
     * - показывает название, имя преподавателя и средний балл;
     * - выделяет активную карточку фоном [R.drawable.card_discipline_selected];
     * - подключает обработчики кнопок «Редактировать», «Удалить» и «Добавить оценку».
     */
    private fun renderDisciplines() {
        disciplinesContainer.removeAllViews()
        disciplineCount.text = disciplines.size.toString()

        // Показываем заглушку «список пуст», если дисциплин нет.
        emptyDisciplines.visibility = if (disciplines.isEmpty()) View.VISIBLE else View.GONE

        disciplines.forEach { discipline ->
            val card = LayoutInflater.from(this)
                .inflate(R.layout.item_discipline, disciplinesContainer, false)

            card.findViewById<TextView>(R.id.disciplineName).text = discipline.name
            card.findViewById<TextView>(R.id.teacherName).text =
                discipline.teacherName?.takeIf { it.isNotBlank() } ?: "Преподаватель не указан"

            // Вычисляем и отображаем средний балл по дисциплине.
            val disciplineGrades = allGrades.filter { it.disciplineId == discipline.id }
            val averageLabel = card.findViewById<TextView>(R.id.disciplineAverageValue)
            if (disciplineGrades.isEmpty()) {
                averageLabel.text = "Средний балл: пока нет оценок"
                averageLabel.setTextColor(getColor(R.color.text_secondary))
            } else {
                val average = disciplineGrades.map { it.value }.average()
                // Если средний балл ниже порога — помечаем его как «низкий» и красим в красный.
                averageLabel.text = if (average < LOW_AVERAGE_THRESHOLD) {
                    "Низкий средний балл: ${formatAverage(average)}"
                } else {
                    "Средний балл: ${formatAverage(average)}"
                }
                averageLabel.setTextColor(
                    getColor(if (average < LOW_AVERAGE_THRESHOLD) R.color.action_delete else R.color.text_primary)
                )
            }

            // Выделяем фон активной карточки.
            card.setBackgroundResource(
                if (selectedDiscipline?.id == discipline.id) {
                    R.drawable.card_discipline_selected
                } else {
                    R.drawable.card_discipline_bg
                }
            )

            // Клик по карточке выбирает дисциплину и обновляет оценки.
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
            // Кнопка «+» выбирает дисциплину и открывает диалог добавления оценки.
            card.findViewById<Button>(R.id.addGradeButton).setOnClickListener {
                selectedDiscipline = discipline
                renderDisciplines()
                showGradeDialog(null)
            }

            disciplinesContainer.addView(card)
        }
    }

    /**
     * Перестраивает секцию оценок: применяет фильтры по дате и типу работы,
     * выбранную сортировку, вычисляет статистику и добавляет карточки оценок
     * в [gradesContainer].
     *
     * Если даты введены в неверном формате или диапазон некорректен —
     * показывает [filterMessage] и прерывает рендеринг без вывода оценок.
     */
    private fun renderGrades() {
        gradesContainer.removeAllViews()
        val current = selectedDiscipline
        selectedDisciplineName.text = current?.name ?: "Все дисциплины"

        val averageView = findViewById<TextView>(R.id.averageGrade)

        // Глобальная статистика
        globalAverage.text = if (allGrades.isEmpty()) {
            "Общий средний балл по всем записям: — (оценок пока нет)"
        } else {
            "Общий средний балл по всем записям: ${formatAverage(allGrades.map { it.value }.average())}"
        }

        // Средний балл по каждой дисциплине отдельно.
        val disciplineLines = disciplines.mapNotNull { discipline ->
            val items = allGrades.filter { it.disciplineId == discipline.id }
            if (items.isEmpty()) null else "${discipline.name}: ${formatAverage(items.map { it.value }.average())}"
        }
        disciplineAverage.text = if (disciplineLines.isEmpty()) "Средние баллы по дисциплинам: пока нет оценок."
        else "Средние баллы по дисциплинам:\n" + disciplineLines.joinToString("\n")

        //  Валидация периода фильтрации
        val from = dateFromFilter.text.toString().trim()
        val to = dateToFilter.text.toString().trim()
        val validFrom = from.isBlank() || validateDate(from) == null
        val validTo = to.isBlank() || validateDate(to) == null

        // Если даты некорректны или начало позже конца — показываем ошибку.
        if (!validFrom || !validTo || (from.isNotBlank() && to.isNotBlank() && from > to)) {
            averageView.text = "Средний балл по выбранному фильтру: —"
            findViewById<TextView>(R.id.weightedAverageGrade).text = "Взвешенный средний балл: —"
            filterMessage.visibility = View.VISIBLE
            filterMessage.text = "Проверьте период: даты должны быть в формате ГГГГ-ММ-ДД, начало не позже конца."
            return
        }
        filterMessage.visibility = View.GONE

        // Применяем фильтры по дате и типу работы
        var visibleGrades = grades.filter { grade ->
            (from.isBlank() || grade.date >= from) && (to.isBlank() || grade.date <= to) &&
                    matchesWorkType(grade.workType, workTypeFilter.selectedItemPosition)
        }

        // Сортировка
        visibleGrades = when (sortGrades.selectedItemPosition) {
            1 -> visibleGrades.sortedWith(compareBy<Grade> { it.date }.thenBy { it.id })          // сначала старые
            2 -> visibleGrades.sortedBy { it.value }                                               // по возрастанию оценки
            3 -> visibleGrades.sortedByDescending { it.value }                                     // по убыванию оценки
            else -> visibleGrades.sortedWith(compareByDescending<Grade> { it.date }.thenByDescending { it.id }) // сначала новые
        }

        //  Статистика по отфильтрованным оценкам
        if (visibleGrades.isEmpty()) {
            averageView.text = "Средний балл по выбранному фильтру: —"
            findViewById<TextView>(R.id.weightedAverageGrade).text = "Взвешенный средний балл: —"
            emptyGrades.text = if (grades.isEmpty()) "По этой дисциплине оценок пока нет."
            else "За выбранный период и тип работы оценок нет."
            emptyGrades.visibility = if (current != null || allGrades.isNotEmpty()) View.VISIBLE else View.GONE
        } else {
            emptyGrades.visibility = View.GONE
            // Простое среднее по отфильтрованным оценкам.
            averageView.text = "Средний балл по выбранному фильтру: ${formatAverage(visibleGrades.map { it.value }.average())}"

            // Взвешенное среднее: каждый тип работы имеет свой вес ([weightForWorkType]).
            val totalWeight = visibleGrades.sumOf { weightForWorkType(it.workType) }
            val weightedAverage = visibleGrades.sumOf { it.value * weightForWorkType(it.workType) } / totalWeight
            findViewById<TextView>(R.id.weightedAverageGrade).text =
                "Взвешенный средний балл: ${formatAverage(weightedAverage)}"
        }

        // Динамика по месяцам
        // Группируем оценки по первым 7 символам даты («ГГГГ-ММ»).
        val monthly = visibleGrades.groupBy { it.date.take(7) }.toSortedMap()
        progressDynamics.text = if (monthly.isEmpty()) "Динамика по месяцам: пока недостаточно данных."
        else "Динамика по месяцам (среднее арифметическое):\n" +
                monthly.entries.joinToString("\n") { (month, monthGrades) ->
                    "$month — ${formatAverage(monthGrades.map { it.value }.average())} (${monthGrades.size} оцен.)"
                }

        //  Карточки оценок
        visibleGrades.forEach { grade ->
            val card = LayoutInflater.from(this).inflate(R.layout.item_grade, gradesContainer, false)
            card.findViewById<TextView>(R.id.gradeValue).text = "Оценка: ${grade.value}"
            card.findViewById<TextView>(R.id.gradeDate).text = "Дата: ${grade.date}"
            card.findViewById<TextView>(R.id.gradeWeight).text =
                "Вес работы: ${formatAverage(weightForWorkType(grade.workType))}"
            card.findViewById<TextView>(R.id.gradeWorkType).text = "Тип работы: ${grade.workType}"

            // Комментарий скрываем, если он пустой.
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

    //  Расчёт целевого среднего балла
    private fun calculateTargetAverage() {
        // Заменяем запятую на точку для поддержки локалей с дробным разделителем «,».
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

        // Обрабатываем все возможные результаты расчёта.
        when (val result = TargetAverageCalculator.calculate(target, currentGrades)) {
            is TargetAverageResult.OneGradeEnough -> {
                val currentAverage = if (currentGrades.isEmpty()) 0.0 else currentGrades.average()
                targetAverageResult.text =
                    "Дисциплина: ${discipline.name}\n" +
                    "Текущий средний балл: ${formatAverage(currentAverage)}\n" +
                    "Желаемый средний балл: ${formatAverage(target)}\n\n" +
                    "Достаточно одной следующей оценки: ${result.requiredGrade}."
            }
            is TargetAverageResult.MoreThanOneGrade -> {
                targetAverageResult.text =
                    "Одной оценки не хватит.\n\n" +
                    "Чтобы достичь среднего балла ${formatAverage(target)}, потребуется минимум ${result.requiredCount} оценок по 5."
            }
            is TargetAverageResult.Impossible -> {
                targetAverageResult.text =
                    "Одной оценки не хватит.\n\n" +
                    "При текущих оценках достичь среднего балла 5,00 невозможно даже несколькими оценками: средний балл будет только приближаться к 5."
            }
            is TargetAverageResult.InvalidTarget -> {
                targetAverageResult.text = "Целевой средний балл должен быть от 2 до 5."
            }
            is TargetAverageResult.InvalidGrades -> {
                targetAverageResult.text = "В сохранённых оценках обнаружены некорректные значения. Проверьте данные дисциплины."
            }
        }
    }

    // Вспомогательные методы

    /**
     * Проверяет, совпадает ли тип работы [workType] с позицией фильтра [selectedPosition].
     *
     * Позиции:
     * - 0 — все типы работ;
     * - 1 — практическая;
     * - 2 — лабораторная;
     * - 3 — контрольная.
     *
     * Сравнение выполняется по подстроке в нижнем регистре для устойчивости
     * к вариациям написания (например, «Практическая работа», «Практическая»).
     */
    private fun matchesWorkType(workType: String, selectedPosition: Int): Boolean {
        val normalized = workType.trim().lowercase(Locale.ROOT)
        return when (selectedPosition) {
            1 -> "практичес" in normalized
            2 -> "лаборатор" in normalized
            3 -> "контрольн" in normalized
            else -> true
        }
    }

    /**
     * Создаёт простой [android.widget.AdapterView.OnItemSelectedListener],
     * который вызывает [onSelected] при выборе любого элемента.
     */
    private fun simpleSelectionListener(onSelected: () -> Unit) =
        object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                onSelected()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }

    //  Диалоги

    /**
     * Показывает диалог добавления или редактирования дисциплины.
     *
     * @param initial `null` — создать новую дисциплину; ненулевое значение — редактировать.
     *
     * Валидация:
     * - название не должно быть пустым;
     * - минимальная длина — 2 символа;
     * - максимальная длина — 100 символов.
     */
    private fun showDisciplineDialog(initial: Discipline?) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_discipline, null)
        val nameInput = dialogView.findViewById<EditText>(R.id.inputDisciplineName)
        val teacherInput = dialogView.findViewById<EditText>(R.id.inputTeacherName)

        // Предзаполняем поля при редактировании.
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
                                    // Создаём новую дисциплину привязанную к текущему студенту
                                    database.disciplineDao().insert(
                                        Discipline(
                                            name = name,
                                            teacherName = teacher.ifBlank { null },
                                            studentId = currentStudentId
                                        )
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

        val weightHint = dialogView.findViewById<TextView>(R.id.workTypeWeightHint)
        workTypeInput.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedType = workTypes[position]
                val weight = weightForWorkType(selectedType)
                weightHint.text = "Вес оценки во взвешенном среднем: ${String.format(Locale.forLanguageTag("ru-RU"), "%.1f", weight)}"
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                weightHint.text = "Вес оценки: не выбран"
            }
        }

        val commentInput = dialogView.findViewById<EditText>(R.id.inputGradeComment)

        // Предзаполняем поля при редактировании.
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
            .setPositiveButton("Сохранить", null)   // null — переопределяем ниже
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
                                    // Добавляем новую оценку к выбранной дисциплине.
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
                                    // Обновляем существующую оценку, сохраняя ID и disciplineId.
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

    //  Подтверждение удаления

    /**
     * Показывает диалог подтверждения удаления [discipline].
     * После подтверждения удаляет дисциплину из базы (каскадно удаляются все её оценки),
     * сбрасывает [selectedDiscipline] если удалённая дисциплина была активной.
     */
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
                    // Сбрасываем выбор, если была удалена активная дисциплина.
                    if (selectedDiscipline?.id == discipline.id) {
                        selectedDiscipline = null
                    }
                    refreshData()
                }
            }
            .show()
    }

    /**
     * Показывает диалог подтверждения удаления [grade].
     * После подтверждения удаляет оценку из базы и обновляет данные.
     */
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

    //  Вычисления

    /**
     * Возвращает коэффициент веса для расчёта взвешенного среднего балла.
     *
     * Веса определены по типу учебной работы:
     * - Практическая работа → 1.0
     * - Лабораторная работа → 1.5 (более сложная, весит больше)
     * - Контрольная работа  → 2.0 (наиболее значимая)
     * - Неизвестный тип     → 1.0 (по умолчанию)
     */
    private fun weightForWorkType(workType: String): Double {
        val normalizedType = workType.trim().lowercase(Locale.ROOT)
        return when {
            "практичес" in normalizedType -> 1.0
            "лаборатор" in normalizedType -> 1.5
            "контрольн" in normalizedType -> 2.0
            else -> 1.0
        }
    }

    // Форматирование и валидация

    /**
     * Форматирует вещественное число в строку вида «X,XX» с точностью до сотых
     * с использованием русской локали (разделитель — запятая).
     */
    private fun formatAverage(value: Double): String =
        String.format(Locale.forLanguageTag("ru-RU"), "%.2f", value)

    /**
     * Форматирует оценку для вывода: если значение является целым числом,
     * выводит без дробной части; иначе использует [formatAverage].
     *
     * Используется в результатах расчёта целевого среднего.
     */
    private fun formatRequiredGrade(value: Double): String =
        if (kotlin.math.abs(value - kotlin.math.round(value)) < 1e-9) {
            kotlin.math.round(value).toInt().toString()
        } else {
            formatAverage(value)
        }

    /**
     * Проверяет, что строка [input] является корректной датой в формате «ГГГГ-ММ-ДД».
     *
     * Выполняет:
     * - проверку формата регулярным выражением;
     * - проверку диапазона месяца (1–12);
     * - проверку количества дней в месяце с учётом високосного года.
     *
     * @return `null`, если дата корректна; строку с описанием ошибки в противном случае.
     */
    private fun validateDate(input: String): String? {
        if (!Regex("""^\d{4}-\d{2}-\d{2}$""").matches(input)) {
            return "Введите дату в формате ГГГГ-ММ-ДД"
        }
        val parts = input.split("-")
        val year = parts[0].toInt()
        val month = parts[1].toInt()
        val day = parts[2].toInt()
        if (month !in 1..12) return "Месяц должен быть от 01 до 12"

        // Определяем, является ли год високосным.
        val leap = (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
        val daysInMonth = when (month) {
            1, 3, 5, 7, 8, 10, 12 -> 31
            4, 6, 9, 11 -> 30
            else -> if (leap) 29 else 28
        }
        if (day !in 1..daysInMonth) return "Некорректный день для выбранного месяца"
        return null
    }

    //  Ошибки

    /** Показывает сообщение об ошибке в поле [errorMessage]. */
    private fun showError(message: String) {
        errorMessage.text = message
        errorMessage.visibility = View.VISIBLE
    }

    /** Скрывает сообщение об ошибке в поле [errorMessage]. */
    private fun clearError() {
        errorMessage.text = ""
        errorMessage.visibility = View.GONE
    }

    //  Константы

    companion object {
        /**
         * Порог среднего балла, ниже которого значение считается «низким»
         * и отображается красным цветом.
         */
        private const val LOW_AVERAGE_THRESHOLD = 3.5

        /** Ключ Intent: Long — id студента. */
        const val EXTRA_STUDENT_ID = "extra_student_id"

        /** Ключ Intent: String — имя студента. */
        const val EXTRA_STUDENT_NAME = "extra_student_name"
    }
}
