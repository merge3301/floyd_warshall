package ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import ui.RandomGraph

/**
 * Виджет для ввода и отображения матрицы смежности графа.
 *
 * Позволяет вручную или случайно задавать матрицу, выбирать размер графа и количество рёбер,
 * а также подсвечивать ячейки и заголовки для визуализации работы алгоритма.
 */
class MatrixInput {
    private val MAX_SIZE = 15

    /** Спиннер для выбора размера графа (количество вершин). */
    val sizeSpinner = Spinner<Int>(1, MAX_SIZE, 3)
    /** Спиннер для выбора числа рёбер при генерации случайной матрицы. */
    private val edgeSpinner = Spinner<Int>(0, 6, 6)

    /** Основная таблица для ввода матрицы смежности. */
    val matrixGrid = GridPane()
    /** Кнопка генерации случайной матрицы. */
    val randomButton = Button("Случайная матрица")
    /** Кнопка очистки (обнуления) матрицы. */
    val clearButton = Button("Очистить")
    /** Главная "обёртка" для вставки в интерфейс. */
    val view: VBox

    /** 2D-список полей для ввода значений матрицы. */
    val matrixFields = mutableListOf<MutableList<TextField>>()

    /** Ссылки на заголовки столбцов (верх) и строк (слева). */
    private val colHeaders = mutableListOf<Label>()
    private val rowHeaders = mutableListOf<Label>()

    init {
        sizeSpinner.isEditable = true
        edgeSpinner.isEditable = true

        matrixGrid.hgap = 5.0
        matrixGrid.vgap = 5.0
        matrixGrid.padding = Insets(5.0)

        rebuildMatrixGrid(sizeSpinner.value)

        // При изменении размера пересобираем сетку и корректируем максимальное число рёбер.
        sizeSpinner.valueProperty().addListener { _, _, newSize ->
            if (newSize != null) {
                rebuildMatrixGrid(newSize)
                updateEdgeSpinnerMax(newSize)
            }
        }

        // Генерация случайной матрицы по размеру и числу рёбер.
        randomButton.setOnAction {
            val size = sizeSpinner.value
            val edges = edgeSpinner.value
            val matrix = RandomGraph.generateAdjacencyMatrix(size, edges)
            updateMatrixDisplay(matrix)
        }

        // Обнуление матрицы
        clearButton.setOnAction {
            val size = sizeSpinner.value
            val zeroMatrix = Array(size) { IntArray(size) { 0 } }
            updateMatrixDisplay(zeroMatrix)
        }

        view = VBox(12.0,
            Label("Размер графа:"), sizeSpinner,
            Label("Количество рёбер:"), edgeSpinner,
            matrixGrid,
            HBox(10.0, randomButton, clearButton)
        ).apply {
            padding = Insets(15.0)
            style = "-fx-background-color: #f9f9f9; -fx-border-color: #cccccc; -fx-border-radius: 5;"
        }

        updateEdgeSpinnerMax(sizeSpinner.value)
    }

    /**
     * Обновляет максимальное значение для edgeSpinner в зависимости от [size].
     */
    private fun updateEdgeSpinnerMax(size: Int) {
        val maxEdges = size * (size - 1)
        val current = edgeSpinner.value.coerceAtMost(maxEdges)
        edgeSpinner.valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(0, maxEdges, current)
    }

    /**
     * Перестраивает сетку ввода матрицы под новый размер [size].
     * Сохраняет уже введённые значения по возможности.
     */
    private fun rebuildMatrixGrid(size: Int) {
        val oldMatrix = getMatrix()

        matrixGrid.children.clear()
        matrixFields.clear()
        colHeaders.clear()
        rowHeaders.clear()

        // Заголовки столбцов (верх)
        for (j in 0 until size) {
            val label = styledHeaderLabel(j + 1)
            colHeaders.add(label)
            matrixGrid.add(label, j + 1, 0)
        }

        // Строки и заголовки строк (слева)
        for (i in 0 until size) {
            val row = mutableListOf<TextField>()
            val rowLabel = styledHeaderLabel(i + 1)
            rowHeaders.add(rowLabel)
            matrixGrid.add(rowLabel, 0, i + 1)

            for (j in 0 until size) {
                val field = TextField("0")
                field.prefWidth = 42.0
                field.alignment = Pos.CENTER

                if (i < oldMatrix.size && j < oldMatrix.size) {
                    field.text = oldMatrix[i][j].toString()
                }

                if (i == j) {
                    // Диагональ — не редактируется (нет петель)
                    field.isEditable = false
                } else {
                    // Проверка ввода: только "0" или "1"
                    field.focusedProperty().addListener { _, wasFocused, isFocused ->
                        if (wasFocused && !isFocused) {
                            val value = field.text
                            val cleaned = value.trimStart('0')
                            if (cleaned != "" && cleaned != "1") {
                                showErrorDialog("Некорректное значение: \"$value\". Разрешены только 0 и 1.")
                                field.text = "0"
                                field.requestFocus()
                            } else {
                                field.text = if (cleaned == "1") "1" else "0"
                            }
                        }
                    }
                }
                row.add(field)
                matrixGrid.add(field, j + 1, i + 1)
            }
            matrixFields.add(row)
        }
    }

