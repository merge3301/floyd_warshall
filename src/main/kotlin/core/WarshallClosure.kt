package core

abstract class WarshallClosure {
    fun computeClosure(matrix: List<List<Int>>): List<List<Int>> {
        val n = matrix.size
        val closure = Array(n) { i -> matrix[i].toIntArray() }
        for (k in 0 until n) {
            for (i in 0 until n) {
                for (j in 0 until n) {
                    if (closure[i][k] == 1 && closure[k][j] == 1) {
                        closure[i][j] = 1
                    }
                }
            }
        }
        return closure.map { it.toList() }
    }
}