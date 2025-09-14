# MyLintApplication 🕵️‍♂️

Custom **Android Lint Rules** project for enforcing **accessibility, UI, and security best practices** in Android applications.

## 📌 Overview

This repository demonstrates how to create and use **custom lint checks** in an Android project.  
It consists of two modules:

- **`app/`** → Sample Android application (Jetpack Compose) that consumes and validates the lint rules.
- **`lint/`** → Library module defining custom lint rules (detectors + issue registry).

The lint rules currently cover accessibility-focused checks such as:

- ✅ Touch target size validation  
- ✅ Button usage & accessibility labels  
- ✅ Text field best practices  

## 📂 Project Structure

```
MyLintApplication/
 ├── app/                # Sample Android app using the lint rules
 │   ├── src/main/java    # Compose demo
 │   └── lint-baseline.xml
 ├── lint/               # Custom lint rule definitions
 │   ├── IssueRegistry.kt
 │   └── RawComponentDetector.kt
 ├── build.gradle.kts
 ├── settings.gradle.kts
 └── README.md
```

## ⚙️ How It Works

1. **`RawComponentDetector`** → Implements `Detector, SourceCodeScanner`  
   Defines multiple issues (touch target, button, text field, etc.).

2. **`IssueRegistry`** → Registers all issues for Android Lint to discover.

3. **Integration** → The `app/` module automatically picks up the custom lint rules when building.

## 🚀 Running Lint

Run lint checks from the command line:

```bash
./gradlew lintDebug
```

Results are generated as HTML/XML reports inside:

```
app/build/reports/lint-results.html
```

## 🧪 Testing Custom Rules (Recommended)

Add a `lint-tests/` module and use `LintDetectorTest` to verify each rule:

```kotlin
class ButtonDetectorTest : LintDetectorTest() {
    override fun getDetector() = ButtonDetector()
    override fun getIssues() = listOf(ButtonDetector.ISSUE)

    @Test
    fun testInvalidButton() {
        lint().files(
            kotlin("... invalid code ...")
        ).run().expectErrorCount(1)
    }
}
```

## 📦 Publishing (Optional)

To share the lint rules across multiple projects:

- Configure `maven-publish` in `lint/build.gradle.kts`.
- Publish the artifact to your internal Maven repo / GitHub Packages.

## 🔮 Roadmap

- Split `RawComponentDetector` into multiple detectors for readability.  
- Add **Compose-specific checks** (e.g., `Modifier.semantics`, `contentDescription`).  
- Add **security checks** (SSL pinning, sensitive logging prevention).  
- Provide **lint-tests** for each detector.  

## 📜 License

This project is licensed under the [MIT License](LICENSE).

---

👨‍💻 Created by [Vipin B P](https://github.com/vipinbpkyr) — exploring Android **lint automation**, **Compose migration**, and **accessibility compliance**.