    /**
     * Создаёт заголовок для строки/столбца с красивым стилем.
     * @param index Индекс вершины (от 1).
     */
    private fun styledHeaderLabel(index: Int): Label {
        return Label(index.toString()).apply {
            alignment = Pos.CENTER
            prefWidth = 42.0
            font = Font.font("System", FontWeight.BOLD, 13.0)
            style = "-fx-text-fill: #333333; -fx-effect: dropshadow(one-pass-box, #bbbbbb, 1, 0.0, 0.5, 0.5);"
        }
    }

    /**
     * Показывает диалоговое окно об ошибке ввода.
     * @param msg Сообщение пользователю.
     */
    private fun showErrorDialog(msg: String) {
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = "Ошибка ввода"
        alert.headerText = "Некорректные данные"
        alert.contentText = msg
        alert.showAndWait()
    }

    /**
     * Получает текущую матрицу смежности из полей ввода.
     * @return Матрица смежности (массив массивов Int).
     */
    fun getMatrix(): Array<IntArray> {
        return matrixFields.map { row ->
            row.map { it.text.toIntOrNull() ?: 0 }.toIntArray()
        }.toTypedArray()
    }

    /**
     * Отображает переданную матрицу [matrix] в полях ввода.
     * Если размер изменился, пересобирает сетку.
     */
    fun updateMatrixDisplay(matrix: Array<IntArray>) {
        val size = matrix.size
        if (size != matrixFields.size) rebuildMatrixGrid(size)

        for (i in 0 until size) {
            for (j in 0 until size) {
                matrixFields[i][j].text = if (matrix[i][j] == 1) "1" else "0"
            }
        }
    }

    /** ===== ПОДСВЕТКА ЯЧЕЕК ===== */

    /**
     * Подсвечивает одну ячейку ([i], [j]) цветом [color].
     * @param i Индекс строки.
     * @param j Индекс столбца.
     * @param color Цвет фона (по умолчанию жёлтый).
     */
    fun highlightCell(i: Int, j: Int, color: String = "#fff59d") {
        if (i in matrixFields.indices && j in matrixFields[i].indices) {
            val field = matrixFields[i][j]
            field.style = "-fx-background-color: $color; -fx-text-fill: #222222;"
        }
    }

    /**
     * Подсвечивает набор ячеек согласно их типу ("candidate", "target", "added" и др).
     * @param cells Список тройных индексов: (i, j, тип).
     */
    fun highlightCells(cells: List<Triple<Int, Int, String>>) {
        for ((i, j, type) in cells) {
            val color = when (type) {
                "candidate" -> "#ffe0b2"
                "target"    -> "#fff9c4"
                "added"     -> "#c8e6c9"
                else        -> "#e0e0e0"
            }
            highlightCell(i, j, color)
        }
    }

    /**
     * Снимает подсветку со всех ячеек.
     */
    fun clearHighlights() {
        for (i in matrixFields.indices) {
            for (j in matrixFields[i].indices) {
                matrixFields[i][j].style = ""
            }
        }
        clearHeaderHighlights()
    }

    /** ===== ПОДСВЕТКА ЗАГОЛОВКОВ ===== */

    /**
     * Подсвечивает заголовок строки и столбца с номером [k] цветом [color].
     */
    fun highlightHeader(k: Int, color: String = "#ffd54f") {
        clearHeaderHighlights()
        if (k in colHeaders.indices) {
            colHeaders[k].style = "-fx-background-color: $color; -fx-text-fill: #000;"
        }
        if (k in rowHeaders.indices) {
            rowHeaders[k].style = "-fx-background-color: $color; -fx-text-fill: #000;"
        }
    }

    /**
     * Сбрасывает подсветку у всех заголовков.
     */
    fun clearHeaderHighlights() {
        for (label in colHeaders + rowHeaders) {
            label.style = "-fx-text-fill: #333333; -fx-effect: dropshadow(one-pass-box, #bbbbbb, 1, 0.0, 0.5, 0.5);"
        }
    }
}