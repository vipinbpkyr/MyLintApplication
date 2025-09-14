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

@Suppress("UnstableApiUsage")
class ClickableElementSemanticsDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames(): List<String> = listOf("clickable")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val composable = node.getContainingUFile()?.classes?.firstOrNull()
        val hasText = composable?.allMethods?.any { it.name == "Text" } ?: false
        val hasContentDescription = node.valueArguments.any { it.parameter?.name == "contentDescription" }

        if (!hasText && !hasContentDescription) {
            context.report(
                ISSUE_CLICKABLE_ELEMENT_SEMANTICS,
                node,
                context.getNameLocation(node),
                "Clickable element missing semantics. Provide a `contentDescription` or `text`."
            )
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
