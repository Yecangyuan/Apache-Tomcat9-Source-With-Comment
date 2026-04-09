package org.apache.jasper.runtime;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.security.AccessController;
import java.security.PrivilegedAction;

import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.BodyContent;

import org.apache.jasper.Constants;
import org.apache.jasper.compiler.Localizer;

/**
 * BodyContent 的实现类，用于 JSP 标签体内容处理。
 * 
 * 将文本写入字符输出流，通过缓冲字符来实现对单个字符、字符数组和字符串的高效写入。
 * 支持丢弃已缓冲的输出内容。
 * 
 * @author Rajiv Mordani
 * @author Jan Luehe
 */
public class BodyContentImpl extends BodyContent {

    private static final boolean LIMIT_BUFFER;
    private static final int TAG_BUFFER_SIZE;

    static {
        if (System.getSecurityManager() == null) {
            LIMIT_BUFFER = Boolean.parseBoolean(System.getProperty(
                    "org.apache.jasper.runtime.BodyContentImpl.LIMIT_BUFFER", "false"));
            TAG_BUFFER_SIZE = Integer.getInteger(
                    "org.apache.jasper.runtime.BodyContentImpl.BUFFER_SIZE",
                    Constants.DEFAULT_TAG_BUFFER_SIZE).intValue();
        } else {
            LIMIT_BUFFER = AccessController.doPrivileged(
                    new PrivilegedAction<Boolean>() {
                        @Override
                        public Boolean run() {
                            return Boolean.valueOf(System.getProperty(
                                    "org.apache.jasper.runtime.BodyContentImpl.LIMIT_BUFFER",
                                    "false"));
                        }
                    }
            ).booleanValue();
            TAG_BUFFER_SIZE = AccessController.doPrivileged(
                    new PrivilegedAction<Integer>() {
                        @Override
                        public Integer run() {
                            return Integer.getInteger(
                                    "org.apache.jasper.runtime.BodyContentImpl.BUFFER_SIZE",
                                    Constants.DEFAULT_TAG_BUFFER_SIZE);
                        }
                    }
            ).intValue();
        }
    }

    /** 字符缓冲区，用于存储标签体内容 */
    private char[] cb;
    private int nextChar;
    /** 是否已关闭 */
    private boolean closed;

    /**
     * Enclosed writer to which any output is written
     */
    private Writer writer;

    /**
     * 构造方法。
     * 
     * @param enclosingWriter 被包装的 JspWriter 写入器
     */
    public BodyContentImpl(JspWriter enclosingWriter) {
        super(enclosingWriter);
        cb = new char[TAG_BUFFER_SIZE];
        bufferSize = cb.length;
        nextChar = 0;
        closed = false;
    }

    /**
     * 写入单个字符。
     * 
     * @param c 要写入的字符（低16位）
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void write(int c) throws IOException {
        if (writer != null) {
            writer.write(c);
        } else {
            ensureOpen();
            if (nextChar >= bufferSize) {
                reAllocBuff (1);
            }
            cb[nextChar++] = (char) c;
        }
    }

    /**
     * 写入字符数组的一部分。
     * 
     * @param cbuf 字符数组
     * @param off  起始偏移量
     * @param len  要写入的字符数
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void write(char[] cbuf, int off, int len) throws IOException {
        if (writer != null) {
            writer.write(cbuf, off, len);
        } else {
            ensureOpen();

            if ((off < 0) || (off > cbuf.length) || (len < 0) ||
                    ((off + len) > cbuf.length) || ((off + len) < 0)) {
                throw new IndexOutOfBoundsException();
            } else if (len == 0) {
                return;
            }

            if (len >= bufferSize - nextChar) {
                reAllocBuff (len);
            }

            System.arraycopy(cbuf, off, cb, nextChar, len);
            nextChar+=len;
        }
    }

    /**
     * 写入整个字符数组。
     * 
     * @param buf 要写入的字符数组
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void write(char[] buf) throws IOException {
        if (writer != null) {
            writer.write(buf);
        } else {
            write(buf, 0, buf.length);
        }
    }

    /**
     * 写入字符串的一部分。
     * 
     * @param s   要写入的字符串
     * @param off 起始偏移量
     * @param len 要写入的字符数
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void write(String s, int off, int len) throws IOException {
        if (writer != null) {
            writer.write(s, off, len);
        } else {
            ensureOpen();
            if (len >= bufferSize - nextChar) {
                reAllocBuff(len);
            }

            s.getChars(off, off + len, cb, nextChar);
            nextChar += len;
        }
    }

    /**
     * 写入整个字符串。
     * 
     * @param s 要写入的字符串
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void write(String s) throws IOException {
        if (writer != null) {
            writer.write(s);
        } else {
            write(s, 0, s.length());
        }
    }

    @Override
    public void newLine() throws IOException {
        if (writer != null) {
            writer.write(System.lineSeparator());
        } else {
            write(System.lineSeparator());
        }
    }

    @Override
    public void print(boolean b) throws IOException {
        if (writer != null) {
            writer.write(b ? "true" : "false");
        } else {
            write(b ? "true" : "false");
        }
    }

    @Override
    public void print(char c) throws IOException {
        if (writer != null) {
            writer.write(String.valueOf(c));
        } else {
            write(String.valueOf(c));
        }
    }

    @Override
    public void print(int i) throws IOException {
        if (writer != null) {
            writer.write(String.valueOf(i));
        } else {
            write(String.valueOf(i));
        }
    }

    @Override
    public void print(long l) throws IOException {
        if (writer != null) {
            writer.write(String.valueOf(l));
        } else {
            write(String.valueOf(l));
        }
    }

    @Override
    public void print(float f) throws IOException {
        if (writer != null) {
            writer.write(String.valueOf(f));
        } else {
            write(String.valueOf(f));
        }
    }

    @Override
    public void print(double d) throws IOException {
        if (writer != null) {
            writer.write(String.valueOf(d));
        } else {
            write(String.valueOf(d));
        }
    }

    @Override
    public void print(char[] s) throws IOException {
        if (writer != null) {
            writer.write(s);
        } else {
            write(s);
        }
    }

    @Override
    public void print(String s) throws IOException {
        if (s == null) {
            s = "null";
        }
        if (writer != null) {
            writer.write(s);
        } else {
            write(s);
        }
    }

    @Override
    public void print(Object obj) throws IOException {
        if (writer != null) {
            writer.write(String.valueOf(obj));
        } else {
            write(String.valueOf(obj));
        }
    }

    @Override
    public void println() throws IOException {
        newLine();
    }

    @Override
    public void println(boolean x) throws IOException {
        print(x);
        println();
    }

    @Override
    public void println(char x) throws IOException {
        print(x);
        println();
    }

    @Override
    public void println(int x) throws IOException {
        print(x);
        println();
    }

    @Override
    public void println(long x) throws IOException {
        print(x);
        println();
    }

    @Override
    public void println(float x) throws IOException {
        print(x);
        println();
    }

    @Override
    public void println(double x) throws IOException{
        print(x);
        println();
    }

    @Override
    public void println(char x[]) throws IOException {
        print(x);
        println();
    }

    @Override
    public void println(String x) throws IOException {
        print(x);
        println();
    }

    @Override
    public void println(Object x) throws IOException {
        print(x);
        println();
    }

    /**
     * 清空缓冲区，丢弃所有已缓冲的输出内容。
     * 
     * @throws IOException 如果流已关闭或发生 I/O 错误
     */
    @Override
    public void clear() throws IOException {
        if (writer != null) {
            throw new IOException();
        } else {
            nextChar = 0;
            if (LIMIT_BUFFER && (cb.length > TAG_BUFFER_SIZE)) {
                cb = new char[TAG_BUFFER_SIZE];
                bufferSize = cb.length;
            }
        }
    }

