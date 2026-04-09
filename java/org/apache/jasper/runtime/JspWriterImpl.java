package org.apache.jasper.runtime;

import java.io.IOException;
import java.io.Writer;

import javax.servlet.jsp.JspWriter;

import org.apache.jasper.Constants;
import org.apache.jasper.compiler.Localizer;

/**
 * JspWriter 的默认实现类，用于 JSP 页面输出。
 * <p>
 * 该类实现了 JspWriter 抽象类，提供了带缓冲的字符输出功能。
 * 支持自动刷新和手动刷新模式，保护 JSP 页面生成的内容不被意外清除。
 * </p>
 *
 * @author Anil K. Vijendran
 */
public class JspWriterImpl extends JspWriter {

    // 基础字符缓冲区大小
    private static final int BUFFER_SIZE = Constants.DEFAULT_BUFFER_SIZE;
    // 是否处于错误状态
    private boolean errorState = false;

    /**
     * 输出缓冲区。
     * <p>
     * 用于存储待写入的字符数据，当缓冲区满或调用 flush() 时写入底层 Writer。
     * </p>
     */
    protected char[] cb;
    /**
     * 下一个字符的写入位置。
     */
    protected int nextChar;
    /**
     * 底层输出 Writer。
     * <p>
     * 实际执行 I/O 操作的 Writer，缓冲区的数据最终会写入到这里。
     * </p>
     */
    protected Writer out;

    /**
     * 构造方法。
     * <p>
     * 使用指定的响应 Writer 和缓冲区大小创建 JspWriterImpl 实例。
     * </p>
     *
     * @param responseWriter 响应 Writer，用于实际输出
     * @param sz 缓冲区大小
     * @param autoFlush 是否自动刷新
     */
    public JspWriterImpl(Writer responseWriter, int sz, boolean autoFlush) {
        super(sz, autoFlush);
        if (sz < 0) {
            throw new IllegalArgumentException("Buffer size <= 0");
        }
        this.out = responseWriter;
        cb = sz == 0 ? null : new char[sz];
        nextChar = 0;
    }

    /**
     * 设置底层输出 Writer。
     * <p>
     * 用于在页面转发或包含时切换输出目标。
     * </p>
     *
     * @param responseWriter 新的响应 Writer
     */
    public void setWriter(Writer responseWriter) {
        out = responseWriter;
    }

    /**
     * 判断缓冲区是否已写满。
     *
     * @return 如果缓冲区已满则返回 true
     */
    private boolean isFull() {
        return (nextChar >= bufferSize);
    }

    /**
     * 将缓冲区的内容刷新到底层 Writer。
     *
     * @throws IOException 如果发生 I/O 错误
     */
    protected final void flushBuffer() throws IOException {
        if (bufferSize == 0) {
            return;
        }
        flushed = true;
        ensureOpen();
        if (nextChar == 0) {
            return;
        }
        initOut();
        out.write(cb, 0, nextChar);
        nextChar = 0;
    }

    private final void initOut() throws IOException {
        if (out == null) {
            throw new IOException(Constants.getString("jsp.error.copyBuffer"));
        }
    }

    /**
     * 清空缓冲区内容。
     * <p>
     * 丢弃缓冲区中的所有内容，不进行输出。
     * 注意：如果缓冲区之前已经被刷新过（如转发后的缓冲区），
     * 则不能清空，会抛出异常。
     * </p>
     *
     * @throws IOException 如果缓冲区不能被清空（如已刷新过）
     */
    public final void clear() throws IOException {
        if (bufferSize == 0) {
            return;
        }
        ensureOpen();
        if (flushed) {
            throw new IOException(
                    Localizer.getMessage("jsp.error.ise_on_clear"));
        }
        nextChar = 0;
    }

    /**
     * 清空缓冲区，但不抛出已刷新异常。
     * <p>
     * 与 clear() 不同，即使缓冲区已经被刷新，也不会抛出异常。
     * </p>
     *
     * @throws IOException 如果发生 I/O 错误
     */
    public final void clearBuffer() throws IOException {
        ensureOpen();
        if (bufferSize == 0) {
            return;
        }
        nextChar = 0;
    }

    private final void bufferOverflow() throws IOException {
        throw new IOException(Localizer.getMessage("jsp.error.overflow"));
    }

