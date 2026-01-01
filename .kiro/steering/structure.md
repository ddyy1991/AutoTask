# Project Structure

## Directory Organization

```
AutoTask/
├── app/                                    # Main application module
│   ├── src/main/
│   │   ├── java/com/dy/autotask/
│   │   │   ├── MainActivity.java           # Main entry point, permission management
│   │   │   ├── ImageAnalysisActivity.java  # Image analysis UI
│   │   │   ├── AccessibilityServiceUtil.java # Accessibility service implementation
│   │   │   │
│   │   │   ├── adapter/                    # UI adapters
│   │   │   │   ├── LayoutHierarchyAdapter.java
│   │   │   │   └── FlatLayoutHierarchyAdapter.java
│   │   │   │
│   │   │   ├── model/                      # Data models
│   │   │   │   ├── AnalysisHistory.java
│   │   │   │   ├── ImageAnalysisRequest.java
│   │   │   │   ├── ImageAnalysisResponse.java
│   │   │   │   └── NodeInfo.java
│   │   │   │
│   │   │   ├── task/                       # Task automation core
│   │   │   │   ├── AutomationTask.java     # Single task definition & execution
│   │   │   │   ├── AutomationTaskManager.java # Task queue & lifecycle management
│   │   │   │   ├── TaskLogFileWriter.java  # Persistent task logging
│   │   │   │   └── TaskLogFloatingWindow.java # Real-time log display overlay
│   │   │   │
│   │   │   ├── ui/                         # UI components
│   │   │   │   ├── imageanalysis/
│   │   │   │   │   └── ImageAnalysisViewModel.java
│   │   │   │   ├── layoutinspector/        # Layout debugging tools
│   │   │   │   ├── multilevel/             # Hierarchical list UI
│   │   │   │   │   ├── ItemInfo.java
│   │   │   │   │   ├── MultiLevelListAdapter.java
│   │   │   │   │   └── MultiLevelListView.java
│   │   │   │   ├── overlay/                # Screen overlays
│   │   │   │   │   └── HighlightOverlayView.java
│   │   │   │   └── widget/                 # Custom widgets
│   │   │   │       └── LevelBeamView.java
│   │   │   │
│   │   │   └── utils/                      # Utility classes
│   │   │       ├── AnalysisCallback.java   # Callback interfaces
│   │   │       ├── AutoJsTool.java         # JavaScript automation
│   │   │       ├── AutoTaskHelper.java     # Accessibility service helper
│   │   │       ├── Base64Util.java         # Encoding utilities
│   │   │       ├── GLMImageAnalysisTool.java # GLM API integration
│   │   │       ├── ImageCompressUtil.java  # Image compression
│   │   │       ├── LayoutHierarchyTreeView.java # UI tree visualization
│   │   │       ├── RootUtil.java           # Root permission handling
│   │   │       ├── ScreenshotUtil.java     # Screenshot capture
│   │   │       └── SettingsManager.java    # App preferences
│   │   │
│   │   ├── res/
│   │   │   ├── drawable/                   # Vector drawables & shapes
│   │   │   ├── layout/                     # XML layout files
│   │   │   ├── mipmap-*/                   # App icons (multiple densities)
│   │   │   ├── values/                     # Strings, colors, dimensions, themes
│   │   │   └── xml/                        # Accessibility & backup configs
│   │   │
│   │   └── AndroidManifest.xml             # App manifest with permissions & services
│   │
│   ├── src/test/                           # Unit tests
│   ├── src/androidTest/                    # Instrumented tests
│   ├── build.gradle                        # App module build config
│   └── proguard-rules.pro                  # ProGuard obfuscation rules
│
├── gradle/                                 # Gradle wrapper & configuration
│   ├── wrapper/
│   │   ├── gradle-wrapper.jar
│   │   └── gradle-wrapper.properties
│   └── libs.versions.toml                  # Centralized dependency versions
│
├── build.gradle                            # Root project build config
├── settings.gradle                         # Project structure definition
├── gradle.properties                       # Gradle properties
├── gradlew & gradlew.bat                   # Gradle wrapper scripts
├── local.properties                        # Local SDK path (not in VCS)
└── .gitignore                              # Git ignore rules
```

## Key Architectural Patterns

### Task Automation Flow
1. **AutomationTask**: Defines a sequence of actions (click, input, wait, find)
2. **AutomationTaskManager**: Manages task queue and execution lifecycle
3. **AccessibilityServiceUtil**: Executes actions via accessibility framework
4. **TaskLogFloatingWindow**: Displays real-time execution logs

### Utility Organization
- **Service Helpers**: AutoTaskHelper, RootUtil, ScreenshotUtil
- **API Integration**: GLMImageAnalysisTool for AI analysis
- **Data Persistence**: SettingsManager for preferences, TaskLogFileWriter for logs
- **UI Utilities**: Image compression, layout inspection, tree visualization

### Resource Organization
- **Drawables**: Custom shapes for buttons and UI elements
- **Values**: Centralized strings, colors, dimensions, and themes
- **XML Configs**: Accessibility service configuration, backup rules

## Naming Conventions
- **Classes**: PascalCase (e.g., AutomationTask, GLMImageAnalysisTool)
- **Methods**: camelCase (e.g., executeTask, addLog)
- **Constants**: UPPER_SNAKE_CASE
- **Packages**: Reverse domain notation (com.dy.autotask.*)
- **Resources**: snake_case (e.g., activity_main, ic_launcher)

## Module Dependencies
- **app**: Single-module project, all code in one module
- **No library modules**: All functionality integrated into main app module