    /**
     * 清空缓冲区内容，但不抛出异常（与 clear() 的区别是不会在 writer 不为 null 时抛出异常）。
     * 
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void clearBuffer() throws IOException {
        if (writer == null) {
            this.clear();
        }
    }

    /**
     * 关闭此 BodyContent。
     * 关闭后，写入操作将抛出 IOException。
     * 
     * @throws IOException 如果发生 I/O 错误
     */
    @Override
    public void close() throws IOException {
        if (writer != null) {
            writer.close();
        } else {
            closed = true;
        }
    }

    @Override
    public int getBufferSize() {
        // According to the spec, the JspWriter returned by
        // JspContext.pushBody(java.io.Writer writer) must behave as
        // though it were unbuffered. This means that its getBufferSize()
        // must always return 0.
        return (writer == null) ? bufferSize : 0;
    }

    @Override
    public int getRemaining() {
        return (writer == null) ? bufferSize-nextChar : 0;
    }

    /**
     * 获取一个 Reader 来读取缓冲区中的内容。
     * 
     * @return 包含缓冲区内容的 Reader，如果 writer 不为 null 则返回 null
     */
    @Override
    public Reader getReader() {
        return (writer == null) ? new CharArrayReader (cb, 0, nextChar) : null;
    }

    /**
     * 将缓冲区内容作为字符串返回。
     * 
     * @return 缓冲区中的字符串内容，如果 writer 不为 null 则返回 null
     */
    @Override
    public String getString() {
        return (writer == null) ? new String(cb, 0, nextChar) : null;
    }

    @Override
    public void writeOut(Writer out) throws IOException {
        if (writer == null) {
            out.write(cb, 0, nextChar);
            // Flush not called as the writer passed could be a BodyContent and
            // it doesn't allow to flush.
        }
    }

    /**
     * Sets the writer to which all output is written.
     */
    void setWriter(Writer writer) {
        this.writer = writer;
        closed = false;
        if (writer == null) {
            clearBody();
        }
    }

    /**
     * This method shall "reset" the internal state of a BodyContentImpl,
     * releasing all internal references, and preparing it for potential
     * reuse by a later invocation of {@link PageContextImpl#pushBody(Writer)}.
     *
     * <p>Note, that BodyContentImpl instances are usually owned by a
     * PageContextImpl instance, and PageContextImpl instances are recycled
     * and reused.
     *
     * @see PageContextImpl#release()
     */
    protected void recycle() {
        this.writer = null;
        try {
            this.clear();
        } catch (IOException ex) {
            // ignore
        }
    }

    private void ensureOpen() throws IOException {
        if (closed) {
            throw new IOException(Localizer.getMessage("jsp.error.stream.closed"));
        }
    }

    /**
     * Reallocates buffer since the spec requires it to be unbounded.
     */
    private void reAllocBuff(int len) {

        if (bufferSize + len <= cb.length) {
            bufferSize = cb.length;
            return;
        }

        if (len < cb.length) {
            len = cb.length;
        }

        char[] tmp = new char[cb.length + len];
        System.arraycopy(cb, 0, tmp, 0, cb.length);
        cb = tmp;
        bufferSize = cb.length;
    }


}
