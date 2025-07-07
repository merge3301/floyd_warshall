package ui

import javafx.geometry.VPos
import javafx.scene.layout.Pane
import javafx.scene.paint.Color
import javafx.scene.shape.Circle
import javafx.scene.shape.Line
import javafx.scene.shape.Polygon
import javafx.scene.text.Font
import javafx.scene.text.Text
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Набор утилит для отрисовки и интерактивного обновления графа на JavaFX Pane.
 * Позволяет строить рёбра, вершины, петли, а также полностью центрировать номера вершин.
 *
 * Используется для визуализации шагов алгоритма Уоршелла.
 */
object GraphDrawingUtils {

    /**
     * Полностью обновляет граф (все рёбра, лупы, вершины и номера) на указанном Pane.
     *
     * @param graphLayer   Pane, на котором размещается граф
     * @param matrixInput  Источник матрицы смежности (MatrixInput)
     * @param size         Размер графа (число вершин)
     * @param highlights   Список подсвечиваемых рёбер/ячеек [(i, j, тип)]
     * @param nodePositions Список координат вершин (обновляется при drag)
     * @param edgeMap      Мапа рёбер (i, j) → (Line, Polygon), для быстрой перерисовки
     * @param nodeMap      Мапа вершин (i → Circle)
     * @param onVertexClicked Коллбек для клика по вершине
     * @param selectedNodes Список номеров выделенных вершин (по умолчанию пуст)
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
        // Очищаем всё на слое перед новой отрисовкой
        graphLayer.children.clear()
        edgeMap.keys.removeIf { it.first >= size || it.second >= size }
        nodeMap.clear()

        // Добавляем рёбра (в т.ч. "phantom" — пунктирные)
        val allEdges = buildEdges(graphLayer, matrixInput, size, highlights, nodePositions, edgeMap)
        // Добавляем петли (loops)
        drawLoops(graphLayer, matrixInput, size, nodePositions)
        // Добавляем вершины и подписи
        val (circles, labels) = buildNodes(
            graphLayer, size, nodePositions, selectedNodes,
            matrixInput, highlights, edgeMap, onVertexClicked
        )
        // Последовательное добавление: рёбра → вершины → лейблы (чтобы лейблы были поверх)
        allEdges.forEach { (l, a) -> graphLayer.children += listOf(l, a) }
        graphLayer.children += circles
        graphLayer.children += labels
        // Обновляем nodeMap для быстрой адресации по номеру вершины
        circles.forEachIndexed { idx, c -> nodeMap[idx] = c }
    }

    // -------------------- EDGES --------------------

    /**
     * Строит и возвращает список рёбер (Line + Polygon) для текущего состояния графа.
     *
     * @return Список пар (Line, Polygon) для отрисовки на Pane
     */
    private fun buildEdges(
        graphLayer: Pane,
        matrixInput: MatrixInput,
        size: Int,
        highlights: List<Triple<Int, Int, String>>,
        nodePositions: List<Pair<Double, Double>>,
        edgeMap: MutableMap<Pair<Int, Int>, Pair<Line, Polygon>>
    ): List<Pair<Line, Polygon>> {
        val edges = mutableListOf<Pair<Line, Polygon>>()
        // Вспомогательный addArrow: рисует ребро и применяет стиль
        fun addArrow(i: Int, j: Int, style: (Line, Polygon) -> Unit = { _, _ -> }) {
            val (l, a) = arrow(nodePositions[i], nodePositions[j])
            style(l, a)
            edges += l to a
            edgeMap[i to j] = l to a
        }
        // Phantom edges (кандидаты, пунктирные)
        for ((i, j, type) in highlights) if (type == "candidate" && i != j) {
            if (matrixInput.matrixFields[i][j].text.trim().toIntOrNull() != 1) {
                addArrow(i, j) { l, a ->
                    l.strokeDashArray += listOf(8.0, 8.0)
                    l.stroke = Color.ORANGE
                    l.opacity = 0.6
                    a.fill = Color.ORANGE
                    a.opacity = 0.4
                }
            }
        }
        // Обычные рёбра (по матрице смежности)
        for (i in 0 until size) for (j in 0 until size) if (i != j) {
            if (matrixInput.matrixFields[i][j].text.trim().toIntOrNull() == 1) {
                val added = highlights.any { it.first == i && it.second == j && it.third == "added" }
                addArrow(i, j) { l, a ->
                    if (added) {
                        l.stroke = Color.FORESTGREEN
                        l.strokeWidth = 3.0
                        a.fill = Color.FORESTGREEN
                    } else {
                        l.stroke = Color.DARKBLUE
                        l.strokeWidth = 2.5
                        a.fill = Color.DARKBLUE
                    }
                }
            }
        }
        return edges
    }

    // -------------------- LOOPS --------------------

    /**
     * Рисует все петли (loops) для вершин, у которых matrix[i][i] == 1.
     *
     * @param graphLayer   Слой для рисования
     * @param matrixInput  Матрица смежности
     * @param size         Количество вершин
     * @param nodePositions Список координат всех вершин
     */
    private fun drawLoops(
        graphLayer: Pane,
        matrixInput: MatrixInput,
        size: Int,
        nodePositions: List<Pair<Double, Double>>
    ) {
        val r = 175.0
        val cx = 600.0
        val cy = 600.0
        repeat(size) { i ->
            if (matrixInput.matrixFields[i][i].text.trim().toIntOrNull() == 1) {
                val (x, y) = nodePositions[i]
                graphLayer.children += loop(x, y, cx, cy)
            }
        }
    }

    // -------------------- NODES --------------------

