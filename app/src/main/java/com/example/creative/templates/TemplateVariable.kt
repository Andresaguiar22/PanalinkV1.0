package com.example.creative.templates

/**
 * P6.6.4.1 - Template Variable
 * Dynamic variables for template text placeholders.
 */
enum class TemplateVariable(val placeholder: String, val defaultLabel: String) {
    TITLE("{TITLE}", "TÍTULO PRINCIPAL"),
    SUBTITLE("{SUBTITLE}", "Subtítulo o descripción corta"),
    AUTHOR("{AUTHOR}", "@usuario"),
    LOCATION("{LOCATION}", "📍 Ubicación"),
    DATE("{DATE}", "📅 Fecha"),
    HASHTAG("{HASHTAG}", "#PanaLink #Creative");

    companion object {
        fun replacePlaceholders(
            templateText: String,
            values: Map<TemplateVariable, String>
        ): String {
            var result = templateText
            values.forEach { (variable, value) ->
                result = result.replace(variable.placeholder, value)
            }
            // Replace remaining unfulfilled placeholders with default label
            entries.forEach { entry ->
                if (result.contains(entry.placeholder)) {
                    result = result.replace(entry.placeholder, entry.defaultLabel)
                }
            }
            return result
        }
    }
}
