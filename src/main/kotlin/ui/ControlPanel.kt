package ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.Alert
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.Button
import javafx.scene.control.Label
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox

class ControlPanel {
    val startButton = Button("Запуск")
    val stepForward = Button("Шаг >")
    val stepBack = Button("< Шаг")
    val resetButton = Button("Сброс")
    val helpButton = Button("Справка")
    val view: VBox

    init {
        val controlBar = HBox(15.0, startButton, stepForward, stepBack, resetButton, helpButton)
        controlBar.padding = Insets(10.0)
        controlBar.alignment = Pos.CENTER
        controlBar.style = "-fx-background-color: #f0f8ff;"

        view = VBox(controlBar)

        helpButton.setOnAction {
            val alert = Alert(AlertType.INFORMATION)
            alert.title = "Справка"
            alert.headerText = "О программе: Транзитивное замыкание (алгоритм Уоршелла)"
            alert.contentText = """
                Эта программа позволяет вычислить транзитивное замыкание ориентированного графа с помощью алгоритма Уоршелла.

                Вы можете:
                • Ввести матрицу смежности вручную или сгенерировать случайно.
                • Нажать "Запуск" — чтобы увидеть финальный результат.
                • Использовать "Шаг >" и "< Шаг" для пошагового выполнения алгоритма.
                • "Сброс" — возвращает начальное состояние.
                • На каждом шаге в статусе отображается подробное пояснение.

                Транзитивное замыкание показывает, достижима ли вершина j из вершины i по одному или нескольким рёбрам.
            """.trimIndent()
            alert.showAndWait()
        }
    }
}