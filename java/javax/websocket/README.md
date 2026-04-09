# javax.websocket 包功能说明

## 目录

- [包概述](#包概述)
- [核心组件](#核心组件)
- [API 分类](#api-分类)
- [使用示例](#使用示例)
- [生命周期说明](#生命周期说明)
- [与服务器端 API 的关系](#与服务器端-api-的关系)

---

## 包概述

`javax.websocket` 包是 Java WebSocket API (JSR-356) 的核心包，提供了构建 WebSocket 客户端和服务器端应用的基础接口和类。该包定义了 WebSocket 通信的完整生命周期管理、消息处理、连接配置等核心功能。

### 主要功能

| 功能 | 说明 |
|------|------|
| **连接管理** | 建立、维护和关闭 WebSocket 连接 |
| **消息处理** | 支持文本、二进制、Ping/Pong 等多种消息类型 |
| **编解码** | 提供自定义消息编解码机制 |
| **生命周期** | 定义连接打开、消息接收、错误处理、连接关闭等回调 |
| **配置管理** | 支持端点配置、子协议协商、扩展协商 |

---

## 核心组件

### 1. Endpoint (端点)

`Endpoint` 是 WebSocket 端点的抽象基类，定义了连接生命周期的回调方法。

```java
/**
 * WebSocket端点的基类，定义了WebSocket连接生命周期中的回调方法。
 * 开发者需要继承此类并实现onOpen方法来处理新的WebSocket连接。
 */
public abstract class Endpoint {
    // 连接建立时调用
    public abstract void onOpen(Session session, EndpointConfig config);
    
    // 连接关闭时调用
    public void onClose(Session session, CloseReason closeReason) { }
    
    // 发生错误时调用
    public void onError(Session session, Throwable throwable) { }
}
```

**使用方式**：
- **编程式端点**：继承 `Endpoint` 类，重写生命周期方法
- **注解式端点**：使用 `@ClientEndpoint` 或 `@ServerEndpoint` 注解

---

### 2. Session (会话)

`Session` 代表一个 WebSocket 连接会话，提供与远程端点通信的方法。

```java
public interface Session extends Closeable {
    // 获取远程端点（同步）
    RemoteEndpoint.Basic getBasicRemote();
    
    // 获取远程端点（异步）
    RemoteEndpoint.Async getAsyncRemote();
    
    // 注册消息处理器
    void addMessageHandler(MessageHandler handler);
    
    // 检查连接状态
    boolean isOpen();
    
    // 关闭连接
    void close(CloseReason closeReason);
}
```

**关键功能**：
- 发送消息（同步/异步）
- 注册消息处理器
- 管理会话属性
- 获取连接信息

---

### 3. 消息处理注解

| 注解 | 说明 | 使用位置 |
|------|------|----------|
| `@OnOpen` | 连接建立时调用 | 方法 |
| `@OnMessage` | 收到消息时调用 | 方法 |
| `@OnClose` | 连接关闭时调用 | 方法 |
| `@OnError` | 发生错误时调用 | 方法 |

**示例**：
```java
@ClientEndpoint
public class MyClient {
    
    @OnOpen
    public void onOpen(Session session) {
        System.out.println("连接已建立");
    }
    
    @OnMessage
    public void onMessage(String message) {
        System.out.println("收到消息: " + message);
    }
    
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("连接已关闭: " + reason);
    }
    
    @OnError
    public void onError(Session session, Throwable error) {
        System.err.println("发生错误: " + error);
    }
}
```

---

### 4. 编解码器 (Encoder/Decoder)

#### Encoder (编码器)

将 Java 对象编码为 WebSocket 消息：

```java
public interface Encoder {
    // 文本编码器
    interface Text<T> extends Encoder {
        String encode(T object) throws EncodeException;
    }
    
    // 二进制编码器
    interface Binary<T> extends Encoder {
        ByteBuffer encode(T object) throws EncodeException;
    }
}
```

#### Decoder (解码器)

将 WebSocket 消息解码为 Java 对象：

```java
public interface Decoder {
    // 文本解码器
    interface Text<T> extends Decoder {
        T decode(String s) throws DecodeException;
        boolean willDecode(String s);
    }
    
    // 二进制解码器
    interface Binary<T> extends Decoder {
        T decode(ByteBuffer bytes) throws DecodeException;
        boolean willDecode(ByteBuffer bytes);
    }
}
```

---

### 5. RemoteEndpoint (远程端点)

用于向远程端点发送消息，分为同步和异步两种方式。

#### 同步发送 (Basic)

```java
public interface Basic extends RemoteEndpoint {
    void sendText(String text) throws IOException;
    void sendBinary(ByteBuffer data) throws IOException;
    void sendObject(Object data) throws IOException, EncodeException;
}
```

#### 异步发送 (Async)

```java
public interface Async extends RemoteEndpoint {
    Future<Void> sendText(String text);
    void sendText(String text, SendHandler completion);
    Future<Void> sendBinary(ByteBuffer data);
    void sendObject(Object obj, SendHandler completion);
}
```

---

### 6. WebSocketContainer (容器)

`WebSocketContainer` 是 WebSocket 容器的核心接口，用于：

- 连接到 WebSocket 服务器（客户端）
- 配置默认超时时间和缓冲区大小
- 获取已安装的扩展

```java
public interface WebSocketContainer {
    // 连接到服务器（客户端）
    Session connectToServer(Object endpoint, URI path) 
        throws DeploymentException, IOException;
    
    // 配置超时
    void setDefaultMaxSessionIdleTimeout(long timeout);
    long getDefaultMaxSessionIdleTimeout();
    
    // 配置缓冲区
    void setDefaultMaxTextMessageBufferSize(int max);
    void setDefaultMaxBinaryMessageBufferSize(int max);
}
```

---

## API 分类

### 客户端 API

| 类/接口 | 说明 |
|---------|------|
| `@ClientEndpoint` | 标识客户端端点 |
| `ClientEndpointConfig` | 客户端端点配置 |
| `ContainerProvider` | 获取 WebSocket 容器 |
| `WebSocketContainer` | 客户端容器，用于连接服务器 |

### 服务器端 API

| 类/接口 | 说明 |
|---------|------|
| `@ServerEndpoint` | 标识服务器端点 (在 server 子包中) |
| `ServerEndpointConfig` | 服务器端点配置 (在 server 子包中) |
| `ServerContainer` | 服务器容器 (在 server 子包中) |

### 通用 API

| 类/接口 | 说明 |
|---------|------|
| `Endpoint` | 端点基类 |
| `Session` | 会话管理 |
| `RemoteEndpoint` | 远程端点通信 |
| `MessageHandler` | 消息处理 |
| `Encoder/Decoder` | 编解码器 |
| `CloseReason` | 关闭原因 |

---

## 使用示例

### 示例 1: 简单客户端

```java
import javax.websocket.*;
import java.net.URI;

@ClientEndpoint
public class EchoClient {
    
    @OnOpen
    public void onOpen(Session session) {
        try {
            session.getBasicRemote().sendText("Hello WebSocket!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @OnMessage
    public void onMessage(String message) {
        System.out.println("收到: " + message);
    }
    
    public static void main(String[] args) throws Exception {
        WebSocketContainer container = ContainerProvider.getWebSocketContainer();
        Session session = container.connectToServer(
            EchoClient.class, 
            new URI("ws://localhost:8080/echo")
        );
        
        // 等待...
        Thread.sleep(10000);
        session.close();
    }
}
```

### 示例 2: 自定义编解码器

```java
// 消息对象
public class ChatMessage {
    private String from;
    private String content;
    // getters/setters
}

// 编码器
public class ChatMessageEncoder implements Encoder.Text<ChatMessage> {
    @Override
    public String encode(ChatMessage message) throws EncodeException {
        return message.getFrom() + ":" + message.getContent();
    }
    
    @Override
    public void init(EndpointConfig config) { }
    
    @Override
    public void destroy() { }
}

// 解码器
public class ChatMessageDecoder implements Decoder.Text<ChatMessage> {
    @Override
    public ChatMessage decode(String s) throws DecodeException {
        String[] parts = s.split(":", 2);
        ChatMessage msg = new ChatMessage();
        msg.setFrom(parts[0]);
        msg.setContent(parts[1]);
        return msg;
    }
    
    @Override
    public boolean willDecode(String s) {
        return s.contains(":");
    }
    
    @Override
    public void init(EndpointConfig config) { }
    
    @Override
    public void destroy() { }
}

// 使用
@ClientEndpoint(
    encoders = {ChatMessageEncoder.class},
    decoders = {ChatMessageDecoder.class}
)
public class ChatClient {
    @OnMessage
    public void onMessage(ChatMessage message) {
        System.out.println(message.getFrom() + " 说: " + message.getContent());
    }
}
```

### 示例 3: 异步消息发送

```java
@ClientEndpoint
public class AsyncClient {
    
    @OnOpen
    public void onOpen(Session session) {
        RemoteEndpoint.Async async = session.getAsyncRemote();
        
        // 方式 1: 使用 Future
        Future<Void> future = async.sendText("Hello");
        try {
            future.get(); // 等待发送完成
        } catch (Exception e) {
            e.printStackTrace();
        }
        
        // 方式 2: 使用回调
        async.sendText("World", new SendHandler() {
            @Override
            public void onResult(SendResult result) {
                if (result.isOK()) {
                    System.out.println("发送成功");
                } else {
                    System.err.println("发送失败: " + result.getException());
                }
            }
        });
    }
}
```

### 示例 4: 处理分片消息

```java
@ClientEndpoint
public class StreamingClient {
    
    // 处理完整消息
    @OnMessage
    public void onTextMessage(String message) {
        System.out.println("收到完整消息: " + message);
    }
    
    // 处理分片消息
    @OnMessage
    public void onPartialMessage(String partial, boolean last) {
        System.out.println("收到片段: " + partial + ", 是否最后: " + last);
    }
    
    // 处理二进制消息
    @OnMessage
    public void onBinaryMessage(ByteBuffer data) {
        // 处理二进制数据
    }
}
```

---

## 生命周期说明

```
┌─────────────────────────────────────────────────────────────┐
│                     WebSocket 生命周期                        │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  1. 创建 Endpoint 实例                                        │
│        │                                                     │
│        ▼                                                     │
│  2. 调用 @OnOpen 方法                                         │
│        │  ┌──────────────────────────────────────┐          │
│        │  │ 可执行操作:                           │          │
│        │  │ - 保存 Session 对象                   │          │
│        │  │ - 注册 MessageHandler                 │          │
│        │  │ - 发送初始消息                        │          │
│        │  └──────────────────────────────────────┘          │
│        │                                                     │
│        ▼                                                     │
│  3. 连接打开状态 ◄───── @OnMessage 接收/发送消息 ────►       │
│        │                                                     │
│        │  ┌──────────────────────────────────────┐          │
│        │  │ 可接收消息:                           │          │
│        │  │ - 文本消息 (String)                   │          │
│        │  │ - 二进制消息 (ByteBuffer)             │          │
│        │  │ - Pong 消息 (PongMessage)             │          │
│        │  │ - Ping 消息 (自动处理)                │          │
│        │  └──────────────────────────────────────┘          │
│        │                                                     │
│        ▼                                                     │
│  4. 连接关闭                                                  │
│        │                                                     │
│        ├── 正常关闭 ──► @OnClose 方法                         │
│        │                                                     │
│        └── 异常关闭 ──► @OnError 方法 ──► @OnClose 方法      │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### 异常处理

```java
@ClientEndpoint
public class RobustClient {
    
    @OnError
    public void onError(Session session, Throwable error) {
        if (error instanceof DecodeException) {
            System.err.println("消息解码失败: " + error.getMessage());
        } else if (error instanceof IOException) {
            System.err.println("I/O 错误: " + error.getMessage());
        } else {
            System.err.println("未知错误: " + error);
        }
    }
    
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        CloseReason.CloseCode code = reason.getCloseCode();
        String phrase = reason.getReasonPhrase();
        
        switch (code.getCode()) {
            case 1000:
                System.out.println("正常关闭");
                break;
            case 1001:
                System.out.println("服务端关闭");
                break;
            case 1006:
                System.out.println("异常关闭");
                break;
            default:
                System.out.println("关闭代码: " + code + ", 原因: " + phrase);
        }
    }
}
```

---

## 与服务器端 API 的关系

```
┌─────────────────────────────────────────────────────────────────────┐
│                      Java WebSocket API 架构                         │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  javax.websocket (本包)                                              │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ 核心接口:                                                    │   │
│  │ - Endpoint, Session, RemoteEndpoint                         │   │
│  │ - Encoder, Decoder                                          │   │
│  │ - MessageHandler                                            │   │
│  │ - WebSocketContainer (客户端)                               │   │
│  │                                                             │   │
│  │ 注解:                                                       │   │
│  │ - @ClientEndpoint                                           │   │
│  │ - @OnOpen, @OnMessage, @OnClose, @OnError                   │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                              │                                       │
│                              │ 扩展                                    │
│                              ▼                                       │
│  javax.websocket.server                                              │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │ 服务器端扩展:                                                │   │
│  │ - @ServerEndpoint                                           │   │
│  │ - ServerEndpointConfig                                      │   │
│  │ - ServerContainer                                           │   │
│  │ - HandshakeRequest                                          │   │
│  │ - PathParam                                                 │   │
│  └─────────────────────────────────────────────────────────────┘   │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

**设计原则**：
- `javax.websocket` 包含客户端和服务器端的通用 API
- `javax.websocket.server` 专门用于服务器端部署和配置
- 两个包共享相同的 `Session`、`RemoteEndpoint` 等核心接口

---

## 参考资料

- [JSR-356: Java API for WebSocket](https://www.jcp.org/en/jsr/detail?id=356)
- [RFC 6455: The WebSocket Protocol](https://tools.ietf.org/html/rfc6455)
- [Tomcat WebSocket Documentation](https://tomcat.apache.org/tomcat-9.0-doc/web-socket-howto.html)

---

*文档生成时间: 2026-04-09*
*基于 Apache Tomcat 9.0.90 源码*
