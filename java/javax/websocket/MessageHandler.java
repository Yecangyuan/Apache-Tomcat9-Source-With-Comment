package javax.websocket;

/**
 * 消息处理器接口，用于处理从 WebSocket 连接接收到的消息。
 * 此接口有两个子接口：{@link Whole} 用于处理完整消息，{@link Partial} 用于处理分片消息。
 */
public interface MessageHandler {

    /**
     * 分片消息处理器接口，用于处理以多个片段形式到达的消息。
     *
     * @param <T> 消息的数据类型
     */
    interface Partial<T> extends MessageHandler {

        /**
         * 当消息的一部分可用时被调用。
         *
         * @param messagePart 消息的片段
         * @param last        如果这是该消息的最后一部分则为 <code>true</code>，否则为 <code>false</code>
         */
        void onMessage(T messagePart, boolean last);
    }

    /**
     * 完整消息处理器接口，用于处理以完整形式到达的消息。
     *
     * @param <T> 消息的数据类型
     */
    interface Whole<T> extends MessageHandler {

        /**
         * 当整个消息可用时被调用。
         *
         * @param message 完整的消息
         */
        void onMessage(T message);
    }
}
