package com.example.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression
import java.awt.Color
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

class ColorContrastDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> = listOf("Text")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val textColor = node.getArgumentByName("color")?.let { extractColorFromExpression(it) }
        val backgroundColor = extractBackgroundColor(node, context)

        if (textColor != null && backgroundColor != null) {
            val fontSizeExpression = node.getArgumentByName("fontSize")
            val fontSize = extractFontSize(fontSizeExpression)
            // val fontWeightExpression = node.getArgumentByName("fontWeight", context) // For future enhancement

            val isLarge = isLargeText(fontSize  /* fontWeightExpression */)
            val requiredContrast = if (isLarge) 3.0 else 4.5

            val contrastRatio = calculateContrastRatio(textColor, backgroundColor)

            if (contrastRatio < requiredContrast) {
                val message = String.format(
                    Locale.US,
                    "Insufficient color contrast of %.2f:1. WCAG requires at least %.1f:1 for %s text.",
                    contrastRatio,
                    requiredContrast,
                    if (isLarge) "large" else "normal"
                )
                context.report(
                    ISSUE_COLOR_CONTRAST,
                    node,
                    context.getNameLocation(node),
                    message
                )
            }
        }
    }

    private fun UCallExpression.getArgumentByName(name: String): UExpression? {
        val resolvedMethod = resolve() ?: return null
        val psiParameter = resolvedMethod.parameterList.parameters.firstOrNull { it.name == name }
        if (psiParameter != null) {
            val parameterIndex = resolvedMethod.parameterList.getParameterIndex(psiParameter)
            if (parameterIndex != -1 && parameterIndex < valueArguments.size) {
                 // Check if the argument exists for that parameter index
                val argument = getArgumentForParameter(parameterIndex)
                if (argument != null) return argument
            }
        }
        // Fallback for varargs or complex scenarios - try to find by named argument syntax if any
        // This part is more complex and might need specific handling if Compose uses special argument mapping
        return valueArguments.firstOrNull { it is org.jetbrains.uast.UNamedExpression && it.name == name }
            ?: valueArguments.getOrNull(resolvedMethod.parameterList.parameters.indexOfFirst { it.name == name })

    }


    private fun extractColorFromExpression(expression: UExpression?): Int? {
        if (expression == null) return null

        val source = expression.sourcePsi?.text ?: return null
        // Handle Color(0xFF...) , Color(0X...) , Color(color = 0xFF...)
        val hexRegex = Regex("""Color\s*\(\s*(?:color\s*=\s*)?0[xX]([0-9a-fA-F]+)\s*\)""")
        val match = hexRegex.find(source)
        if (match != null) {
            val hexString = match.groupValues[1]
            // Ensure ARGB by padding if only RGB is provided. Assume FF for alpha if not present.
            val fullHexString = when (hexString.length) {
                6 -> "FF$hexString" // RGB to ARGB
                8 -> hexString        // ARGB
                else -> return null   // Invalid length
            }
            return fullHexString.toLongOrNull(16)?.toInt()
        }

        // Handle direct integer literals if they represent colors (less common for Compose Color)
        if (expression is org.jetbrains.uast.ULiteralExpression && expression.value is Int) {
            return expression.value as Int
        }
        
        // Handle named Color constants like Color.White
        if (expression is UQualifiedReferenceExpression) {
            if (expression.receiver.sourcePsi?.text == "Color") {
                 return when (expression.selector.sourcePsi?.text) {
                    "Black" -> 0xFF000000.toInt()
                    "DarkGray" -> 0xFF444444.toInt()
                    "Gray" -> 0xFF888888.toInt()
                    "LightGray" -> 0xFFCCCCCC.toInt()
                    "White" -> 0xFFFFFFFF.toInt()
                    "Red" -> 0xFFFF0000.toInt()
                    "Green" -> 0xFF00FF00.toInt()
                    "Blue" -> 0xFF0000FF.toInt()
                    "Yellow" -> 0xFFFFFF00.toInt()
                    "Cyan" -> 0xFF00FFFF.toInt()
                    "Magenta" -> 0xFFFF00FF.toInt()
                    "Transparent" -> 0x00000000
                    // Add more common colors if needed
                    else -> null
                }
            }
        }
        return null
    }

    private fun extractBackgroundColor(textNode: UCallExpression, context: JavaContext): Int? {
        // 1. Check Modifier.background on the Text composable itself
        textNode.getArgumentByName("modifier")?.let { modifierExpression ->
            findBackgroundColorInModifierChain(modifierExpression, context)?.let { return it }
        }

        // 2. Search UAST parents for Surface or other background-defining composables
        var currentElement: UElement? = textNode.uastParent
        while (currentElement != null) {
            if (currentElement is UCallExpression) {
                // Check for Surface(color = ...)
                if (currentElement.methodIdentifier?.name == "Surface") {
                    currentElement.getArgumentByName("color")?.let { colorExpr ->
                        extractColorFromExpression(colorExpr)?.let { return it }
                    }
                }
                // Potentially check for Box(modifier = Modifier.background(...)) or similar container patterns
                currentElement.getArgumentByName("modifier")?.let { modifierExpr ->
                     findBackgroundColorInModifierChain(modifierExpr, context)?.let { return it }
                }
            }
            currentElement = currentElement.uastParent
        }
        return null // Default or indicate not found
    }

    private fun findBackgroundColorInModifierChain(modifierExpression: UExpression, context: JavaContext): Int? {
        when (modifierExpression) {
            is UCallExpression -> { // e.g., Modifier.background(Color.Red)
                if (modifierExpression.methodIdentifier?.name == "background") {
                    // Try "color" named param or the first param if it's a direct color
                    val colorArg = modifierExpression.getArgumentByName("color")
                        ?: modifierExpression.valueArguments.firstOrNull()
                    return extractColorFromExpression(colorArg)
                }
                // Recursively check the receiver if it's a chain
                modifierExpression.receiver?.let {
                    return findBackgroundColorInModifierChain(it, context)
                }
            }
            is UQualifiedReferenceExpression -> { // e.g., Modifier.padding().background(Color.Red)
                 // Check the selector (the method call part)
                (modifierExpression.selector as? UCallExpression)?.let { call ->
                    if (call.methodIdentifier?.name == "background") {
                         val colorArg = call.getArgumentByName("color")
                            ?: call.valueArguments.firstOrNull()
                        extractColorFromExpression(colorArg)?.let { return it }
                    }
                }
                // Recursively check the receiver (the preceding part of the chain)
                return findBackgroundColorInModifierChain(modifierExpression.receiver, context)
            }
        }
        return null
    }


    private fun extractFontSize(expression: UExpression?): Float? {
        if (expression == null) return null
        val source = expression.sourcePsi?.text ?: return null
        // Handles "18.sp", "18.dp.sp" (though unusual), or direct number if unitless assumed sp
        val match = Regex("(\\d+\\.?\\d*|\\d*\\.?\\d+)\\.sp").find(source)
        if (match != null) {
            return match.groupValues[1].toFloatOrNull()
        }
        // Fallback for direct float/int literals if .sp is implicit or default
        if (expression is org.jetbrains.uast.ULiteralExpression) {
            (expression.value as? Number)?.toFloat()?.let { return it }
        }
        return null // Default font size if not found or not in sp
    }

    private fun isLargeText(fontSize: Float?): Boolean {
        if (fontSize == null) return false // Cannot determine if size is unknown

        // TODO: Implement fontWeight check. For now, only size-based.
        // val isBold = fontWeightExpression?.sourcePsi?.text?.contains("Bold", ignoreCase = true) == true
        // WCAG: 18pt (approx 18sp) or 14pt bold (approx 14sp bold)
        // if (isBold) {
        //     return fontSize >= 14f
        // }
        return fontSize >= 18f
    }

    private fun calculateLuminance(color: Int): Double {
        val awtColor = Color(color, true) // true to include alpha
        val r = awtColor.red / 255.0
        val g = awtColor.green / 255.0
        val b = awtColor.blue / 255.0

        val rLinear = if (r <= 0.03928) r / 12.92 else ((r + 0.055) / 1.055).pow(2.4)
        val gLinear = if (g <= 0.03928) g / 12.92 else ((g + 0.055) / 1.055).pow(2.4)
        val bLinear = if (b <= 0.03928) b / 12.92 else ((b + 0.055) / 1.055).pow(2.4)

        return 0.2126 * rLinear + 0.7152 * gLinear + 0.0722 * bLinear
    }

    private fun calculateContrastRatio(foreground: Int, background: Int): Double {
        val lum1 = calculateLuminance(foreground)
        val lum2 = calculateLuminance(background)

        val lighter = max(lum1, lum2)
        val darker = min(lum1, lum2)

        return (lighter + 0.05) / (darker + 0.05)
    }

    companion object {
        val ISSUE_COLOR_CONTRAST: Issue = Issue.create(
            id = "ColorContrast",
            briefDescription = "Insufficient color contrast",
            explanation = """
                The contrast between the text color and its background is too low. 
                WCAG 2.1 AA requires a contrast ratio of at least 4.5:1 for normal text 
                and 3.0:1 for large text (18pt or 14pt bold). Ensure sufficient contrast 
                to maintain readability and accessibility.
            """.trimIndent(),
            category = Category.A11Y,
            priority = 7, // High priority for accessibility
            severity = Severity.ERROR,
            implementation = Implementation(
                ColorContrastDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
