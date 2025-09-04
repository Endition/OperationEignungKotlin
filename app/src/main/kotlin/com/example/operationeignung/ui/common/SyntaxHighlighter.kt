@file:Suppress("RegExpRedundantEscape", "unused")

package com.example.operationeignung.ui.common

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max

// ---------------------------- Theme ----------------------------

data class SyntaxTheme(
    val keyword: Color,
    val string: Color,
    val comment: Color,
    val number: Color,
    val type: Color,
    val function: Color,
    val punctuation: Color,
    val lineNumber: Color,
) {
    companion object {
        fun monokaiLike() = SyntaxTheme(
            keyword     = Color(0xFFF92672),
            string      = Color(0xFFE6DB74),
            comment     = Color(0xFF75715E),
            number      = Color(0xFFAE81FF),
            type        = Color(0xFFA6E22E),
            function    = Color(0xFF66D9EF),
            punctuation = Color(0xFFCDD3E2),
            lineNumber  = Color(0xFF9AA0A6)
        )
    }
}

// ---------------------------- Public API ----------------------------

@Composable
fun SyntaxHighlighterComposable(
    code: String,
    typeHint: String? = null,            // z.B. "kotlin", "kt", "json", "py", "js"
    showLineNumbers: Boolean = true,
    theme: SyntaxTheme = SyntaxTheme.monokaiLike()
) {
    if (code.isEmpty()) return

    // Tabs "glätten" für stabile Ausrichtung in Monospace
    val normalizedCode = remember(code) { code.replace("\t", "    ") }
    val lang = remember(normalizedCode, typeHint) { detectLanguage(typeHint, normalizedCode) }

    // Highlighting im Hintergrund-Thread berechnen
    val annotated: AnnotatedString? by produceState<AnnotatedString?>(initialValue = null, normalizedCode, lang, theme) {
        value = withContext(Dispatchers.Default) { highlight(normalizedCode, lang, theme) }
    }

    Surface(tonalElevation = 2.dp, shape = MaterialTheme.shapes.medium) {
        SelectionContainer {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.Top
            ) {
                if (showLineNumbers) {
                    val lineCount = normalizedCode.count { it == '\n' } + 1
                    val width = max(2, lineCount.toString().length)
                    val nums = buildString {
                        for (i in 1..lineCount) {
                            append(i.toString().padStart(width))
                            if (i < lineCount) append('\n')
                        }
                    }
                    Text(
                        text = nums,
                        color = theme.lineNumber,
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .align(Alignment.Top)
                            .padding(end = 12.dp)
                    )
                }

                Text(
                    text = annotated ?: AnnotatedString(normalizedCode),
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall,
                    softWrap = false
                )
            }
        }
    }
}

// ---------------------------- Core Highlighting ----------------------------

private enum class Lang { KOTLIN, PYTHON, JS, JSON, XML, UNKNOWN }

private enum class TokenKind {
    MARK,
    COMMENT,
    STRING,
    NUMBER,
    KEYWORD,
    TYPE,
    FUNCTION,
    PUNCT
}

private data class Rules(
    val styles: Map<TokenKind, SpanStyle>,
    val patterns: Map<TokenKind, List<Regex>>
)

private fun highlight(code: String, lang: Lang, theme: SyntaxTheme): AnnotatedString {
    val rules = when (lang) {
        Lang.KOTLIN  -> kotlinRules(theme)
        Lang.PYTHON  -> pythonRules(theme)
        Lang.JS      -> jsRules(theme)
        Lang.JSON    -> jsonRules(theme)
        Lang.XML     -> xmlRules(theme)
        else         -> kotlinRules(theme) // brauchbare Defaults
    }

    val builder = AnnotatedString.Builder(code)
    val mask = BooleanArray(code.length) // verhindert Überschneidung von Spans

    // Reihenfolge ist wichtig:
    // 1) MARK
    // 2) STRING/COMMENT  → "blockieren" Folge-Tokens
    // 3) Rest
    val order = listOf(
        TokenKind.MARK,
        TokenKind.STRING,
        TokenKind.COMMENT,
        TokenKind.NUMBER,
        TokenKind.KEYWORD,
        TokenKind.TYPE,
        TokenKind.FUNCTION,
        TokenKind.PUNCT
    )

    fun apply(kind: TokenKind) {
        val style = rules.styles[kind] ?: return
        val patterns = rules.patterns[kind] ?: return
        for (regex in patterns) {
            regex.findAll(code).forEach { m ->
                var start = m.range.first
                var endExcl = m.range.last + 1
                if (start < 0 || endExcl <= start) return@forEach
                if (endExcl > code.length) endExcl = code.length

                // Bereich frei?
                var free = true
                var i = start
                while (i < endExcl) {
                    if (mask[i]) { free = false; break }
                    i++
                }
                if (!free) return@forEach

                builder.addStyle(style, start, endExcl)
                java.util.Arrays.fill(mask, start, endExcl, true)
            }
        }
    }

    order.forEach { apply(it) }
    return builder.toAnnotatedString()
}

