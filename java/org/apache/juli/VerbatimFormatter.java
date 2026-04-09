package org.apache.juli;

import java.util.logging.Formatter;
import java.util.logging.LogRecord;

/**
 * 仅输出日志消息，不添加任何额外元素。
 * 不记录堆栈跟踪。日志消息之间使用 <code>System.lineSeparator()</code> 分隔。
 * 适用于访问日志等需要完全控制输出格式的场景。
 */
public class VerbatimFormatter extends Formatter {

    /**
     * 格式化日志记录。
     * 仅返回日志消息内容加上系统换行符，不包含时间戳、日志级别等其他信息。
     *
     * @param record 日志记录对象
     * @return 格式化后的日志字符串
     */
    @Override
    public String format(LogRecord record) {
        // Timestamp + New line for next record
        return record.getMessage() + System.lineSeparator();
    }

}
