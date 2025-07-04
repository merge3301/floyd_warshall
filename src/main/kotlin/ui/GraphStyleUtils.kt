package ui

import javafx.scene.paint.Color
import javafx.scene.shape.*

/**
 * Утилиты для стилизации и подсветки рёбер и вершин графа на визуализации.
 *
 * Позволяет централизованно управлять цветами подсветки, сбросом и выделением элементов графа.
 */
object GraphStyleUtils {

    /**
     * Подсвечивает список рёбер на графе согласно их типу.
     *
     * Для каждого ребра из [edges] ищет соответствующий Line и Polygon в [edgeMap] и окрашивает их:
     * - "candidate" — оранжевый (кандидат на добавление)
     * - "added" — зелёный (новое достижение)
     * - "target" — золотой (текущая проверяемая пара)
     * - "selected" — синий (явно выбранное пользователем)
     * - любое другое значение — серый
     *
     * Перед подсветкой сбрасывает все рёбра к цвету по умолчанию (тёмно-синий).
     *
     * @param edgeMap Словарь: пара вершин → (отрезок, стрелка).
     * @param edges Список рёбер для подсветки: Triple(i, j, type).
     */
    fun highlightEdges(
        edgeMap: Map<Pair<Int, Int>, Pair<Line, Polygon>>,
        edges: List<Triple<Int, Int, String>>
    ) {
        // Сбросить цвета перед новой подсветкой (иначе старые останутся)
        for ((_, pair) in edgeMap) {
            pair.first.stroke = Color.DARKBLUE
            pair.second.fill = Color.DARKBLUE
        }
        for ((i, j, type) in edges) {
            val pair = edgeMap[i to j]
            if (pair != null) {
                val (line, arrow) = pair
                val color = when (type) {
                    "candidate" -> Color.ORANGE
                    "added"     -> Color.FORESTGREEN
                    "target"    -> Color.GOLD
                    "selected"  -> Color.BLUE
                    else        -> Color.GRAY
                }
                line.stroke = color
                arrow.fill = color
            }
        }
    }

    /**
     * Подсвечивает только выбранные вершины.
     *
     * Для каждой вершины из [nodes] задаёт обводку цветом [border] и заливку [color].
     * Остальным вершинам возвращает стиль по умолчанию.
     *
     * @param nodeMap Словарь: номер вершины → Circle.
     * @param nodes Список индексов выделяемых вершин.
     * @param color Цвет заливки выбранных вершин (по умолчанию — оранжевый).
     * @param border Цвет границы выбранных вершин (по умолчанию — синий).
     */
    fun highlightNodes(
        nodeMap: Map<Int, Circle>,
        nodes: List<Int>,
        color: Color = Color.ORANGE,
        border: Color = Color.BLUE
    ) {
        for ((idx, circle) in nodeMap) {
            if (idx in nodes) {
                circle.stroke = border
                circle.strokeWidth = 4.0
                circle.fill = color
            } else {
                circle.stroke = Color.DODGERBLUE
                circle.strokeWidth = 2.0
                circle.fill = Color.LIGHTBLUE
            }
        }
    }

    /**
     * Сбрасывает все подсветки у рёбер и вершин на стандартные значения.
     *
     * Используется для очистки состояния после шагов алгоритма.
     *
     * @param edgeMap Словарь: пара вершин → (отрезок, стрелка).
     * @param nodeMap Словарь: номер вершины → Circle.
     */
    fun clearHighlights(
        edgeMap: Map<Pair<Int, Int>, Pair<Line, Polygon>>,
        nodeMap: Map<Int, Circle>
    ) {
        for ((_, pair) in edgeMap) {
            pair.first.stroke = Color.DARKBLUE
            pair.second.fill = Color.DARKBLUE
        }
        for ((_, circle) in nodeMap) {
            circle.stroke = Color.DODGERBLUE
            circle.strokeWidth = 2.0
            circle.fill = Color.LIGHTBLUE
        }
    }
}