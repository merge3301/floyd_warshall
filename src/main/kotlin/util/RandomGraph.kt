package util

import kotlin.random.Random

object RandomGraph {
    /**
     * Генерирует случайную матрицу смежности размера [size] с количеством рёбер [edges].
     * Матрица не содержит петель и дублирующихся рёбер.
     */
    fun generateAdjacencyMatrix(size: Int, edges: Int): Array<IntArray> {
        val matrix = Array(size) { IntArray(size) { 0 } }
        val maxEdges = size * (size - 1) // без петель
        val actualEdges = edges.coerceAtMost(maxEdges)

        val allPossibleEdges = mutableListOf<Pair<Int, Int>>()
        for (i in 0 until size) {
            for (j in 0 until size) {
                if (i != j) allPossibleEdges.add(i to j)
            }
        }
        allPossibleEdges.shuffle(Random.Default)

        repeat(actualEdges) { idx ->
            val (i, j) = allPossibleEdges[idx]
            matrix[i][j] = 1
        }

        return matrix
    }

    /**
     * Преобразует матрицу в строку для отображения в TextArea
     */
    fun matrixToText(matrix: Array<IntArray>): String {
        return matrix.joinToString("\n") { row ->
            row.joinToString(" ")
        }
    }
}
