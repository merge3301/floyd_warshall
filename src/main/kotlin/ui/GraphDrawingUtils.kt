package ui

import javafx.scene.layout.Pane
import javafx.scene.shape.*
import javafx.scene.text.Text
import javafx.scene.text.Font
import javafx.scene.paint.Color
import kotlin.math.*

object GraphDrawingUtils {
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
        nodePositions.clear()
        edgeMap.clear()
        nodeMap.clear()

        val radius = 175.0
        val centerX = 600.0
        val centerY = 600.0

        for (i in 0 until size) {
            val angle = 2 * Math.PI * i / size
            val x = centerX + radius * cos(angle)
            val y = centerY + radius * sin(angle)
            nodePositions.add(Pair(x, y))
        }

        // PHANTOM EDGES
        for ((i, j, type) in highlights) {
            if (type == "candidate") {
                val exists = (i in 0 until size) && (j in 0 until size) &&
                        (matrixInput.matrixFields[i][j].text.trim().toIntOrNull() == 1)
                if (!exists && i != j) {
                    val (x1, y1) = nodePositions[i]
                    val (x2, y2) = nodePositions[j]
                    val (line, arrow) = drawArrow(x1, y1, x2, y2, graphLayer)
                    line.strokeDashArray.addAll(8.0, 8.0)
                    line.stroke = Color.ORANGE
                    line.opacity = 0.6
                    arrow.fill = Color.ORANGE
                    arrow.opacity = 0.4
                }
            }
        }

        // Обычные рёбра
        for (i in 0 until size) {
            for (j in 0 until size) {
                val value = matrixInput.matrixFields[i][j].text.trim().toIntOrNull() ?: 0
                if (value == 1 && i != j) {
                    val (x1, y1) = nodePositions[i]
                    val (x2, y2) = nodePositions[j]
                    val (line, arrow) = drawArrow(x1, y1, x2, y2, graphLayer)
                    val isAdded = highlights.any { it.first == i && it.second == j && it.third == "added" }
                    if (isAdded) {
                        line.stroke = Color.FORESTGREEN
                        arrow.fill = Color.FORESTGREEN
                        line.strokeWidth = 3.0
                        arrow.opacity = 1.0
                        line.opacity = 1.0
                    } else {
                        line.stroke = Color.DARKBLUE
                        arrow.fill = Color.DARKBLUE
                        line.strokeWidth = 2.5
                        arrow.opacity = 1.0
                        line.opacity = 1.0
                    }
                    edgeMap[i to j] = line to arrow
                }
            }
        }

        // Лупы
        for (i in 0 until size) {
            val value = matrixInput.matrixFields[i][i].text.trim().toIntOrNull() ?: 0
            if (value == 1) {
                val (x, y) = nodePositions[i]
                drawLoop(x, y, centerX, centerY, graphLayer)
            }
        }

        // Вершины
        for (i in 0 until size) {
            val (x, y) = nodePositions[i]
            val highlighted = selectedNodes.contains(i)
            val node = Circle(x, y, 22.0,
                if (highlighted) Color.GOLD else Color.LIGHTBLUE).apply {
                stroke = if (highlighted) Color.ORANGE else Color.DODGERBLUE
                strokeWidth = if (highlighted) 3.5 else 2.0
                setOnMouseClicked { onVertexClicked(i) }
            }
            val label = Text(x - 7, y + 6, (i + 1).toString()).apply {
                font = Font.font(17.0)
                fill = if (highlighted) Color.DARKRED else Color.BLACK
            }
            nodeMap[i] = node
            graphLayer.children.addAll(node, label)
        }
    }

    private fun drawArrow(fromX: Double, fromY: Double, toX: Double, toY: Double, graphLayer: Pane): Pair<Line, Polygon> {
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

        graphLayer.children.addAll(line, arrow)
        return line to arrow
    }

    private fun drawLoop(x: Double, y: Double, cx: Double, cy: Double, graphLayer: Pane) {
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
        val loopCircle = Circle(loopCenterX, loopCenterY, loopRadius, Color.TRANSPARENT).apply {
            stroke = Color.DARKMAGENTA
            strokeWidth = 2.2
        }
        graphLayer.children.add(loopCircle)
    }
}