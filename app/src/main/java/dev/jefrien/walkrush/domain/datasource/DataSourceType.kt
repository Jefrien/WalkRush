package dev.jefrien.walkrush.domain.datasource

enum class DataSourceType(val displayName: String, val description: String) {
    HEALTH_CONNECT(
        displayName = "Health Connect",
        description = "Datos agregados de todas las apps (Google, Samsung, etc.)"
    ),
    SAMSUNG_HEALTH(
        displayName = "Samsung Health",
        description = "Datos directos de Samsung Health (requiere SDK)"
    );

    companion object {
        fun fromName(name: String): DataSourceType =
            entries.find { it.name == name } ?: HEALTH_CONNECT
    }
}
