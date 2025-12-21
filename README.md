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
```

## 许可证

MIT License