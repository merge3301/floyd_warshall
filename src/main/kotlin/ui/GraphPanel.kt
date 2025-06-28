package ui

import javafx.beans.value.ObservableValue
import javafx.scene.input.MouseButton
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.Pane
import javafx.scene.layout.StackPane
import javafx.scene.paint.Color
import javafx.scene.shape.*
import javafx.scene.text.Font
import javafx.scene.text.Text
import kotlin.math.*

class GraphPanel(val matrixInput: MatrixInput) {
    // Просто графика — без заливки фона
    private val graphLayer = Pane().apply {
        prefWidth = 20000.0
        prefHeight = 20000.0
        // Без background!
        style = ""
    }
    val view: StackPane

    private val nodePositions = mutableListOf<Pair<Double, Double>>()
    private var scale = 1.0
    private var dragOriginX = 0.0
    private var dragOriginY = 0.0
    private var paneOriginX = 0.0
    private var paneOriginY = 0.0

    init {
        // Только слой графа
        view = StackPane().apply {
            children.add(graphLayer)
            prefWidth = 1200.0
            prefHeight = 1200.0
            // Можно здесь задать фон, если нужно:
            // style = "-fx-background-color: #ececec;" // светло-серый фон
        }

        // Масштабирование колесом мыши
        view.addEventFilter(ScrollEvent.SCROLL) { event ->
            val zoomFactor = if (event.deltaY > 0) 1.1 else 1 / 1.1
            scale = (scale * zoomFactor).coerceIn(0.2, 5.0)
            graphLayer.scaleX = scale
            graphLayer.scaleY = scale
            event.consume()
        }

        // Перетаскивание мышкой
        view.setOnMousePressed { event ->
            if (event.button == MouseButton.PRIMARY) {
                dragOriginX = event.sceneX
                dragOriginY = event.sceneY
                paneOriginX = graphLayer.translateX
                paneOriginY = graphLayer.translateY
            }
        }
        view.setOnMouseDragged { event ->
            if (event.button == MouseButton.PRIMARY) {
                graphLayer.translateX = paneOriginX + (event.sceneX - dragOriginX)
                graphLayer.translateY = paneOriginY + (event.sceneY - dragOriginY)
            }
        }

        // Слушатель размера графа
        matrixInput.sizeSpinner.valueProperty().addListener { _: ObservableValue<out Int>?, _, newSize ->
            setupFieldListeners(newSize as Int)
            updateVisualizationNodes(newSize)
        }
        // Инициализация
        setupFieldListeners(matrixInput.sizeSpinner.value)
        updateVisualizationNodes(matrixInput.sizeSpinner.value)
    }

    private fun setupFieldListeners(size: Int) {
        for (i in 0 until size) {
            for (j in 0 until size) {
                val field = matrixInput.matrixFields[i][j]
                field.textProperty().addListener { _: ObservableValue<out String>?, _: String?, _: String? ->
                    updateVisualizationNodes(size)
                }
            }
        }
    }

    private fun updateVisualizationNodes(size: Int) {
        graphLayer.children.clear()
        nodePositions.clear()

        val radius = 175.0
        val centerX = 600.0
        val centerY = 600.0

        // Вершины
        for (i in 0 until size) {
            val angle = 2 * Math.PI * i / size
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            nodePositions.add(Pair(x, y))
        }
        // Рёбра
        for (i in 0 until size) {
            for (j in 0 until size) {
                val value = matrixInput.matrixFields[i][j].text.trim().toIntOrNull() ?: 0
                if (value == 1 && i != j) {
                    val (x1, y1) = nodePositions[i]
                    val (x2, y2) = nodePositions[j]
                    drawArrow(x1, y1, x2, y2)
                }
            }
        }
        // Петли
        for (i in 0 until size) {
            val value = matrixInput.matrixFields[i][i].text.trim().toIntOrNull() ?: 0
            if (value == 1) {
                val (x, y) = nodePositions[i]
                drawLoop(x, y, centerX, centerY)
            }
        }
        // Круги и подписи
        for (i in 0 until size) {
            val (x, y) = nodePositions[i]
            val node = Circle(x, y, 22.0, Color.LIGHTBLUE)
            node.stroke = Color.DODGERBLUE
            node.strokeWidth = 2.0
            val label = Text(x - 7, y + 6, (i + 1).toString())
            label.font = Font.font(17.0)
            graphLayer.children.add(node)
            graphLayer.children.add(label)
        }
    }

    private fun drawArrow(fromX: Double, fromY: Double, toX: Double, toY: Double) {
        val dx = toX - fromX
        val dy = toY - fromY
        val len = sqrt(dx * dx + dy * dy)
        if (len < 1e-6) return
        val radius = 22.0
        val fx = fromX + dx * radius / len
        val fy = fromY + dy * radius / len
        val tx = toX - dx * radius / len
        val ty = toY - dy * radius / len
        val line = Line(fx, fy, tx, ty)
        line.stroke = Color.DARKBLUE
        line.strokeWidth = 2.5
        val angle = atan2(ty - fy, tx - fx)
        val arrowLength = 15.0
        val x1 = tx - arrowLength * cos(angle - Math.PI / 10)
        val y1 = ty - arrowLength * sin(angle - Math.PI / 10)
        val x2 = tx - arrowLength * cos(angle + Math.PI / 10)
        val y2 = ty - arrowLength * sin(angle + Math.PI / 10)
        val arrowHead = Polygon(tx, ty, x1, y1, x2, y2)
        arrowHead.fill = Color.DARKBLUE
        graphLayer.children.add(line)
        graphLayer.children.add(arrowHead)
    }

    private fun drawLoop(x: Double, y: Double, cx: Double, cy: Double) {
        val vx = x - cx
        val vy = y - cy
        val len = sqrt(vx * vx + vy * vy)
        val norm = if (len > 0.001) 1.0 / len else 0.0
        val dx = vx * norm
        val dy = vy * norm
        val loopCenterX = x + dx * 30 - dy * 22
        val loopCenterY = y + dy * 30 + dx * 22
        val arc = Arc(loopCenterX, loopCenterY, 18.0, 14.0, 120.0, 300.0)
        arc.stroke = Color.DARKBLUE
        arc.strokeWidth = 2.2
        arc.fill = Color.TRANSPARENT
        arc.type = ArcType.OPEN
        val angle = Math.toRadians(120.0 + 300.0)
        val endX = loopCenterX + 18.0 * cos(angle)
        val endY = loopCenterY + 14.0 * sin(angle)
        val arrowLength = 10.0
        val arrowAngle = Math.toRadians(18.0)
        val arrowX1 = endX - arrowLength * cos(angle - arrowAngle)
        val arrowY1 = endY - arrowLength * sin(angle - arrowAngle)
        val arrowX2 = endX - arrowLength * cos(angle + arrowAngle)
        val arrowY2 = endY - arrowLength * sin(angle + arrowAngle)
        val arrowHead = Polygon(endX, endY, arrowX1, arrowY1, arrowX2, arrowY2)
        arrowHead.fill = Color.DARKBLUE
        graphLayer.children.add(arc)
        graphLayer.children.add(arrowHead)
    }
}