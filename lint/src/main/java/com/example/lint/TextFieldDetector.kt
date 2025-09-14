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
class TextFieldDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf("OutlinedTextField", "TextField")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        context.report(
            ISSUE_TEXTFIELD, node, context.getNameLocation(node),
            "Use AccessibleTextField instead of raw TextField for WCAG compliance."
        )
    }

    companion object {
        private val IMPLEMENTATION = Implementation(
            TextFieldDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        val ISSUE_TEXTFIELD: Issue = Issue.create(
            id = "RawTextFieldUsage",
            briefDescription = "Use AccessibleTextField",
            explanation = "Raw TextField does not guarantee accessibility. Use AccessibleTextField instead.",
            category = Category.USABILITY,
            priority = 8,
            severity = Severity.ERROR,
            implementation = IMPLEMENTATION
        )
    }
}
