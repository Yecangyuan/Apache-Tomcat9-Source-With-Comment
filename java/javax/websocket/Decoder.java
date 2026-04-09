package javax.websocket;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;

/**
 * Decoder 接口定义了 WebSocket 解码器的基本生命周期方法。
 * 该接口包含四个子接口，分别用于解码不同类型的消息：
 * - Binary：用于解码二进制消息（ByteBuffer）
 * - BinaryStream：用于解码二进制流消息（InputStream）
 * - Text：用于解码文本消息（String）
 * - TextStream：用于解码文本流消息（Reader）
 */
public interface Decoder {

    /**
     * 初始化解码器。
     *
     * @param endpointConfig 端点配置对象，包含解码器所需的配置信息
     */
    void init(EndpointConfig endpointConfig);

    /**
     * 销毁解码器，释放资源。
     */
    void destroy();

    /**
     * Binary 子接口用于将二进制 WebSocket 消息解码为 Java 对象。
     *
     * @param <T> 解码后返回的 Java 对象类型
     */
    interface Binary<T> extends Decoder {

        /**
         * 将二进制数据解码为指定类型的 Java 对象。
         *
         * @param bytes 待解码的二进制数据
         * @return 解码后的 Java 对象
         * @throws DecodeException 如果解码过程中发生错误
         */
        T decode(ByteBuffer bytes) throws DecodeException;

        /**
         * 判断此解码器是否能够解码给定的二进制数据。
         *
         * @param bytes 待检查的二进制数据
         * @return 如果此解码器可以解码该数据则返回 true，否则返回 false
         */
        boolean willDecode(ByteBuffer bytes);
    }

    /**
     * BinaryStream 子接口用于将二进制流 WebSocket 消息解码为 Java 对象。
     *
     * @param <T> 解码后返回的 Java 对象类型
     */
    interface BinaryStream<T> extends Decoder {

        /**
         * 将二进制输入流解码为指定类型的 Java 对象。
         *
         * @param is 待解码的二进制输入流
         * @return 解码后的 Java 对象
         * @throws DecodeException 如果解码过程中发生错误
         * @throws IOException 如果读取输入流时发生 I/O 错误
         */
        T decode(InputStream is) throws DecodeException, IOException;
    }

    /**
     * Text 子接口用于将文本 WebSocket 消息解码为 Java 对象。
     *
     * @param <T> 解码后返回的 Java 对象类型
     */
    interface Text<T> extends Decoder {

        /**
         * 将文本字符串解码为指定类型的 Java 对象。
         *
         * @param s 待解码的文本字符串
         * @return 解码后的 Java 对象
         * @throws DecodeException 如果解码过程中发生错误
         */
        T decode(String s) throws DecodeException;

        /**
         * 判断此解码器是否能够解码给定的文本字符串。
         *
         * @param s 待检查的文本字符串
         * @return 如果此解码器可以解码该字符串则返回 true，否则返回 false
         */
        boolean willDecode(String s);
    }

    /**
     * TextStream 子接口用于将文本流 WebSocket 消息解码为 Java 对象。
     *
     * @param <T> 解码后返回的 Java 对象类型
     */
    interface TextStream<T> extends Decoder {

        /**
         * 将字符阅读器解码为指定类型的 Java 对象。
         *
         * @param reader 待解码的字符阅读器
         * @return 解码后的 Java 对象
         * @throws DecodeException 如果解码过程中发生错误
         * @throws IOException 如果读取字符流时发生 I/O 错误
         */
        T decode(Reader reader) throws DecodeException, IOException;
    }
}
