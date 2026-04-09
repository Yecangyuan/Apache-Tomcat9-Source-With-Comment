package javax.websocket;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 用于标注当 WebSocket 连接关闭时要调用的方法。
 * 被标注的方法可以接收以下类型的参数：
 * - Session：可选，表示当前 WebSocket 会话
 * - CloseReason：可选，表示连接关闭的原因
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OnClose {
}
