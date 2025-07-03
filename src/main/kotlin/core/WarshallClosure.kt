package core

data class WarshallStep(
    val matrix: Array<IntArray>,
    val k: Int,
    val i: Int,
    val j: Int,
    val message: String,
    val involved: List<Triple<Int, Int, String>> = emptyList() // (i, j, type)
)

class WarshallStepper(private val startMatrix: Array<IntArray>) {
    private val n = startMatrix.size
    private var k = 0
    private var i = 0
    private var j = 0
    private var finished = false
    private val matrix = Array(n) { startMatrix[it].copyOf() }
    private var message: String = "Начальное состояние."
    private val history = mutableListOf<WarshallStep>()

    /**
     * Снимок текущего состояния (например, для отображения до первого шага)
     */
    fun currentStep(): WarshallStep {
        return buildStep(k, i, j, message, matrix)
    }

    /**
     * Выполняет один шаг алгоритма Уоршелла и возвращает состояние этого шага.
     * Внимание: состояние "начальное" (до шагов) не считается шагом алгоритма!
     */
    fun stepForward(): WarshallStep {
        if (finished) {
            message = "Алгоритм завершён."
            return currentStep()
        }

        // Сохраняем снимок для возможности шага назад
        history.add(currentStep())

        // Подсветка для визуализации на этом шаге
        val involved = mutableListOf<Triple<Int, Int, String>>()
        if (i < n && k < n) involved.add(Triple(i, k, "candidate"))
        if (k < n && j < n) involved.add(Triple(k, j, "candidate"))
        if (i < n && j < n) involved.add(Triple(i, j, "target"))

        // Основная логика
        val shouldAdd = (i < n && j < n && k < n) &&
                matrix[i][k] == 1 && matrix[k][j] == 1 && matrix[i][j] == 0

        if (shouldAdd) {
            matrix[i][j] = 1
            involved.add(Triple(i, j, "added"))
            message = "Добавлено достижение: ${i + 1} → ${j + 1} через ${k + 1}."
        } else {
            message = "Ничего не меняется для ${i + 1} → ${j + 1} при промежуточном ${k + 1}."
        }

        // Создаём снимок текущего шага (до сдвига индексов!)
        val step = WarshallStep(
            matrix.map { it.copyOf() }.toTypedArray(),
            k, i, j, message, involved
        )

        // Переход к следующему состоянию
        j++
        if (j >= n) {
            j = 0
            i++
        }
        if (i >= n) {
            i = 0
            k++
        }
        if (k >= n) {
            finished = true
            message = "Алгоритм завершён."
        }

        return step
    }

    /**
     * Шаг назад: возвращает WarshallStep соответствующий предыдущему состоянию
     */
    fun stepBack(): WarshallStep {
        if (history.isNotEmpty()) {
            val prev = history.removeAt(history.size - 1)
            for (x in 0 until n) for (y in 0 until n) matrix[x][y] = prev.matrix[x][y]
            k = prev.k
            i = prev.i
            j = prev.j
            finished = false
            message = prev.message
            return prev
        }
        return currentStep()
    }

    /**
     * Полный сброс
     */
    fun reset() {
        k = 0
        i = 0
        j = 0
        finished = false
        for (x in 0 until n) for (y in 0 until n) matrix[x][y] = startMatrix[x][y]
        message = "Начальное состояние."
        history.clear()
    }

    fun isFinished(): Boolean = finished

    /**
     * Формирует шаг для визуализации состояния
     */
    private fun buildStep(k: Int, i: Int, j: Int, msg: String, mat: Array<IntArray>): WarshallStep {
        val involved = mutableListOf<Triple<Int, Int, String>>()
        if (!finished) {
            if (i < n && k < n) involved.add(Triple(i, k, "candidate"))
            if (k < n && j < n) involved.add(Triple(k, j, "candidate"))
            if (i < n && j < n) involved.add(Triple(i, j, "target"))
        }
        return WarshallStep(
            mat.map { it.copyOf() }.toTypedArray(),
            k, i, j, msg, involved
        )
    }
}