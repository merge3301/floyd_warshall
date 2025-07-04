package ui

import javafx.geometry.Insets
import javafx.geometry.Pos
import javafx.scene.control.*
import javafx.scene.layout.*
import javafx.scene.text.Font
import javafx.scene.text.FontWeight
import ui.RandomGraph

class MatrixInput {
    private val MAX_SIZE = 15

    val sizeSpinner = Spinner<Int>(1, MAX_SIZE, 3)
    private val edgeSpinner = Spinner<Int>(0, 6, 6)

    val matrixGrid = GridPane()
    val randomButton = Button("Случайная матрица")
    val clearButton = Button("Очистить")
    val view: VBox

    val matrixFields = mutableListOf<MutableList<TextField>>()

    // --- Ссылки на заголовки
    private val colHeaders = mutableListOf<Label>()
    private val rowHeaders = mutableListOf<Label>()

    init {
        sizeSpinner.isEditable = true
        edgeSpinner.isEditable = true

        matrixGrid.hgap = 5.0
        matrixGrid.vgap = 5.0
        matrixGrid.padding = Insets(5.0)

        rebuildMatrixGrid(sizeSpinner.value)

        sizeSpinner.valueProperty().addListener { _, _, newSize ->
            if (newSize != null) {
                rebuildMatrixGrid(newSize)
                updateEdgeSpinnerMax(newSize)
            }
        }

        randomButton.setOnAction {
            val size = sizeSpinner.value
            val edges = edgeSpinner.value
            val matrix = RandomGraph.generateAdjacencyMatrix(size, edges)
            updateMatrixDisplay(matrix)
        }

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

    private fun updateEdgeSpinnerMax(size: Int) {
        val maxEdges = size * (size - 1)
        val current = edgeSpinner.value.coerceAtMost(maxEdges)
        edgeSpinner.valueFactory = SpinnerValueFactory.IntegerSpinnerValueFactory(0, maxEdges, current)
    }

    private fun rebuildMatrixGrid(size: Int) {
        val oldMatrix = getMatrix()

        matrixGrid.children.clear()
        matrixFields.clear()
        colHeaders.clear()
        rowHeaders.clear()

        // Заголовки столбцов
        for (j in 0 until size) {
            val label = styledHeaderLabel(j + 1)
            colHeaders.add(label)
            matrixGrid.add(label, j + 1, 0)
        }

        // Строки и заголовки строк
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
                    field.isEditable = false
                } else {
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

    private fun styledHeaderLabel(index: Int): Label {
        return Label(index.toString()).apply {
            alignment = Pos.CENTER
            prefWidth = 42.0
            font = Font.font("System", FontWeight.BOLD, 13.0)
            style = "-fx-text-fill: #333333; -fx-effect: dropshadow(one-pass-box, #bbbbbb, 1, 0.0, 0.5, 0.5);"
        }
    }

    private fun showErrorDialog(msg: String) {
        val alert = Alert(Alert.AlertType.ERROR)
        alert.title = "Ошибка ввода"
        alert.headerText = "Некорректные данные"
        alert.contentText = msg
        alert.showAndWait()
    }

    fun getMatrix(): Array<IntArray> {
        return matrixFields.map { row ->
            row.map { it.text.toIntOrNull() ?: 0 }.toIntArray()
        }.toTypedArray()
    }

    fun updateMatrixDisplay(matrix: Array<IntArray>) {
        val size = matrix.size
        if (size != matrixFields.size) rebuildMatrixGrid(size)

        for (i in 0 until size) {
            for (j in 0 until size) {
                matrixFields[i][j].text = if (matrix[i][j] == 1) "1" else "0"
            }
        }
    }

    /** ---- ПОДСВЕТКА ---- */

    fun highlightCell(i: Int, j: Int, color: String = "#fff59d") {
        if (i in matrixFields.indices && j in matrixFields[i].indices) {
            val field = matrixFields[i][j]
            field.style = "-fx-background-color: $color; -fx-text-fill: #222222;"
        }
    }

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

    fun clearHighlights() {
        for (i in matrixFields.indices) {
            for (j in matrixFields[i].indices) {
                matrixFields[i][j].style = ""
            }
        }
        clearHeaderHighlights()
    }

    /** ---- ПОДСВЕТКА ЗАГОЛОВКОВ ---- */
    fun highlightHeader(k: Int, color: String = "#ffd54f") {
        clearHeaderHighlights()
        if (k in colHeaders.indices) {
            colHeaders[k].style = "-fx-background-color: $color; -fx-text-fill: #000;"
        }
        if (k in rowHeaders.indices) {
            rowHeaders[k].style = "-fx-background-color: $color; -fx-text-fill: #000;"
        }
    }

    fun clearHeaderHighlights() {
        for (label in colHeaders + rowHeaders) {
            label.style = "-fx-text-fill: #333333; -fx-effect: dropshadow(one-pass-box, #bbbbbb, 1, 0.0, 0.5, 0.5);"
        }
    }
}