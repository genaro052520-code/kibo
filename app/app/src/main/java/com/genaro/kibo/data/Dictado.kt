package com.genaro.kibo.data

import java.text.Normalizer

/** Un alimento detectado en la frase. */
data class FraseParseada(
    val cantidad: Double,
    val unidad: String,     // abreviatura: pza, kg, L, g
    val consulta: String,   // lo que se busca en el catálogo
    val original: String
)

/** Una línea de la pantalla de confirmación. */
data class LineaDictada(
    val id: Int,
    val consulta: String,
    val cantidad: Double,
    val unidad: String,
    val candidatos: List<ProductoDto>,
    val elegidoId: Int?,
    val incluir: Boolean
) {
    val elegido: ProductoDto? get() = candidatos.firstOrNull { it.id_producto == elegidoId }
}

object Dictado {

    private val numeros = mapOf(
        "un" to 1.0, "uno" to 1.0, "una" to 1.0, "dos" to 2.0, "tres" to 3.0,
        "cuatro" to 4.0, "cinco" to 5.0, "seis" to 6.0, "siete" to 7.0, "ocho" to 8.0,
        "nueve" to 9.0, "diez" to 10.0, "once" to 11.0, "doce" to 12.0,
        "quince" to 15.0, "veinte" to 20.0,
        "medio" to 0.5, "media" to 0.5, "cuarto" to 0.25
    )

    // palabra -> (abreviatura de la unidad en la base, factor)
    private val unidades = mapOf(
        "litro" to ("L" to 1.0), "litros" to ("L" to 1.0), "lt" to ("L" to 1.0),
        "kilo" to ("kg" to 1.0), "kilos" to ("kg" to 1.0), "kg" to ("kg" to 1.0),
        "kilogramo" to ("kg" to 1.0), "kilogramos" to ("kg" to 1.0),
        "gramo" to ("g" to 1.0), "gramos" to ("g" to 1.0), "gr" to ("g" to 1.0),
        "pieza" to ("pza" to 1.0), "piezas" to ("pza" to 1.0), "pza" to ("pza" to 1.0),
        "pzas" to ("pza" to 1.0),
        "docena" to ("pza" to 12.0), "docenas" to ("pza" to 12.0)
    )

    // Palabras que se ignoran al inicio de cada alimento
    private val relleno = setOf(
        "compre", "compramos", "compro", "compraste", "agrega", "agregar", "agregame",
        "agregale", "anade", "anadir", "tengo", "tenemos", "hay", "quiero", "necesito",
        "guarda", "guardar", "puse", "meti", "me", "quedan", "quedaron", "tambien", "ya",
        "pues", "entonces", "hoy", "ayer", "en", "el", "la", "los", "las"
    )

    // Palabras que se quitan antes del nombre del producto
    private val articulos = setOf("de", "del", "la", "el", "los", "las", "un", "una",
        "unos", "unas", "poco", "poquito")

    /** Minúsculas, sin acentos ni signos raros. Conserva comas, punto y coma y decimales. */
    fun normalizar(s: String): String =
        Normalizer.normalize(s, Normalizer.Form.NFD)
            .replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase()
            .replace(Regex("(\\d),(\\d)"), "\$1.\$2")          // 1,5 -> 1.5
            .replace(Regex("[^a-z0-9.,; ]"), " ")
            .replace(Regex("(?<!\\d)\\.|\\.(?!\\d)"), " ")      // puntos de oración
            .trim()

    /** Convierte una frase en una lista de alimentos con cantidad y unidad. */
    fun interpretar(frase: String): List<FraseParseada> {
        val texto = normalizar(frase)
        val segmentos = texto.split(
            Regex("""\s*(?:,|;|\b(?:y|e|mas|luego|tambien|ademas)\b)\s*""")
        )
        return segmentos.mapNotNull { interpretarSegmento(it) }
    }

    private fun numero(token: String): Double? {
        numeros[token]?.let { return it }
        return if (token.matches(Regex("\\d+(\\.\\d+)?"))) token.toDouble() else null
    }

    private fun interpretarSegmento(segmento: String): FraseParseada? {
        val t = segmento.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        var i = 0
        while (i < t.size && t[i] in relleno) i++

        var cantidad = 1.0
        var unidad = "pza"

        // "un cuarto de kilo": el "un" sobra
        if (i + 1 < t.size && (t[i] == "un" || t[i] == "una") && t[i + 1] == "cuarto") i++

        if (i < t.size) {
            val n = numero(t[i])
            if (n != null) {
                cantidad = n
                i++
                // "cuarto de kilo": se salta el "de" si sigue una unidad
                if (i + 1 < t.size && t[i] == "de" && t[i + 1] in unidades) i++
            }
        }
        if (i < t.size) {
            val u = unidades[t[i]]
            if (u != null) {
                unidad = u.first
                cantidad *= u.second
                i++
            }
        }
        while (i < t.size && t[i] in articulos) i++

        var consulta = t.drop(i).joinToString(" ")
        // "leche en el refri" -> "leche"
        consulta = Regex("""\s+(en el|en la|en mi|para)\b.*$""").replace(consulta, "")
        if (consulta.isBlank()) return null

        return FraseParseada(cantidad, unidad, consulta.trim(), segmento.trim())
    }
}