package javax.websocket;

/**
 * 表示WebSocket连接关闭的原因。
 * 包含一个状态码和一个可选的关闭原因描述。
 *
 * @since 7.0.50
 */
public class CloseReason {

    private final CloseCode closeCode;
    private final String reasonPhrase;

    /**
     * 构造一个新的关闭原因实例。
     *
     * @param closeCode    WebSocket连接关闭的状态码
     * @param reasonPhrase 关闭原因的描述信息，可以为null
     */
    public CloseReason(CloseReason.CloseCode closeCode, String reasonPhrase) {
        this.closeCode = closeCode;
        this.reasonPhrase = reasonPhrase;
    }

    /**
     * 获取WebSocket连接关闭的状态码。
     *
     * @return 关闭状态码
     */
    public CloseCode getCloseCode() {
        return closeCode;
    }

    /**
     * 获取WebSocket连接关闭的原因描述。
     *
     * @return 关闭原因描述，如果未设置则可能为null
     */
    public String getReasonPhrase() {
        return reasonPhrase;
    }

    @Override
    public String toString() {
        return "CloseReason: code [" + closeCode.getCode() + "], reason [" + reasonPhrase + "]";
    }

    /**
     * 表示WebSocket连接关闭代码的接口。
     */
    public interface CloseCode {
        /**
         * 获取关闭代码的数值。
         *
         * @return 关闭代码值
         */
        int getCode();
    }

    /**
     * 定义了RFC 6455规范中标准的WebSocket关闭代码枚举。
     * 这些代码用于表示连接关闭的不同原因。
     */
    public enum CloseCodes implements CloseReason.CloseCode {

        /** 正常关闭（1000）- 表示连接已成功完成任务 */
        NORMAL_CLOSURE(1000),
        /** 正在离开（1001）- 表示终端正在离开，例如服务器或浏览器正在关闭 */
        GOING_AWAY(1001),
        /** 协议错误（1002）- 表示由于协议错误而终止连接 */
        PROTOCOL_ERROR(1002),
        /** 无法接受（1003）- 表示端点接收到无法处理的数据类型 */
        CANNOT_ACCEPT(1003),
        /** 保留（1004）- 保留字段 */
        RESERVED(1004),
        /** 没有状态码（1005）- 保留字段，表示没有期望的状态码 */
        NO_STATUS_CODE(1005),
        /** 异常关闭（1006）- 保留字段，表示连接异常关闭 */
        CLOSED_ABNORMALLY(1006),
        /** 不一致（1007）- 表示端点接收到与消息类型不一致的数据 */
        NOT_CONSISTENT(1007),
        /** 违反策略（1008）- 表示端点接收到违反策略的消息 */
        VIOLATED_POLICY(1008),
        /** 消息过大（1009）- 表示端点接收到过大的消息而无法处理 */
        TOO_BIG(1009),
        /** 缺少扩展（1010）- 表示客户端期望服务器协商一个或多个扩展，但服务器未处理 */
        NO_EXTENSION(1010),
        /** 意外情况（1011）- 表示服务器由于意外情况而终止连接 */
        UNEXPECTED_CONDITION(1011),
        /** 服务重启（1012）- 表示服务器正在重启 */
        SERVICE_RESTART(1012),
        /** 稍后重试（1013）- 表示服务器由于临时情况而终止连接 */
        TRY_AGAIN_LATER(1013),
        /** TLS握手失败（1015）- 保留字段，表示TLS握手失败 */
        TLS_HANDSHAKE_FAILURE(1015);

        private int code;

        /**
         * 构造关闭代码枚举实例。
         *
         * @param code 关闭代码数值
         */
        CloseCodes(int code) {
            this.code = code;
        }

        /**
         * 根据代码值获取对应的CloseCode实例。
         *
         * @param code 关闭代码数值
         * @return 对应的CloseCode实例
         * @throws IllegalArgumentException 如果代码值无效
         */
        public static CloseCode getCloseCode(final int code) {
            if (code > 2999 && code < 5000) {
                return new CloseCode() {
                    @Override
                    public int getCode() {
                        return code;
                    }
                };
            }
            switch (code) {
                case 1000:
                    return NORMAL_CLOSURE;
                case 1001:
                    return GOING_AWAY;
                case 1002:
                    return PROTOCOL_ERROR;
                case 1003:
                    return CANNOT_ACCEPT;
                case 1004:
                    return RESERVED;
                case 1005:
                    return NO_STATUS_CODE;
                case 1006:
                    return CLOSED_ABNORMALLY;
                case 1007:
                    return NOT_CONSISTENT;
                case 1008:
                    return VIOLATED_POLICY;
                case 1009:
                    return TOO_BIG;
                case 1010:
                    return NO_EXTENSION;
                case 1011:
                    return UNEXPECTED_CONDITION;
                case 1012:
                    return SERVICE_RESTART;
                case 1013:
                    return TRY_AGAIN_LATER;
                case 1015:
                    return TLS_HANDSHAKE_FAILURE;
                default:
                    throw new IllegalArgumentException("Invalid close code: [" + code + "]");
            }
        }

        @Override
        public int getCode() {
            return code;
        }
    }
}
