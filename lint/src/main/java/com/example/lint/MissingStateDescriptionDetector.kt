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
class MissingStateDescriptionDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf("Switch", "Checkbox")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val stateDescriptionArgument = node.valueArguments.find { it.parameter?.name == "stateDescription" }
        if (stateDescriptionArgument == null) {
            context.report(
                ISSUE_MISSING_STATE_DESCRIPTION,
                node,
                context.getNameLocation(node),
                "Missing stateDescription for toggleable component. This is important for accessibility."
            )
        }
    }

    companion object {
        private val IMPLEMENTATION = Implementation(
            MissingStateDescriptionDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        val ISSUE_MISSING_STATE_DESCRIPTION: Issue = Issue.create(
            id = "MissingStateDescription",
            briefDescription = "Missing stateDescription",
            explanation = "Toggleable components like Switch and Checkbox should have a `stateDescription` to be accessible. This is important for screen readers to announce the state of the component.",
            category = Category.A11Y,
            priority = 6,
            severity = Severity.WARNING,
            implementation = IMPLEMENTATION
        )
    }
}