    /**
     * Рисует вершины (Circle) и подписи (Text), полностью центрируя номер по центру круга.
     * Реализует drag'n'drop для каждой вершины.
     *
     * @return Пара: список Circle (вершины) и список Text (номера)
     */
    private fun buildNodes(
        graphLayer: Pane,
        size: Int,
        nodePositions: MutableList<Pair<Double, Double>>,
        selected: Collection<Int>,
        matrixInput: MatrixInput,
        highlights: List<Triple<Int, Int, String>>,
        edgeMap: MutableMap<Pair<Int, Int>, Pair<Line, Polygon>>,
        onVertexClicked: (Int) -> Unit
    ): Pair<List<Circle>, List<Text>> {
        val circles = mutableListOf<Circle>()
        val labels  = mutableListOf<Text>()

        repeat(size) { i ->
            val (x, y) = nodePositions[i]
            val hl = i in selected
            val circle = Circle(x, y, 22.0).apply {
                fill = if (hl) Color.GOLD else Color.LIGHTBLUE
                stroke = if (hl) Color.ORANGE else Color.DODGERBLUE
                strokeWidth = if (hl) 3.5 else 2.0
            }
            val label = Text((i + 1).toString()).apply {
                font = Font.font(17.0)
                fill = if (hl) Color.DARKRED else Color.BLACK
                textOrigin = VPos.TOP
                isMouseTransparent = true
                // Центрирование label вручную
                relocate(x - boundsInLocal.width / 2, y - boundsInLocal.height / 2)
            }

            // Drag — центрируем label при каждом движении
            var offX = 0.0; var offY = 0.0
            circle.setOnMousePressed { e ->
                val p = graphLayer.sceneToLocal(e.sceneX, e.sceneY)
                offX = p.x - circle.centerX
                offY = p.y - circle.centerY
                circle.stroke = Color.FIREBRICK
                circle.toFront()
                label.toFront()
                e.consume()
            }
            circle.setOnMouseDragged { e ->
                val p = graphLayer.sceneToLocal(e.sceneX, e.sceneY)
                val nx = p.x - offX
                val ny = p.y - offY
                nodePositions[i] = nx to ny
                circle.centerX = nx
                circle.centerY = ny
                label.relocate(nx - label.boundsInLocal.width / 2, ny - label.boundsInLocal.height / 2)
                redrawEdges(graphLayer, nodePositions, matrixInput, highlights, edgeMap)
                e.consume()
            }
            circle.setOnMouseReleased { e ->
                circle.stroke = if (hl) Color.ORANGE else Color.DODGERBLUE
                e.consume()
            }
            circle.setOnMouseClicked { onVertexClicked(i) }
            circles += circle
            labels += label
        }
        return circles to labels
    }

    // -------------------- PRIMITIVES --------------------

    /**
     * Рисует ориентированное ребро (Line + Polygon-стрелка) между двумя вершинами.
     * Координаты корректируются так, чтобы линия не заходила внутрь круга.
     *
     * @param from Начальная точка (координаты центра)
     * @param to   Конечная точка (координаты центра)
     * @return Пара (Line, Polygon) — линия и стрелка
     */
    private fun arrow(from: Pair<Double, Double>, to: Pair<Double, Double>): Pair<Line, Polygon> {
        val (fx, fy) = from
        val (tx, ty) = to
        val dx = tx - fx; val dy = ty - fy; val len = hypot(dx, dy)
        val r = 22.0
        val sx = fx + dx * r / len
        val sy = fy + dy * r / len
        val ex = tx - dx * r / len
        val ey = ty - dy * r / len
        val line = Line(sx, sy, ex, ey).apply { stroke = Color.DARKBLUE; strokeWidth = 2.5 }
        val ang = atan2(ey - sy, ex - sx); val size = 15.0
        val x1 = ex - size * cos(ang - Math.PI / 10); val y1 = ey - size * sin(ang - Math.PI / 10)
        val x2 = ex - size * cos(ang + Math.PI / 10); val y2 = ey - size * sin(ang + Math.PI / 10)
        val poly = Polygon(ex, ey, x1, y1, x2, y2).apply { fill = Color.DARKBLUE }
        return line to poly
    }

    /**
     * Рисует "петлю" у вершины (самореферентное ребро).
     *
     * @param x X-координата вершины
     * @param y Y-координата вершины
     * @param cx X-координата центра сцены
     * @param cy Y-координата центра сцены
     * @return Круг (Circle), стилизованный как лупа
     */
    private fun loop(x: Double, y: Double, cx: Double, cy: Double): Circle {
        val r = 16.0
        val shift = 24.0
        val vx = x - cx; val vy = y - cy; val len = hypot(vx, vy)
        val n = if (len > 1e-6) 1 / len else 0.0
        val dx = vx * n; val dy = vy * n
        val lx = x + dx * shift - dy * 6
        val ly = y + dy * shift + dx * 6
        return Circle(lx, ly, r).apply {
            fill = Color.TRANSPARENT
            stroke = Color.DARKMAGENTA
            strokeWidth = 2.2
        }
    }

    // -------------------- QUICK EDGE REDRAW --------------------

    /**
     * Быстро перерисовывает только рёбра (используется при drag вершины).
     */
    private fun redrawEdges(
        graphLayer: Pane,
        nodePositions: List<Pair<Double, Double>>,
        matrixInput: MatrixInput,
        highlights: List<Triple<Int, Int, String>>,
        edgeMap: MutableMap<Pair<Int, Int>, Pair<Line, Polygon>>
    ) {
        graphLayer.children.removeIf { it is Line || it is Polygon }
        edgeMap.clear()
        buildEdges(graphLayer, matrixInput, nodePositions.size, highlights, nodePositions, edgeMap)
            .forEach { (l, a) -> graphLayer.children += listOf(l, a) }
    }
}