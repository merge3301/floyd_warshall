package core

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import kotlin.random.Random

@TestMethodOrder(MethodOrderer.DisplayName::class)
class WarshallStepperTest {

    private val baseInitial = arrayOf(
        intArrayOf(0, 1, 0),
        intArrayOf(0, 0, 1),
        intArrayOf(0, 0, 0)
    )

    private val baseClosure = arrayOf(
        intArrayOf(0, 1, 1),
        intArrayOf(0, 0, 1),
        intArrayOf(0, 0, 0)
    )

    @Test
    @DisplayName("1.1  Полный проход — получаем правильное замыкание")
    fun fullRunProducesCorrectClosure() {
        val st = WarshallStepper(baseInitial)
        while (!st.isFinished()) st.stepForward()
        assertArrayEquals(baseClosure, st.currentStep().matrix)
    }

    @Test
    @DisplayName("2.1  stepBack действительно возвращает предыдущий снимок (только по матрице)")
    fun stepBackReturnsPreviousSnapshot() {
        val st = WarshallStepper(baseInitial)
        val snap0 = st.currentStep().matrix
        val snap1 = st.stepForward().matrix
        st.stepForward()
        st.stepBack()
        val afterBack1 = st.currentStep().matrix
        assertArrayEquals(snap1, afterBack1)
        st.stepBack()
        val afterBack0 = st.currentStep().matrix
        assertArrayEquals(snap0, afterBack0)
    }

    @Test
    @DisplayName("3.1  reset сбрасывает и очищает историю")
    fun resetRestoresInitialMatrix() {
        val st = WarshallStepper(baseInitial)
        repeat(5) { st.stepForward() }
        st.reset()
        val cur = st.currentStep().matrix
        assertArrayEquals(baseInitial, cur)
        assertFalse(st.isFinished())
        val same = st.stepBack().matrix
        assertArrayEquals(baseInitial, same)
    }

    @Test
    @DisplayName("4.1  Результат совпадает с чистой реализацией Warshall")
    fun matchesPureWarshall() {
        val size = 6
        val rndMatrix = Array(size) { IntArray(size) { if (Random.nextDouble() < 0.3) 1 else 0 } }
        for (d in 0 until size) rndMatrix[d][d] = 0
        val etalon = rndMatrix.map { it.copyOf() }.toTypedArray()
        for (k in 0 until size)
            for (i in 0 until size)
                for (j in 0 until size)
                    if (etalon[i][k] == 1 && etalon[k][j] == 1)
                        etalon[i][j] = 1
        val st = WarshallStepper(rndMatrix)
        while (!st.isFinished()) st.stepForward()
        val result = st.currentStep().matrix
        assertArrayEquals(etalon, result)
    }

    @Test
    @DisplayName("5.1  100 случайных матриц 10×10 дают корректный результат")
    fun randomStress100() {
        repeat(100) {
            val n = 10
            val m = Array(n) { IntArray(n) { if (Random.nextDouble() < 0.15) 1 else 0 } }
            for (d in 0 until n) m[d][d] = 0
            val st = WarshallStepper(m)
            while (!st.isFinished()) st.stepForward()
            val got = st.currentStep().matrix
            val ref = warshallPure(m)
            assertArrayEquals(ref, got)
        }
    }

    @Test
    @DisplayName("6.1  Матрица 1×1 сразу finished, back бессмысленен")
    fun singleVertexGraph() {
        val st = WarshallStepper(arrayOf(intArrayOf(0)))
        assertTrue(st.isFinished())
        val before = st.currentStep().matrix
        st.stepBack()
        assertArrayEquals(arrayOf(intArrayOf(0)), st.currentStep().matrix)
        assertArrayEquals(before, st.currentStep().matrix)
    }

    @Test
    @DisplayName("6.2  Матрица 0×0 сразу finished, back бессмысленен")
    fun emptyMatrixGraph() {
        val st = WarshallStepper(emptyArray())
        assertTrue(st.isFinished())
        val before = st.currentStep().matrix
        st.stepBack()
        assertArrayEquals(emptyArray<IntArray>(), st.currentStep().matrix)
        assertArrayEquals(before, st.currentStep().matrix)
    }

    @Test
    @DisplayName("7.1  Цикл forward-back-forward восстанавливает корректность")
    fun sawForwardBackForward() {
        val st = WarshallStepper(baseInitial)
        val first = st.stepForward().matrix
        st.stepBack()
        val again = st.stepForward().matrix
        assertArrayEquals(first, again)
    }

    /** «Чистый» алгоритм — для проверки. */
    private fun warshallPure(src: Array<IntArray>): Array<IntArray> {
        val n = src.size
        val a = src.map { it.copyOf() }.toTypedArray()
        for (k in 0 until n)
            for (i in 0 until n)
                for (j in 0 until n)
                    if (a[i][k] == 1 && a[k][j] == 1) a[i][j] = 1
        return a
    }
}