# org.apache.juli 包功能说明

## 目录

- [包概述](#包概述)
- [核心组件](#核心组件)
- [日志管理器](#日志管理器)
- [日志处理器](#日志处理器)
- [日志格式化器](#日志格式化器)
- [日志抽象层](#日志抽象层)
- [使用示例](#使用示例)

---

## 包概述

`org.apache.juli`（Java Utility Logging Implementation）是 Tomcat 的日志框架实现，它基于 Java 标准库的 `java.util.logging` (JUL) 进行了增强和扩展，提供了更适合 Web 应用服务器使用的日志功能。

### 主要特性

| 特性 | 说明 |
|------|------|
| **类加载器隔离** | 每个 Web 应用拥有独立的日志配置 |
| **日志文件轮转** | 支持按日期自动轮转日志文件 |
| **异步日志写入** | 提供异步日志处理器，减少 I/O 阻塞 |
| **多种格式化器** | 单行、紧凑、原文等多种格式 |
| **日志清理** | 自动清理超过指定天数的旧日志 |

---

## 核心组件

```
┌─────────────────────────────────────────────────────────────────┐
│                        JULI 架构图                               │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │           ClassLoaderLogManager (日志管理器)               │  │
│  │  - 每个类加载器独立的日志配置                                │  │
│  │  - 层级结构的日志记录器管理                                  │  │
│  └──────────────────────┬────────────────────────────────────┘  │
│                         │                                        │
│                         ▼                                        │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              Handler (日志处理器)                          │  │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐     │  │
│  │  │ FileHandler  │  │ AsyncFileHandler│ │ ConsoleHandler│    │  │
│  │  │ (文件处理器)  │  │ (异步处理器)   │  │ (控制台)      │    │  │
│  │  └──────────────┘  └──────────────┘  └──────────────┘     │  │
│  └──────────────────────┬────────────────────────────────────┘  │
│                         │                                        │
│                         ▼                                        │
│  ┌───────────────────────────────────────────────────────────┐  │
│  │              Formatter (格式化器)                          │  │
│  │  ┌────────────────┐ ┌──────────────┐ ┌────────────────┐   │  │
│  │  │ OneLineFormatter│ │JdkLoggerFormatter│ │VerbatimFormatter│ │  │
│  │  │ (单行格式)      │ │(紧凑格式)     │ │(原文格式)      │   │  │
│  │  └────────────────┘ └──────────────┘ └────────────────┘   │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 日志管理器

### ClassLoaderLogManager

基于类加载器的日志管理器，实现 Web 应用间的日志隔离。

```java
/**
 * 基于类加载器的 LogManager 实现。
 * 支持每个类加载器拥有独立的日志配置，实现应用程序之间的日志隔离。
 */
public class ClassLoaderLogManager extends LogManager {
    
    // 类加载器日志信息映射
    protected final Map<ClassLoader, ClassLoaderLogInfo> classLoaderLoggers;
    
    // 添加日志记录器
    public synchronized boolean addLogger(final Logger logger);
    
    // 获取日志记录器
    public synchronized Logger getLogger(final String name);
    
    // 关闭日志系统
    public synchronized void shutdown();
}
```

**关键功能**：
- **配置隔离**：每个 Web 应用使用自己的 `logging.properties`
- **层级查找**：当前类加载器找不到配置时，向父类加载器查找
- **关闭钩子**：JVM 关闭时自动刷新和关闭处理器

---

## 日志处理器

### FileHandler

文件日志处理器，支持按日期轮转。

```java
/**
 * 将日志消息追加到文件的 Handler 实现。
 * 支持按日期轮转日志文件。
 */
public class FileHandler extends Handler {
    
    private String directory = "logs";      // 日志目录
    private String prefix = "juli.";        // 文件名前缀
    private String suffix = ".log";         // 文件名后缀
    private Boolean rotatable = true;       // 是否轮转
    private Integer maxDays = -1;           // 最大保留天数
    
    // 格式化并发布日志记录
    @Override
    public void publish(LogRecord record);
}
```

**配置属性**：

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `directory` | 日志文件目录 | `logs` |
| `prefix` | 文件名前缀 | `juli.` |
| `suffix` | 文件名后缀 | `.log` |
| `rotatable` | 是否按日期轮转 | `true` |
| `maxDays` | 最大保留天数 | `-1`（永久） |
| `bufferSize` | 缓冲区大小 | `-1` |

### AsyncFileHandler

异步文件日志处理器，使用队列实现非阻塞写入。

```java
/**
 * 使用日志条目队列的 FileHandler 实现，支持异步写入。
 */
public class AsyncFileHandler extends FileHandler {
    
    // 队列溢出处理策略
    public static final int OVERFLOW_DROP_LAST = 1;     // 丢弃最新的
    public static final int OVERFLOW_DROP_FIRST = 2;    // 丢弃最早的
    public static final int OVERFLOW_DROP_FLUSH = 3;    // 等待刷新
    public static final int OVERFLOW_DROP_CURRENT = 4;  // 丢弃当前的
    
    // 发布日志记录（异步）
    @Override
    public void publish(LogRecord record);
}
```

**系统属性**：

| 属性 | 说明 | 默认值 |
|------|------|--------|
| `org.apache.juli.AsyncOverflowDropType` | 队列溢出处理类型 | `1` |
| `org.apache.juli.AsyncMaxRecordCount` | 最大队列长度 | `10000` |

---

## 日志格式化器

### OneLineFormatter

单行日志格式化器，便于 grep 搜索。

```java
/**
 * 单行日志格式化器。提供与默认日志格式相同的信息，但输出在单行上，
 * 便于使用grep搜索日志。
 */
public class OneLineFormatter extends Formatter {
    
    private static final String DEFAULT_TIME_FORMAT = "dd-MMM-yyyy HH:mm:ss.SSS";
    
    // 输出格式：时间戳 + 级别 + [线程] + 类.方法 + 消息
    // 示例：12-Jan-2024 10:30:45.123 INFO [main] org.example.MyClass.method Message
}
```

### JdkLoggerFormatter

紧凑格式的日志格式化器，类似于 log4j 风格。

```java
/**
 * 更紧凑的日志格式化器，类似于 log4j 的格式。
 */
public class JdkLoggerFormatter extends Formatter {
    
    // 输出格式：时间戳 + 级别缩写 + 日志器名 + 消息
    // 示例：1130122891846 Http11BaseProtocol I Initializing Coyote HTTP/1.1
}
```

### VerbatimFormatter

原文输出格式化器，仅输出消息内容。

```java
/**
 * 仅输出日志消息，不添加任何额外元素。
 * 适用于访问日志等需要完全控制输出格式的场景。
 */
public class VerbatimFormatter extends Formatter {
    
    @Override
    public String format(LogRecord record) {
        return record.getMessage() + System.lineSeparator();
    }
}
```

---

## 日志抽象层

### Log 接口

对标 Apache Commons Logging 的日志接口。

```java
/**
 * 一个简单的日志接口，抽象了各种日志API。
 * 支持六个日志级别（从低到高）：trace, debug, info, warn, error, fatal
 */
public interface Log {
    
    // 日志级别检查
    boolean isDebugEnabled();
    boolean isInfoEnabled();
    boolean isWarnEnabled();
    boolean isErrorEnabled();
    
    // 日志记录方法
    void debug(Object message);
    void debug(Object message, Throwable t);
    void info(Object message);
    void info(Object message, Throwable t);
    void warn(Object message);
    void warn(Object message, Throwable t);
    void error(Object message);
    void error(Object message, Throwable t);
    void fatal(Object message);
    void fatal(Object message, Throwable t);
}
```

### LogFactory

日志工厂，用于创建 Log 实例。

```java
/**
 * 这是一个修改过的 LogFactory，使用简单的 {@link ServiceLoader} 机制进行服务发现，
 * 默认使用 JDK 日志。
 */
public class LogFactory {
    
    // 获取 Log 实例（按名称）
    public static Log getLog(String name);
    
    // 获取 Log 实例（按类）
    public static Log getLog(Class<?> clazz);
    
    // 释放资源
    public static void release(ClassLoader classLoader);
}
```

### DirectJDKLog

直接使用 JDK 日志的实现类。

```java
/**
 * 硬编码的 java.util.logging commons-logging 实现。
 */
class DirectJDKLog implements Log {
    
    public final Logger logger;
    
    // 日志级别映射：
    // trace -> FINER
    // debug -> FINE
    // info  -> INFO
    // warn  -> WARNING
    // error -> SEVERE
    // fatal -> SEVERE
}
```

---

## 使用示例

### 示例 1: 配置日志文件

```properties
# logging.properties
handlers = org.apache.juli.FileHandler

# 文件处理器配置
org.apache.juli.FileHandler.directory = ${catalina.base}/logs
org.apache.juli.FileHandler.prefix = myapp.
org.apache.juli.FileHandler.suffix = .log
org.apache.juli.FileHandler.rotatable = true
org.apache.juli.FileHandler.maxDays = 30

# 格式化器
org.apache.juli.FileHandler.formatter = org.apache.juli.OneLineFormatter

# 日志级别
.level = INFO
com.mycompany.level = DEBUG
```

### 示例 2: 使用日志抽象层

```java
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;

public class MyClass {
    
    private static final Log log = LogFactory.getLog(MyClass.class);
    
    public void doSomething() {
        log.debug("开始执行 doSomething");
        
        try {
            // 执行业务逻辑
            log.info("操作成功完成");
        } catch (Exception e) {
            log.error("操作失败", e);
        }
    }
}
```

### 示例 3: 使用异步日志

```properties
# 配置异步处理器
handlers = org.apache.juli.AsyncFileHandler

# 异步处理器配置
org.apache.juli.AsyncFileHandler.directory = ${catalina.base}/logs
org.apache.juli.AsyncFileHandler.prefix = async.
org.apache.juli.AsyncFileHandler.maxDays = 7
```

### 示例 4: 程序中配置日志

```java
import org.apache.juli.ClassLoaderLogManager;
import org.apache.juli.FileHandler;
import java.util.logging.Logger;

public class LoggingConfig {
    
    public void setupLogging() {
        // 创建文件处理器
        FileHandler handler = new FileHandler(
            "/var/log/myapp",  // 目录
            "app.",            // 前缀
            ".log",            // 后缀
            30,                // 最大保留天数
            true,              // 是否轮转
            -1                 // 缓冲区大小
        );
        
        // 设置日志级别
        Logger rootLogger = Logger.getLogger("");
        rootLogger.addHandler(handler);
        rootLogger.setLevel(java.util.logging.Level.INFO);
    }
}
```

---

## 配置文件位置

Tomcat 中 JULI 的配置文件搜索顺序：

1. **`$CATALINA_BASE/conf/logging.properties`** - 全局配置
2. **`/WEB-INF/classes/logging.properties`** - Web 应用配置
3. **系统属性 `java.util.logging.config.file`** - JVM 参数指定

### 标准 logging.properties 示例

```properties
# 处理器列表
handlers = 1catalina.org.apache.juli.FileHandler, \
           2localhost.org.apache.juli.FileHandler, \
           3manager.org.apache.juli.FileHandler, \
           java.util.logging.ConsoleHandler

# 处理器配置
1catalina.org.apache.juli.FileHandler.level = FINE
1catalina.org.apache.juli.FileHandler.directory = ${catalina.base}/logs
1catalina.org.apache.juli.FileHandler.prefix = catalina.
1catalina.org.apache.juli.FileHandler.formatter = org.apache.juli.OneLineFormatter

# 日志器级别
org.apache.catalina.core.ContainerBase.[Catalina].[localhost].level = INFO
org.apache.catalina.core.ContainerBase.[Catalina].[localhost].handlers = 2localhost.org.apache.juli.FileHandler
```

---

## 参考资料

- [Tomcat Logging Documentation](https://tomcat.apache.org/tomcat-9.0-doc/logging.html)
- [Java Util Logging Guide](https://docs.oracle.com/javase/8/docs/technotes/guides/logging/overview.html)

---

*文档生成时间: 2026-04-09*
*基于 Apache Tomcat 9.0.90 源码*
