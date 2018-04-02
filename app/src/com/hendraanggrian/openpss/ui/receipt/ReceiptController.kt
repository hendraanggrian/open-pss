package com.hendraanggrian.openpss.ui.receipt

import com.hendraanggrian.openpss.R
import com.hendraanggrian.openpss.db.buildQuery
import com.hendraanggrian.openpss.db.schema.Customer
import com.hendraanggrian.openpss.db.schema.Customers
import com.hendraanggrian.openpss.db.schema.Employees
import com.hendraanggrian.openpss.db.schema.Offset
import com.hendraanggrian.openpss.db.schema.Other
import com.hendraanggrian.openpss.db.schema.Payment
import com.hendraanggrian.openpss.db.schema.Payments
import com.hendraanggrian.openpss.db.schema.Plate
import com.hendraanggrian.openpss.db.schema.Receipt
import com.hendraanggrian.openpss.db.schema.Receipts
import com.hendraanggrian.openpss.db.transaction
import com.hendraanggrian.openpss.scene.control.CountBox
import com.hendraanggrian.openpss.scene.layout.DateBox
import com.hendraanggrian.openpss.time.PATTERN_DATETIME_EXTENDED
import com.hendraanggrian.openpss.ui.Addable
import com.hendraanggrian.openpss.ui.Controller
import com.hendraanggrian.openpss.ui.Refreshable
import com.hendraanggrian.openpss.ui.controller
import com.hendraanggrian.openpss.ui.pane
import com.hendraanggrian.openpss.util.currencyCell
import com.hendraanggrian.openpss.util.doneCell
import com.hendraanggrian.openpss.util.getNullable
import com.hendraanggrian.openpss.util.getResource
import com.hendraanggrian.openpss.util.numberCell
import com.hendraanggrian.openpss.util.stringCell
import com.hendraanggrian.openpss.util.yesNoAlert
import javafx.beans.property.SimpleObjectProperty
import javafx.fxml.FXML
import javafx.fxml.FXMLLoader
import javafx.scene.Node
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.control.ChoiceBox
import javafx.scene.control.Label
import javafx.scene.control.MenuItem
import javafx.scene.control.Pagination
import javafx.scene.control.RadioButton
import javafx.scene.control.Tab
import javafx.scene.control.TableColumn
import javafx.scene.control.TableView
import javafx.scene.control.TableView.CONSTRAINED_RESIZE_POLICY
import javafx.stage.Modality.APPLICATION_MODAL
import javafx.util.Callback
import kotlinx.nosql.equal
import kotlinx.nosql.id
import ktfx.application.later
import ktfx.beans.binding.bindingOf
import ktfx.beans.binding.or
import ktfx.beans.binding.stringBindingOf
import ktfx.beans.property.toProperty
import ktfx.collections.emptyBinding
import ktfx.collections.emptyObservableList
import ktfx.collections.toMutableObservableList
import ktfx.collections.toObservableList
import ktfx.coroutines.onAction
import ktfx.layouts.columns
import ktfx.layouts.contextMenu
import ktfx.layouts.menuItem
import ktfx.layouts.separatorMenuItem
import ktfx.layouts.tableView
import ktfx.stage.stage
import java.net.URL
import java.util.ResourceBundle
import kotlin.math.ceil

class ReceiptController : Controller(), Refreshable, Addable {

