package ui

import javafx.beans.value.ObservableValue
import javafx.scene.input.MouseButton
import javafx.scene.input.ScrollEvent
import javafx.scene.layout.*
import kotlin.math.*

/**
 * Панель для визуального отображения графа и взаимодействия с ним.
 *
 * — Рисует вершины и рёбра по матрице смежности.
 * — Реализует drag'n'drop перемещение графа и вершин.
 * — Позволяет добавлять вершины правым кликом.
 * — Синхронизируется с MatrixInput и обновляет граф при изменении матрицы.
 *
 * @property matrixInput Компонент для ввода/отображения матрицы смежности.
 */
class GraphPanel(val matrixInput: MatrixInput) {

    /** Слой, на котором рисуется весь граф (вершины, рёбра, подписи). */
    private val graphLayer = Pane().apply {
        prefWidth = 20000.0   // Для поддержки drag и больших графов
        prefHeight = 20000.0
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

    // --- Для выделения вершины/ребра кликом (логика "выбрать → соединить") ---
    private var selectedNode: Int? = null

    /**
     * Инициализация панели:
     * — Подключает обработчики мыши для зума/движения/клика.
     * — Устанавливает слушатели на изменение размера графа/матрицы.
     * — Заполняет стартовые координаты для круговой раскладки.
     */
    init {
        view = StackPane(graphLayer).apply {
            prefWidth = 1200.0
            prefHeight = 1200.0
        }

        // --- Масштабирование колёсиком мыши ---
        view.addEventFilter(ScrollEvent.SCROLL) { event ->
            val zoom = if (event.deltaY > 0) 1.1 else 1 / 1.1
            scale = (scale * zoom).coerceIn(0.2, 5.0)
            graphLayer.scaleX = scale
            graphLayer.scaleY = scale
            event.consume()
        }

        // --- Перемещение всей сцены мышью (drag пустого места) ---
        view.setOnMousePressed {
            if (it.button == MouseButton.PRIMARY && !overNode(it.x, it.y)) {
                dragOriginX = it.sceneX
                dragOriginY = it.sceneY
                paneOriginX = graphLayer.translateX
                paneOriginY = graphLayer.translateY
            }
        }
        view.setOnMouseDragged {
            if (it.button == MouseButton.PRIMARY) {
                graphLayer.translateX = paneOriginX + (it.sceneX - dragOriginX)
                graphLayer.translateY = paneOriginY + (it.sceneY - dragOriginY)
            }
        }

        // --- Добавление новой вершины ПКМ по пустому месту ---
        view.setOnMouseClicked { event ->
            if (event.button == MouseButton.SECONDARY) {
                if (!overNode(event.x, event.y)) {
                    addNewNode(event.x, event.y)
                }
            }
        }

        // --- Автоматическая обработка изменения размера графа (через MatrixInput) ---
        matrixInput.sizeSpinner.valueProperty().addListener { _: ObservableValue<out Int>?, oldSize, newSize ->
            val old = oldSize as? Int ?: nodePositions.size
            val newS = newSize as Int
            if (newS > old) {
                // Добавляем координаты для новых вершин по кругу
                val radius = 175.0
                val centerX = 600.0
                val centerY = 600.0
                for (i in nodePositions.size until newS) {
                    val angle = 2 * Math.PI * i / newS
                    val x = centerX + radius * cos(angle)
                    val y = centerY + radius * sin(angle)
                    nodePositions.add(Pair(x, y))
                }
            } else if (newS < old && newS < nodePositions.size) {
                // Удаляем координаты "лишних" вершин
                while (nodePositions.size > newS) nodePositions.removeAt(nodePositions.size - 1)
            }
            setupFieldListeners(newS)
            updateVisualizationNodes(newS)
        }

        // --- Слушатели на все поля матрицы (обновляют граф при изменении) ---
        setupFieldListeners(matrixInput.sizeSpinner.value)

        // --- Начальная раскладка: если nodePositions пуст, размещаем вершины по кругу ---
        if (nodePositions.isEmpty()) {
            val size = matrixInput.sizeSpinner.value
            val radius = 175.0
            val centerX = 600.0
            val centerY = 600.0
            for (i in 0 until size) {
                val angle = 2 * Math.PI * i / size
                val x = centerX + radius * cos(angle)
                val y = centerY + radius * sin(angle)
                nodePositions.add(Pair(x, y))
            }
        }
        updateVisualizationNodes(matrixInput.sizeSpinner.value)
    }

    /**
     * Основной метод для обновления визуализации графа.
     * Обновляет матрицу и вызывает перерисовку с подсветками.
     *
     * @param matrix Матрица смежности (0/1).
     * @param highlights Подсвечиваемые рёбра/ячейки: список (i, j, тип).
     * @param highlightedNodes Подсвечиваемые вершины (например, выбранные пользователем).
     */
    fun updateGraph(
        matrix: Array<IntArray>,
        highlights: List<Triple<Int, Int, String>> = emptyList(),
        highlightedNodes: Collection<Int> = emptyList()
    ) {
        matrixInput.updateMatrixDisplay(matrix)
        updateVisualizationNodes(matrix.size, highlights, highlightedNodes)
    }

    /**
     * Устанавливает слушатели изменений для всех полей матрицы.
     * При изменении значения вызывает обновление визуализации графа.
     *
     * @param size Размер матрицы (и графа).
     */
    private fun setupFieldListeners(size: Int) {
        for (i in 0 until size) {
            for (j in 0 until size) {
                val field = matrixInput.matrixFields[i][j]
                field.textProperty().addListener { _: ObservableValue<out String>?, _, _ ->
                    updateVisualizationNodes(size)
                }
            }
        }
    }

    /**
     * Обработчик клика по вершине (выделение/снятие/соединение).
     * Позволяет кликнуть по двум вершинам подряд и добавить/удалить ребро между ними.
     *
     * @param idx Номер вершины.
     */
    private fun onVertexClicked(idx: Int) {
        if (selectedNode == null) {
            // Первый клик — просто выделить вершину
            selectedNode = idx
            updateVisualizationNodes(matrixInput.sizeSpinner.value, emptyList(), listOf(idx))
        } else if (selectedNode == idx) {
            // Повторный клик по той же — снять выделение
            selectedNode = null
            updateVisualizationNodes(matrixInput.sizeSpinner.value)
        } else {
            // Клик по другой — переключить ребро (если i ≠ j)
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

    /**
     * Добавляет новую вершину с координатами (x, y) и увеличивает размер графа.
     *
     * @param x X-координата нажатия мыши.
     * @param y Y-координата нажатия мыши.
     */
    private fun addNewNode(x: Double, y: Double) {
        matrixInput.sizeSpinner.valueFactory.value += 1
        nodePositions.add(Pair(x, y))
        updateVisualizationNodes(matrixInput.sizeSpinner.value)
    }

    /**
     * Проверяет, попадает ли точка (x, y) внутрь какой-либо вершины (используется для drag/select).
     *
     * @param x X-координата.
     * @param y Y-координата.
     * @return true, если клик был по вершине.
     */
    private fun overNode(x: Double, y: Double): Boolean =
        nodePositions.any { (nx, ny) -> hypot(nx - x, ny - y) < 24.0 }

    /**
     * Перерисовывает все вершины и рёбра.
     * Передаёт параметры в GraphDrawingUtils для отрисовки, подсветки и drag’n’drop.
     *
     * @param size Количество вершин.
     * @param highlights Список подсвечиваемых рёбер/ячеек.
     * @param selectedNodes Список выделенных вершин.
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