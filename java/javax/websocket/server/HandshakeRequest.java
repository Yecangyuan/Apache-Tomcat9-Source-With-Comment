package javax.websocket.server;

import java.net.URI;
import java.security.Principal;
import java.util.List;
import java.util.Map;

/**
 * 表示请求升级到 WebSocket 的 HTTP 请求。
 */
public interface HandshakeRequest {

    /**
     * WebSocket 密钥请求头名称，用于 WebSocket 握手时的安全验证。
     */
    String SEC_WEBSOCKET_KEY = "Sec-WebSocket-Key";

    /**
     * WebSocket 子协议请求头名称，用于协商应用层协议。
     */
    String SEC_WEBSOCKET_PROTOCOL = "Sec-WebSocket-Protocol";

    /**
     * WebSocket 版本请求头名称，表示客户端支持的 WebSocket 协议版本。
     */
    String SEC_WEBSOCKET_VERSION = "Sec-WebSocket-Version";

    /**
     * WebSocket 扩展请求头名称，用于协商要使用的 WebSocket 扩展。
     */
    String SEC_WEBSOCKET_EXTENSIONS = "Sec-WebSocket-Extensions";

    /**
     * 获取 HTTP 请求头信息。
     *
     * @return 包含所有请求头的 Map，键为头名称，值为该头的所有值列表
     */
    Map<String,List<String>> getHeaders();

    /**
     * 获取与当前请求关联的用户主体信息。
     *
     * @return 表示已认证用户的 Principal 对象，如果未认证则返回 null
     */
    Principal getUserPrincipal();

    /**
     * 获取请求的 URI。
     *
     * @return 请求的目标 URI
     */
    URI getRequestURI();

    /**
     * 检查当前用户是否属于指定角色。
     *
     * @param role 要检查的角色名称
     * @return 如果用户属于该角色则返回 true，否则返回 false
     */
    boolean isUserInRole(String role);

    /**
     * 获取与此请求关联的 HTTP Session 对象。使用 Object 类型是为了避免直接依赖于 Servlet API。
     *
     * @return 与此请求关联的 javax.servlet.http.HttpSession 对象，如果没有则返回 null
     */
    Object getHttpSession();

    /**
     * 获取请求参数映射。
     *
     * @return 包含所有请求参数的 Map，键为参数名，值为该参数的所有值列表
     */
    Map<String,List<String>> getParameterMap();

    /**
     * 获取查询字符串。
     *
     * @return 请求 URL 中的查询字符串部分，如果没有则返回 null
     */
    String getQueryString();
}
