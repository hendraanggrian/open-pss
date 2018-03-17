package com.hendraanggrian.openpss.ui.order

import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.db.schema.Customer
import com.hendraanggrian.openpss.db.schema.Customers
import com.hendraanggrian.openpss.db.transaction
import com.hendraanggrian.openpss.ui.Resourced
import javafx.scene.control.ButtonType.OK
import javafx.scene.control.Dialog
import javafx.scene.control.ListView
import javafx.scene.control.TextField
import javafx.scene.image.ImageView
import ktfx.application.later
import ktfx.beans.binding.bindingOf
import ktfx.collections.toMutableObservableList
import ktfx.layouts.listView
import ktfx.layouts.textField
import ktfx.layouts.vbox
import ktfx.scene.control.cancelButton
import ktfx.scene.control.graphicIcon
import ktfx.scene.control.headerTitle
import ktfx.scene.control.okButton
import kotlinx.nosql.equal

class SearchCustomerDialog(resourced: Resourced) : Dialog<Customer>(), Resourced by resourced {

    companion object {
        private const val ITEMS_PER_PAGE = 15
    }

    private lateinit var textField: TextField
    private lateinit var listView: ListView<Customer>

    init {
        headerTitle = getString(R.string.search_customer)
        graphicIcon = ImageView(R.image.ic_user)
        dialogPane.content = vbox {
            textField = textField { promptText = getString(R.string.customer) }
            listView = listView<Customer> {
                itemsProperty().bind(bindingOf(textField.textProperty()) {
                    transaction {
                        when {
                            textField.text.isEmpty() -> Customers.find()
                            else -> Customers.find { name.equal(textField.text) }
                        }.take(ITEMS_PER_PAGE).toMutableObservableList()
                    }
                })
            } marginTop 8
        }
        cancelButton()
        okButton {
            disableProperty().bind(listView.selectionModel.selectedItemProperty().isNull)
        }
        later { textField.requestFocus() }
        setResultConverter {
            if (it == OK) listView.selectionModel.selectedItem
            else null
        }
    }
}