// ---------------------------- Language Detection ----------------------------

private fun detectLanguage(typeHint: String?, code: String): Lang {
    val hint = (typeHint ?: "").lowercase()
    if (hint.contains("kt") || hint.contains("kotlin")) return Lang.KOTLIN
    if (hint.contains("py")) return Lang.PYTHON
    if (hint.contains("js") || hint.contains("ts")) return Lang.JS
    if (hint.contains("json")) return Lang.JSON
    if (hint.contains("xml") || hint.contains("html")) return Lang.XML

    val head = code.trimStart().take(200)
    if (head.startsWith("{") || head.startsWith("[")) return Lang.JSON
    if ("class " in head || "fun " in head || "data class" in head) return Lang.KOTLIN
    if ("def " in head || head.startsWith("#!") || "import " in head) return Lang.PYTHON
    if ("function " in head || "const " in head || "let " in head) return Lang.JS
    if (head.startsWith("<")) return Lang.XML
    return Lang.KOTLIN
}

// ---------------------------- Regex Rule-Sets (cached) ----------------------------

private fun wordRegex(vararg words: String): Regex {
    // \b(?:w1|w2|...)\b
    val body = words.joinToString("|") { Regex.escape(it) }
    return Regex("\\b(?:$body)\\b")
}

// ---- Kotlin ----

private val KOTLIN_PATTERNS by lazy {
    mapOf(
        TokenKind.MARK to listOf(
            Regex("\\b(TODO|FIXME|HACK|BUG)\\b")
        ),
        TokenKind.COMMENT to listOf(
            Regex("(?m)//[^\\n\\r]*"),
            Regex("(?s)/\\*.*?\\*/")
        ),
        TokenKind.STRING to listOf(
            Regex("(?s)\"\"\".*?\"\"\""),                    // """ ... """
            Regex("\"([^\"\\\\]|\\\\.)*\""),                // "..."
            Regex("'([^'\\\\]|\\\\.)'")                     // Char-Literal
        ),
        TokenKind.NUMBER to listOf(
            Regex("\\b\\d+(?:\\.\\d+)?\\b")
        ),
        TokenKind.KEYWORD to listOf(
            wordRegex(
                "package","import","class","object","interface","data","sealed","enum",
                "fun","val","var","if","else","when","for","while","do","return","break","continue",
                "try","catch","finally","throw","in","is","as","null","true","false","where"
            ),
            Regex("@\\w+") // Annotationen
        ),
        TokenKind.TYPE to listOf(
            Regex("\\b([A-Z][A-Za-z0-9_]*)\\b")
        ),
        TokenKind.FUNCTION to listOf(
            Regex("\\b([a-zA-Z_][A-Za-z0-9_]*)\\s*(?=\\()")
        ),
        TokenKind.PUNCT to listOf(
            Regex("[{}\\[\\]();,.<>:+\\-*/=!&|]")
        )
    )
}

private fun kotlinRules(theme: SyntaxTheme) = Rules(
    styles = mapOf(
        TokenKind.MARK      to SpanStyle(theme.keyword),
        TokenKind.COMMENT   to SpanStyle(theme.comment),
        TokenKind.STRING    to SpanStyle(theme.string),
        TokenKind.NUMBER    to SpanStyle(theme.number),
        TokenKind.KEYWORD   to SpanStyle(theme.keyword),
        TokenKind.TYPE      to SpanStyle(theme.type),
        TokenKind.FUNCTION  to SpanStyle(theme.function),
        TokenKind.PUNCT     to SpanStyle(theme.punctuation),
    ),
    patterns = KOTLIN_PATTERNS
)

// ---- Python ----

private val PY_PATTERNS by lazy {
    mapOf(
        TokenKind.MARK to listOf(
            Regex("\\b(TODO|FIXME|HACK|BUG)\\b")
        ),
        TokenKind.COMMENT to listOf(
            Regex("(?m)#.*$")
        ),
        TokenKind.STRING to listOf(
            Regex("(?s)\"\"\".*?\"\"\""),
            Regex("(?s)'''.*?'''"),
            Regex("\"([^\"\\\\]|\\\\.)*\""),
            Regex("'([^'\\n\\\\]|\\\\.)*'")
        ),
        TokenKind.NUMBER to listOf(
            Regex("\\b\\d+(?:\\.\\d+)?\\b")
        ),
        TokenKind.KEYWORD to listOf(
            wordRegex(
                "def","class","return","if","elif","else","for","while","break","continue",
                "try","except","finally","raise","import","from","as","in","is","and","or","not",
                "None","True","False","with","yield","lambda","global","nonlocal","pass"
            )
        ),
        TokenKind.FUNCTION to listOf(
            Regex("\\b([A-Za-z_][A-Za-z0-9_]*)\\s*(?=\\()")
        ),
        TokenKind.PUNCT to listOf(
            Regex("[{}\\[\\]();,.<>:+\\-*/=!&|]")
        )
    )
}

