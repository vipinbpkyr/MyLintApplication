package com.example.lint

import com.android.tools.lint.client.api.UElementHandler
import com.android.tools.lint.detector.api.Category
import com.android.tools.lint.detector.api.Context
import com.android.tools.lint.detector.api.DefaultPosition
import com.android.tools.lint.detector.api.Detector
import com.android.tools.lint.detector.api.Implementation
import com.android.tools.lint.detector.api.Issue
import com.android.tools.lint.detector.api.JavaContext
import com.android.tools.lint.detector.api.LintFix
import com.android.tools.lint.detector.api.Scope
import com.android.tools.lint.detector.api.Severity
import com.android.tools.lint.detector.api.SourceCodeScanner
import com.android.tools.lint.detector.api.TextFormat
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtPostfixExpression
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UPostfixExpression

/**
 * Flags any usage of Kotlin not-null assertion operator (!!).
 */
class NotNullAssertionDetector : Detector(), SourceCodeScanner {

    override fun getApplicableUastTypes(): List<Class<out UElement>> =
        listOf(UPostfixExpression::class.java)

    override fun createUastHandler(context: JavaContext): UElementHandler {
        return object : UElementHandler() {
            override fun visitPostfixExpression(node: UPostfixExpression) {
                val kt = node.sourcePsi as? KtPostfixExpression ?: return
                // The Kotlin PSI for `expr!!`
                // operationReference is `!!`, token is KtTokens.EXCLEXCL
                val token = kt.operationReference.getReferencedNameElementType()
                if (token == KtTokens.EXCLEXCL) {
                    reportNotNullAssertion(context, node, kt)
                }
            }
        }
    }

    private fun reportNotNullAssertion(
        context: JavaContext,
        node: UPostfixExpression,
        kt: KtPostfixExpression
    ) {
        val message = ISSUE.getExplanation(TextFormat.TEXT)

        // Build an auto-fix: replace `X!!` with `requireNotNull(X)`
        // Works in expression position and keeps value semantics.
        val operandText = node.operand.sourcePsi?.text ?: return
        val fullExprLoc = context.getLocation(node)

        val replaceWithRequireFix = LintFix.create()
            .name("Replace with requireNotNull(...)")
            .replace()
            .range(fullExprLoc)
            .with("requireNotNull($operandText)")
            .reformat(true)
            .build()

        // Secondary fix: if user prefers a safer explicit check, insert an Elvis guard.
        // We keep this as a suggestive fix (no shadows of local flow).
        val cursor = context.getLocation(kt.operationReference)
        val elvisFix = LintFix.create()
            .name("Use Elvis to handle null (?:)")
            .replace()
            .range(DefaultPosition(
                cursor.start?.line ?: 0,
                cursor.start?.column ?: 0,
                0
            ), DefaultPosition(
                cursor.end?.line ?: 0,
                cursor.end?.column ?: 0,
                0
            ))
            // Replace only the `!!` token with ` ?: error("null value")`
            // e.g., `x!!` -> `x ?: error("null value")`
            .with(" ?: error(\"null value\")")
            .build()

        context.report(
            ISSUE,
            node,
            fullExprLoc,
            message,
            arrayOf(replaceWithRequireFix, elvisFix)
        )
    }

    companion object {
        private val IMPLEMENTATION = Implementation(
            NotNullAssertionDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        @JvmField
        val ISSUE: Issue = Issue.create(
            id = "NotNullAssertionOperator",
            briefDescription = "Avoid Kotlin not-null assertion (!!)",
            explanation = """
                Using the Kotlin not-null assertion operator (`!!`) will throw a `NullPointerException` \
                at runtime if the value is null. Prefer safer alternatives such as `requireNotNull(...)`, \
                explicit null handling with Elvis (`?:`) or redesigning the types to be non-nullable.
            """.trimIndent(),
            category = Category.CORRECTNESS,
            priority = 7,
            severity = Severity.ERROR,
            implementation = IMPLEMENTATION,
            androidSpecific = false
        )
    }
}