import javafx.application.Application
import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Label
import javafx.scene.control.ScrollPane
import javafx.scene.control.SplitPane
import javafx.scene.layout.BorderPane
import javafx.scene.layout.HBox
import javafx.scene.layout.Priority
import javafx.scene.layout.VBox
import javafx.stage.Stage
import ui.ControlPanel
import ui.GraphPanel
import ui.MatrixInput

class WarshallVisualizerApp : Application() {
    // Многострочный статус
    private val statusLabel = Label("Статус: Готов к запуску").apply {
        isWrapText = true
        minHeight = 38.0
        maxHeight = 80.0
        style = "-fx-font-size: 14px;"
    }
    private val iterationLabel = Label("Итерация: 0").apply {
        style = "-fx-font-size: 13px;"
    }

    override fun start(primaryStage: Stage) {
        val root = BorderPane()
        val matrixInput = MatrixInput()
        val graphPanel = GraphPanel(matrixInput)

        // Прокидываем все компоненты для поэтапного контроля
        val controlPanel = ControlPanel(
            graphPanel,
            matrixInput,
            statusLabel,
            iterationLabel
        )

        val inputScroll = ScrollPane(matrixInput.view).apply {
            prefWidth = 330.0
            isFitToWidth = true
        }
        val graphWithOverlay = graphPanel.view
        val splitPane = SplitPane()
        splitPane.items.addAll(inputScroll, graphWithOverlay)
        splitPane.setDividerPositions(0.38)

        root.center = splitPane

        // Нижняя панель: control panel + статус-бар
        val bottomBox = VBox(
            controlPanel.view,
            makeStatusBar()
        )
        VBox.setVgrow(controlPanel.view, Priority.NEVER)
        VBox.setVgrow(statusLabel, Priority.ALWAYS)
        root.bottom = bottomBox

        primaryStage.scene = Scene(root, 1050.0, 700.0)
        primaryStage.title = "Визуализатор алгоритма Уоршелла"
        primaryStage.show()
    }

    private fun makeStatusBar(): HBox {
        val bar = HBox(30.0, statusLabel, iterationLabel)
        bar.padding = Insets(7.0, 15.0, 7.0, 15.0)
        bar.alignment = Pos.CENTER_LEFT
        bar.style = "-fx-background-color: #f5f5f5; -fx-border-color: #cccccc; -fx-border-width: 1px 0 0 0;"
        // statusLabel тянется на всю ширину, iterationLabel — справа
        HBox.setHgrow(statusLabel, Priority.ALWAYS)
        statusLabel.maxWidth = Double.MAX_VALUE
        return bar
    }
}

fun main() {
    Application.launch(WarshallVisualizerApp::class.java)
}