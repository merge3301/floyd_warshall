package ui

import javafx.scene.layout.Pane
import javafx.scene.shape.*
import javafx.scene.text.Text
import javafx.scene.text.Font
import javafx.scene.paint.Color
import kotlin.math.*

/**
 * Вспомогательные функции для визуализации графа на Pane.
 *
 * Используется для отрисовки рёбер, луп, вершин, drag'n'drop перемещений,
 * а также для подсветки текущих шагов алгоритма Уоршелла.
 */
object GraphDrawingUtils {

    /**
     * Перерисовывает граф: вершины, рёбра, лупы и лейблы.
     *
     * @param graphLayer Слой (Pane), на который всё рисуется.
     * @param matrixInput Матрица смежности (в т.ч. для получения содержимого и текстовых полей).
     * @param size Размер графа (количество вершин).
     * @param highlights Подсветки рёбер/вершин [(i, j, тип)], например, "added", "candidate".
     * @param nodePositions Список координат вершин (обновляется при drag).
     * @param edgeMap Мапа рёбер (i, j) → (Line, Polygon).
     * @param nodeMap Мапа вершин (i) → Circle.
     * @param onVertexClicked Callback для клика по вершине (передаёт номер вершины).
     * @param selectedNodes Список выделенных вершин (по умолчанию пуст).
     */
    fun updateVisualizationNodes(
        graphLayer: Pane,
        matrixInput: MatrixInput,
        size: Int,
        highlights: List<Triple<Int, Int, String>>,
        nodePositions: MutableList<Pair<Double, Double>>,
        edgeMap: MutableMap<Pair<Int, Int>, Pair<Line, Polygon>>,
        nodeMap: MutableMap<Int, Circle>,
        onVertexClicked: (Int) -> Unit,
        selectedNodes: Collection<Int> = emptyList()
    ) {
        graphLayer.children.clear()
        edgeMap.keys.removeIf { it.first >= size || it.second >= size }
        nodeMap.clear()

        val radius = 175.0
        val centerX = 600.0
        val centerY = 600.0

        // Только если не хватает позиций — расставляем по кругу
        if (nodePositions.size < size) {
            // Добавляем только новые позиции!
            val radius = 175.0
            val centerX = 600.0
            val centerY = 600.0
            for (i in nodePositions.size until size) {
                val angle = 2 * Math.PI * i / size
                val x = centerX + radius * cos(angle)
                val y = centerY + radius * sin(angle)
                nodePositions.add(Pair(x, y))
            }
        } else if (nodePositions.size > size) {
            // Удаляем только лишние
            while (nodePositions.size > size) nodePositions.removeAt(nodePositions.size - 1)
        }

        // Обычные рёбра и phantom edges
        val allEdges = mutableListOf<Pair<Line, Polygon>>()
        for ((i, j, type) in highlights) {
            if (type == "candidate") {
                val exists = (i in 0 until size) && (j in 0 until size) &&
                        (matrixInput.matrixFields[i][j].text.trim().toIntOrNull() == 1)
                if (!exists && i != j) {
                    val (x1, y1) = nodePositions[i]
                    val (x2, y2) = nodePositions[j]
                    val (line, arrow) = drawArrow(x1, y1, x2, y2)
                    line.strokeDashArray.addAll(8.0, 8.0)
                    line.stroke = Color.ORANGE
                    line.opacity = 0.6
                    arrow.fill = Color.ORANGE
                    arrow.opacity = 0.4
                    allEdges.add(line to arrow)
                }
            }
        }
        for (i in 0 until size) {
            for (j in 0 until size) {
                val value = matrixInput.matrixFields[i][j].text.trim().toIntOrNull() ?: 0
                if (value == 1 && i != j) {
                    val (x1, y1) = nodePositions[i]
                    val (x2, y2) = nodePositions[j]
                    val (line, arrow) = drawArrow(x1, y1, x2, y2)
                    val isAdded = highlights.any { it.first == i && it.second == j && it.third == "added" }
                    if (isAdded) {
                        line.stroke = Color.FORESTGREEN
                        arrow.fill = Color.FORESTGREEN
                        line.strokeWidth = 3.0
                    } else {
                        line.stroke = Color.DARKBLUE
                        arrow.fill = Color.DARKBLUE
                        line.strokeWidth = 2.5
                    }
                    edgeMap[i to j] = line to arrow
                    allEdges.add(line to arrow)
                }
            }
        }

        // Лупы
        for (i in 0 until size) {
            val value = matrixInput.matrixFields[i][i].text.trim().toIntOrNull() ?: 0
            if (value == 1) {
                val (x, y) = nodePositions[i]
                val loop = drawLoop(x, y, centerX, centerY)
                graphLayer.children.add(loop)
            }
        }

        // Вершины и лейблы (ОТДЕЛЬНО, чтобы всегда были сверху!)
        val nodeCircles = mutableListOf<Circle>()
        val nodeLabels = mutableListOf<Text>()
        for (i in 0 until size) {
            val (x, y) = nodePositions[i]
            val highlighted = selectedNodes.contains(i)
            val node = Circle(x, y, 22.0,
                if (highlighted) Color.GOLD else Color.LIGHTBLUE).apply {
                stroke = if (highlighted) Color.ORANGE else Color.DODGERBLUE
                strokeWidth = if (highlighted) 3.5 else 2.0
            }
            val label = Text(x - 7, y + 6, (i + 1).toString()).apply {
                font = Font.font(17.0)
                fill = if (highlighted) Color.DARKRED else Color.BLACK
                mouseTransparentProperty().set(true) // чтобы не мешал drag
            }
            // --- DRAG'n'DROP ---
            var dragDeltaX = 0.0
            var dragDeltaY = 0.0
            node.setOnMousePressed { event ->
                dragDeltaX = event.sceneX - node.centerX
                dragDeltaY = event.sceneY - node.centerY
                node.stroke = Color.FIREBRICK
                node.toFront()
                event.consume()
            }
            node.setOnMouseDragged { event ->
                node.centerX = event.sceneX - dragDeltaX
                node.centerY = event.sceneY - dragDeltaY
                nodePositions[i] = node.centerX to node.centerY
                // Передвигаем label вместе с node
                label.x = node.centerX - 7
                label.y = node.centerY + 6
                // Перерисовываем только edges!
                redrawEdges(graphLayer, nodePositions, matrixInput, highlights, edgeMap)
                event.consume()
            }
            node.setOnMouseReleased { event ->
                node.stroke = if (highlighted) Color.ORANGE else Color.DODGERBLUE
                event.consume()
            }
            node.setOnMouseClicked { onVertexClicked(i) }
            nodeCircles.add(node)
            nodeLabels.add(label)
            nodeMap[i] = node
        }

        // 1. Рёбра и лупы
        allEdges.forEach { (line, arrow) -> graphLayer.children.addAll(line, arrow) }
        // 2. Вершины
        nodeCircles.forEach { graphLayer.children.add(it) }
        // 3. Лейблы
        nodeLabels.forEach { graphLayer.children.add(it) }
    }