    /**
     * 刷新输出流。
     * <p>
     * 将缓冲区的内容写入底层 Writer，然后刷新底层 Writer。
     * 如果未启用自动刷新且缓冲区已满，则抛出异常。
     * </p>
     *
     * @throws IOException 如果发生 I/O 错误
     */
    public final void flush() throws IOException {
        flushBuffer();
        ensureOpen();
        if (out != null) {
            out.flush();
        }
    }

    /**
     * 关闭 Writer。
     * <p>
     * 刷新并关闭输出流。关闭后不能再进行写入操作。
     * </p>
     *
     * @throws IOException 如果发生 I/O 错误
     */
    public final void close() throws IOException {
        if (out == null || errorState) {
            return;
        }
        flush();
    }

    /**
     * 返回缓冲区大小。
     *
     * @return 缓冲区大小（字符数）
     */
    public final int getBufferSize() {
        return bufferSize;
    }

    /**
     * 返回缓冲区中剩余的可用空间。
     *
     * @return 剩余可用字符数
     */
    public final int getRemaining() {
        return bufferSize - nextChar;
    }

    /**
     * 写入单个字符。
     *
     * @param c 要写入的字符
     * @throws IOException 如果发生 I/O 错误
     */
    public final void write(int c) throws IOException {
        if (bufferSize == 0) {
            initOut();
            out.write(c);
        } else {
            ensureOpen();
            if (nextChar >= bufferSize) {
                if (autoFlush) {
                    flushBuffer();
                } else {
                    bufferOverflow();
                }
            }
            cb[nextChar++] = (char) c;
        }
    }

    /**
     * 写入字符数组。
     *
     * @param cbuf 要写入的字符数组
     * @param off 起始偏移量
     * @param len 写入长度
     * @throws IOException 如果发生 I/O 错误
     */
    public final void write(char[] cbuf, int off, int len) throws IOException {
        if (bufferSize == 0) {
            initOut();
            out.write(cbuf, off, len);
            return;
        }

        ensureOpen();

        if ((off < 0) || (off > cbuf.length) || (len < 0) ||
            ((off + len) > cbuf.length) || ((off + len) < 0)) {
            throw new IndexOutOfBoundsException();
        } else if (len == 0) {
            return;
        }

        if (len >= bufferSize) {
            if (autoFlush) {
                flushBuffer();
            }
            initOut();
            out.write(cbuf, off, len);
            return;
        }

        int b = off, t = off + len;
        while (b < t) {
            int d = min(bufferSize - nextChar, t - b);
            System.arraycopy(cbuf, b, cb, nextChar, d);
            b += d;
            nextChar += d;
            if (nextChar >= bufferSize) {
                if (autoFlush) {
                    flushBuffer();
                } else {
                    bufferOverflow();
                }
            }
        }
    }

    /**
     * 写入字符串。
     *
     * @param s 要写入的字符串
     * @param off 起始偏移量
     * @param len 写入长度
     * @throws IOException 如果发生 I/O 错误
     */
    public final void write(String s, int off, int len) throws IOException {
        if (bufferSize == 0) {
            initOut();
            out.write(s, off, len);
            return;
        }

        ensureOpen();

        int b = off, t = off + len;
        while (b < t) {
            int d = min(bufferSize - nextChar, t - b);
            s.getChars(b, b + d, cb, nextChar);
            b += d;
            nextChar += d;
            if (nextChar >= bufferSize) {
                if (autoFlush) {
                    flushBuffer();
                } else {
                    bufferOverflow();
                }
            }
        }
    }

    /**
     * 将字符串写入底层输出流，不经过缓冲。
     * <p>
     * 直接写入，不使用内部缓冲区。如果需要在打印
     * 错误消息时确保没有缓冲数据丢失，此方法很有用。
     * </p>
     *
     * @param s 要打印的字符串
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeOut(String s) throws IOException {
        writeOut(s.toCharArray(), 0, s.length());
    }

    /**
     * 将字符数组写入底层输出流，不经过缓冲。
     *
     * @param buf 要写入的字符数组
     * @param off 起始偏移量
     * @param len 写入长度
     * @throws IOException 如果发生 I/O 错误
     */
    public void writeOut(char[] buf, int off, int len) throws IOException {
        initOut();
        out.write(buf, off, len);
    }

    private static final int min(int a, int b) {
        return (a < b ? a : b);
    }

    private void ensureOpen() throws IOException {
        if (out == null) {
            throw new IOException("Stream closed");
        }
    }

    /**
     * 写入换行符。
     *
     * @throws IOException 如果发生 I/O 错误
     */
    public void newLine() throws IOException {
        write(lineSeparator);
    }

