import javafx.application.Application
import javafx.scene.Scene
import javafx.scene.control.ScrollPane
import javafx.scene.control.SplitPane
import javafx.scene.layout.BorderPane
import javafx.stage.Stage
import ui.ControlPanel
import ui.GraphPanel
import ui.MatrixInput

class WarshallVisualizerApp : Application() {
    override fun start(primaryStage: Stage) {
        val root = BorderPane()
        val matrixInput = MatrixInput()
        val graphPanel = GraphPanel(matrixInput)
        val controlPanel = ControlPanel()

        val inputScroll = ScrollPane(matrixInput.view).apply {
            prefWidth = 330.0
            isFitToWidth = true
        }

        val splitPane = SplitPane()
        splitPane.items.addAll(inputScroll, graphPanel.view)
        splitPane.setDividerPositions(0.38)

        root.center = splitPane
        root.bottom = controlPanel.view

        primaryStage.scene = Scene(root, 1050.0, 700.0)
        primaryStage.title = "Визуализатор алгоритма Уоршелла"
        primaryStage.show()
    }
}

fun main() {
    Application.launch(WarshallVisualizerApp::class.java)
}