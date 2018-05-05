package com.hendraanggrian.openpss.ui.invoice

import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.controls.Popup
import com.hendraanggrian.openpss.db.dbDateTime
import com.hendraanggrian.openpss.db.schemas.Customer
import com.hendraanggrian.openpss.db.schemas.Customers
import com.hendraanggrian.openpss.db.schemas.Employee
import com.hendraanggrian.openpss.db.schemas.Employees
import com.hendraanggrian.openpss.db.schemas.Invoice
import com.hendraanggrian.openpss.db.transaction
import com.hendraanggrian.openpss.io.properties.SettingsFile.INVOICE_QUICK_SELECT_CUSTOMER
import com.hendraanggrian.openpss.resources.Resourced
import com.hendraanggrian.openpss.ui.invoice.order.AddOffsetPopup
import com.hendraanggrian.openpss.ui.invoice.order.AddOtherPopup
import com.hendraanggrian.openpss.ui.invoice.order.AddPlatePopup
import com.hendraanggrian.openpss.util.PATTERN_DATE
import com.hendraanggrian.openpss.util.currencyCell
import com.hendraanggrian.openpss.util.currencyConverter
import com.hendraanggrian.openpss.util.getColor
import com.hendraanggrian.openpss.util.getFont
import com.hendraanggrian.openpss.util.getStyle
import com.hendraanggrian.openpss.util.numberCell
import com.hendraanggrian.openpss.util.stringCell
import javafx.beans.property.DoubleProperty
import javafx.beans.property.ObjectProperty
import javafx.beans.property.SimpleDoubleProperty
import javafx.beans.property.SimpleObjectProperty
import javafx.scene.Node
import javafx.scene.control.ButtonType.CANCEL
import javafx.scene.control.Dialog
import javafx.scene.control.TableView
import javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY
import javafx.scene.control.TextArea
import javafx.scene.image.ImageView
import ktfx.application.later
import ktfx.beans.binding.`when`
import ktfx.beans.binding.doubleBindingOf
import ktfx.beans.binding.otherwise
import ktfx.beans.binding.stringBindingOf
import ktfx.beans.binding.then
import ktfx.beans.value.greater
import ktfx.beans.value.lessEq
import ktfx.beans.value.or
import ktfx.collections.isEmpty
import ktfx.coroutines.onAction
import ktfx.coroutines.onKeyPressed
import ktfx.layouts.LayoutManager
import ktfx.layouts.button
import ktfx.layouts.columns
import ktfx.layouts.contextMenu
import ktfx.layouts.gridPane
import ktfx.layouts.label
import ktfx.layouts.separatorMenuItem
import ktfx.layouts.tableView
import ktfx.layouts.textArea
import ktfx.scene.control.cancelButton
import ktfx.scene.control.graphicIcon
import ktfx.scene.control.headerTitle
import ktfx.scene.control.okButton
import ktfx.scene.input.isDelete
import ktfx.scene.layout.gap
import org.joda.time.DateTime

