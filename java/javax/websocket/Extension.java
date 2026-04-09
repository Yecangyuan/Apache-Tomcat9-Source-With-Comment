package javax.websocket;

import java.util.List;

/**
 * 表示 WebSocket 扩展。WebSocket 扩展提供了一种在握手期间协商
 * 和使用扩展功能的方式，例如压缩或其他协议级别的扩展。
 */
public interface Extension {

    /**
     * 获取此扩展的名称。名称是用于在 WebSocket 握手期间
     * 标识扩展的字符串。
     *
     * @return 扩展的名称
     */
    String getName();

    /**
     * 获取此扩展的参数列表。参数用于配置扩展的
     * 具体行为。
     *
     * @return 扩展参数的列表，如果没有参数则返回空列表
     */
    List<Parameter> getParameters();

    /**
     * 表示 WebSocket 扩展的参数。参数是用于配置
     * 扩展行为的名称-值对。
     */
    interface Parameter {

        /**
         * 获取参数的名称。
         *
         * @return 参数的名称
         */
        String getName();

        /**
         * 获取参数的值。
         *
         * @return 参数的值
         */
        String getValue();
    }
}