    /**
     * Перерисовывает только рёбра (без вершин и лейблов).
     * Используется при перемещении (drag'n'drop) вершины.
     *
     * @param graphLayer Слой для рёбер.
     * @param nodePositions Координаты всех вершин.
     * @param matrixInput Матрица смежности.
     * @param highlights Текущие подсветки рёбер.
     * @param edgeMap Мапа для хранения новых рёбер.
     */
    private fun redrawEdges(
        graphLayer: Pane,
        nodePositions: List<Pair<Double, Double>>,
        matrixInput: MatrixInput,
        highlights: List<Triple<Int, Int, String>>,
        edgeMap: MutableMap<Pair<Int, Int>, Pair<Line, Polygon>>
    ) {
        // Сначала удаляем старые рёбра
        graphLayer.children.removeIf { it is Line || it is Polygon }
        edgeMap.clear()

        val size = nodePositions.size
        for ((i, j, type) in highlights) {
            if (type == "candidate") {
                val exists = (i in 0 until size) && (j in 0 until size) &&
                        (matrixInput.matrixFields[i][j].text.trim().toIntOrNull() == 1)
                if (!exists && i != j) {
                    val (x1, y1) = nodePositions[i]
                    val (x2, y2) = nodePositions[j]
                    val (line, arrow) = drawArrow(x1, y1, x2, y2)
                    line.strokeDashArray.addAll(8.0, 8.0)
                    line.stroke = Color.ORANGE
                    line.opacity = 0.6
                    arrow.fill = Color.ORANGE
                    arrow.opacity = 0.4
                    graphLayer.children.addAll(line, arrow)
                }
            }
        }
        for (i in 0 until size) {
            for (j in 0 until size) {
                val value = matrixInput.matrixFields[i][j].text.trim().toIntOrNull() ?: 0
                if (value == 1 && i != j) {
                    val (x1, y1) = nodePositions[i]
                    val (x2, y2) = nodePositions[j]
                    val (line, arrow) = drawArrow(x1, y1, x2, y2)
                    val isAdded = highlights.any { it.first == i && it.second == j && it.third == "added" }
                    if (isAdded) {
                        line.stroke = Color.FORESTGREEN
                        arrow.fill = Color.FORESTGREEN
                        line.strokeWidth = 3.0
                    } else {
                        line.stroke = Color.DARKBLUE
                        arrow.fill = Color.DARKBLUE
                        line.strokeWidth = 2.5
                    }
                    edgeMap[i to j] = line to arrow
                    graphLayer.children.addAll(line, arrow)
                }
            }
        }
    }

