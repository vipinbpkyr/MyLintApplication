package com.example.lint

import com.android.tools.lint.detector.api.*
import org.jetbrains.uast.*

class DoubleNegationDetector : Detector(), Detector.UastScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UUnaryExpression::class.java, UQualifiedReferenceExpression::class.java)

    override fun createUastHandler(context: JavaContext) =
        object : com.android.tools.lint.client.api.UElementHandler() {

            override fun visitUnaryExpression(node: UUnaryExpression) {
                // Detect Kotlin non-null assertion (!!)
                if (node.sourcePsi?.text?.contains("!!") == true) {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        "Avoid using double-bang (`!!`). Consider safer alternatives (?. or requireNotNull)."
                    )
                }

                // Detect !!expr (double logical NOT)
                if (node.operator == UastPrefixOperator.LOGICAL_NOT &&
                    node.operand is UUnaryExpression &&
                    (node.operand as UUnaryExpression).operator == UastPrefixOperator.LOGICAL_NOT
                ) {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        "Redundant double negation (`!!expr`). Simplify to just `expr`."
                    )
                }
            }

            override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression) {
                val call = node.selector as? UCallExpression ?: return
                if (call.methodName != "not") return

                val parent = node.uastParent

                // Case 1: !expr.not()
                if (parent is UUnaryExpression &&
                    parent.operator == UastPrefixOperator.LOGICAL_NOT
                ) {
                    val src = node.receiver?.sourcePsi?.text ?: "expr"
                    context.report(
                        ISSUE,
                        parent,
                        context.getLocation(parent),
                        "Simplify `!$src.not()` to `$src`."
                    )
                }

                // Case 2: expr.not().not()
                var current: UQualifiedReferenceExpression? = node
                var depth = 0
                while (current != null) {
                    val sel = current.selector as? UCallExpression
                    if (sel?.methodName == "not") {
                        depth++
                        if (depth == 2) {
                            val src = (current.receiver as? UQualifiedReferenceExpression)
                                ?.receiver?.sourcePsi?.text ?: "expr"
                            context.report(
                                ISSUE,
                                current,
                                context.getLocation(current),
                                "Simplify `$src.not().not()` to `$src`."
                            )
                            break
                        }
                    }
                    current = current.receiver as? UQualifiedReferenceExpression
                }

                // Case 3: expr.isEmpty().not() → expr.isNotEmpty()
                val receiverText = node.receiver?.sourcePsi?.text ?: return
                if (receiverText.endsWith("isEmpty()")) {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        "Simplify `$receiverText.not()` to `${receiverText.removeSuffix("isEmpty()")}isNotEmpty()`."
                    )
                }
                if (receiverText.endsWith("isNotEmpty()")) {
                    context.report(
                        ISSUE,
                        node,
                        context.getLocation(node),
                        "Simplify `$receiverText.not()` to `${receiverText.removeSuffix("isNotEmpty()")}isEmpty()`."
                    )
                }
            }
        }

    companion object {
        val ISSUE: Issue = Issue.create(
            id = "DoubleNegation",
            briefDescription = "Double Negation Detected",
            explanation = """
                Double negations reduce readability and can usually be simplified:
                
                * `!!foo` → unsafe, use safe calls or `requireNotNull`.
                * `!!expr` → redundant, just `expr`.
                * `!expr.not()` → simplify to `expr`.
                * `expr.not().not()` → simplify to `expr`.
                * `list.isEmpty().not()` → simplify to `list.isNotEmpty()`.
                * `list.isNotEmpty().not()` → simplify to `list.isEmpty()`.
            """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 7,
            severity = Severity.WARNING,
            implementation = Implementation(
                DoubleNegationDetector::class.java,
                Scope.JAVA_FILE_SCOPE
            )
        )
    }
}