class InvoiceDialog(
    resourced: Resourced,
    private val prefill: Invoice? = null,
    employee: Employee? = prefill?.let { transaction { Employees[prefill.employeeId].single() } }
) : Dialog<Invoice>(), Resourced by resourced {

    private lateinit var plateTable: TableView<Invoice.Plate>
    private lateinit var offsetTable: TableView<Invoice.Offset>
    private lateinit var otherTable: TableView<Invoice.Other>
    private lateinit var noteArea: TextArea

    private val dateTime: DateTime = prefill?.dateTime ?: dbDateTime
    private val customerProperty: ObjectProperty<Customer> = SimpleObjectProperty(when {
        isEdit() -> transaction { Customers[prefill!!.customerId].single() }
        else -> null
    })
    private val totalProperty: DoubleProperty = SimpleDoubleProperty()

    init {
        headerTitle = getString(if (!isEdit()) R.string.add_invoice else R.string.edit_invoice)
        graphicIcon = ImageView(R.image.header_invoice)
        dialogPane.run {
            stylesheets += getStyle(R.style.openpss)
            content = gridPane {
                gap = 8.0
                label(getString(R.string.employee)) col 0 row 0
                label(employee!!.name) { font = getFont(R.font.sf_pro_text_bold) } col 1 row 0
                label(getString(R.string.date)) col 0 row 1
                label(dateTime.toString(PATTERN_DATE)) { font = getFont(R.font.sf_pro_text_bold) } col 1 row 1
                label(getString(R.string.customer)) col 0 row 2
                button {
                    isDisable = isEdit()
                    textProperty().bind(stringBindingOf(customerProperty) {
                        customerProperty.value?.toString() ?: getString(R.string.search_customer)
                    })
                    onAction {
                        SearchCustomerPopup(this@InvoiceDialog).show(this@button) { customerProperty.set(it) }
                    }
                    if (INVOICE_QUICK_SELECT_CUSTOMER && !isEdit()) fire()
                } col 1 row 2
                label(getString(R.string.plate)) col 0 row 3
                plateTable = invoiceTableView<Invoice.Plate>({ AddPlatePopup(this@InvoiceDialog) }) {
                    columns {
                        getString(R.string.machine)<String> { stringCell { machine } }
                        getString(R.string.title)<String> { stringCell { title } }
                        getString(R.string.qty)<String> { numberCell { qty } }
                        getString(R.string.price)<String> { currencyCell { price } }
                        getString(R.string.total)<String> { currencyCell { total } }
                    }
                    if (isEdit()) items.addAll(prefill!!.plates)
                } col 1 row 3
                label(getString(R.string.offset)) col 0 row 4
                offsetTable = invoiceTableView<Invoice.Offset>({ AddOffsetPopup(this@InvoiceDialog) }) {
                    columns {
                        getString(R.string.machine)<String> { stringCell { machine } }
                        getString(R.string.title)<String> { stringCell { title } }
                        getString(R.string.qty)<String> { numberCell { qty } }
                        getString(R.string.min_qty)<String> { numberCell { minQty } }
                        getString(R.string.min_price)<String> { currencyCell { minPrice } }
                        getString(R.string.excess_price)<String> { currencyCell { excessPrice } }
                        getString(R.string.total)<String> { currencyCell { total } }
                    }
                    if (isEdit()) items.addAll(prefill!!.offsets)
                } col 1 row 4
                label(getString(R.string.others)) col 0 row 5
                otherTable = invoiceTableView<Invoice.Other>({ AddOtherPopup(this@InvoiceDialog) }) {
                    columns {
                        getString(R.string.title)<String> { stringCell { title } }
                        getString(R.string.qty)<String> { numberCell { qty } }
                        getString(R.string.price)<String> { currencyCell { price } }
                        getString(R.string.total)<String> { currencyCell { total } }
                    }
                    if (isEdit()) items.addAll(prefill!!.others)
                } col 1 row 5
                totalProperty.bind(doubleBindingOf(plateTable.items, offsetTable.items, otherTable.items) {
                    plateTable.items.sumByDouble { it.total } +
                        offsetTable.items.sumByDouble { it.total } +
                        otherTable.items.sumByDouble { it.total }
                })
                label(getString(R.string.note)) col 0 row 6
                noteArea = textArea {
                    prefHeight = 48.0
                    if (isEdit()) text = prefill!!.note
                } col 1 row 6
                label(getString(R.string.total)) col 0 row 7
                label {
                    font = getFont(R.font.sf_pro_text_bold)
                    textProperty().bind(stringBindingOf(totalProperty) { currencyConverter.toString(totalProperty.value) })
                    textFillProperty().bind(`when`(totalProperty greater 0)
                        then getColor(R.color.teal)
                        otherwise getColor(R.color.red))
                } col 1 row 7
            }
        }
        cancelButton()
        okButton().disableProperty().bind(customerProperty.isNull or totalProperty.lessEq(0))
        setResultConverter {
            when (it) {
                CANCEL -> null
                else -> when {
                    isEdit() -> prefill!!.apply {
                        plates = plateTable.items
                        offsets = offsetTable.items
                        others = otherTable.items
                        note = noteArea.text
                    }
                    else -> Invoice.new(
                        employee!!.id,
                        customerProperty.value.id,
                        dateTime,
                        plateTable.items,
                        offsetTable.items,
                        otherTable.items,
                        noteArea.text
                    )
                }
            }
        }
    }

    private fun isEdit(): Boolean = prefill != null

    private fun <S> LayoutManager<Node>.invoiceTableView(
        newAddOrderPopup: () -> Popup<S>,
        init: TableView<S>.() -> Unit
    ): TableView<S> = tableView {
        prefHeight = 96.0
        columnResizePolicy = CONSTRAINED_RESIZE_POLICY
        init()
        columns.forEach {
            val minWidth = 768.0 / columns.size
            it.minWidth = when (it.text) {
                getString(R.string.title) -> 256.0
                else -> minWidth
            }
        }
        contextMenu {
            getString(R.string.add)(ImageView(R.image.menu_add)) {
                onAction { newAddOrderPopup().show(this@tableView) { this@tableView.items.add(it) } }
            }
            separatorMenuItem()
            getString(R.string.delete)(ImageView(R.image.menu_delete)) {
                later { disableProperty().bind(this@tableView.selectionModel.selectedItemProperty().isNull) }
                onAction { this@tableView.items.remove(this@tableView.selectionModel.selectedItem) }
            }
            getString(R.string.clear)(ImageView(R.image.menu_clear)) {
                later { disableProperty().bind(this@tableView.items.isEmpty) }
                onAction { this@tableView.items.clear() }
            }
        }
        onKeyPressed {
            if (it.code.isDelete() && selectionModel.selectedItem != null) items.remove(selectionModel.selectedItem)
        }
    }
}