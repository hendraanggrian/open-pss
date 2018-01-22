package com.wijayaprinting.manager

import com.wijayaprinting.db.Database.login
import com.wijayaprinting.db.Employee
import com.wijayaprinting.db.PlateOrder
import com.wijayaprinting.db.PlateOrders
import com.wijayaprinting.db.transaction
import com.wijayaprinting.io.DatabaseFile
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import kotlin.test.assertNotNull

// @RunWith(JUnitPlatform::class)
object DatabaseSpec : Spek({

    if (DatabaseFile.isValid) given("a database") {
        login(DatabaseFile.host.value, DatabaseFile.port.value.toInt(), DatabaseFile.user.value, DatabaseFile.password.value, Employee.name, Employee.password).subscribe({
            transaction {
                val id = PlateOrders.insert(PlateOrder(null, 10, 100.0, 1000.0))
                it("should return id") {
                    assertNotNull(id)
                }
            }
        }) { error(it.message.toString()) }
    }
})