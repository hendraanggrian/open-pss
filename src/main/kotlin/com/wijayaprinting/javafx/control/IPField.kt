package com.wijayaprinting.javafx.control

import javafx.beans.property.SimpleBooleanProperty
import kotfx.bindings.booleanBindingOf
import org.apache.commons.validator.routines.InetAddressValidator

/**
 * @author Hendra Anggrian (hendraanggrian@gmail.com)
 */
class IPField : TextField2 {

    val validProperty = SimpleBooleanProperty().apply { bind(booleanBindingOf(textProperty()) { InetAddressValidator.getInstance().isValidInet4Address(text) }) }
    val isValid = validProperty.value

    constructor() : super()

    constructor(promptText: String) : super(promptText)

    constructor(promptText: String, text: String) : super(promptText, text)
}