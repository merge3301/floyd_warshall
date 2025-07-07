package ui

import javafx.beans.value.ObservableValue
import javafx.scene.input.MouseButton
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.*
import kotlin.math.*

/**
 * Панель для визуального отображения графа и интерактивного взаимодействия с ним.
 *
 * — Рисует вершины и рёбра по матрице смежности.
 * — Реализует drag'n'drop перемещение графа и вершин.
 * — Позволяет добавлять вершины правым кликом мыши.
 * — Синхронизируется с MatrixInput и обновляет граф при изменении матрицы.
 *
 * @property matrixInput Компонент для ввода и отображения матрицы смежности.
 */
class GraphPanel(val matrixInput: MatrixInput) {

    /** Слой, на котором рисуется весь граф (вершины, рёбра, подписи). */
    private val graphLayer = Pane().apply {
        prefWidth = 20_000.0   // Поддержка drag и очень больших графов
        prefHeight = 20_000.0
    }

    /** Главный контейнер для интеграции в интерфейс (используется в других панелях/окнах). */
    val view: StackPane

    /** Позиции всех вершин (x, y) для отрисовки и drag. */
    internal val nodePositions = mutableListOf<Pair<Double, Double>>()

    /** Мапа рёбер: (i, j) → (Line, Arrow), для быстрого обновления/перерисовки. */
    internal val edgeMap = mutableMapOf<Pair<Int, Int>, Pair<javafx.scene.shape.Line, javafx.scene.shape.Polygon>>()

    /** Мапа: номер вершины → объект Circle для drag и выделения. */
    internal val nodeMap = mutableMapOf<Int, javafx.scene.shape.Circle>()

    // --- Параметры для drag/move всей сцены ---
    private var scale = 1.0
    private var dragOriginX = 0.0
    private var dragOriginY = 0.0
    private var paneOriginX = 0.0
    private var paneOriginY = 0.0
    private var isPanning = false          // новое: отслеживаем режим «перемещение сцены»

    // --- Для выделения вершины/ребра кликом (логика «выбрать → соединить») ---
    private var selectedNode: Int? = null

    /**
     * Инициализация панели:
     * — Настраивает drag'n'drop, zoom, добавление вершин.
     * — Слушает изменение размера графа (MatrixInput).
     * — Устанавливает начальное расположение вершин по кругу.
     */
    init {
        view = StackPane(graphLayer).apply {
            prefWidth = 1_200.0
            prefHeight = 1_200.0
        }

        // ─────────────── Масштабирование колёсиком мыши ───────────────
        view.addEventFilter(ScrollEvent.SCROLL) { e ->
            val zoom = if (e.deltaY > 0) 1.1 else 1 / 1.1
            scale = (scale * zoom).coerceIn(0.2, 5.0)
            graphLayer.scaleX = scale
            graphLayer.scaleY = scale
            e.consume()
        }

        // ─────────────── Перемещение всей сцены (drag пустого места) ───────────────
        view.setOnMousePressed { e ->
            if (e.button == MouseButton.PRIMARY && !overNode(e.sceneX, e.sceneY)) {
                isPanning = true
                dragOriginX = e.sceneX
                dragOriginY = e.sceneY
                paneOriginX = graphLayer.translateX
                paneOriginY = graphLayer.translateY
                e.consume()
            }
        }
        view.setOnMouseDragged { e ->
            if (isPanning && e.button == MouseButton.PRIMARY) {
                val dx = e.sceneX - dragOriginX
                val dy = e.sceneY - dragOriginY
                graphLayer.translateX = paneOriginX + dx
                graphLayer.translateY = paneOriginY + dy
                e.consume()
            }
        }
        view.setOnMouseReleased { e ->
            if (isPanning && e.button == MouseButton.PRIMARY) {
                isPanning = false
                e.consume()
            }
        }

        // ─────────────── Добавление новой вершины ПКМ ───────────────
        view.setOnMouseClicked { e ->
            if (e.button == MouseButton.SECONDARY && !overNode(e.sceneX, e.sceneY)) {
                val p = graphLayer.sceneToLocal(e.sceneX, e.sceneY)
                addNewNode(p.x, p.y)
            }
        }

        // ─────────────── Слушатель изменения размера графа ───────────────
        matrixInput.sizeSpinner.valueProperty().addListener { _: ObservableValue<out Int>?, oldSize, newSize ->
            val old = oldSize as? Int ?: nodePositions.size
            val newS = newSize as Int
            if (newS > old) {
                val radius = 175.0
                val centerX = 600.0
                val centerY = 600.0
                while (nodePositions.size < newS) {
                    val angle = 2 * Math.PI * nodePositions.size / newS
                    nodePositions += (centerX + radius * cos(angle)) to (centerY + radius * sin(angle))
                }
            } else {
                while (nodePositions.size > newS) nodePositions.removeLast()
            }
            setupFieldListeners(newS)
            updateVisualizationNodes(newS)
        }

        // ─────────────── Слушатели на все поля матрицы ───────────────
        setupFieldListeners(matrixInput.sizeSpinner.value)

        // ─────────────── Стартовая раскладка ───────────────
        if (nodePositions.isEmpty()) {
            val n = matrixInput.sizeSpinner.value
            val radius = 175.0
            val cx = 600.0
            val cy = 600.0
            repeat(n) { i ->
                val angle = 2 * Math.PI * i / n
                nodePositions += (cx + radius * cos(angle)) to (cy + radius * sin(angle))
            }
        }
        updateVisualizationNodes(matrixInput.sizeSpinner.value)
    }

