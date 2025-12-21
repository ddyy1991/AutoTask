# AutoTask

AutoTask是一个基于Android无障碍服务的自动化任务工具，支持布局分析、元素操作等功能。

## 功能特性

- 无障碍服务支持
- 布局层次分析
- 元素点击和操作
- 悬浮窗交互
- 树形结构可视化布局

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

## 项目结构

```
app/
├── src/
│   └── main/
│       ├── java/com/dy/autotask/
│       │   ├── AccessibilityServiceUtil.java  # 无障碍服务实现
│       │   └── utils/
│       │       ├── AutoJs6Tool.java           # 布局分析工具类
│       │       └── LayoutHierarchyTreeView.java  # 树形布局视图
│       └── res/                               # 资源文件
└── build.gradle                               # 构建配置
```

## 许可证

MIT License