    @FXML lateinit var customerButton: Button
    @FXML lateinit var countBox: CountBox
    @FXML lateinit var statusBox: ChoiceBox<String>
    @FXML lateinit var allDateRadio: RadioButton
    @FXML lateinit var pickDateRadio: RadioButton
    @FXML lateinit var dateBox: DateBox
    @FXML lateinit var receiptPagination: Pagination
    @FXML lateinit var plateTab: Tab
    @FXML lateinit var plateTable: TableView<Plate>
    @FXML lateinit var plateTypeColumn: TableColumn<Plate, String>
    @FXML lateinit var plateTitleColumn: TableColumn<Plate, String>
    @FXML lateinit var plateQtyColumn: TableColumn<Plate, String>
    @FXML lateinit var platePriceColumn: TableColumn<Plate, String>
    @FXML lateinit var plateTotalColumn: TableColumn<Plate, String>
    @FXML lateinit var offsetTab: Tab
    @FXML lateinit var offsetTable: TableView<Offset>
    @FXML lateinit var offsetTypeColumn: TableColumn<Offset, String>
    @FXML lateinit var offsetTitleColumn: TableColumn<Offset, String>
    @FXML lateinit var offsetQtyColumn: TableColumn<Offset, String>
    @FXML lateinit var offsetMinQtyColumn: TableColumn<Offset, String>
    @FXML lateinit var offsetMinPriceColumn: TableColumn<Offset, String>
    @FXML lateinit var offsetExcessPriceColumn: TableColumn<Offset, String>
    @FXML lateinit var offsetTotalColumn: TableColumn<Offset, String>
    @FXML lateinit var otherTab: Tab
    @FXML lateinit var otherTable: TableView<Other>
    @FXML lateinit var otherTitleColumn: TableColumn<Other, String>
    @FXML lateinit var otherQtyColumn: TableColumn<Other, String>
    @FXML lateinit var otherPriceColumn: TableColumn<Other, String>
    @FXML lateinit var otherTotalColumn: TableColumn<Other, String>
    @FXML lateinit var noteTab: Tab
    @FXML lateinit var noteLabel: Label
    @FXML lateinit var coverLabel: Label
    @FXML lateinit var paymentTab: Tab
    @FXML lateinit var paymentTable: TableView<Payment>
    @FXML lateinit var paymentEmployeeColumn: TableColumn<Payment, String>
    @FXML lateinit var paymentDateTimeColumn: TableColumn<Payment, String>
    @FXML lateinit var paymentValueColumn: TableColumn<Payment, String>

    private val customerProperty = SimpleObjectProperty<Customer>()
    private lateinit var receiptTable: TableView<Receipt>

    override fun initialize(location: URL, resources: ResourceBundle) {
        super.initialize(location, resources)
        refresh()

        customerButton.textProperty().bind(stringBindingOf(customerProperty) {
            customerProperty.value?.toString() ?: getString(R.string.search_customer)
        })

        countBox.desc = getString(R.string.items)
        statusBox.items = listOf(R.string.any, R.string.unpaid, R.string.paid).map { getString(it) }.toObservableList()
        statusBox.selectionModel.selectFirst()
        pickDateRadio.graphic.disableProperty().bind(!pickDateRadio.selectedProperty())

        plateTypeColumn.stringCell { type }
        plateTitleColumn.stringCell { title }
        plateQtyColumn.numberCell { qty }
        platePriceColumn.currencyCell { price }
        plateTotalColumn.currencyCell { total }
        offsetTypeColumn.stringCell { type }
        offsetTitleColumn.stringCell { title }
        offsetQtyColumn.numberCell { qty }
        offsetMinQtyColumn.numberCell { minQty }
        offsetMinPriceColumn.currencyCell { minPrice }
        offsetExcessPriceColumn.currencyCell { excessPrice }
        offsetTotalColumn.currencyCell { total }
        otherTitleColumn.stringCell { title }
        otherQtyColumn.numberCell { qty }
        otherPriceColumn.currencyCell { price }
        otherTotalColumn.currencyCell { total }
    }

