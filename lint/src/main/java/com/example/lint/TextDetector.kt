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
class TextDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf("Text")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        context.report(
            ISSUE_TEXT, node, context.getNameLocation(node),
            "Use AccessibleText instead of raw Text for WCAG compliance."
        )
    }

    companion object {
        private val IMPLEMENTATION = Implementation(
            TextDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        val ISSUE_TEXT: Issue = Issue.create(
            id = "RawTextUsage",
            briefDescription = "Use AccessibleText",
            explanation = "Raw Text does not guarantee accessibility. Use AccessibleText instead.",
            category = Category.USABILITY,
            priority = 8,
            severity = Severity.ERROR,
            implementation = IMPLEMENTATION
        )
    }
}
