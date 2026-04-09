package javax.websocket;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.ByteBuffer;

/**
 * Encoder 接口是 WebSocket 编码器的标记接口。
 * 它定义了用于将 Java 对象编码为 WebSocket 消息的各种编码器子接口。
 * 实现类必须提供将特定 Java 类型转换为 WebSocket 文本或二进制消息的逻辑。
 */
public interface Encoder {

    /**
     * 初始化编码器。
     * 此方法在编码器实例创建后被调用，用于传入端点配置信息。
     *
     * @param endpointConfig 端点配置信息
     */
    void init(EndpointConfig endpointConfig);

    /**
     * 销毁编码器。
     * 此方法在编码器不再使用时被调用，用于释放资源。
     */
    void destroy();

    /**
     * 文本编码器接口。
     * 将 Java 对象编码为 WebSocket 文本消息。
     *
     * @param <T> 要编码的 Java 对象类型
     */
    interface Text<T> extends Encoder {

        /**
         * 将 Java 对象编码为字符串格式的文本消息。
         *
         * @param object 要编码的 Java 对象
         * @return 编码后的字符串
         * @throws EncodeException 如果编码过程中发生错误
         */
        String encode(T object) throws EncodeException;
    }

    /**
     * 文本流编码器接口。
     * 将 Java 对象编码并写入字符输出流，用于生成 WebSocket 文本消息。
     *
     * @param <T> 要编码的 Java 对象类型
     */
    interface TextStream<T> extends Encoder {

        /**
         * 将 Java 对象编码并写入字符输出流。
         *
         * @param object 要编码的 Java 对象
         * @param writer 字符输出流，用于写入编码后的文本
         * @throws EncodeException 如果编码过程中发生错误
         * @throws IOException 如果写入输出流时发生 I/O 错误
         */
        void encode(T object, Writer writer) throws EncodeException, IOException;
    }

    /**
     * 二进制编码器接口。
     * 将 Java 对象编码为 WebSocket 二进制消息。
     *
     * @param <T> 要编码的 Java 对象类型
     */
    interface Binary<T> extends Encoder {

        /**
         * 将 Java 对象编码为 ByteBuffer 格式的二进制消息。
         *
         * @param object 要编码的 Java 对象
         * @return 编码后的 ByteBuffer
         * @throws EncodeException 如果编码过程中发生错误
         */
        ByteBuffer encode(T object) throws EncodeException;
    }

    /**
     * 二进制流编码器接口。
     * 将 Java 对象编码并写入字节输出流，用于生成 WebSocket 二进制消息。
     *
     * @param <T> 要编码的 Java 对象类型
     */
    interface BinaryStream<T> extends Encoder {

        /**
         * 将 Java 对象编码并写入字节输出流。
         *
         * @param object 要编码的 Java 对象
         * @param os 字节输出流，用于写入编码后的二进制数据
         * @throws EncodeException 如果编码过程中发生错误
         * @throws IOException 如果写入输出流时发生 I/O 错误
         */
        void encode(T object, OutputStream os) throws EncodeException, IOException;
    }
}
