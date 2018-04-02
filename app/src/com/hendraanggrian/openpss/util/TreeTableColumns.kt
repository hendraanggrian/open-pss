@file:Suppress("NOTHING_TO_INLINE")

package com.hendraanggrian.openpss.util

import javafx.scene.control.TreeTableColumn
import ktfx.beans.property.toProperty

inline fun <T> TreeTableColumn<T, String>.stringCell(noinline target: T.() -> Any) =
    setCellValueFactory { it.value.value.target().let { it as? String ?: it.toString() }.toProperty() }