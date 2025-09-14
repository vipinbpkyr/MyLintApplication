package com.example.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class IssueRegistry : IssueRegistry() {
    override val api: Int = CURRENT_API
    override val minApi: Int = 8 // optional
    override val issues: List<Issue> = listOf(
        ImageDetector.ISSUE_TOUCHTARGET,
        ButtonDetector.ISSUE_BUTTON,
        TextFieldDetector.ISSUE_TEXTFIELD,
        ImageDetector.ISSUE_IMAGE,
        TextDetector.ISSUE_TEXT,
        HardcodedTextSizeDetector.ISSUE_HARDCODED_TEXT_SIZE,
        ClickableElementSemanticsDetector.ISSUE_CLICKABLE_ELEMENT_SEMANTICS,
        MissingStateDescriptionDetector.ISSUE_MISSING_STATE_DESCRIPTION
    )
}