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
import org.jetbrains.uast.ULiteralExpression
import org.jetbrains.uast.getArgumentMapping

@Suppress("UnstableApiUsage")
class HardcodedTextSizeDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf("Text")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val argumentMapping = node.getArgumentMapping()
        val fontSizeArgument = node.valueArguments.find {
            argumentMapping[it]?.name == "fontSize"
        }
        if (fontSizeArgument is ULiteralExpression) {
            context.report(
                ISSUE_HARDCODED_TEXT_SIZE,
                fontSizeArgument,
                context.getLocation(fontSizeArgument),
                "Avoid using hardcoded text sizes. Use theme typography instead."
            )
        }
    }

    companion object {
        private val IMPLEMENTATION = Implementation(
            HardcodedTextSizeDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        val ISSUE_HARDCODED_TEXT_SIZE: Issue = Issue.create(
            id = "HardcodedTextSize",
            briefDescription = "Hardcoded text size",
            explanation = "Hardcoding text sizes can lead to inconsistent UI and accessibility issues. It's recommended to use typography styles from the theme.",
            category = Category.USABILITY,
            priority = 5,
            severity = Severity.WARNING,
            implementation = IMPLEMENTATION
        )
    }
}
