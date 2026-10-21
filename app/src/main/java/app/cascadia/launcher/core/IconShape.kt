package app.cascadia.launcher.core

enum class IconShape(val label: String) {
    SQUARE("Square"), SQUIRCLE("Squircle"), CIRCLE("Circle");

    companion object {
        fun fromStored(value: String?): IconShape = entries.firstOrNull { it.name == value } ?: SQUIRCLE
    }
}