    /**
     * 打印布尔值。
     *
     * @param b 要打印的布尔值
     * @throws IOException 如果发生 I/O 错误
     */
    public void print(boolean b) throws IOException {
        write(b ? "true" : "false");
    }

    /**
     * 打印字符。
     *
     * @param c 要打印的字符
     * @throws IOException 如果发生 I/O 错误
     */
    public void print(char c) throws IOException {
        write(c);
    }

    /**
     * 打印整数。
     *
     * @param i 要打印的整数
     * @throws IOException 如果发生 I/O 错误
     */
    public void print(int i) throws IOException {
        write(String.valueOf(i));
    }

    /**
     * 打印长整数。
     *
     * @param l 要打印的长整数
     * @throws IOException 如果发生 I/O 错误
     */
    public void print(long l) throws IOException {
        write(String.valueOf(l));
    }

    /**
     * 打印浮点数。
     *
     * @param f 要打印的浮点数
     * @throws IOException 如果发生 I/O 错误
     */
    public void print(float f) throws IOException {
        write(String.valueOf(f));
    }

    /**
     * 打印双精度浮点数。
     *
     * @param d 要打印的双精度浮点数
     * @throws IOException 如果发生 I/O 错误
     */
    public void print(double d) throws IOException {
        write(String.valueOf(d));
    }

    /**
     * 打印字符数组。
     *
     * @param s 要打印的字符数组
     * @throws IOException 如果发生 I/O 错误
     */
    public void print(char[] s) throws IOException {
        write(s);
    }

    /**
     * 打印字符串。
     * <p>
     * 如果字符串为 null，则打印 "null"。
     * </p>
     *
     * @param s 要打印的字符串
     * @throws IOException 如果发生 I/O 错误
     */
    public void print(String s) throws IOException {
        if (s == null) {
            s = "null";
        }
        write(s);
    }

    /**
     * 打印对象。
     * <p>
     * 使用对象的 toString() 方法转换为字符串后打印。
     * </p>
     *
     * @param obj 要打印的对象
     * @throws IOException 如果发生 I/O 错误
     */
    public void print(Object obj) throws IOException {
        write(String.valueOf(obj));
    }

    /**
     * 打印换行符。
     *
     * @throws IOException 如果发生 I/O 错误
     */
    public void println() throws IOException {
        newLine();
    }

    /**
     * 打印布尔值并换行。
     *
     * @param x 要打印的布尔值
     * @throws IOException 如果发生 I/O 错误
     */
    public void println(boolean x) throws IOException {
        print(x);
        println();
    }

    /**
     * 打印字符并换行。
     *
     * @param x 要打印的字符
     * @throws IOException 如果发生 I/O 错误
     */
    public void println(char x) throws IOException {
        print(x);
        println();
    }

    /**
     * 打印整数并换行。
     *
     * @param x 要打印的整数
     * @throws IOException 如果发生 I/O 错误
     */
    public void println(int x) throws IOException {
        print(x);
        println();
    }

    /**
     * 打印长整数并换行。
     *
     * @param x 要打印的长整数
     * @throws IOException 如果发生 I/O 错误
     */
    public void println(long x) throws IOException {
        print(x);
        println();
    }

    /**
     * 打印浮点数并换行。
     *
     * @param x 要打印的浮点数
     * @throws IOException 如果发生 I/O 错误
     */
    public void println(float x) throws IOException {
        print(x);
        println();
    }

    /**
     * 打印双精度浮点数并换行。
     *
     * @param x 要打印的双精度浮点数
     * @throws IOException 如果发生 I/O 错误
     */
    public void println(double x) throws IOException {
        print(x);
        println();
    }

    /**
     * 打印字符数组并换行。
     *
     * @param x 要打印的字符数组
     * @throws IOException 如果发生 I/O 错误
     */
    public void println(char[] x) throws IOException {
        print(x);
        println();
    }

    /**
     * 打印字符串并换行。
     *
     * @param x 要打印的字符串
     * @throws IOException 如果发生 I/O 错误
     */
    public void println(String x) throws IOException {
        print(x);
        println();
    }

    /**
     * 打印对象并换行。
     *
     * @param x 要打印的对象
     * @throws IOException 如果发生 I/O 错误
     */
    public void println(Object x) throws IOException {
        print(x);
        println();
    }

    /**
     * 清空流。
     * <p>
     * 如果流发生错误，此方法可以重置错误状态。
     * </p>
     */
    void clearError() {
        errorState = false;
    }
}
