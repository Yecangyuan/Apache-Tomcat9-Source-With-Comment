package org.apache.juli.logging;

/**
 * <p>一个简单的日志接口，抽象了各种日志API。为了能够被 {@link LogFactory} 成功实例化，
 * 实现此接口的类必须有一个接受单个字符串参数的构造函数，该字符串参数表示此日志的"名称"。</p>
 *
 * <p><code>Log</code> 使用的六个日志级别（按严重程度从低到高排序）如下：</p>
 * <ol>
 * <li>trace（最轻微的）</li>
 * <li>debug（调试）</li>
 * <li>info（信息）</li>
 * <li>warn（警告）</li>
 * <li>error（错误）</li>
 * <li>fatal（最严重的）</li>
 * </ol>
 * <p>这些日志级别到底层日志系统所使用概念的映射取决于具体实现。
 * 但实现应确保此排序按预期工作。</p>
 *
 * <p>性能通常是日志记录的一个关注点。通过检查适当的属性，
 * 组件可以避免执行昂贵的操作（生成要记录的信息）。</p>
 *
 * <p>例如：
 * <code>
 *    if (log.isDebugEnabled()) {
 *        ... 执行某些昂贵的操作 ...
 *        log.debug(theResult);
 *    }
 * </code>
 * </p>
 *
 * <p>底层日志系统的配置通常通过该日志系统所支持的机制在日志API外部完成。</p>
 *
 * @author <a href="mailto:sanders@apache.org">Scott Sanders</a>
 * @author Rod Waldhoff
 */
public interface Log {


    // ----------------------------------------------------- Logging Properties


    /**
     * <p>当前是否启用了 debug 级别日志记录？</p>
     *
     * <p>调用此方法可以避免在日志级别高于 debug 时执行昂贵的操作
     * （例如，<code>String</code> 拼接）。</p>
     *
     * @return 如果启用了 debug 级别日志记录则返回 <code>true</code>，否则返回 <code>false</code>
     */
    boolean isDebugEnabled();


    /**
     * <p>当前是否启用了 error 级别日志记录？</p>
     *
     * <p>调用此方法可以避免在日志级别高于 error 时执行昂贵的操作
     * （例如，<code>String</code> 拼接）。</p>
     *
     * @return 如果启用了 error 级别日志记录则返回 <code>true</code>，否则返回 <code>false</code>
     */
    boolean isErrorEnabled();


    /**
     * <p>当前是否启用了 fatal 级别日志记录？</p>
     *
     * <p>调用此方法可以避免在日志级别高于 fatal 时执行昂贵的操作
     * （例如，<code>String</code> 拼接）。</p>
     *
     * @return 如果启用了 fatal 级别日志记录则返回 <code>true</code>，否则返回 <code>false</code>
     */
    boolean isFatalEnabled();


    /**
     * <p>当前是否启用了 info 级别日志记录？</p>
     *
     * <p>调用此方法可以避免在日志级别高于 info 时执行昂贵的操作
     * （例如，<code>String</code> 拼接）。</p>
     *
     * @return 如果启用了 info 级别日志记录则返回 <code>true</code>，否则返回 <code>false</code>
     */
    boolean isInfoEnabled();


    /**
     * <p>当前是否启用了 trace 级别日志记录？</p>
     *
     * <p>调用此方法可以避免在日志级别高于 trace 时执行昂贵的操作
     * （例如，<code>String</code> 拼接）。</p>
     *
     * @return 如果启用了 trace 级别日志记录则返回 <code>true</code>，否则返回 <code>false</code>
     */
    boolean isTraceEnabled();


    /**
     * <p>当前是否启用了 warn 级别日志记录？</p>
     *
     * <p>调用此方法可以避免在日志级别高于 warn 时执行昂贵的操作
     * （例如，<code>String</code> 拼接）。</p>
     *
     * @return 如果启用了 warn 级别日志记录则返回 <code>true</code>，否则返回 <code>false</code>
     */
    boolean isWarnEnabled();


    // -------------------------------------------------------- Logging Methods


    /**
     * <p>使用 trace 日志级别记录一条消息。</p>
     *
     * @param message 要记录的消息
     */
    void trace(Object message);


    /**
     * <p>使用 trace 日志级别记录一条消息和异常。</p>
     *
     * @param message 要记录的消息
     * @param t 要记录的异常原因
     */
    void trace(Object message, Throwable t);


    /**
     * <p>使用 debug 日志级别记录一条消息。</p>
     *
     * @param message 要记录的消息
     */
    void debug(Object message);


    /**
     * <p>使用 debug 日志级别记录一条消息和异常。</p>
     *
     * @param message 要记录的消息
     * @param t 要记录的异常原因
     */
    void debug(Object message, Throwable t);


    /**
     * <p>使用 info 日志级别记录一条消息。</p>
     *
     * @param message 要记录的消息
     */
    void info(Object message);


    /**
     * <p>使用 info 日志级别记录一条消息和异常。</p>
     *
     * @param message 要记录的消息
     * @param t 要记录的异常原因
     */
    void info(Object message, Throwable t);


    /**
     * <p>使用 warn 日志级别记录一条消息。</p>
     *
     * @param message 要记录的消息
     */
    void warn(Object message);


    /**
     * <p>使用 warn 日志级别记录一条消息和异常。</p>
     *
     * @param message 要记录的消息
     * @param t 要记录的异常原因
     */
    void warn(Object message, Throwable t);


    /**
     * <p>使用 error 日志级别记录一条消息。</p>
     *
     * @param message 要记录的消息
     */
    void error(Object message);


    /**
     * <p>使用 error 日志级别记录一条消息和异常。</p>
     *
     * @param message 要记录的消息
     * @param t 要记录的异常原因
     */
    void error(Object message, Throwable t);


    /**
     * <p>使用 fatal 日志级别记录一条消息。</p>
     *
     * @param message 要记录的消息
     */
    void fatal(Object message);


    /**
     * <p>使用 fatal 日志级别记录一条消息和异常。</p>
     *
     * @param message 要记录的消息
     * @param t 要记录的异常原因
     */
    void fatal(Object message, Throwable t);


}
