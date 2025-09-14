package com.example.lint

import com.android.tools.lint.client.api.IssueRegistry
import com.android.tools.lint.detector.api.CURRENT_API
import com.android.tools.lint.detector.api.Issue

class IssueRegistry : IssueRegistry() {
    override val api: Int = CURRENT_API
    override val minApi: Int = 8 // optional
    override val issues: List<Issue> = listOf(
        RawComponentDetector.ISSUE_TOUCHTARGET,
        RawComponentDetector.ISSUE_BUTTON,
        RawComponentDetector.ISSUE_TEXTFIELD,
        RawComponentDetector.ISSUE_IMAGE,
        RawComponentDetector.ISSUE_TEXT
    )
}