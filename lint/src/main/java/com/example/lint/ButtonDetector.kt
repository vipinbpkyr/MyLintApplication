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
class ButtonDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf("Button")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        context.report(
            ISSUE_BUTTON, node, context.getNameLocation(node),
            "Use AccessibleButton instead of raw Button for WCAG compliance."
        )
    }

    companion object {
        private val IMPLEMENTATION = Implementation(
            ButtonDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        val ISSUE_BUTTON: Issue = Issue.create(
            id = "RawButtonUsage",
            briefDescription = "Use AccessibleButton",
            explanation = "Raw Button does not guarantee accessibility. Use AccessibleButton instead.",
            category = Category.USABILITY,
            priority = 8,
            severity = Severity.ERROR,
            implementation = IMPLEMENTATION
        )
    }
}
