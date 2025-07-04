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

/**
 * Основное приложение JavaFX для визуализации алгоритма Уоршелла (транзитивное замыкание).
 *
 * Строит интерфейс из трёх основных компонентов:
 * - Панель ввода матрицы (слева, MatrixInput)
 * - Область визуализации графа (справа, GraphPanel)
 * - Нижняя панель управления (ControlPanel) и статус-бар
 *
 * Все компоненты связаны между собой для интерактивного поэтапного выполнения алгоритма.
 */
class WarshallVisualizerApp : Application() {
    /**
     * Многострочный статус-лейбл для отображения текущего состояния.
     */
    private val statusLabel = Label("Статус: Готов к запуску").apply {
        isWrapText = true
        minHeight = 38.0
        maxHeight = 80.0
        style = "-fx-font-size: 14px;"
    }

    /**
     * Лейбл для текущих индексов алгоритма (итерация по k, i, j).
     */
    private val iterationLabel = Label("Итерация: 0").apply {
        style = "-fx-font-size: 13px;"
    }

    /**
     * Точка входа JavaFX. Формирует все компоненты интерфейса, размещает их в окне.
     *
     * @param primaryStage Главная сцена приложения.
     */
    override fun start(primaryStage: Stage) {
        val root = BorderPane()
        val matrixInput = MatrixInput()
        val graphPanel = GraphPanel(matrixInput)

        // Панель управления, получает ссылки на все компоненты и статус-лейблы
        val controlPanel = ControlPanel(
            graphPanel,
            matrixInput,
            statusLabel,
            iterationLabel
        )

        // Область с прокруткой для ввода матрицы
        val inputScroll = ScrollPane(matrixInput.view).apply {
            prefWidth = 330.0
            isFitToWidth = true
        }
        val graphWithOverlay = graphPanel.view
        val splitPane = SplitPane()
        splitPane.items.addAll(inputScroll, graphWithOverlay)
        splitPane.setDividerPositions(0.38)

        root.center = splitPane

        // Нижняя панель: ControlPanel и статус-бар
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

    /**
     * Формирует статус-бар с лейблами состояния и текущей итерации.
     *
     * @return HBox — горизонтальная панель для нижней части окна.
     */
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

/**
 * Точка входа для запуска приложения.
 */
fun main() {
    Application.launch(WarshallVisualizerApp::class.java)
}