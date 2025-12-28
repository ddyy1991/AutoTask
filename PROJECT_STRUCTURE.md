# AutoTask 项目结构

## 项目概述
Android 自动化任务框架，提供无障碍服务支持的UI自动化功能。

## 核心模块

### 1. 任务管理 (`task/`)
- `AutomationTask.java` - 自动化任务基类
- `AutomationTaskManager.java` - 任务管理器，负责任务调度和执行
- `TaskLogFileWriter.java` - 任务日志文件写入
- `TaskLogFloatingWindow.java` - 任务日志悬浮窗显示

### 2. UI组件 (`ui/`)

#### 布局检查器 (`ui/layoutinspector/`)
- `LayoutHierarchyView.java` - 布局层级视图

#### 多级列表 (`ui/multilevel/`)
- `ItemInfo.java` - 列表项信息模型
- `MultiLevelListAdapter.java` - 多级列表适配器
- `MultiLevelListView.java` - 多级列表视图

#### 覆盖层 (`ui/overlay/`)
- `HighlightOverlayView.java` - 高亮覆盖视图

#### 自定义控件 (`ui/widget/`)
- `LevelBeamView.java` - 层级连接线视图

### 3. 适配器 (`adapter/`)
- `FlatLayoutHierarchyAdapter.java` - 扁平化布局层级适配器
- `LayoutHierarchyAdapter.java` - 布局层级适配器

### 4. 工具类 (`utils/`)
- `AutoJsTool.java` - AutoJs工具类
- `AutoTaskHelper.java` - 自动化任务辅助工具
- `LayoutHierarchyTreeView.java` - 布局层级树视图
- `SettingsManager.java` - 设置管理器

### 5. 模型 (`model/`)
- `NodeInfo.java` - 节点信息模型

### 6. 核心类
- `MainActivity.java` - 主Activity
- `AccessibilityServiceUtil.java` - 无障碍服务工具类

## 项目架构

```
AutoTask/
├── app/src/main/java/com/dy/autotask/
│   ├── task/              # 任务管理核心
│   ├── ui/                # UI组件
│   │   ├── layoutinspector/
│   │   ├── multilevel/
│   │   ├── overlay/
│   │   └── widget/
│   ├── adapter/           # 适配器
│   ├── utils/             # 工具类
│   ├── model/             # 数据模型
│   ├── MainActivity.java
│   └── AccessibilityServiceUtil.java
└── .gradle/               # Gradle构建文件
```
