package ui

import core.WarshallStepper
import core.WarshallStep
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox

/**
 * Панель управления для визуализатора алгоритма Уоршелла.
 *
 * Позволяет запускать, поэтапно выполнять и сбрасывать выполнение алгоритма,
 * а также отображать шаги, статус и управляющие кнопки.
 *
 * @property graphPanel      Панель визуализации графа (обновляет вершины и рёбра)
 * @property matrixInput     Панель для отображения и ввода матрицы смежности
 * @property statusLabel     Label для отображения статуса выполнения
 * @property iterationLabel  Label для отображения текущих индексов k, i, j
 */
class ControlPanel(
    private val graphPanel: GraphPanel,
    private val matrixInput: MatrixInput,
    private val statusLabel: Label,
    private val iterationLabel: Label
) {
    /** Кнопка запуска нового прохода алгоритма */
    val startButton = Button("Запуск")
    /** Кнопка шага назад (откат на 1 итерацию) */
    val stepBack   = Button("< Шаг")
    /** Кнопка малый шаг — один шаг по j */
    val stepSmall  = Button("Малый >")
    /** Кнопка средний шаг — пройти строку (i фиксирован, шаги по j) */
    val stepMedium = Button("Средний >>")
    /** Кнопка крупный шаг — пройти слой (k фиксирован, шаги по i/j) */
    val stepBig    = Button("Крупный >>>")
    /** Кнопка сброса состояния (вернуть к начальному виду) */
    val resetButton= Button("Сброс")
    /** Кнопка полного выполнения алгоритма */
    val runAllButton = Button("Выполнить всё")
    /** Кнопка вызова окна справки */
    val helpButton = Button("Справка")

    /** Текстовое поле для лога шагов алгоритма */
    val logArea = TextArea().apply {
        isEditable = false
        prefRowCount = 10
        prefHeight = 140.0
        style = "-fx-font-family: monospace; -fx-font-size: 12px;"
    }

    /** Корневой контейнер панели управления */
    val view: VBox

    private var stepper: WarshallStepper? = null
    private var finishedLogged = false

    /**
     * Инициализация панели: кнопки, биндинги, обработчики событий.
     * Настраивает запуск, шаги, сброс, выполнение и справку.
     */
    init {
        val bar = HBox(
            15.0,
            startButton, stepBack, stepSmall, stepMedium, stepBig, runAllButton, resetButton, helpButton
        ).apply {
            padding = Insets(10.0)
            alignment = Pos.CENTER
            style = "-fx-background-color: #f0f8ff;"
        }
        view = VBox(10.0, bar, logArea).apply { padding = Insets(10.0) }

        // --- запуск алгоритма ---
        startButton.setOnAction {
            stepper = WarshallStepper(matrixInput.getMatrix()).also { st ->
                logArea.clear()
                finishedLogged = false
                log("▶️ Запуск алгоритма Уоршелла")
                matrixInput.clearHeaderHighlights()
                updateStep(st.currentStep())
                setStepButtonsEnabled(true)
            }
        }

        // --- пошаговые действия ---
        stepSmall.setOnAction { doSmallStep() }
        stepMedium.setOnAction { doMediumStep() }
        stepBig.setOnAction { doBigStep() }
        stepBack.setOnAction  { doBack() }

        // --- сброс ---
        resetButton.setOnAction {
            stepper?.reset()
            finishedLogged = false
            log("🔄 Алгоритм сброшен")
            matrixInput.clearHeaderHighlights()
            stepper?.currentStep()?.let { updateStep(it) }
            setStepButtonsEnabled(true)
        }

        // --- выполнение до конца ---
        runAllButton.setOnAction {
            val st = stepper ?: return@setOnAction
            while (!st.isFinished()) { logStep(st.stepForward()) }
            updateStep(st.currentStep())
            checkFinished(st)
        }

        // --- справка ---
        helpButton.setOnAction { showHelp() }
    }

    // ─────────────────── Логика пошагового выполнения ───────────────────

    /**
     * Выполняет малый шаг (одна итерация по j).
     */
    private fun doSmallStep() {
        stepper?.let { if (!it.isFinished()) { updateStep(it.stepForward()); checkFinished(it) } }
    }

    /**
     * Выполняет шаг назад (откат на одну итерацию).
     */
    private fun doBack() {
        stepper?.let {
            updateStep(it.stepBack())
            finishedLogged = false
            setStepButtonsEnabled(true)
        }
    }

    /**
     * Выполняет средний шаг (до смены строки i).
     */
    private fun doMediumStep() {
        val st = stepper ?: return
        if (st.isFinished()) return
        val startI = st.currentStep().i
        val startK = st.currentStep().k
        var step: WarshallStep
        do {
            step = st.stepForward()
            logStep(step)
        } while (!st.isFinished() && step.i == startI && step.k == startK)
        updateStep(step)
        checkFinished(st)
    }

    /**
     * Выполняет крупный шаг (до смены слоя k).
     */
    private fun doBigStep() {
        val st = stepper ?: return
        if (st.isFinished()) return
        val startK = st.currentStep().k
        var step: WarshallStep
        do {
            step = st.stepForward()
            logStep(step)
        } while (!st.isFinished() && step.k == startK)
        updateStep(step)
        checkFinished(st)
    }

    // ─────────────────── Обновление интерфейса ───────────────────

    /**
     * Обновляет визуализацию матрицы и графа после очередного шага.
     *
     * @param step Текущее состояние (WarshallStep)
     */
    private fun updateStep(step: WarshallStep) {
        matrixInput.updateMatrixDisplay(step.matrix)
        matrixInput.clearHighlights()

        // убираем пунктиры, ставшие настоящими рёбрами
        val filtered = step.involved.filterNot { (i, j, t) ->
            t == "candidate" && step.matrix[i][j] == 1
        }
        if (filtered.isNotEmpty()) matrixInput.highlightCells(filtered)
        matrixInput.highlightHeader(step.k)

        graphPanel.updateGraph(
            step.matrix,
            highlights = filtered,
            highlightedNodes = listOf(step.i, step.j, step.k)
        )

        statusLabel.text = if (stepper?.isFinished() == true)
            "Статус: Алгоритм завершён."
        else
            "Статус: ${step.message}"
        iterationLabel.text = "k=${step.k + 1}, i=${step.i + 1}, j=${step.j + 1}"
    }

    /**
     * Проверяет завершён ли алгоритм и блокирует шаги, если да.
     *
     * @param st Экземпляр WarshallStepper
     */
    private fun checkFinished(st: WarshallStepper) {
        if (st.isFinished() && !finishedLogged) {
            finishedLogged = true
            log("✅ Алгоритм завершён.")
            statusLabel.text = "Статус: Алгоритм завершён."
            setStepButtonsEnabled(false)
            matrixInput.clearHeaderHighlights()
        }
    }

    /**
     * Включает или отключает кнопки выполнения шагов.
     *
     * @param state true — кнопки включены, false — отключены
     */
    private fun setStepButtonsEnabled(state: Boolean) {
        runAllButton.isDisable = !state
        stepSmall.isDisable = !state
        stepMedium.isDisable = !state
        stepBig.isDisable = !state
    }

    // ─────────────────── Логирование ───────────────────

    /**
     * Добавляет сообщение в лог.
     *
     * @param msg Текст сообщения
     */
    private fun log(msg: String) = logArea.appendText("$msg\n")

    /**
     * Добавляет сообщение о текущем шаге.
     *
     * @param s WarshallStep для лога
     */
    private fun logStep(s: WarshallStep) = log("Шаг: k=${s.k + 1}, i=${s.i + 1}, j=${s.j + 1} — ${s.message}")

    /**
     * Показывает справочное окно с описанием интерфейса.
     */
    private fun showHelp() {
        Alert(Alert.AlertType.INFORMATION).apply {
            title = "Справка"
            headerText = "Алгоритм Уоршелла"
            contentText = """
                • Введите матрицу смежности или сгенерируйте её.
                • "Запуск" – покадровый режим.
                • Кнопки Малый/Средний/Крупный – шаги по j / i / k.
                • "Выполнить всё" – сразу финальный результат.
                • "Сброс" – вернуться к исходному состоянию.
            """.trimIndent()
        }.showAndWait()
    }
}