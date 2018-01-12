package com.wijayaprinting.controller

import com.wijayaprinting.R
import com.wijayaprinting.Refreshable
import com.wijayaprinting.nosql.Recess
import com.wijayaprinting.nosql.Recesses
import com.wijayaprinting.scene.PATTERN_TIME
import com.wijayaprinting.scene.layout.TimeBox
import com.wijayaprinting.scene.layout.timeBox
import com.wijayaprinting.utils.gap
import com.wijayaprinting.utils.transaction
import javafx.fxml.FXML
import javafx.scene.control.Button
import javafx.scene.control.ButtonType.*
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.image.ImageView
import kotfx.*
import kotlinx.nosql.equal
import kotlinx.nosql.id
import org.joda.time.LocalTime

class AttendanceRecessController : Controller(), Refreshable {

    @FXML lateinit var deleteButton: Button

    @FXML lateinit var tableView: TableView<Recess>
    @FXML lateinit var startColumn: TableColumn<Recess, String>
    @FXML lateinit var endColumn: TableColumn<Recess, String>

    @FXML
    fun initialize() {
        deleteButton.disableProperty() bind tableView.selectionModel.selectedItemProperty().isNull
        startColumn.setCellValueFactory { it.value.start.toString(PATTERN_TIME).asProperty() }
        endColumn.setCellValueFactory { it.value.end.toString(PATTERN_TIME).asProperty() }
        refresh()
    }

    @FXML fun refreshOnAction() = refresh()

    @FXML
    fun addOnAction() = dialog<Pair<LocalTime, LocalTime>>(getString(R.string.add_reccess), getString(R.string.add_reccess), ImageView(R.png.ic_clock)) {
        lateinit var startBox: TimeBox
        lateinit var endBox: TimeBox
        content = gridPane {
            gap(8)
            label(getString(R.string.start)) col 0 row 0
            startBox = timeBox() col 1 row 0
            label(getString(R.string.end)) col 0 row 1
            endBox = timeBox() col 1 row 1
        }
        button(CANCEL)
        button(OK).disableProperty() bind booleanBindingOf(startBox.valueProperty, endBox.valueProperty) { startBox.value >= endBox.value }
        setResultConverter { if (it == OK) Pair(startBox.value, endBox.value) else null }
    }.showAndWait().ifPresent { (start, end) ->
        transaction { Recesses.insert(Recess(start, end)) }
        refresh()
    }

    @FXML
    fun deleteOnAction() = confirmAlert(getString(R.string.are_you_sure), YES, NO)
            .showAndWait()
            .filter { it == YES }
            .ifPresent {
                transaction { Recesses.find { id.equal(tableView.selectionModel.selectedItem.id!!.value) }.remove() }
                refresh()
            }

    override fun refresh() {
        tableView.items = transaction { Recesses.find().toMutableObservableList() }
    }
}