    // ────────────────────────────────────────────────────────────────
    //  Публичный API
    // ────────────────────────────────────────────────────────────────

    /**
     * Основной метод для обновления визуализации графа.
     * Обновляет отображение матрицы и вызывает перерисовку с подсветкой.
     *
     * @param matrix           Матрица смежности (0/1)
     * @param highlights       Список подсвечиваемых рёбер/ячеек: (i, j, тип)
     * @param highlightedNodes Список выделенных вершин (например, выбранные пользователем)
     */
    fun updateGraph(
        matrix: Array<IntArray>,
        highlights: List<Triple<Int, Int, String>> = emptyList(),
        highlightedNodes: Collection<Int> = emptyList()
    ) {
        matrixInput.updateMatrixDisplay(matrix)
        updateVisualizationNodes(matrix.size, highlights, highlightedNodes)
    }

    // ────────────────────────────────────────────────────────────────
    //  Внутренние утилиты (не трогать извне)
    // ────────────────────────────────────────────────────────────────

    /**
     * Устанавливает слушатели изменений для всех полей матрицы.
     * При изменении значения вызывает обновление визуализации графа.
     */
    private fun setupFieldListeners(size: Int) {
        for (i in 0 until size) {
            for (j in 0 until size) {
                matrixInput.matrixFields[i][j].textProperty().addListener { _: ObservableValue<out String>?, _, _ ->
                    updateVisualizationNodes(size)
                }
            }
        }
    }

    /**
     * Обработчик клика по вершине: выделение/снятие выделения/соединение.
     *
     * @param idx Номер вершины.
     */
    private fun onVertexClicked(idx: Int) {
        when {
            selectedNode == null -> {
                selectedNode = idx
                updateVisualizationNodes(matrixInput.sizeSpinner.value, selectedNodes = listOf(idx))
            }
            selectedNode == idx -> {
                selectedNode = null
                updateVisualizationNodes(matrixInput.sizeSpinner.value)
            }
            else -> {
                val i = selectedNode!!
                val j = idx
                if (i != j) {
                    val field = matrixInput.matrixFields[i][j]
                    field.text = if ((field.text.toIntOrNull() ?: 0) == 1) "0" else "1"
                }
                selectedNode = null
                updateVisualizationNodes(matrixInput.sizeSpinner.value)
            }
        }
    }

    /**
     * Добавляет новую вершину с координатами (x, y) и увеличивает размер графа.
     *
     * @param x X-координата мыши
     * @param y Y-координата мыши
     */
    private fun addNewNode(x: Double, y: Double) {
        matrixInput.sizeSpinner.valueFactory.value += 1
        nodePositions += x to y
        updateVisualizationNodes(matrixInput.sizeSpinner.value)
    }

    /**
     * Проверяет, попадает ли точка (sceneX, sceneY) внутрь какой-либо вершины.
     *
     * @param sceneX X-координата курсора (scene)
     * @param sceneY Y-координата курсора (scene)
     * @return true — если клик был по вершине
     */
    private fun overNode(sceneX: Double, sceneY: Double): Boolean {
        val p = graphLayer.sceneToLocal(sceneX, sceneY)
        return nodePositions.any { (nx, ny) -> hypot(nx - p.x, ny - p.y) < 24.0 }
    }

    /**
     * Перерисовывает все вершины и рёбра (вызывает GraphDrawingUtils).
     *
     * @param size           Количество вершин
     * @param highlights     Список подсвечиваемых рёбер/ячеек
     * @param selectedNodes  Список выделенных вершин
     */
    private fun updateVisualizationNodes(
        size: Int,
        highlights: List<Triple<Int, Int, String>> = emptyList(),
        selectedNodes: Collection<Int> = emptyList()
    ) {
        GraphDrawingUtils.updateVisualizationNodes(
            graphLayer, matrixInput, size, highlights,
            nodePositions, edgeMap, nodeMap, ::onVertexClicked, selectedNodes
        )
    }
}