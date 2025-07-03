package ui

import javafx.scene.paint.Color
import javafx.scene.shape.*

object GraphStyleUtils {

    /** Подсвечивает список рёбер (список Triple(i, j, type)) */
    fun highlightEdges(
        edgeMap: Map<Pair<Int, Int>, Pair<Line, Polygon>>,
        edges: List<Triple<Int, Int, String>>
    ) {
        // Сбросим цвета перед новой подсветкой (иначе подсвеченные останутся)
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

    /** Подсвечивает только выбранные вершины */
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

    /** Сброс подсветки у рёбер и вершин */
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