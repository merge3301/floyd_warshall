package core

/**
 * Состояние одного шага алгоритма Уоршелла.
 *
 * @property matrix Текущая матрица смежности (графа) после применения шага.
 * @property k Индекс промежуточной вершины (итерация по k).
 * @property i Текущий исходный узел (строка).
 * @property j Текущий целевой узел (столбец).
 * @property message Сообщение для пользователя/визуализации.
 * @property involved Список ячеек, участвующих в данном шаге (для подсветки), формат: (i, j, тип).
 */
data class WarshallStep(
    val matrix: Array<IntArray>,
    val k: Int,
    val i: Int,
    val j: Int,
    val message: String,
    val involved: List<Triple<Int, Int, String>> = emptyList()
)

/**
 * Пошаговый исполнитель алгоритма Уоршелла с историей шагов и возможностью визуализации.
 *
 * @constructor Принимает стартовую матрицу смежности (0/1), не изменяет исходные данные.
 * @param startMatrix Исходная матрица смежности, на которой будет вычисляться транзитивное замыкание.
 */
class WarshallStepper(private val startMatrix: Array<IntArray>) {
    private val n = startMatrix.size
    private var k = 0
    private var i = 0
    private var j = 0
    private var finished = false
    private val matrix = Array(n) { startMatrix[it].copyOf() }
    private var message: String = "Начальное состояние."
    private val history = mutableListOf<WarshallStep>()

    init {
        if (n == 0 || n == 1) finished = true
    }

    /**
     * Возвращает снимок текущего состояния алгоритма без выполнения шага.
     * Обычно используется для отображения стартового состояния.
     *
     * @return WarshallStep, описывающий текущее состояние.
     */
    fun currentStep(): WarshallStep {
        return buildStep(k, i, j, message, matrix)
    }

    /**
     * Выполняет один шаг алгоритма Уоршелла и возвращает снимок состояния после шага.
     * Сохраняет историю для возможности отката.
     * Если алгоритм уже завершён, просто возвращает последнее состояние.
     *
     * @return WarshallStep после текущего шага.
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
     * Откатывает состояние на один шаг назад.
     * Если история пуста, возвращает текущее состояние без изменений.
     *
     * @return WarshallStep — состояние после отката.
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
     * Полностью сбрасывает состояние степпера в начальное, очищая историю шагов.
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

    /**
     * Проверяет, завершён ли алгоритм (все k, i, j обработаны).
     *
     * @return true, если выполнение завершено; иначе — false.
     */
    fun isFinished(): Boolean = finished

    /**
     * Формирует WarshallStep для визуализации текущего состояния.
     *
     * @param k Текущий индекс промежуточной вершины.
     * @param i Текущий исходный узел.
     * @param j Текущий целевой узел.
     * @param msg Сообщение для визуализации.
     * @param mat Матрица текущего состояния.
     * @return WarshallStep с актуальной информацией.
     */
    private fun buildStep(
        k: Int, i: Int, j: Int, msg: String, mat: Array<IntArray>
    ): WarshallStep {
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