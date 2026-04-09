# javax.websocket.server 包功能说明

## 目录

- [包概述](#包概述)
- [核心接口和类](#核心接口和类)
- [详细说明](#详细说明)
- [使用示例](#使用示例)
- [与其他模块的关系](#与其他模块的关系)

---

## 包概述

`javax.websocket.server` 包是 Java WebSocket API (JSR-356) 的服务器端核心包，定义了在服务器端部署和管理 WebSocket 端点所需的全部 API。

### 主要功能

| 功能 | 说明 |
|------|------|
| **端点部署** | 支持注解驱动和编程式两种端点部署方式 |
| **URI 模板** | 提供路径参数(PathParam)支持 RESTful 风格的端点映射 |
| **配置管理** | 提供灵活的服务器端点配置机制 |
| **握手控制** | 允许自定义 WebSocket 握手过程 |
| **子协议协商** | 支持 WebSocket 子协议的选择和协商 |

---

## 核心接口和类

### 1. ServerContainer (服务器容器接口)

**功能**: 提供以编程方式部署 WebSocket 端点的能力。

**核心方法**:
```java
// 通过类对象部署端点
void addEndpoint(Class<?> clazz) throws DeploymentException;

// 通过配置对象部署端点
void addEndpoint(ServerEndpointConfig sec) throws DeploymentException;
```

**使用场景**: 当需要在运行时动态注册 WebSocket 端点时使用。

---

### 2. ServerEndpoint (服务端点注解)

**功能**: 用于标注 POJO 类，将其声明为 WebSocket 服务端端点。

**属性说明**:

| 属性 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | String | 必填 | 端点映射的 URI 或 URI 模板 |
| `subprotocols` | String[] | {} | 支持的子协议列表 |
| `decoders` | Class<? extends Decoder>[] | {} | 消息解码器类列表 |
| `encoders` | Class<? extends Encoder>[] | {} | 消息编码器类列表 |
| `configurator` | Class<? extends Configurator> | Configurator.class | 端点配置器 |

**使用示例**:
```java
@ServerEndpoint(value = "/chat/{roomId}", 
                subprotocols = {"chat-protocol"},
                decoders = {MessageDecoder.class},
                encoders = {MessageEncoder.class})
public class ChatEndpoint {
    // 端点实现
}
```

---

### 3. ServerEndpointConfig (服务端点配置接口)

**功能**: 为发布到服务器的 WebSocket 端点提供配置信息。

**核心方法**:

```java
// 获取端点类
Class<?> getEndpointClass();

// 获取端点路径
String getPath();

// 获取子协议列表
List<String> getSubprotocols();

// 获取扩展列表
List<Extension> getExtensions();

// 获取配置器
Configurator getConfigurator();
```

**Builder 模式**:
```java
ServerEndpointConfig config = ServerEndpointConfig.Builder
    .create(MyEndpoint.class, "/myendpoint")
    .subprotocols(Arrays.asList("protocol1"))
    .encoders(Arrays.asList(MyEncoder.class))
    .decoders(Arrays.asList(MyDecoder.class))
    .configurator(new MyConfigurator())
    .build();
```

---

### 4. ServerEndpointConfig.Configurator (配置器类)

**功能**: 用于配置 WebSocket 端点的握手过程和端点实例创建。

**可自定义的方法**:

| 方法 | 说明 |
|------|------|
| `getNegotiatedSubprotocol()` | 协商子协议 |
| `getNegotiatedExtensions()` | 协商扩展 |
| `checkOrigin()` | 检查请求来源 |
| `modifyHandshake()` | 修改握手信息 |
| `getEndpointInstance()` | 获取端点实例(支持依赖注入) |

**使用场景**:
- 自定义端点实例创建（如与 Spring/CDI 集成）
- 实现自定义的握手验证逻辑
- 协商子协议和扩展

---

### 5. ServerApplicationConfig (应用配置接口)

**功能**: 允许应用程序过滤和控制哪些 WebSocket 端点应该被部署。

**核心方法**:
```java
// 过滤编程式端点
Set<ServerEndpointConfig> getEndpointConfigs(
    Set<Class<? extends Endpoint>> scanned);

// 过滤注解式端点  
Set<Class<?>> getAnnotatedEndpointClasses(
    Set<Class<?>> scanned);
```

**使用场景**: 当需要根据条件动态决定部署哪些端点时使用。

---

### 6. HandshakeRequest (握手请求接口)

**功能**: 表示请求升级到 WebSocket 的 HTTP 请求。

**常量定义**:
```java
String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";
String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";
String SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";
String SEC_WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";
```

**获取信息**:
```java
Map<String, List<String>> getHeaders();      // 请求头
Principal getUserPrincipal();                 // 用户身份
URI getRequestURI();                          // 请求 URI
Object getHttpSession();                      // HTTP Session
Map<String, List<String>> getParameterMap();  // 请求参数
String getQueryString();                      // 查询字符串
```

---

### 7. PathParam (路径参数注解)

**功能**: 用于从 URI 模板中提取路径参数。

**使用示例**:
```java
@ServerEndpoint("/chat/{roomId}/user/{userId}")
public class ChatEndpoint {
    
    @OnMessage
    public void onMessage(String message, 
            @PathParam("roomId") String roomId,
            @PathParam("userId") String userId) {
        // 使用 roomId 和 userId
    }
}
```

---

### 8. DefaultServerEndpointConfig (默认配置实现)

**功能**: `ServerEndpointConfig` 接口的默认实现类，用于存储端点配置信息。

**字段说明**:
```java
// WebSocket 端点类
private final Class<?> endpointClass;
// 端点路径
private final String path;
// 子协议列表
private final List<String> subprotocols;
// 扩展列表
private final List<Extension> extensions;
// 编码器类列表
private final List<Class<? extends Encoder>> encoders;
// 解码器类列表
private final List<Class<? extends Decoder>> decoders;
// 服务器端点配置器
private final Configurator serverEndpointConfigurator;
// 用户属性映射表
private final Map<String,Object> userProperties;
```

---

## 详细说明

### 端点部署方式对比

| 方式 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **注解驱动** (`@ServerEndpoint`) | 简单直观，配置集中 | 灵活性较低 | 大多数常规场景 |
| **编程式** (`ServerEndpointConfig`) | 灵活性高，可动态配置 | 代码较复杂 | 需要动态配置的场景 |

### URI 模板支持

支持 Ant 风格的路径模式：
- `/chat/{roomId}` - 提取路径参数
- `/events/*` - 通配符匹配
- `/stream/{type}/{id}` - 多路径参数

### 子协议协商流程

```
1. 客户端发送支持的子协议列表 (Sec-WebSocket-Protocol)
           │
           ▼
2. 服务器通过 Configurator.getNegotiatedSubprotocol()
   选择双方都支持的子协议
           │
           ▼
3. 返回选定的子协议给客户端
```

---

## 使用示例

### 示例 1: 基本注解式端点

```java
@ServerEndpoint("/echo")
public class EchoEndpoint {
    
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("连接已建立: " + session.getId());
    }
    
    @OnMessage
    public String onMessage(String message) {
        return "Echo: " + message;
    }
    
    @OnClose
    public void onClose(Session session) {
        System.out.println("连接已关闭: " + session.getId());
    }
}
```

### 示例 2: 带路径参数的端点

```java
@ServerEndpoint("/chat/{roomId}")
public class ChatRoomEndpoint {
    
    private static Map<String, Set<Session>> rooms = new ConcurrentHashMap<>();
    
    @OnOpen
    public void onOpen(Session session, @PathParam("roomId") String roomId) {
        session.getUserProperties().put("roomId", roomId);
        rooms.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet())
             .add(session);
    }
    
    @OnMessage
    public void onMessage(String message, Session session, 
            @PathParam("roomId") String roomId) {
        // 广播消息给房间内的所有用户
        rooms.get(roomId).forEach(s -> {
            s.getAsyncRemote().sendText(message);
        });
    }
}
```

### 示例 3: 自定义 Configurator

```java
public class SpringConfigurator extends ServerEndpointConfig.Configurator {
    
    @Override
    public <T> T getEndpointInstance(Class<T> endpointClass) 
            throws InstantiationException {
        // 从 Spring 容器中获取 Bean
        return SpringContext.getBean(endpointClass);
    }
    
    @Override
    public void modifyHandshake(ServerEndpointConfig sec, 
            HandshakeRequest request, HandshakeResponse response) {
        // 添加自定义握手逻辑
        String token = request.getParameterMap().get("token").get(0);
        // 验证 token...
    }
}

@ServerEndpoint(value = "/secured", configurator = SpringConfigurator.class)
public class SecuredEndpoint {
    // 端点实现
}
```

### 示例 4: 编程式部署

```java
public class WebSocketInitializer implements ServletContextListener {
    
    @Override
    public void contextInitialized(ServletContextEvent sce) {
        ServletContext sc = sce.getServletContext();
        ServerContainer container = (ServerContainer) sc.getAttribute(
            "javax.websocket.server.ServerContainer");
        
        try {
            // 方法 1: 直接部署类
            container.addEndpoint(MyEndpoint.class);
            
            // 方法 2: 使用配置对象
            ServerEndpointConfig config = ServerEndpointConfig.Builder
                .create(AnotherEndpoint.class, "/another")
                .build();
            container.addEndpoint(config);
            
        } catch (DeploymentException e) {
            e.printStackTrace();
        }
    }
}
```

---

## 与其他模块的关系

```
┌─────────────────────────────────────────────────────────────────┐
│                    WebSocket 模块关系图                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  javax.websocket.server (本包)                                  │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  ServerEndpoint (注解)                                   │   │
│  │  ServerEndpointConfig (配置接口)                         │   │
│  │  ServerContainer (容器接口)                              │   │
│  └─────────────────────────────────────────────────────────┘   │
│                         │                                       │
│                         │ 部署和管理                             │
│                         ▼                                       │
│  javax.websocket                                            │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  Endpoint (端点基类)                                     │   │
│  │  Session (会话)                                          │   │
│  │  MessageHandler (消息处理器)                             │   │
│  │  Encoder/Decoder (编解码器)                              │   │
│  └─────────────────────────────────────────────────────────┘   │
│                         │                                       │
│                         │ 实现                                    │
│                         ▼                                       │
│  org.apache.tomcat.websocket.server                          │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │  WsServerContainer (Tomcat 实现类)                       │   │
│  │  WsProtocolHandler (协议处理器)                          │   │
│  │  DefaultServerEndpointConfigurator (默认配置器)          │   │
│  └─────────────────────────────────────────────────────────┘   │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

### 依赖关系

| 模块 | 依赖关系 | 说明 |
|------|----------|------|
| `javax.websocket` | 本包依赖它 | 使用 Endpoint、Session、编解码器等 |
| `org.apache.tomcat.websocket.server` | 它实现本包 | Tomcat 的具体实现 |
| `javax.servlet` | 间接依赖 | 通过 HandshakeRequest 访问 HttpSession |

---

## 参考资料

- [JSR-356: Java API for WebSocket](https://www.jcp.org/en/jsr/detail?id=356)
- [Tomcat WebSocket Documentation](https://tomcat.apache.org/tomcat-9.0-doc/web-socket-howto.html)
- [RFC 6455: The WebSocket Protocol](https://tools.ietf.org/html/rfc6455)

---

*文档生成时间: 2026-04-09*
*基于 Apache Tomcat 9.0.90 源码*
