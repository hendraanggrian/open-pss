package com.wijayaprinting.manager.io

open class MySQLFile : PropertiesFile(".mysql",
        Pair(USERNAME, ""),
        Pair(IP, ""),
        Pair(PORT, "")
) {
    companion object : MySQLFile() {
        const val USERNAME = "username"
        const val IP = "ip"
        const val PORT = "port"
    }
}