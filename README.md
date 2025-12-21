# AutoTask

AutoTask是一个基于Android无障碍服务的自动化任务工具，支持布局分析、元素操作和任务自动化执行等功能。

## 功能特性

- 无障碍服务支持
- 布局层次分析
- 元素点击和操作
- 悬浮窗交互
- 树形结构可视化布局
- 任务自动化执行
- 链式调用API
- 任务队列管理
- 系统按键操作（菜单键、Home键、返回键、电源键）
- 任务执行日志悬浮窗
- 任务失败/超时/取消Toast提示
- 固定在左上角且不接收触摸事件的任务日志悬浮窗
- 全局配置选项控制日志悬浮窗显示
- 蓝色悬浮按钮中的子按钮用于快速切换日志悬浮窗显示状态

## 开发环境

- Android Studio
- Java 17
- Gradle 8.7

## 安装使用

1. 克隆仓库
   ```
   git clone https://github.com/ddyy1991/AutoTask.git
   ```

2. 打开Android Studio，导入项目
3. 构建并运行应用
4. 启用无障碍服务权限
5. 开始使用自动化功能

## 主要功能

### 布局层次分析
- 支持全屏悬浮窗显示布局层次
- 树形结构可视化布局
- 节点详情查看
- 点击交互支持

### 元素操作
- 元素点击
- 元素信息查看
- 骨架图绘制

### 任务自动化
- 任务队列管理
- 链式调用API
- 任务状态监控
- 超时控制

## 项目结构

```
app/
├── src/
│   └── main/
│       ├── java/com/dy/autotask/
│       │   ├── AccessibilityServiceUtil.java     # 无障碍服务实现
│       │   ├── task/                            # 任务系统
│       │   │   ├── AutomationTaskManager.java    # 任务管理器
│       │   │   ├── AutomationTask.java          # 任务对象
│       │   │   └── TaskUsageExample.java        # 使用示例
│       │   └── utils/
│       │       ├── AutoJs6Tool.java              # 布局分析工具类
│       │       └── LayoutHierarchyTreeView.java  # 树形布局视图
│       └── res/                                 # 资源文件
└── build.gradle                               # 构建配置
```

## 使用说明

### 任务系统使用

任务系统提供了链式调用API，可以轻松创建和执行自动化任务：

```java
// 创建任务管理器
AutomationTaskManager taskManager = AutomationTaskManager.getInstance();

// 设置上下文以启用Toast提示和日志悬浮窗
// 注意：需要传入有效的Context对象
// taskManager.setContext(context);

// 启用任务日志悬浮窗（默认启用）
taskManager.setLogWindowEnabled(true);

// 创建自动化任务
AutomationTask task = new AutomationTask("登录任务")
        .click("login_button")                           // 点击登录按钮
        .waitFor(1000)                                   // 等待1秒
        .findElement("username_input")                   // 查找用户名输入框
        .inputText("username_input", "test_user")       // 输入用户名
        .findElement("password_input")                   // 查找密码输入框
        .inputText("password_input", "test_password")   // 输入密码
        .click("submit_button")                          // 点击提交按钮
        .setTimeout(30000)                               // 设置超时时间为30秒
        .onResult((taskObj, status, message) -> {         // 设置结果回调
            // 处理任务执行结果
        });

// 创建包含系统按键操作的任务
AutomationTask keyTask = new AutomationTask("系统操作任务")
        .click("某个按钮")                                // 点击某个按钮
        .waitFor(1000)                                   // 等待1秒
        .pressMenu()                                     // 点击菜单键
        .waitFor(1000)                                   // 等待1秒
        .pressBack()                                     // 点击返回键
        .waitFor(1000)                                   // 等待1秒
        .pressHome()                                     // 点击Home键
        .onResult((taskObj, status, message) -> {         // 设置结果回调
            // 处理任务执行结果
        });

// 添加任务到队列并执行
taskManager.addTask(task);
taskManager.executeTasks();
```

### 任务管理操作

```java
// 暂停当前任务（将当前任务移到队列末尾）
taskManager.pauseCurrentTask();

// 停止并移除当前任务
taskManager.stopAndRemoveCurrentTask();

// 停止并清空所有任务
taskManager.stopAndClearAllTasks();

// 控制任务日志悬浮窗显示/隐藏
taskManager.showLogWindow();  // 显示日志悬浮窗
taskManager.hideLogWindow();  // 隐藏日志悬浮窗

// 控制是否启用任务日志悬浮窗
taskManager.setLogWindowEnabled(true);   // 启用日志悬浮窗
taskManager.setLogWindowEnabled(false);  // 禁用日志悬浮窗
```

## 任务执行日志和错误提示

任务系统提供了详细的任务执行日志和错误提示功能：

1. **任务日志悬浮窗**：任务执行过程中会在屏幕上显示一个固定在左上角的小型悬浮窗，实时显示任务执行的各种状态和步骤。该悬浮窗不会接收触摸事件，避免影响自动化任务的执行。
2. **Toast提示**：当任务执行失败、超时或被取消时，会自动显示Toast提示，告知用户具体的错误信息。
3. **日志记录**：所有任务操作都会被记录在日志悬浮窗中，方便调试和问题排查。

用户可以通过以下方式控制日志功能：

```java
// 启用/禁用日志悬浮窗
taskManager.setLogWindowEnabled(true);   // 启用（默认）
taskManager.setLogWindowEnabled(false);  // 禁用

// 手动显示/隐藏日志悬浮窗
taskManager.showLogWindow();  // 显示
taskManager.hideLogWindow();  // 隐藏
```

此外，用户还可以通过主界面的设置选项全局控制是否启用任务日志悬浮窗，或者通过点击蓝色悬浮按钮中的"日志"子按钮来快速切换日志悬浮窗的显示状态。

## 许可证

MIT License