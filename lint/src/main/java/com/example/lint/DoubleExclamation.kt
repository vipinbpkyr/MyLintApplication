package com.example.lintchecks

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UPostfixExpression
import org.jetbrains.uast.getContainingUFile

class DoubleExclamationDetector : Detector(), Detector.UastScanner {

    companion object {
        val ISSUE = Issue.create(
            id = "DoubleExclamationUsage",
            briefDescription = "Avoid using double exclamation (non-null assertion) operator",
            explanation = """
                The double exclamation mark (`!!`) operator in Kotlin forces a nullable type to be non-null, which can lead to a `NullPointerException` if the value is null. Consider using safe calls (`?.`), Elvis operator (`?:`), or null checks instead.
            """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 6,
            severity = Severity.WARNING,
            implementation = Implementation(
                DoubleExclamationDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }

    override fun getApplicableUastTypes(): List<Class<out UElement>> {
        return listOf(UPostfixExpression::class.java)
    }

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitPostfixExpression(node: UPostfixExpression) {
                if (node.operator.text == "!!") {
                    context.report(
                        issue = ISSUE,
                        scope = node,
                        location = context.getLocation(node),
                        message = "Avoid using `!!` operator; it may cause NullPointerException."
                    )
                }
            }
        }
    }
}