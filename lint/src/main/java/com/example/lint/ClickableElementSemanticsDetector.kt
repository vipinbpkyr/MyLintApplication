package com.example.lint

import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.UastScanner
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UElementHandler
import org.jetbrains.uast.getArgumentMapping

@Suppress("UnstableApiUsage")
class ClickableElementSemanticsDetector : Detector(), UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UCallExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler =
        object : UElementHandler() {
            override fun visitCallExpression(node: UCallExpression) {
                // Is it a composable function? (Heuristic: starts with uppercase)
                if (node.methodName?.firstOrNull()?.isUpperCase() != true) {
                    return
                }

                val argumentMapping = node.getArgumentMapping()
                val modifierArgument = argumentMapping.entries
                    .find { it.value.name == "modifier" }
                    ?.key

                if (modifierArgument != null) {
                    // Check if the modifier expression contains a "clickable" call
                    val source = modifierArgument.asSourceString()
                    if (source.contains(".clickable")) {
                        // This is a clickable component. Now check for semantics.
                        val hasText = argumentMapping.values.any { it.name == "text" }
                        val hasContentDescription = argumentMapping.values.any { it.name == "contentDescription" }

                        if (!hasText && !hasContentDescription) {
                            context.report(
                                ISSUE_CLICKABLE_ELEMENT_SEMANTICS,
                                node,
                                context.getNameLocation(node),
                                "Clickable element missing semantics. Provide a `contentDescription` or `text`."
                            )
                        }
                    }
                }
            }
        }

    companion object {
        private val IMPLEMENTATION = Implementation(
            ClickableElementSemanticsDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        val ISSUE_CLICKABLE_ELEMENT_SEMANTICS: Issue = Issue.create(
            id = "ClickableElementSemantics",
            briefDescription = "Clickable element missing semantics",
            explanation = "Clickable elements should have a `contentDescription` or `text` to be accessible. This is important for screen readers.",
            category = Category.A11Y,
            priority = 6,
            severity = Severity.WARNING,
            implementation = IMPLEMENTATION
        )
    }
}
