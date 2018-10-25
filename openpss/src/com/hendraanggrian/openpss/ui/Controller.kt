package com.hendraanggrian.openpss.ui

import com.hendraanggrian.openpss.Context
import com.hendraanggrian.openpss.db.schemas.Employee
import javafx.fxml.Initializable
import javafx.scene.layout.StackPane
import java.net.URL
import java.util.ResourceBundle

/** Base class of all controllers. */
open class Controller : Initializable, Context {

    override lateinit var employee: Employee
    override lateinit var dialogContainer: StackPane

    override lateinit var resources: ResourceBundle

    private var extras: MutableMap<String, Any>? = null

    override fun initialize(location: URL, resources: ResourceBundle) {
        this.resources = resources
        (this as? Refreshable)?.refresh()
    }

    /** Register extra [value] with [key]. */
    fun addExtra(key: String, value: Any): Controller {
        if (extras == null) extras = mutableMapOf()
        extras!![key] = value
        return this
    }

    /** Get extra registered with [key], should be executed in platform thread. */
    fun <T : Any> getExtra(key: String): T {
        checkNotNull(extras) { "No extras found in this controller." }
        @Suppress("UNCHECKED_CAST") return checkNotNull(extras!![key] as T) { "No extra found with that key." }
    }
}