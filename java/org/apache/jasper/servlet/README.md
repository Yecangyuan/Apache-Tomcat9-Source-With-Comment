# org.apache.jasper.servlet 包

## 包功能概述

`org.apache.jasper.servlet` 包是 Apache Jasper JSP 引擎的 Servlet 层，提供了 JSP 处理的核心 Servlet 实现和辅助类。该包负责 JSP 页面的请求处理、编译管理、类加载和 TLD（Tag Library Descriptor）扫描等功能。

## 主要类和接口

### 核心 Servlet 类

| 类名 | 功能描述 |
|------|----------|
| `JspServlet` | Jasper JSP 引擎的主 Servlet，处理所有 JSP 请求，管理 JSP 运行时上下文 |
| `JspServletWrapper` | JSP 页面和标签文件的包装器，负责编译、加载和执行 JSP |
| `JasperInitializer` | Jasper JSP 引擎的初始化器，实现 `ServletContainerInitializer` 接口 |

### 类加载和资源管理

| 类名 | 功能描述 |
|------|----------|
| `JasperLoader` | 用于加载 JSP 编译后的 Servlet 类和标签处理器类的自定义类加载器 |
| `JspCServletContext` | 为 JspC（JSP 预编译器）提供的简单 `ServletContext` 实现，用于命令行预编译 |

### TLD 扫描和处理

| 类名 | 功能描述 |
|------|----------|
| `TldScanner` | 扫描 Web 应用中的 TLD（标签库描述符）文件，支持多种扫描位置 |
| `TldPreScanned` | 处理预扫描的 TLD URL 集合，用于优化启动性能 |

## 架构设计

### JSP 请求处理流程

```
[JspServlet] → [JspServletWrapper] → [编译检查] → [加载 Servlet] → [执行服务]
                    ↓
            [JspCompilationContext] → [编译 JSP] → [生成 Servlet 类]
```

### 类加载层次

```
[系统类加载器]
      ↑
[Web 应用类加载器]
      ↑
[JasperLoader] - 专门加载 org.apache.jsp 包下的类
```

### TLD 扫描顺序

按照 JSP 规范，`TldScanner` 按以下顺序扫描 TLD：

1. **平台定义的 TLD** - 由 Servlet 容器提供
2. **web.xml 中定义的 TLD** - `<jsp-config>` 中声明的标签库
3. **WEB-INF 目录下的 TLD** - 包括 `/WEB-INF/tags/implicit.tld`
4. **JAR 文件中的 TLD** - `/WEB-INF/lib` 下 JAR 的 `META-INF/*.tld`
5. **容器提供的其他 TLD**

## 关键特性

### 1. JSP 生命周期管理

`JspServletWrapper` 实现了完整的 JSP 生命周期：

- **编译阶段**：检查 JSP 是否需要重新编译（开发模式下自动检测修改）
- **加载阶段**：使用 `JasperLoader` 加载编译后的 Servlet 类
- **执行阶段**：调用 Servlet 的 `service()` 方法处理请求
- **销毁阶段**：调用 `destroy()` 方法并清理资源

### 2. 自动重新编译

在开发模式下（`development=true`）：

- 每次请求检查 JSP 文件是否被修改
- 检测到修改后自动重新编译
- 通过 `reload` 标志控制类重新加载

### 3. 错误处理

`JspServletWrapper.handleJspException()` 提供了智能错误处理：

- 使用 SMAP（Source Map）将 Java 行号映射回 JSP 行号
- 显示 JSP 源代码片段，方便调试
- 在开发模式下提供详细的错误信息

### 4. JSP 卸载策略

支持基于数量和空闲时间的 JSP 卸载：

- `maxLoadedJsps`：限制同时加载的 JSP 数量
- `jspIdleTimeout`：自动卸载长时间未使用的 JSP

### 5. 预编译支持

`JspServlet.preCompile()` 支持 JSP 预编译请求：

- 通过 `?jsp_precompile=true` 参数触发
- 仅编译不执行，用于部署时预编译

## 使用场景

### 1. Web 容器集成

在 `web.xml` 中配置 `JspServlet` 处理 JSP 请求：

```xml
<servlet>
    <servlet-name>jsp</servlet-name>
    <servlet-class>org.apache.jasper.servlet.JspServlet</servlet-class>
    <init-param>
        <param-name>development</param-name>
        <param-value>false</param-value>
    </init-param>
</servlet>

<servlet-mapping>
    <servlet-name>jsp</servlet-name>
    <url-pattern>*.jsp</url-pattern>
</servlet-mapping>
```

### 2. 程序化初始化

使用 `JasperInitializer` 在 Servlet 3.0+ 容器中自动初始化：

```java
// 在 ServletContainerInitializer 实现中
JasperInitializer initializer = new JasperInitializer();
initializer.onStartup(classes, servletContext);
```

### 3. 命令行预编译

使用 `JspCServletContext` 在命令行预编译 JSP：

```java
JspCServletContext context = new JspCServletContext(
    logWriter, baseUrl, classLoader, validate, blockExternal);
// 使用 context 进行 JSP 编译
```

## 配置参数

| 参数名 | 默认值 | 说明 |
|--------|--------|------|
| `development` | `true` | 开发模式，启用自动重新编译 |
| `checkInterval` | `0` | 检查修改的间隔时间（秒），0 表示每次请求检查 |
| `modificationTestInterval` | `4` | 修改测试最小间隔（秒） |
| `maxLoadedJsps` | `-1` | 最大加载 JSP 数，-1 表示无限制 |
| `jspIdleTimeout` | `-1` | JSP 空闲超时时间（秒），-1 表示不卸载 |
| `compiler` | - | 指定使用的 Java 编译器 |
| `compilerTargetVM` | - | 目标 JVM 版本 |
| `compilerSourceVM` | - | 源代码版本 |

## 注意事项

1. **线程安全**：`JspServletWrapper` 使用双重检查锁定（DCL）确保线程安全的 Servlet 加载
2. **内存管理**：合理配置 `maxLoadedJsps` 和 `jspIdleTimeout` 避免内存溢出
3. **生产环境**：建议关闭开发模式（`development=false`）以获得更好性能
4. **类加载**：`JasperLoader` 只允许加载 `org.apache.jsp` 包下的类，保证安全性
