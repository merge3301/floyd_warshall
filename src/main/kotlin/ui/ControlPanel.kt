package ui

import core.WarshallStepper
import core.WarshallStep
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox

class ControlPanel(
    private val graphPanel: GraphPanel,
    private val matrixInput: MatrixInput,
    private val statusLabel: Label,
    private val iterationLabel: Label
) {
    val startButton = Button("Запуск")
    val stepBack = Button("< Шаг")
    val stepSmall = Button("Малый >")     // по j
    val stepMedium = Button("Средний >>") // по i
    val stepBig = Button("Крупный >>>")   // по k
    val resetButton = Button("Сброс")
    val runAllButton = Button("Выполнить всё")
    val helpButton = Button("Справка")
    val logArea = TextArea().apply {
        isEditable = false
        prefRowCount = 10
        prefHeight = 140.0
        style = "-fx-font-family: monospace; -fx-font-size: 12px;"
    }
    val view: VBox

    private var warshallStepper: WarshallStepper? = null
    private var finishedLogged = false

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

        stepSmall.setOnAction { doSmallStep() }
        stepMedium.setOnAction { doMediumStep() }
        stepBig.setOnAction { doBigStep() }

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

    private fun checkFinished(stepper: WarshallStepper) {
        if (stepper.isFinished() && !finishedLogged) {
            finishedLogged = true
            log("✅ Алгоритм завершён.")
            statusLabel.text = "Статус: Алгоритм завершён."
            setStepButtonsEnabled(false)
            matrixInput.clearHeaderHighlights()
        }
    }

    private fun setStepButtonsEnabled(state: Boolean) {
        runAllButton.isDisable = !state
        stepSmall.isDisable = !state
        stepMedium.isDisable = !state
        stepBig.isDisable = !state
    }

    private fun doSmallStep() {
        val stepper = warshallStepper
        if (stepper != null && !stepper.isFinished()) {
            val step = stepper.stepForward()
            logStep(step)
            updateStep(step)
            checkFinished(stepper)
        }
    }

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

    private fun updateStep(step: WarshallStep) {
        matrixInput.updateMatrixDisplay(step.matrix)
        matrixInput.clearHighlights()
        if (step.involved.isNotEmpty()) {
            matrixInput.highlightCells(step.involved)
        }
        matrixInput.highlightHeader(step.k)  // <-- Подсветка k
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

    private fun log(message: String) {
        logArea.appendText("$message\n")
    }

    private fun logStep(step: WarshallStep) {
        log("Шаг: k=${step.k + 1}, i=${step.i + 1}, j=${step.j + 1} — ${step.message}")
    }
}