    override fun refresh() = receiptPagination.pageFactoryProperty()
        .bind(bindingOf(customerProperty, countBox.countProperty, statusBox.valueProperty(),
            allDateRadio.selectedProperty(), pickDateRadio.selectedProperty(), dateBox.dateProperty) {
            Callback<Int, Node> { page ->
                receiptTable = tableView {
                    columnResizePolicy = CONSTRAINED_RESIZE_POLICY
                    columns {
                        column<String>(getString(R.string.id)) { stringCell { id } }
                        column<String>(getString(R.string.date)) {
                            stringCell { dateTime.toString(PATTERN_DATETIME_EXTENDED) }
                        }
                        column<String>(getString(R.string.employee)) {
                            stringCell { transaction { Employees.find { id.equal(employeeId) }.single() }!! }
                        }
                        column<String>(getString(R.string.customer)) {
                            stringCell { transaction { Customers.find { id.equal(customerId) }.single() }!! }
                        }
                        column<String>(getString(R.string.total)) { currencyCell { total } }
                        column<Boolean>(getString(R.string.paid)) { doneCell { paid } }
                        column<Boolean>(getString(R.string.print)) { doneCell { printed } }
                    }
                    contextMenu {
                        menuItem(getString(R.string.add)) { onAction { this@ReceiptController.add() } }
                        separatorMenuItem()
                        menuItem(getString(R.string.edit)) {
                            bindDisable()
                            onAction {
                                ReceiptDialog(this@ReceiptController, receiptTable.selectionModel.selectedItem)
                                    .showAndWait()
                            }
                        }
                        menuItem(getString(R.string.delete)) {
                            bindDisable()
                            onAction {
                                yesNoAlert(getString(R.string.are_you_sure)) {
                                    receiptTable.selectionModel.selectedItem.let {
                                        transaction { Receipts.find { id.equal(it.id.value) }.remove() }
                                        receiptTable.items.remove(it)
                                    }
                                }
                            }
                        }
                    }
                    later {
                        transaction {
                            val receipts = Receipts.find {
                                buildQuery {
                                    if (customerProperty.value != null)
                                        and(customerId.equal(customerProperty.value.id))
                                    when (statusBox.value) {
                                        getString(R.string.paid) -> and(paid.equal(true))
                                        getString(R.string.unpaid) -> and(paid.equal(false))
                                    }
                                    if (pickDateRadio.isSelected)
                                        and(dateTime.matches(dateBox.date.toString().toPattern()))
                                }
                            }
                            receiptPagination.pageCount = ceil(receipts.count() / countBox.count.toDouble()).toInt()
                            items = receipts.skip(countBox.count * page).take(countBox.count).toMutableObservableList()
                        }
                    }
                }
                plateTable.bindTable(plateTab) { plates }
                offsetTable.bindTable(offsetTab) { offsets }
                otherTable.bindTable(otherTab) { others }
                noteLabel.textProperty().bind(stringBindingOf(receiptTable.selectionModel.selectedItemProperty()) {
                    receipt?.note ?: ""
                })
                paymentTable.bindTable(paymentTab) {
                    transaction { Payments.find { employeeId.equal(this@bindTable.employeeId) }.toList() }!!
                }
                coverLabel.visibleProperty().bind(receiptTable.selectionModel.selectedItemProperty().isNull)
                receiptTable
            }
        })

    override fun add() = ReceiptDialog(this).showAndWait().ifPresent {
        transaction {
            it.id = Receipts.insert(it)
            receiptTable.items.add(it)
            receiptTable.selectionModel.selectFirst()
        }
    }

    @FXML fun selectCustomer() = customerProperty.set(SearchCustomerDialog(this).showAndWait().getNullable())

    @FXML fun platePrice() = stage(getString(R.string.plate_price)) {
        initModality(APPLICATION_MODAL)
        val loader = FXMLLoader(getResource(R.layout.controller_price_plate), resources)
        scene = Scene(loader.pane)
        isResizable = false
        loader.controller._employee = _employee
    }.showAndWait()

    @FXML fun offsetPrice() = stage(getString(R.string.offset_price)) {
        initModality(APPLICATION_MODAL)
        val loader = FXMLLoader(getResource(R.layout.controller_price_offset), resources)
        scene = Scene(loader.pane)
        isResizable = false
        loader.controller._employee = _employee
    }.showAndWait()

    private inline val receipt: Receipt? get() = receiptTable.selectionModel.selectedItem

    private fun <S> TableView<S>.bindTable(tab: Tab, target: Receipt.() -> List<S>) {
        itemsProperty().bind(bindingOf(receiptTable.selectionModel.selectedItemProperty()) {
            receipt?.target()?.toObservableList() ?: emptyObservableList()
        })
        tab.graphicProperty().bind(bindingOf(itemsProperty()) {
            when {
                items.isEmpty() -> null
                else -> Label(items.size.toString())
            }
        })
    }

    private fun MenuItem.bindDisable() = later {
        disableProperty().bind(receiptTable.selectionModel.selectedItems.emptyBinding() or
            !isFullAccess.toProperty())
    }
}