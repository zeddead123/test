MIDNIGHT ANDROID - GITHUB UPLOAD

1. Extract this ZIP fully.
2. Run PREPARE_GITHUB.bat once.
3. Run CHECK_GITHUB_STRUCTURE.bat. It should show OK for all items.
4. Upload the CONTENTS of this folder to the root of your GitHub repository.

Important: .github is a dot-folder. If your file manager does not show it, PREPARE_GITHUB.bat opens it automatically after creation.

The visible file BUILD_APK_WORKFLOW.yml is a backup copy of the GitHub Actions workflow. The real workflow must be located at:
.github/workflows/build-apk.yml
