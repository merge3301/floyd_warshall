package ui

import javafx.beans.value.ObservableValue
import javafx.scene.input.MouseButton
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.*
import kotlin.math.*

class GraphPanel(val matrixInput: MatrixInput) {
    private val graphLayer = Pane().apply {
        prefWidth = 20000.0
        prefHeight = 20000.0
    }
    val view: StackPane

    internal val nodePositions = mutableListOf<Pair<Double, Double>>()
    internal val edgeMap = mutableMapOf<Pair<Int, Int>, Pair<javafx.scene.shape.Line, javafx.scene.shape.Polygon>>()
    internal val nodeMap = mutableMapOf<Int, javafx.scene.shape.Circle>()

    private var scale = 1.0
    private var dragOriginX = 0.0
    private var dragOriginY = 0.0
    private var paneOriginX = 0.0
    private var paneOriginY = 0.0

    private var selectedNode: Int? = null

    init {
        view = StackPane(graphLayer).apply {
            prefWidth = 1200.0
            prefHeight = 1200.0
        }

        view.addEventFilter(ScrollEvent.SCROLL) { event ->
            val zoom = if (event.deltaY > 0) 1.1 else 1 / 1.1
            scale = (scale * zoom).coerceIn(0.2, 5.0)
            graphLayer.scaleX = scale
            graphLayer.scaleY = scale
            event.consume()
        }

        view.setOnMousePressed {
            if (it.button == MouseButton.PRIMARY && !overNode(it.x, it.y)) {
                dragOriginX = it.sceneX
                dragOriginY = it.sceneY
                paneOriginX = graphLayer.translateX
                paneOriginY = graphLayer.translateY
            }
        }
        view.setOnMouseDragged {
            if (it.button == MouseButton.PRIMARY) {
                graphLayer.translateX = paneOriginX + (it.sceneX - dragOriginX)
                graphLayer.translateY = paneOriginY + (it.sceneY - dragOriginY)
            }
        }

        view.setOnMouseClicked { event ->
            if (event.button == MouseButton.SECONDARY) {
                if (!overNode(event.x, event.y)) {
                    addNewNode(event.x, event.y)
                }
            }
        }

        // Слушатель для sizeSpinner — только добавляем новые позиции
        matrixInput.sizeSpinner.valueProperty().addListener { _: ObservableValue<out Int>?, oldSize, newSize ->
            val old = oldSize as? Int ?: nodePositions.size
            val newS = newSize as Int
            if (newS > old) {
                // Добавить новые позиции на окружности (или по какому-то своему правилу)
                val radius = 175.0
                val centerX = 600.0
                val centerY = 600.0
                for (i in nodePositions.size until newS) {
                    val angle = 2 * Math.PI * i / newS
                    val x = centerX + radius * cos(angle)
                    val y = centerY + radius * sin(angle)
                    nodePositions.add(Pair(x, y))
                }
            } else if (newS < old && newS < nodePositions.size) {
                // Удалить лишние
                while (nodePositions.size > newS) nodePositions.removeAt(nodePositions.size - 1)
            }
            setupFieldListeners(newS)
            updateVisualizationNodes(newS)
        }

        setupFieldListeners(matrixInput.sizeSpinner.value)
        if (nodePositions.isEmpty()) {
            // Инициализация для старта
            val size = matrixInput.sizeSpinner.value
            val radius = 175.0
            val centerX = 600.0
            val centerY = 600.0
            for (i in 0 until size) {
                val angle = 2 * Math.PI * i / size
                val x = centerX + radius * cos(angle)
                val y = centerY + radius * sin(angle)
                nodePositions.add(Pair(x, y))
            }
        }
        updateVisualizationNodes(matrixInput.sizeSpinner.value)
    }

    // Главный метод для визуализации: все выделения только через параметры!
    fun updateGraph(
        matrix: Array<IntArray>,
        highlights: List<Triple<Int, Int, String>> = emptyList(),
        highlightedNodes: Collection<Int> = emptyList()
    ) {
        matrixInput.updateMatrixDisplay(matrix)
        updateVisualizationNodes(matrix.size, highlights, highlightedNodes)
    }

    private fun setupFieldListeners(size: Int) {
        for (i in 0 until size) {
            for (j in 0 until size) {
                val field = matrixInput.matrixFields[i][j]
                field.textProperty().addListener { _: ObservableValue<out String>?, _, _ ->
                    updateVisualizationNodes(size)
                }
            }
        }
    }

    private fun onVertexClicked(idx: Int) {
        if (selectedNode == null) {
            selectedNode = idx
            updateVisualizationNodes(matrixInput.sizeSpinner.value, emptyList(), listOf(idx))
        } else if (selectedNode == idx) {
            selectedNode = null
            updateVisualizationNodes(matrixInput.sizeSpinner.value)
        } else {
            val i = selectedNode!!
            val j = idx
            if (i != j) {
                val field = matrixInput.matrixFields[i][j]
                field.text = if ((field.text.toIntOrNull() ?: 0) == 1) "0" else "1"
            }
            selectedNode = null
            updateVisualizationNodes(matrixInput.sizeSpinner.value)
        }
    }

    private fun addNewNode(x: Double, y: Double) {
        // Просто добавляем одну новую позицию вручную!
        matrixInput.sizeSpinner.valueFactory.value = matrixInput.sizeSpinner.valueFactory.value + 1
        nodePositions.add(Pair(x, y))
        updateVisualizationNodes(matrixInput.sizeSpinner.value)
    }

    private fun overNode(x: Double, y: Double): Boolean {
        for ((_, pos) in nodePositions.withIndex()) {
            val dx = x - pos.first
            val dy = y - pos.second
            if (dx * dx + dy * dy < 24.0 * 24.0) return true
        }
        return false
    }

    private fun updateVisualizationNodes(
        size: Int,
        highlights: List<Triple<Int, Int, String>> = emptyList(),
        selectedNodes: Collection<Int> = emptyList()
    ) {
        GraphDrawingUtils.updateVisualizationNodes(
            graphLayer, matrixInput, size, highlights,
            nodePositions, edgeMap, nodeMap, ::onVertexClicked, selectedNodes
        )
    }
}