    /**
     * Рисует ориентированное ребро (Line+Polygon-стрелка) между двумя точками.
     * Рёбра корректно укорачиваются до границ кружков.
     *
     * @param fromX X-координата начальной вершины.
     * @param fromY Y-координата начальной вершины.
     * @param toX X-координата конечной вершины.
     * @param toY Y-координата конечной вершины.
     * @return Пара (Line, Polygon-стрелка).
     */
    private fun drawArrow(fromX: Double, fromY: Double, toX: Double, toY: Double): Pair<Line, Polygon> {
        val dx = toX - fromX
        val dy = toY - fromY
        val len = sqrt(dx * dx + dy * dy)
        val r = 22.0
        val fx = fromX + dx * r / len
        val fy = fromY + dy * r / len
        val tx = toX - dx * r / len
        val ty = toY - dy * r / len

        val line = Line(fx, fy, tx, ty).apply {
            stroke = Color.DARKBLUE
            strokeWidth = 2.5
        }

        val angle = atan2(ty - fy, tx - fx)
        val arrowSize = 15.0
        val x1 = tx - arrowSize * cos(angle - Math.PI / 10)
        val y1 = ty - arrowSize * sin(angle - Math.PI / 10)
        val x2 = tx - arrowSize * cos(angle + Math.PI / 10)
        val y2 = ty - arrowSize * sin(angle + Math.PI / 10)

        val arrow = Polygon(tx, ty, x1, y1, x2, y2).apply {
            fill = Color.DARKBLUE
        }
        return line to arrow
    }

    /**
     * Рисует "луп" (петлю) на вершине.
     *
     * @param x X-координата вершины.
     * @param y Y-координата вершины.
     * @param cx X-координата центра сцены (для отступа).
     * @param cy Y-координата центра сцены.
     * @return Круг (Circle), стилизованный как лупа.
     */
    private fun drawLoop(x: Double, y: Double, cx: Double, cy: Double): Circle {
        val loopRadius = 16.0
        val shift = 24.0
        val vx = x - cx
        val vy = y - cy
        val len = sqrt(vx * vx + vy * vy)
        val norm = if (len > 0.001) 1.0 / len else 0.0
        val dx = vx * norm
        val dy = vy * norm
        val loopCenterX = x + dx * shift - dy * 6
        val loopCenterY = y + dy * shift + dx * 6
        return Circle(loopCenterX, loopCenterY, loopRadius, Color.TRANSPARENT).apply {
            stroke = Color.DARKMAGENTA
            strokeWidth = 2.2
        }
    }
}