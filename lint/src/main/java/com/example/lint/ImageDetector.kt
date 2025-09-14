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

class  ImageDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() = listOf("Image", "Icon")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val hasContentDesc = node.valueArguments.any { arg ->
            arg.sourcePsi?.text?.contains("contentDescription") == true
        }

        if (!hasContentDesc) {
            context.report(
                ISSUE_IMAGE, node, context.getNameLocation(node),
                "Image/Icon is missing contentDescription. Provide one or use null explicitly if decorative."
            )
        }

        // Touch target check (common on icons)
        val sizeTooSmall = node.valueArguments.any { arg ->
            arg.sourcePsi?.text?.contains("dp") == true &&
                    arg.sourcePsi!!.text.replace("[^0-9]".toRegex(), "").toIntOrNull()?.let {
                        it < 48
                    } == true
        }

        if (sizeTooSmall) {
            context.report(
                ISSUE_TOUCHTARGET, node, context.getNameLocation(node),
                "Touch target smaller than 48dp. Increase size or add padding."
            )
        }
    }

    companion object {
        private val IMPLEMENTATION = Implementation(
            ImageDetector::class.java,
            Scope.JAVA_FILE_SCOPE
        )

        val ISSUE_IMAGE: Issue = Issue.create(
            id = "MissingImageDescription",
            briefDescription = "Image/Icon missing contentDescription",
            explanation = "Images must provide contentDescription, or set it null if decorative.",
            category = Category.USABILITY,
            priority = 7,
            severity = Severity.ERROR,
            implementation = IMPLEMENTATION
        )

        val ISSUE_TOUCHTARGET: Issue = Issue.create(
            id = "SmallTouchTarget",
            briefDescription = "Touch target too small (<48dp)",
            explanation = """
                Interactive elements must be at least 48dp in both dimensions
                (WCAG 2.1 Guideline 2.5.5).
                Use Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp).
            """.trimIndent(),
            category = Category.USABILITY,
            priority = 7,
            severity = Severity.ERROR,
            implementation = IMPLEMENTATION
        )
    }
}
