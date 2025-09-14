package com.example.lint

import com.android.tools.lint.detector.api.*
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.*
import kotlin.collections.any
import kotlin.jvm.java
import kotlin.let
import kotlin.text.contains
import kotlin.text.replace
import kotlin.text.toIntOrNull
import kotlin.text.toRegex
import kotlin.text.trimIndent

@Suppress("UnstableApiUsage")
class RawComponentDetector : Detector(), SourceCodeScanner {

    override fun getApplicableMethodNames() =
        listOf("Button", "OutlinedTextField", "TextField", "Image", "Icon", "Text")

    override fun visitMethodCall(context: JavaContext, node: UCallExpression, method: PsiMethod) {
        val methodName = node.methodName ?: return

        when (methodName) {
            "Button" -> context.report(
                ISSUE_BUTTON, node, context.getNameLocation(node),
                "Use AccessibleButton instead of raw Button for WCAG compliance."
            )
            "Text" -> context.report(
                ISSUE_TEXT, node, context.getNameLocation(node),
                "Use AccessibleText instead of raw Text for WCAG compliance."
            )

            "OutlinedTextField", "TextField" -> context.report(
                ISSUE_TEXTFIELD, node, context.getNameLocation(node),
                "Use AccessibleTextField instead of raw TextField for WCAG compliance."
            )

            "Image", "Icon" -> {
                val hasContentDesc = node.valueArguments.any { arg ->
                    arg?.sourcePsi?.text?.contains("contentDescription") == true
                }

                if (!hasContentDesc) {
                    context.report(
                        ISSUE_IMAGE, node, context.getNameLocation(node),
                        "Image/Icon is missing contentDescription. Provide one or use null explicitly if decorative."
                    )
                }

                // Touch target check (common on icons)
                val sizeTooSmall = node.valueArguments.any { arg ->
                    arg?.sourcePsi?.text?.contains("dp") == true &&
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
        }
    }

    companion object {
        private val IMPLEMENTATION = Implementation(
            RawComponentDetector::class.java,
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

        val ISSUE_TEXT: Issue = Issue.create(
            id = "RawTextUsage",
            briefDescription = "Use AccessibleText",
            explanation = "Raw Text does not guarantee accessibility. Use AccessibleText instead.",
            category = Category.USABILITY,
            priority = 8,
            severity = Severity.ERROR,
            implementation = IMPLEMENTATION
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

