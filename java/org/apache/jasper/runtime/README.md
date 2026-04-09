# org.apache.jasper.runtime 包

## 包功能概述

`org.apache.jasper.runtime` 包是 Apache Jasper JSP 引擎的核心运行时包，提供了 JSP 页面执行所需的基础类和工具。该包实现了 JSP 规范中定义的关键接口，为 JSP 页面的编译后执行提供运行时支持。

## 主要类和接口

### 核心类

| 类名 | 功能描述 |
|------|----------|
| `HttpJspBase` | 所有编译后 JSP Servlet 的基类，实现了 `HttpJspPage` 接口 |
| `PageContextImpl` | `PageContext` 接口的实现，管理 JSP 页面的作用域和 EL 变量解析 |
| `JspFactoryImpl` | `JspFactory` 的实现，使用对象池管理 `PageContext` 实例 |
| `JspApplicationContextImpl` | `JspApplicationContext` 的实现，管理 EL 表达式上下文 |

### JSP 输出相关

| 类名 | 功能描述 |
|------|----------|
| `JspWriterImpl` | `JspWriter` 的默认实现，提供带缓冲的字符输出功能 |
| `BodyContentImpl` | `BodyContent` 的实现，用于处理 JSP 标签体内容 |

### 标签处理相关

| 类名 | 功能描述 |
|------|----------|
| `TagHandlerPool` | 标签处理器池，用于重用标签处理器实例以提高性能 |
| `JspContextWrapper` | JSP 上下文的包装类，用于标签文件的处理 |
| `JspFragmentHelper` | JSP 片段辅助类，支持 JSP 片段的执行 |

### 工具类

| 类名 | 功能描述 |
|------|----------|
| `JspRuntimeLibrary` | JSP 运行时库，提供类型转换、属性访问等实用方法 |
| `JspSourceImports` | JSP 源文件导入信息的管理 |
| `ProtectedFunctionMapper` | 保护 EL 函数映射器，用于访问受限的 EL 函数 |

## 架构设计

### 对象池模式

`JspFactoryImpl` 使用线程本地存储（ThreadLocal）实现 `PageContext` 对象池：

```
[ThreadLocal<PageContextPool>] → [PageContext数组] → [PageContext实例]
```

这种设计避免了频繁创建和销毁 `PageContext` 对象的开销。

### 缓冲机制

`JspWriterImpl` 实现了字符缓冲机制：

- **自动刷新模式**：缓冲区满时自动刷新到输出流
- **手动刷新模式**：缓冲区满时抛出异常，需要显式调用 `flush()`
- **直接写入**：`writeOut()` 方法绕过缓冲区直接写入底层流

### 标签处理器生命周期

`TagHandlerPool` 管理标签处理器的生命周期：

```
获取处理器 → 初始化 → 执行 → 释放 → 返回池中/销毁
```

## 关键特性

1. **安全性支持**：支持 Java 安全管理器，关键操作在特权模式下执行
2. **EL 表达式支持**：提供 EL 上下文、解析器和监听器管理
3. **多作用域管理**：`PageContextImpl` 统一管理 page、request、session、application 四个作用域
4. **属性访问**：提供 JavaBean 属性的读写支持，包括嵌套属性访问

## 使用场景

该包主要在以下场景中使用：

- JSP 页面编译后生成的 Servlet 执行
- 自定义 JSP 标签的实现和执行
- EL 表达式的求值
- JSP 片段（Fragment）的处理

## 注意事项

- `HttpJspBase` 是自动生成的 JSP Servlet 的父类，不应该直接实例化
- `JspWriterImpl` 的缓冲区大小在 `PageContext` 初始化时确定
- 标签处理器池的大小可通过系统属性配置
