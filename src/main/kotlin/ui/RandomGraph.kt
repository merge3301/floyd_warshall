package ui

import kotlin.random.Random

/**
 * Утилита для генерации случайных графов и преобразования матриц смежности.
 *
 * Предоставляет методы для создания случайной матрицы смежности с заданным числом рёбер,
 * а также для удобного представления матрицы в виде строки.
 */
object RandomGraph {

    /**
     * Генерирует случайную матрицу смежности размера [size] с ровно [edges] рёбрами.
     *
     * Рёбра выбираются случайным образом. В графе нет петель (то есть matrix[i][i] всегда 0)
     * и нет дублирующихся рёбер. Если [edges] больше максимально возможного числа рёбер,
     * оно будет автоматически уменьшено до максимума.
     *
     * @param size Размер графа (число вершин).
     * @param edges Желаемое количество рёбер (будет приведено к максимуму, если превышает возможное).
     * @return Массив [size x size] — матрица смежности (0/1).
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
     * Преобразует матрицу смежности [matrix] в текстовый вид.
     *
     * Используется для отображения или отладки — каждая строка это строка матрицы, элементы разделены пробелами.
     *
     * @param matrix Матрица смежности для преобразования.
     * @return Многострочная строка, по одной строке на каждую вершину.
     */
    fun matrixToText(matrix: Array<IntArray>): String {
        return matrix.joinToString("\n") { row ->
            row.joinToString(" ")
        }
    }
}