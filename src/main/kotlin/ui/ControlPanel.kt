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
 * Отвечает за запуск, поэтапное выполнение, откат и сброс алгоритма, отображение лога и статуса,
 * а также за обновление визуализации графа и матрицы на каждом шаге.
 *
 * @property graphPanel Панель визуализации графа (обновляет вершины/рёбра).
 * @property matrixInput Панель для отображения и ввода матрицы смежности.
 * @property statusLabel Строка для отображения статуса выполнения.
 * @property iterationLabel Строка с текущими индексами k, i, j.
 */
class ControlPanel(
    private val graphPanel: GraphPanel,
    private val matrixInput: MatrixInput,
    private val statusLabel: Label,
    private val iterationLabel: Label
) {
    /** Кнопка запуска нового прохода алгоритма */
    val startButton = Button("Запуск")
    /** Кнопка шага назад */
    val stepBack = Button("< Шаг")
    /** Кнопка "малого" шага (по j) */
    val stepSmall = Button("Малый >")
    /** Кнопка "среднего" шага (по i) */
    val stepMedium = Button("Средний >>")
    /** Кнопка "крупного" шага (по k) */
    val stepBig = Button("Крупный >>>")
    /** Кнопка сброса состояния */
    val resetButton = Button("Сброс")
    /** Кнопка мгновенного выполнения алгоритма */
    val runAllButton = Button("Выполнить всё")
    /** Кнопка вызова справки */
    val helpButton = Button("Справка")
    /** Область для текстового лога шагов */
    val logArea = TextArea().apply {
        isEditable = false
        prefRowCount = 10
        prefHeight = 140.0
        style = "-fx-font-family: monospace; -fx-font-size: 12px;"
    }
    /** Основное view — содержит панель кнопок и лог */
    val view: VBox

    private var warshallStepper: WarshallStepper? = null
    private var finishedLogged = false

    /**
     * Инициализация кнопок, биндингов и обработчиков событий.
     * Основная логика по запуску, шагам, сбросу, выполнению и справке.
     */
    init {
        val controlBar = HBox(
            15.0, startButton, stepBack, stepSmall, stepMedium, stepBig, runAllButton, resetButton, helpButton
        ).apply {
            padding = Insets(10.0)
            alignment = Pos.CENTER
            style = "-fx-background-color: #f0f8ff;"
        }

        view = VBox(10.0, controlBar, logArea).apply {
            padding = Insets(10.0)
        }

        // --- Кнопка запуска
        startButton.setOnAction {
            val matrix = matrixInput.getMatrix()
            warshallStepper = WarshallStepper(matrix)
            logArea.clear()
            finishedLogged = false
            log("▶️ Запуск алгоритма Уоршелла")
            matrixInput.clearHeaderHighlights()
            val step = warshallStepper!!.currentStep()
            logStep(step)
            updateStep(step)
            setStepButtonsEnabled(true)
        }

        // --- Шаги алгоритма
        stepSmall.setOnAction { doSmallStep() }
        stepMedium.setOnAction { doMediumStep() }
        stepBig.setOnAction { doBigStep() }

        // --- Откат
        stepBack.setOnAction {
            val stepper = warshallStepper
            if (stepper != null) {
                val step = stepper.stepBack()
                logStep(step)
                updateStep(step)
                if (!stepper.isFinished()) {
                    setStepButtonsEnabled(true)
                    finishedLogged = false
                }
            }
        }

        // --- Сброс
        resetButton.setOnAction {
            warshallStepper?.reset()
            finishedLogged = false
            log("🔄 Алгоритм сброшен к начальному состоянию")
            matrixInput.clearHeaderHighlights()
            val step = warshallStepper!!.currentStep()
            logStep(step)
            updateStep(step)
            setStepButtonsEnabled(true)
        }

        // --- Выполнить всё
        runAllButton.setOnAction {
            val stepper = warshallStepper
            if (stepper != null && !stepper.isFinished()) {
                while (!stepper.isFinished()) {
                    val step = stepper.stepForward()
                    logStep(step)
                }
                val step = stepper.currentStep()
                updateStep(step)
                checkFinished(stepper)
            }
        }

        // --- Справка
        helpButton.setOnAction {
            val alert = Alert(Alert.AlertType.INFORMATION)
            alert.title = "Справка"
            alert.headerText = "О программе: Транзитивное замыкание (алгоритм Уоршелла)"
            alert.contentText = """
                Эта программа позволяет вычислить транзитивное замыкание ориентированного графа с помощью алгоритма Уоршелла.

                Вы можете:
                • Ввести матрицу смежности вручную или сгенерировать случайно.
                • "Запуск" — начать поэтапный просмотр.
                • "< Шаг" — шаг назад.
                • "Малый >" — шаг по j (ячейка).
                • "Средний >>" — шаг по i (строка).
                • "Крупный >>>" — шаг по k (слой).
                • "Выполнить всё" — получить финальный результат за один клик.
                • "Сброс" — возвращает начальное состояние.
                • На каждом шаге в статусе отображается подробное пояснение.
            """.trimIndent()
            alert.showAndWait()
        }
    }

    /**
     * Проверяет завершён ли алгоритм и блокирует шаги, если да.
     *
     * @param stepper Текущий исполнитель алгоритма.
     */
    private fun checkFinished(stepper: WarshallStepper) {
        if (stepper.isFinished() && !finishedLogged) {
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
     * @param state true — кнопки включены; false — выключены.
     */
    private fun setStepButtonsEnabled(state: Boolean) {
        runAllButton.isDisable = !state
        stepSmall.isDisable = !state
        stepMedium.isDisable = !state
        stepBig.isDisable = !state
    }

    /**
     * Выполняет малый шаг (stepForward).
     */
    private fun doSmallStep() {
        val stepper = warshallStepper
        if (stepper != null && !stepper.isFinished()) {
            val step = stepper.stepForward()
            logStep(step)
            updateStep(step)
            checkFinished(stepper)
        }
    }

    /**
     * Выполняет средний шаг — до перехода к следующему i (строка).
     */
    private fun doMediumStep() {
        val stepper = warshallStepper
        if (stepper != null && !stepper.isFinished()) {
            var step: WarshallStep? = null
            val startI = stepper.currentStep().i
            val startK = stepper.currentStep().k
            do {
                step = stepper.stepForward()
                if (step != null) logStep(step)
            } while (!stepper.isFinished() && (step?.i == startI && step.k == startK))
            step?.let {
                updateStep(it)
                checkFinished(stepper)
            }
        }
    }

    /**
     * Выполняет крупный шаг — до перехода к следующему k (слой).
     */
    private fun doBigStep() {
        val stepper = warshallStepper
        if (stepper != null && !stepper.isFinished()) {
            var step: WarshallStep? = null
            val startK = stepper.currentStep().k
            do {
                step = stepper.stepForward()
                if (step != null) logStep(step)
            } while (!stepper.isFinished() && step?.k == startK)
            step?.let {
                updateStep(it)
                checkFinished(stepper)
            }
        }
    }

    /**
     * Обновляет визуализацию матрицы и графа после очередного шага.
     *
     * @param step Текущее состояние (WarshallStep).
     */
    private fun updateStep(step: WarshallStep) {
        matrixInput.updateMatrixDisplay(step.matrix)
        matrixInput.clearHighlights()
        if (step.involved.isNotEmpty()) {
            matrixInput.highlightCells(step.involved)
        }
        matrixInput.highlightHeader(step.k)
        graphPanel.updateGraph(
            step.matrix,
            highlights = step.involved,
            highlightedNodes = listOf(step.i, step.j, step.k)
        )
        statusLabel.text = if (warshallStepper?.isFinished() == true)
            "Статус: Алгоритм завершён."
        else
            "Статус: ${step.message}"
        iterationLabel.text = "k=${step.k + 1}, i=${step.i + 1}, j=${step.j + 1}"
    }

    /**
     * Добавляет сообщение в текстовый лог.
     *
     * @param message Текст сообщения.
     */
    private fun log(message: String) {
        logArea.appendText("$message\n")
    }

    /**
     * Добавляет лог о текущем шаге с индексами и сообщением.
     *
     * @param step Состояние текущего шага.
     */
    private fun logStep(step: WarshallStep) {
        log("Шаг: k=${step.k + 1}, i=${step.i + 1}, j=${step.j + 1} — ${step.message}")
    }
}