private fun pythonRules(theme: SyntaxTheme) = Rules(
    styles = mapOf(
        TokenKind.MARK      to SpanStyle(theme.keyword),
        TokenKind.COMMENT   to SpanStyle(theme.comment),
        TokenKind.STRING    to SpanStyle(theme.string),
        TokenKind.NUMBER    to SpanStyle(theme.number),
        TokenKind.KEYWORD   to SpanStyle(theme.keyword),
        TokenKind.FUNCTION  to SpanStyle(theme.function),
        TokenKind.PUNCT     to SpanStyle(theme.punctuation),
    ),
    patterns = PY_PATTERNS
)

// ---- JavaScript ----

private val JS_PATTERNS by lazy {
    mapOf(
        TokenKind.MARK to listOf(
            Regex("\\b(TODO|FIXME|HACK|BUG)\\b")
        ),
        TokenKind.COMMENT to listOf(
            Regex("(?m)//[^\\n\\r]*"),
            Regex("(?s)/\\*.*?\\*/")
        ),
        TokenKind.STRING to listOf(
            Regex("(?s)`([^`\\\\]|\\\\.)*`"),               // Template String
            Regex("\"([^\"\\\\]|\\\\.)*\""),
            Regex("'([^'\\\\]|\\\\.)*'")
        ),
        TokenKind.NUMBER to listOf(
            Regex("\\b\\d+(?:\\.\\d+)?\\b")
        ),
        TokenKind.KEYWORD to listOf(
            wordRegex(
                "function","return","if","else","for","while","do","break","continue",
                "try","catch","finally","throw","in","instanceof","new","this","typeof",
                "var","let","const","class","extends","super","import","from","export","default",
                "true","false","null","undefined","yield","await","async"
            )
        ),
        TokenKind.FUNCTION to listOf(
            Regex("\\b([A-Za-z_$][A-Za-z0-9_$]*)\\s*(?=\\()")
        ),
        TokenKind.PUNCT to listOf(
            Regex("[{}\\[\\]();,.<>:+\\-*/=!&|]")
        )
    )
}

private fun jsRules(theme: SyntaxTheme) = Rules(
    styles = mapOf(
        TokenKind.MARK      to SpanStyle(theme.keyword),
        TokenKind.COMMENT   to SpanStyle(theme.comment),
        TokenKind.STRING    to SpanStyle(theme.string),
        TokenKind.NUMBER    to SpanStyle(theme.number),
        TokenKind.KEYWORD   to SpanStyle(theme.keyword),
        TokenKind.FUNCTION  to SpanStyle(theme.function),
        TokenKind.PUNCT     to SpanStyle(theme.punctuation),
    ),
    patterns = JS_PATTERNS
)

// ---- JSON ----

private val JSON_PATTERNS by lazy {
    mapOf(
        TokenKind.COMMENT to emptyList(), // JSON hat keine Kommentare (standardkonform)
        TokenKind.STRING to listOf(
            Regex("\"([^\"\\\\]|\\\\.)*\"")
        ),
        TokenKind.NUMBER to listOf(
            Regex("-?\\b\\d+(?:\\.\\d+)?(?:[eE][+\\-]?\\d+)?\\b")
        ),
        TokenKind.KEYWORD to listOf(
            wordRegex("true","false","null")
        ),
        TokenKind.PUNCT to listOf(
            Regex("[{}\\[\\]:,]")
        )
    )
}

private fun jsonRules(theme: SyntaxTheme) = Rules(
    styles = mapOf(
        TokenKind.STRING    to SpanStyle(theme.string),
        TokenKind.NUMBER    to SpanStyle(theme.number),
        TokenKind.KEYWORD   to SpanStyle(theme.keyword),
        TokenKind.PUNCT     to SpanStyle(theme.punctuation),
    ),
    patterns = JSON_PATTERNS
)

// ---- XML (leichtgewichtig) ----

private val XML_PATTERNS by lazy {
    mapOf(
        TokenKind.COMMENT to listOf(
            Regex("(?s)<!--.*?-->")
        ),
        TokenKind.STRING to listOf(
            Regex("\"([^\"\\\\]|\\\\.)*\""),
            Regex("'([^'\\\\]|\\\\.)*'")
        ),
        TokenKind.KEYWORD to listOf( // Tag-Namen, Attribute
            Regex("</?\\s*([A-Za-z_:][A-Za-z0-9_:\\-\\.]*)"),
            Regex("\\b([A-Za-z_:][A-Za-z0-9_:\\-\\.]*)\\s*=")
        ),
        TokenKind.PUNCT to listOf(
            Regex("[<>/=?]")
        )
    )
}

private fun xmlRules(theme: SyntaxTheme) = Rules(
    styles = mapOf(
        TokenKind.COMMENT   to SpanStyle(theme.comment),
        TokenKind.STRING    to SpanStyle(theme.string),
        TokenKind.KEYWORD   to SpanStyle(theme.keyword),
        TokenKind.PUNCT     to SpanStyle(theme.punctuation),
    ),
    patterns = XML_PATTERNS
)
