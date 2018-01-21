package com.wijayaprinting.nosql

import kotlinx.nosql.Id
import kotlinx.nosql.boolean
import kotlinx.nosql.mongodb.DocumentSchema
import kotlinx.nosql.string

object Employees : DocumentSchema<Employee>("employee", Employee::class) {
    val name = string("name")
    val password = string("password")
    val fullAccess = boolean("full_access")
}

data class Employee(
        var name: String,
        var password: String,
        var fullAccess: Boolean
) {
    lateinit var id: Id<String, Employees>
    var firstTimeLogin: Boolean = false

    /** Password are unused after login, clear for better security. */
    fun clearPassword() {
        firstTimeLogin = password == DEFAULT_PASSWORD
        password = ""
    }

    companion object {
        const val DEFAULT_PASSWORD = "1234"
    }
}