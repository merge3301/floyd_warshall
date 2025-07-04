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
    val stepForward = Button("Шаг >")
    val stepBack = Button("< Шаг")
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
        val controlBar = HBox(15.0, startButton, stepForward, stepBack, runAllButton, resetButton, helpButton).apply {
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
            val step = warshallStepper!!.currentStep()
            logStep(step)
            updateStep(step)
            stepForward.isDisable = false
            runAllButton.isDisable = false
        }

        stepForward.setOnAction {
            val stepper = warshallStepper
            if (stepper != null && !stepper.isFinished()) {
                val step = stepper.stepForward()
                logStep(step)
                updateStep(step)
                checkFinished(stepper)
            }
        }

        stepBack.setOnAction {
            val stepper = warshallStepper
            if (stepper != null) {
                val step = stepper.stepBack()
                logStep(step)
                updateStep(step)
                if (!stepper.isFinished()) {
                    stepForward.isDisable = false
                    runAllButton.isDisable = false
                    finishedLogged = false
                }
            }
        }

        resetButton.setOnAction {
            warshallStepper?.reset()
            finishedLogged = false
            log("🔄 Алгоритм сброшен к начальному состоянию")
            val step = warshallStepper!!.currentStep()
            logStep(step)
            updateStep(step)
            stepForward.isDisable = false
            runAllButton.isDisable = false
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
                • Нажать "Запуск" — чтобы начать поэтапный просмотр.
                • Использовать "Шаг >" для пошагового выполнения алгоритма.
                • Использовать "< Шаг" для возврата на один шаг назад.
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
            stepForward.isDisable = true
            runAllButton.isDisable = true
        }
    }

    private fun updateStep(step: WarshallStep) {
        matrixInput.updateMatrixDisplay(step.matrix)
        matrixInput.clearHighlights()                    // <-- сброс перед каждым шагом
        if (step.involved.isNotEmpty()) {
            matrixInput.highlightCells(step.involved)    // <-- подсвечиваем только нужные
        }
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