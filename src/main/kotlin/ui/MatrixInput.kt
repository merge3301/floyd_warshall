package ui

import javafx.geometry.Insets
import javafx.scene.control.*
import javafx.scene.layout.*
import util.RandomGraph

class MatrixInput {
    val sizeSpinner = Spinner<Int>(1, 15, 3)
    val matrixGrid = GridPane()
    val randomButton = Button("Случайная матрица")
    val clearButton = Button("Очистить")
    val view: VBox

    val matrixFields = mutableListOf<MutableList<TextField>>()

    init {
        sizeSpinner.isEditable = true
        matrixGrid.hgap = 15.0
        matrixGrid.vgap = 15.0
        matrixGrid.padding = Insets(5.0)
        rebuildMatrixGrid(sizeSpinner.value)
        sizeSpinner.valueProperty().addListener { _: javafx.beans.value.ObservableValue<out Int>?, _: Int?, newSize: Int? ->
            if (newSize != null) {
                rebuildMatrixGrid(newSize)
            }
        }
        randomButton.setOnAction {
            val size = sizeSpinner.value
            val edges = (1..size * 2).random()
            val matrix = RandomGraph.generateAdjacencyMatrix(size, edges)
            for (i in 0 until size) {
                for (j in 0 until size) {
                    matrixFields[i][j].text = matrix[i][j].toString()
                }
            }
        }
        clearButton.setOnAction {
            val size = sizeSpinner.value
            for (i in 0 until size) {
                for (j in 0 until size) {
                    matrixFields[i][j].text = "0"
                }
            }
        }
        view = VBox(12.0,
            Label("Размер графа:"), sizeSpinner, matrixGrid,
            HBox(10.0, randomButton, clearButton)
        ).apply {
            padding = Insets(15.0)
            style = "-fx-background-color: #f9f9f9; -fx-border-color: #cccccc; -fx-border-radius: 5;"
        }
    }

    private fun rebuildMatrixGrid(size: Int) {
        matrixGrid.children.clear()
        matrixFields.clear()
        for (i in 0 until size) {
            val row = mutableListOf<TextField>()
            for (j in 0 until size) {
                val field = TextField("0")
                field.prefWidth = 42.0
                field.alignment = javafx.geometry.Pos.CENTER
                row.add(field)
                matrixGrid.add(field, j, i)
            }
            matrixFields.add(row)
        }
    }
}