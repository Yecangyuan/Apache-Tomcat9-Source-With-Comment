package org.apache.jasper.runtime;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.Method;
import java.util.Enumeration;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyContent;
import javax.servlet.jsp.tagext.BodyTag;
import javax.servlet.jsp.tagext.Tag;

import org.apache.jasper.JasperException;
import org.apache.jasper.compiler.Localizer;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.InstanceManager;

/**
 * JSP 运行时库类，提供了各种实用方法。
 * 主要用于为 useBean、getProperty 和 setProperty 操作生成的代码提供支持。
 *
 * 类中的 __begin 和 __end 标记用于让 JSP 引擎能够解析此文件，
 * 并在不希望运行时依赖此类的情况下内联这些方法。
 * 不过，目前不确定这个功能是否还能正常工作，它在某个时候被遗忘了。-akv
 *
 * @author Mandar Raje
 * @author Shawn Bayern
 */
public class JspRuntimeLibrary {

    public static final boolean GRAAL;

    static {
        boolean result = false;
        try {
            Class<?> nativeImageClazz = Class.forName("org.graalvm.nativeimage.ImageInfo");
            result = nativeImageClazz.getMethod("inImageCode").invoke(null) != null;
            // Note: This will also be true for the Graal substrate VM
        } catch (ClassNotFoundException e) {
            // Must be Graal
        } catch (ReflectiveOperationException | IllegalArgumentException e) {
            // Should never happen
        }
        GRAAL = result || System.getProperty("org.graalvm.nativeimage.imagecode") != null;
    }

    /**
     * 获取请求中的异常对象。
     * 首先尝试获取 javax.servlet.error.exception 属性，
     * 如果不存在则获取 javax.servlet.jsp.jspException 属性值。
     *
     * 此方法在 JSP 错误页面生成的 Servlet 代码开始时被调用，
     * 用于初始化 "exception" 隐式脚本语言变量。
     *
     * @param request Servlet 请求对象
     * @return 错误属性中的 Throwable 对象，如果没有则返回 null
     */
    public static Throwable getThrowable(ServletRequest request) {
        Throwable error = (Throwable) request.getAttribute(
                RequestDispatcher.ERROR_EXCEPTION);
        if (error == null) {
            error = (Throwable) request.getAttribute(PageContext.EXCEPTION);
            if (error != null) {
                /*
                 * 唯一设置 JSP_EXCEPTION 的地方是 PageContextImpl.handlePageException()。
                 * 它实际上应该设置 SERVLET_EXCEPTION，但那样会干扰 ErrorReportValve。
                 * 因此，如果设置了 JSP_EXCEPTION，我们需要设置 SERVLET_EXCEPTION。
                 */
                request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, error);
            }
        }

        return error;
    }

    /**
     * 将字符串强制转换为布尔值。
     * 如果字符串为 null 或空，则返回 false。
     *
     * @param s 要转换的字符串
     * @return 布尔值
     */
    public static boolean coerceToBoolean(String s) {
        if (s == null || s.length() == 0) {
            return false;
        } else {
            return Boolean.parseBoolean(s);
        }
    }

    /**
     * 将字符串强制转换为字节。
     * 如果字符串为 null 或空，则返回 0。
     *
     * @param s 要转换的字符串
     * @return 字节值
     */
    public static byte coerceToByte(String s) {
        if (s == null || s.length() == 0) {
            return (byte) 0;
        } else {
            return Byte.parseByte(s);
        }
    }

    /**
     * 将字符串强制转换为字符。
     * 如果字符串为 null 或空，则返回字符 0。
     *
     * @param s 要转换的字符串
     * @return 字符值
     */
    public static char coerceToChar(String s) {
        if (s == null || s.length() == 0) {
            return (char) 0;
        } else {
            return s.charAt(0);
        }
    }

    /**
     * 将字符串强制转换为双精度浮点数。
     * 如果字符串为 null 或空，则返回 0。
     *
     * @param s 要转换的字符串
     * @return 双精度浮点数值
     */
    public static double coerceToDouble(String s) {
        if (s == null || s.length() == 0) {
            return 0;
        } else {
            return Double.parseDouble(s);
        }
    }

    /**
     * 将字符串强制转换为单精度浮点数。
     * 如果字符串为 null 或空，则返回 0。
     *
     * @param s 要转换的字符串
     * @return 单精度浮点数值
     */
    public static float coerceToFloat(String s) {
        if (s == null || s.length() == 0) {
            return 0;
        } else {
            return Float.parseFloat(s);
        }
    }

    /**
     * 将字符串强制转换为整数。
     * 如果字符串为 null 或空，则返回 0。
     *
     * @param s 要转换的字符串
     * @return 整数值
     */
    public static int coerceToInt(String s) {
        if (s == null || s.length() == 0) {
            return 0;
        } else {
            return Integer.parseInt(s);
        }
    }

    /**
     * 将字符串强制转换为短整数。
     * 如果字符串为 null 或空，则返回 0。
     *
     * @param s 要转换的字符串
     * @return 短整数值
     */
    public static short coerceToShort(String s) {
        if (s == null || s.length() == 0) {
            return (short) 0;
        } else {
            return Short.parseShort(s);
        }
    }

    /**
     * 将字符串强制转换为长整数。
     * 如果字符串为 null 或空，则返回 0。
     *
     * @param s 要转换的字符串
     * @return 长整数值
     */
    public static long coerceToLong(String s) {
        if (s == null || s.length() == 0) {
            return 0;
        } else {
            return Long.parseLong(s);
        }
    }

    /**
     * 将字符串强制转换为指定类型的包装类对象。
     * 支持 Boolean、Byte、Character、Double、Float、Integer、Short、Long 类型。
     *
     * @param s 要转换的字符串
     * @param target 目标类型
     * @return 转换后的对象
     */
    public static Object coerce(String s, Class<?> target) {

        boolean isNullOrEmpty = (s == null || s.length() == 0);

        if (target == Boolean.class) {
            if (isNullOrEmpty) {
                s = "false";
            }
            return Boolean.valueOf(s);
        } else if (target == Byte.class) {
            if (isNullOrEmpty) {
                return Byte.valueOf((byte) 0);
            } else {
                return Byte.valueOf(s);
            }
        } else if (target == Character.class) {
            if (isNullOrEmpty) {
                return Character.valueOf((char) 0);
            } else {
                @SuppressWarnings("null")
                Character result = Character.valueOf(s.charAt(0));
                return result;
            }
        } else if (target == Double.class) {
            if (isNullOrEmpty) {
                return Double.valueOf(0);
            } else {
                return Double.valueOf(s);
            }
        } else if (target == Float.class) {
            if (isNullOrEmpty) {
                return Float.valueOf(0);
            } else {
                return Float.valueOf(s);
            }
        } else if (target == Integer.class) {
            if (isNullOrEmpty) {
                return Integer.valueOf(0);
            } else {
                return Integer.valueOf(s);
            }
        } else if (target == Short.class) {
            if (isNullOrEmpty) {
                return Short.valueOf((short) 0);
            } else {
                return Short.valueOf(s);
            }
        } else if (target == Long.class) {
            if (isNullOrEmpty) {
                return Long.valueOf(0);
            } else {
                return Long.valueOf(s);
            }
        } else {
            return null;
        }
    }

   // __begin convertMethod
    /**
     * 将字符串转换为指定类型的对象。
     * 支持基本类型及其包装类，以及使用 PropertyEditor 进行自定义转换。
     *
     * @param propertyName 属性名称
     * @param s 要转换的字符串
     * @param t 目标类型
     * @param propertyEditorClass 属性编辑器类
     * @return 转换后的对象
     * @throws JasperException 转换失败时抛出
     */
    public static Object convert(String propertyName, String s, Class<?> t,
            Class<?> propertyEditorClass)
       throws JasperException
    {
        try {
            if (s == null) {
                if (t.equals(Boolean.class) || t.equals(Boolean.TYPE)) {
                    s = "false";
                } else {
                    return null;
                }
            }
            if (propertyEditorClass != null) {
                return getValueFromBeanInfoPropertyEditor(
                                    t, propertyName, s, propertyEditorClass);
            } else if (t.equals(Boolean.class) || t.equals(Boolean.TYPE)) {
                return Boolean.valueOf(s);
            } else if (t.equals(Byte.class) || t.equals(Byte.TYPE)) {
                if (s.length() == 0) {
                    return Byte.valueOf((byte)0);
                } else {
                    return Byte.valueOf(s);
                }
            } else if (t.equals(Character.class) || t.equals(Character.TYPE)) {
                if (s.length() == 0) {
                    return Character.valueOf((char) 0);
                } else {
                    return Character.valueOf(s.charAt(0));
                }
            } else if (t.equals(Double.class) || t.equals(Double.TYPE)) {
                if (s.length() == 0) {
                    return Double.valueOf(0);
                } else {
                    return Double.valueOf(s);
                }
            } else if (t.equals(Integer.class) || t.equals(Integer.TYPE)) {
                if (s.length() == 0) {
                    return Integer.valueOf(0);
                } else {
                    return Integer.valueOf(s);
                }
            } else if (t.equals(Float.class) || t.equals(Float.TYPE)) {
                if (s.length() == 0) {
                    return Float.valueOf(0);
                } else {
                    return Float.valueOf(s);
                }
            } else if (t.equals(Long.class) || t.equals(Long.TYPE)) {
                if (s.length() == 0) {
                    return Long.valueOf(0);
                } else {
                    return Long.valueOf(s);
                }
            } else if (t.equals(Short.class) || t.equals(Short.TYPE)) {
                if (s.length() == 0) {
                    return Short.valueOf((short) 0);
                } else {
                    return Short.valueOf(s);
                }
            } else if ( t.equals(String.class) ) {
                return s;
            } else if (t.getName().equals("java.lang.Object")) {
                return new String(s);
            } else {
                return getValueFromPropertyEditorManager(
                                            t, propertyName, s);
            }
        } catch (Exception ex) {
            throw new JasperException(ex);
        }
    }
    // __end convertMethod

    // __begin introspectMethod
    /**
     * 对 JavaBean 进行自省，根据请求参数设置属性值。
     *
     * @param bean 要进行自省的 JavaBean
     * @param request Servlet 请求对象
     * @throws JasperException 处理失败时抛出
     */
    public static void introspect(Object bean, ServletRequest request)
                                  throws JasperException
    {
        Enumeration<String> e = request.getParameterNames();
        while ( e.hasMoreElements() ) {
            String name  = e.nextElement();
            String value = request.getParameter(name);
            introspecthelper(bean, name, value, request, name, true);
        }
    }
    // __end introspectMethod

    // __begin introspecthelperMethod
    /**
     * 辅助方法，用于对 JavaBean 进行自省并设置属性值。
     * 支持数组类型的属性和单个属性。
     *
     * @param bean 目标 JavaBean
     * @param prop 属性名称
     * @param value 属性值
     * @param request Servlet 请求对象
     * @param param 参数名称
     * @param ignoreMethodNF 是否忽略方法未找到的异常
     * @throws JasperException 处理失败时抛出
     */
    public static void introspecthelper(Object bean, String prop,
                                        String value, ServletRequest request,
                                        String param, boolean ignoreMethodNF)
                                        throws JasperException {
        Method method = null;
        Class<?> type = null;
        Class<?> propertyEditorClass = null;
        try {
            if (GRAAL) {
                method = getWriteMethod(bean.getClass(), prop);
                if (method.getParameterTypes().length > 0) {
                    type = method.getParameterTypes()[0];
                }
            } else {
                java.beans.BeanInfo info
                = java.beans.Introspector.getBeanInfo(bean.getClass());
                if ( info != null ) {
                    java.beans.PropertyDescriptor pd[]
                            = info.getPropertyDescriptors();
                    for (java.beans.PropertyDescriptor propertyDescriptor : pd) {
                        if (propertyDescriptor.getName().equals(prop)) {
                            method = propertyDescriptor.getWriteMethod();
                            type = propertyDescriptor.getPropertyType();
                            propertyEditorClass = propertyDescriptor.getPropertyEditorClass();
                            break;
                        }
                    }
                }
            }
            if (method != null && type != null) {
                if (type.isArray()) {
                    if (request == null) {
                        throw new JasperException(
                            Localizer.getMessage("jsp.error.beans.setproperty.noindexset"));
                    }
                    Class<?> t = type.getComponentType();
                    String[] values = request.getParameterValues(param);
                    //XXX Please check.
                    if(values == null) {
                        return;
                    }
                    if(t.equals(String.class)) {
                        method.invoke(bean, new Object[] { values });
                    } else {
                        createTypedArray (prop, bean, method, values, t,
                                          propertyEditorClass);
                    }
                } else {
                    if(value == null || (param != null && value.equals(""))) {
                        return;
                    }
                    Object oval = convert(prop, value, type, propertyEditorClass);
                    if ( oval != null ) {
                        method.invoke(bean, new Object[] { oval });
                    }
                }
            }
        } catch (Exception ex) {
            Throwable thr = ExceptionUtils.unwrapInvocationTargetException(ex);
            ExceptionUtils.handleThrowable(thr);
            throw new JasperException(ex);
        }
        if (!ignoreMethodNF && (method == null)) {
            if (type == null) {
                throw new JasperException(
                    Localizer.getMessage("jsp.error.beans.noproperty",
                                         prop,
                                         bean.getClass().getName()));
            } else {
                throw new JasperException(
                    Localizer.getMessage("jsp.error.beans.nomethod.setproperty",
                                         prop,
                                         type.getName(),
                                         bean.getClass().getName()));
            }
        }
    }
    // __end introspecthelperMethod

    //-------------------------------------------------------------------
    // functions to convert builtin Java data types to string.
    //-------------------------------------------------------------------
    // __begin toStringMethod
    /**
     * 将对象转换为字符串。
     *
     * @param o 要转换的对象
     * @return 字符串表示
     */
    public static String toString(Object o) {
        return String.valueOf(o);
    }

    /**
     * 将字节转换为字符串。
     *
     * @param b 字节值
     * @return 字符串表示
     */
    public static String toString(byte b) {
        return Byte.toString(b);
    }

    /**
     * 将布尔值转换为字符串。
     *
     * @param b 布尔值
     * @return 字符串表示
     */
    public static String toString(boolean b) {
        return Boolean.toString(b);
    }

    /**
     * 将短整数转换为字符串。
     *
     * @param s 短整数值
     * @return 字符串表示
     */
    public static String toString(short s) {
        return Short.toString(s);
    }

    /**
     * 将整数转换为字符串。
     *
     * @param i 整数值
     * @return 字符串表示
     */
    public static String toString(int i) {
        return Integer.toString(i);
    }

    /**
     * 将单精度浮点数转换为字符串。
     *
     * @param f 单精度浮点数值
     * @return 字符串表示
     */
    public static String toString(float f) {
        return Float.toString(f);
    }

    /**
     * 将长整数转换为字符串。
     *
     * @param l 长整数值
     * @return 字符串表示
     */
    public static String toString(long l) {
        return Long.toString(l);
    }

    /**
     * 将双精度浮点数转换为字符串。
     *
     * @param d 双精度浮点数值
     * @return 字符串表示
     */
    public static String toString(double d) {
        return Double.toString(d);
    }

    /**
     * 将字符转换为字符串。
     *
     * @param c 字符值
     * @return 字符串表示
     */
    public static String toString(char c) {
        return Character.toString(c);
    }
    // __end toStringMethod


    /**
     * Create a typed array.
     * This is a special case where params are passed through
     * the request and the property is indexed.
     * @param propertyName The property name
     * @param bean The bean
     * @param method The method
     * @param values Array values
     * @param t The class
     * @param propertyEditorClass The editor for the property
     * @throws JasperException An error occurred
     */
    public static void createTypedArray(String propertyName,
                                        Object bean,
                                        Method method,
                                        String[] values,
                                        Class<?> t,
                                        Class<?> propertyEditorClass)
                throws JasperException {

        try {
            if (propertyEditorClass != null) {
                Object[] tmpval = new Integer[values.length];
                for (int i=0; i<values.length; i++) {
                    tmpval[i] = getValueFromBeanInfoPropertyEditor(
                            t, propertyName, values[i], propertyEditorClass);
                }
                method.invoke (bean, new Object[] {tmpval});
            } else if (t.equals(Integer.class)) {
                Integer []tmpval = new Integer[values.length];
                for (int i = 0 ; i < values.length; i++) {
                    tmpval[i] =  Integer.valueOf(values[i]);
                }
                method.invoke (bean, new Object[] {tmpval});
            } else if (t.equals(Byte.class)) {
                Byte[] tmpval = new Byte[values.length];
                for (int i = 0 ; i < values.length; i++) {
                    tmpval[i] = Byte.valueOf(values[i]);
                }
                method.invoke (bean, new Object[] {tmpval});
            } else if (t.equals(Boolean.class)) {
                Boolean[] tmpval = new Boolean[values.length];
                for (int i = 0 ; i < values.length; i++) {
                    tmpval[i] = Boolean.valueOf(values[i]);
                }
                method.invoke (bean, new Object[] {tmpval});
            } else if (t.equals(Short.class)) {
                Short[] tmpval = new Short[values.length];
                for (int i = 0 ; i < values.length; i++) {
                    tmpval[i] = Short.valueOf(values[i]);
                }
                method.invoke (bean, new Object[] {tmpval});
            } else if (t.equals(Long.class)) {
                Long[] tmpval = new Long[values.length];
                for (int i = 0 ; i < values.length; i++) {
                    tmpval[i] = Long.valueOf(values[i]);
                }
                method.invoke (bean, new Object[] {tmpval});
            } else if (t.equals(Double.class)) {
                Double[] tmpval = new Double[values.length];
                for (int i = 0 ; i < values.length; i++) {
                    tmpval[i] = Double.valueOf(values[i]);
                }
                method.invoke (bean, new Object[] {tmpval});
            } else if (t.equals(Float.class)) {
                Float[] tmpval = new Float[values.length];
                for (int i = 0 ; i < values.length; i++) {
                    tmpval[i] = Float.valueOf(values[i]);
                }
                method.invoke (bean, new Object[] {tmpval});
            } else if (t.equals(Character.class)) {
                Character[] tmpval = new Character[values.length];
                for (int i = 0 ; i < values.length; i++) {
                    tmpval[i] = Character.valueOf(values[i].charAt(0));
                }
                method.invoke (bean, new Object[] {tmpval});
            } else if (t.equals(int.class)) {
                int []tmpval = new int[values.length];
                for (int i = 0 ; i < values.length; i++) {
                    tmpval[i] = Integer.parseInt (values[i]);
                }
                method.invoke (bean, new Object[] {tmpval});
            } else if (t.equals(byte.class)) {
                byte[] tmpval = new byte[values.length];
                for (int i = 0 ; i < values.length; i++) {
                    tmpval[i] = Byte.parseByte (values[i]);
                }
                method.invoke (bean, new Object[] {tmpval});
            } else if (t.equals(boolean.class)) {
                boolean[] tmpval = new boolean[values.length];
                for (int i = 0 ; i < values.length; i++) {
                    tmpval[i] = Boolean.parseBoolean(values[i]);
                }
                method.invoke (bean, new Object[] {tmpval});
            } else if (t.equals(short.class)) {
                short[] tmpval = new short[values.length];
                for (int i = 0 ; i < values.length; i++) {
                    tmpval[i] = Short.parseShort (values[i]);
                }
                method.invoke (bean, new Object[] {tmpval});
            } else if (t.equals(long.class)) {
                long[] tmpval = new long[values.length];
                for (int i = 0 ; i < values.length; i++) {
                    tmpval[i] = Long.parseLong (values[i]);
                }
                method.invoke (bean, new Object[] {tmpval});
            } else if (t.equals(double.class)) {
                double[] tmpval = new double[values.length];
                for (int i = 0 ; i < values.length; i++) {
                    tmpval[i] = Double.parseDouble(values[i]);
                }
                method.invoke (bean, new Object[] {tmpval});
            } else if (t.equals(float.class)) {
                float[] tmpval = new float[values.length];
                for (int i = 0 ; i < values.length; i++) {
                    tmpval[i] = Float.parseFloat(values[i]);
                }
                method.invoke (bean, new Object[] {tmpval});
            } else if (t.equals(char.class)) {
                char[] tmpval = new char[values.length];
                for (int i = 0 ; i < values.length; i++) {
                    tmpval[i] = values[i].charAt(0);
                }
                method.invoke (bean, new Object[] {tmpval});
            } else {
                Object[] tmpval = new Integer[values.length];
                for (int i=0; i<values.length; i++) {
                    tmpval[i] =
                        getValueFromPropertyEditorManager(
                                            t, propertyName, values[i]);
                }
                method.invoke (bean, new Object[] {tmpval});
            }
        } catch (RuntimeException | ReflectiveOperationException ex) {
            Throwable thr = ExceptionUtils.unwrapInvocationTargetException(ex);
            ExceptionUtils.handleThrowable(thr);
            throw new JasperException ("error in invoking method", ex);
        }
    }

    /**
     * Escape special shell characters.
     * @param unescString The string to shell-escape
     * @return The escaped shell string.
     */
    public static String escapeQueryString(String unescString) {
        if (unescString == null) {
            return null;
        }

        StringBuilder escStringBuilder = new StringBuilder();
        String shellSpChars = "&;`'\"|*?~<>^()[]{}$\\\n";

        for (int index = 0; index < unescString.length(); index++) {
            char nextChar = unescString.charAt(index);

            if (shellSpChars.indexOf(nextChar) != -1) {
                escStringBuilder.append('\\');
            }

            escStringBuilder.append(nextChar);
        }
        return escStringBuilder.toString();
    }

    // __begin lookupReadMethodMethod
    /**
     * 处理获取属性的操作。
     * 通过反射调用 JavaBean 的 getter 方法获取属性值。
     *
     * @param o 目标对象
     * @param prop 属性名称
     * @return 属性值
     * @throws JasperException 处理失败时抛出
     */
    public static Object handleGetProperty(Object o, String prop)
    throws JasperException {
        if (o == null) {
            throw new JasperException(
                    Localizer.getMessage("jsp.error.beans.nullbean"));
        }
        Object value = null;
        try {
            Method method = getReadMethod(o.getClass(), prop);
            value = method.invoke(o, (Object[]) null);
        } catch (Exception ex) {
            Throwable thr = ExceptionUtils.unwrapInvocationTargetException(ex);
            ExceptionUtils.handleThrowable(thr);
            throw new JasperException (ex);
        }
        return value;
    }
    // __end lookupReadMethodMethod

    /**
     * 处理带有 EL 表达式的 setProperty 操作。
     * 用于处理 <jsp:setProperty> 标签中 value 属性为 EL 表达式的情况。
     *
     * @param bean 目标 JavaBean
     * @param prop 属性名称
     * @param expression EL 表达式
     * @param pageContext 页面上下文
     * @param functionMapper 函数映射器
     * @throws JasperException 处理失败时抛出
     */
    public static void handleSetPropertyExpression(Object bean,
        String prop, String expression, PageContext pageContext,
        ProtectedFunctionMapper functionMapper )
        throws JasperException
    {
        try {
            Method method = getWriteMethod(bean.getClass(), prop);
            method.invoke(bean, new Object[] {
                PageContextImpl.proprietaryEvaluate(
                    expression,
                    method.getParameterTypes()[0],
                    pageContext,
                    functionMapper)
            });
        } catch (Exception ex) {
            Throwable thr = ExceptionUtils.unwrapInvocationTargetException(ex);
            ExceptionUtils.handleThrowable(thr);
            throw new JasperException(ex);
        }
    }

    /**
     * 处理 setProperty 操作，设置对象属性值。
     *
     * @param bean 目标 JavaBean
     * @param prop 属性名称
     * @param value 属性值
     * @throws JasperException 处理失败时抛出
     */
    public static void handleSetProperty(Object bean, String prop,
                                         Object value)
        throws JasperException
    {
        try {
            Method method = getWriteMethod(bean.getClass(), prop);
            method.invoke(bean, new Object[] { value });
        } catch (Exception ex) {
            Throwable thr = ExceptionUtils.unwrapInvocationTargetException(ex);
            ExceptionUtils.handleThrowable(thr);
            throw new JasperException(ex);
        }
    }

    /**
     * 处理 setProperty 操作，设置整数属性值。
     *
     * @param bean 目标 JavaBean
     * @param prop 属性名称
     * @param value 整数值
     * @throws JasperException 处理失败时抛出
     */
    public static void handleSetProperty(Object bean, String prop,
                                         int value)
        throws JasperException
    {
        try {
            Method method = getWriteMethod(bean.getClass(), prop);
            method.invoke(bean, new Object[] { Integer.valueOf(value) });
        } catch (Exception ex) {
            Throwable thr = ExceptionUtils.unwrapInvocationTargetException(ex);
            ExceptionUtils.handleThrowable(thr);
            throw new JasperException(ex);
        }
    }

    /**
     * 处理 setProperty 操作，设置短整数属性值。
     *
     * @param bean 目标 JavaBean
     * @param prop 属性名称
     * @param value 短整数值
     * @throws JasperException 处理失败时抛出
     */
    public static void handleSetProperty(Object bean, String prop,
                                         short value)
        throws JasperException
    {
        try {
            Method method = getWriteMethod(bean.getClass(), prop);
            method.invoke(bean, new Object[] { Short.valueOf(value) });
        } catch (Exception ex) {
            Throwable thr = ExceptionUtils.unwrapInvocationTargetException(ex);
            ExceptionUtils.handleThrowable(thr);
            throw new JasperException(ex);
        }
    }

    /**
     * 处理 setProperty 操作，设置长整数属性值。
     *
     * @param bean 目标 JavaBean
     * @param prop 属性名称
     * @param value 长整数值
     * @throws JasperException 处理失败时抛出
     */
    public static void handleSetProperty(Object bean, String prop,
                                         long value)
        throws JasperException
    {
        try {
            Method method = getWriteMethod(bean.getClass(), prop);
            method.invoke(bean, new Object[] { Long.valueOf(value) });
        } catch (Exception ex) {
            Throwable thr = ExceptionUtils.unwrapInvocationTargetException(ex);
            ExceptionUtils.handleThrowable(thr);
            throw new JasperException(ex);
        }
    }

    /**
     * 处理 setProperty 操作，设置双精度浮点数属性值。
     *
     * @param bean 目标 JavaBean
     * @param prop 属性名称
     * @param value 双精度浮点数值
     * @throws JasperException 处理失败时抛出
     */
    public static void handleSetProperty(Object bean, String prop,
                                         double value)
        throws JasperException
    {
        try {
            Method method = getWriteMethod(bean.getClass(), prop);
            method.invoke(bean, new Object[] { Double.valueOf(value) });
        } catch (Exception ex) {
            Throwable thr = ExceptionUtils.unwrapInvocationTargetException(ex);
            ExceptionUtils.handleThrowable(thr);
            throw new JasperException(ex);
        }
    }

    /**
     * 处理 setProperty 操作，设置单精度浮点数属性值。
     *
     * @param bean 目标 JavaBean
     * @param prop 属性名称
     * @param value 单精度浮点数值
     * @throws JasperException 处理失败时抛出
     */
    public static void handleSetProperty(Object bean, String prop,
                                         float value)
        throws JasperException
    {
        try {
            Method method = getWriteMethod(bean.getClass(), prop);
            method.invoke(bean, new Object[] { Float.valueOf(value) });
        } catch (Exception ex) {
            Throwable thr = ExceptionUtils.unwrapInvocationTargetException(ex);
            ExceptionUtils.handleThrowable(thr);
            throw new JasperException(ex);
        }
    }

    /**
     * 处理 setProperty 操作，设置字符属性值。
     *
     * @param bean 目标 JavaBean
     * @param prop 属性名称
     * @param value 字符值
     * @throws JasperException 处理失败时抛出
     */
    public static void handleSetProperty(Object bean, String prop,
                                         char value)
        throws JasperException
    {
        try {
            Method method = getWriteMethod(bean.getClass(), prop);
            method.invoke(bean, new Object[] { Character.valueOf(value) });
        } catch (Exception ex) {
            Throwable thr = ExceptionUtils.unwrapInvocationTargetException(ex);
            ExceptionUtils.handleThrowable(thr);
            throw new JasperException(ex);
        }
    }

    /**
     * 处理 setProperty 操作，设置字节属性值。
     *
     * @param bean 目标 JavaBean
     * @param prop 属性名称
     * @param value 字节值
     * @throws JasperException 处理失败时抛出
     */
    public static void handleSetProperty(Object bean, String prop,
                                         byte value)
        throws JasperException
    {
        try {
            Method method = getWriteMethod(bean.getClass(), prop);
            method.invoke(bean, new Object[] { Byte.valueOf(value) });
        } catch (Exception ex) {
            Throwable thr = ExceptionUtils.unwrapInvocationTargetException(ex);
            ExceptionUtils.handleThrowable(thr);
            throw new JasperException(ex);
        }
    }

    /**
     * 处理 setProperty 操作，设置布尔属性值。
     *
     * @param bean 目标 JavaBean
     * @param prop 属性名称
     * @param value 布尔值
     * @throws JasperException 处理失败时抛出
     */
    public static void handleSetProperty(Object bean, String prop,
                                         boolean value)
        throws JasperException
    {
        try {
            Method method = getWriteMethod(bean.getClass(), prop);
            method.invoke(bean, new Object[] { Boolean.valueOf(value) });
        } catch (Exception ex) {
            Throwable thr = ExceptionUtils.unwrapInvocationTargetException(ex);
            ExceptionUtils.handleThrowable(thr);
            throw new JasperException(ex);
        }
    }

    /**
     * Reverse of Introspector.decapitalize.
     * @param name The name
     * @return the capitalized string
     */
    public static String capitalize(String name) {
        if (name == null || name.length() == 0) {
            return name;
        }
        char chars[] = name.toCharArray();
        chars[0] = Character.toUpperCase(chars[0]);
        return new String(chars);
    }

    /**
     * 获取 JavaBean 的写方法（setter）。
     *
     * @param beanClass JavaBean 类
     * @param prop 属性名称
     * @return 写方法 Method 对象
     * @throws JasperException 方法未找到时抛出
     */
    public static Method getWriteMethod(Class<?> beanClass, String prop)
            throws JasperException {
        Method result = null;
        Class<?> type = null;
        if (GRAAL) {
            String setter = "set" + capitalize(prop);
            Method methods[] = beanClass.getMethods();
            for (Method method : methods) {
                if (setter.equals(method.getName())) {
                    return method;
                }
            }
        } else {
            try {
                java.beans.BeanInfo info = java.beans.Introspector.getBeanInfo(beanClass);
                java.beans.PropertyDescriptor pd[] = info.getPropertyDescriptors();
                for (java.beans.PropertyDescriptor propertyDescriptor : pd) {
                    if (propertyDescriptor.getName().equals(prop)) {
                        result = propertyDescriptor.getWriteMethod();
                        type = propertyDescriptor.getPropertyType();
                        break;
                    }
                }
            } catch (Exception ex) {
                throw new JasperException (ex);
            }
        }
        if (result == null) {
            if (type == null) {
                throw new JasperException(Localizer.getMessage(
                        "jsp.error.beans.noproperty", prop, beanClass.getName()));
            } else {
                throw new JasperException(Localizer.getMessage(
                        "jsp.error.beans.nomethod.setproperty",
                        prop, type.getName(), beanClass.getName()));
            }
        }
        return result;
    }

    /**
     * 获取 JavaBean 的读方法（getter）。
     *
     * @param beanClass JavaBean 类
     * @param prop 属性名称
     * @return 读方法 Method 对象
     * @throws JasperException 方法未找到时抛出
     */
    public static Method getReadMethod(Class<?> beanClass, String prop)
            throws JasperException {
        Method result = null;
        Class<?> type = null;
        if (GRAAL) {
            String setter = "get" + capitalize(prop);
            Method methods[] = beanClass.getMethods();
            for (Method method : methods) {
                if (setter.equals(method.getName())) {
                    return method;
                }
            }
        } else {
            try {
                java.beans.BeanInfo info = java.beans.Introspector.getBeanInfo(beanClass);
                java.beans.PropertyDescriptor pd[] = info.getPropertyDescriptors();
                for (java.beans.PropertyDescriptor propertyDescriptor : pd) {
                    if (propertyDescriptor.getName().equals(prop)) {
                        result = propertyDescriptor.getReadMethod();
                        type = propertyDescriptor.getPropertyType();
                        break;
                    }
                }
            } catch (Exception ex) {
                throw new JasperException (ex);
            }
        }
        if (result == null) {
            if (type == null) {
                throw new JasperException(Localizer.getMessage(
                        "jsp.error.beans.noproperty", prop, beanClass.getName()));
            } else {
                throw new JasperException(Localizer.getMessage(
                        "jsp.error.beans.nomethod", prop, beanClass.getName()));
            }
        }
        return result;
    }

    //*********************************************************************
    // PropertyEditor Support

    /**
     * 使用指定的 PropertyEditor 类从字符串获取值。
     *
     * @param attrClass 属性类型
     * @param attrName 属性名称
     * @param attrValue 属性字符串值
     * @param propertyEditorClass PropertyEditor 类
     * @return 转换后的对象
     * @throws JasperException 转换失败时抛出
     */
    public static Object getValueFromBeanInfoPropertyEditor(
                           Class<?> attrClass, String attrName, String attrValue,
                           Class<?> propertyEditorClass)
        throws JasperException
    {
        try {
            PropertyEditor pe = (PropertyEditor)propertyEditorClass.getConstructor().newInstance();
            pe.setAsText(attrValue);
            return pe.getValue();
        } catch (Exception ex) {
            if (attrValue.length() == 0) {
                return null;
            } else {
                throw new JasperException(
                    Localizer.getMessage("jsp.error.beans.property.conversion",
                                         attrValue, attrClass.getName(), attrName,
                                         ex.getMessage()));
            }
        }
    }

    /**
     * 使用 PropertyEditorManager 从字符串获取值。
     *
     * @param attrClass 属性类型
     * @param attrName 属性名称
     * @param attrValue 属性字符串值
     * @return 转换后的对象
     * @throws JasperException 转换失败时抛出
     */
    public static Object getValueFromPropertyEditorManager(
                     Class<?> attrClass, String attrName, String attrValue)
        throws JasperException
    {
        try {
            PropertyEditor propEditor =
                PropertyEditorManager.findEditor(attrClass);
            if (propEditor != null) {
                propEditor.setAsText(attrValue);
                return propEditor.getValue();
            } else if (attrValue.length() == 0) {
                return null;
            } else {
                throw new IllegalArgumentException(
                    Localizer.getMessage("jsp.error.beans.propertyeditor.notregistered"));
            }
        } catch (IllegalArgumentException ex) {
            if (attrValue.length() == 0) {
                return null;
            } else {
                throw new JasperException(
                    Localizer.getMessage("jsp.error.beans.property.conversion",
                                         attrValue, attrClass.getName(), attrName,
                                         ex.getMessage()));
            }
        }
    }


    // ************************************************************************
    // General Purpose Runtime Methods
    // ************************************************************************


    /**
     * 将可能相对的路径转换为以 '/' 开头的上下文相对路径。
     *
     * @param request 正在处理的 Servlet 请求
     * @param relativePath 可能相对的资源路径
     * @return 绝对路径
     */
    public static String getContextRelativePath(ServletRequest request,
                                                String relativePath) {

        if (relativePath.startsWith("/")) {
            return relativePath;
        }
        if (!(request instanceof HttpServletRequest)) {
            return relativePath;
        }
        HttpServletRequest hrequest = (HttpServletRequest) request;
        String uri = (String) request.getAttribute(
                RequestDispatcher.INCLUDE_SERVLET_PATH);
        if (uri != null) {
            String pathInfo = (String)
                request.getAttribute(RequestDispatcher.INCLUDE_PATH_INFO);
            if (pathInfo == null) {
                if (uri.lastIndexOf('/') >= 0) {
                    uri = uri.substring(0, uri.lastIndexOf('/'));
                }
            }
        } else {
            uri = hrequest.getServletPath();
            if (uri.lastIndexOf('/') >= 0) {
                uri = uri.substring(0, uri.lastIndexOf('/'));
            }
        }
        return uri + '/' + relativePath;

    }


    /**
     * 执行 RequestDispatcher.include() 操作，可选择在之前刷新响应。
     *
     * @param request 正在处理的 Servlet 请求
     * @param response 正在处理的 Servlet 响应
     * @param relativePath 要包含的资源的相对路径
     * @param out 当前正在写入的 Writer
     * @param flush 是否在包含处理之前刷新
     *
     * @exception IOException 如果被包含的 Servlet 抛出
     * @exception ServletException 如果被包含的 Servlet 抛出
     */
    public static void include(ServletRequest request,
                               ServletResponse response,
                               String relativePath,
                               JspWriter out,
                               boolean flush)
        throws IOException, ServletException {

        if (flush && !(out instanceof BodyContent)) {
            out.flush();
        }

        // FIXME - It is tempting to use request.getRequestDispatcher() to
        // resolve a relative path directly, but Catalina currently does not
        // take into account whether the caller is inside a RequestDispatcher
        // include or not.  Whether Catalina *should* take that into account
        // is a spec issue currently under review.  In the mean time,
        // replicate Jasper's previous behavior

        String resourcePath = getContextRelativePath(request, relativePath);
        RequestDispatcher rd = request.getRequestDispatcher(resourcePath);
        if (rd != null) {
            rd.include(request,
                    new ServletResponseWrapperInclude(response, out));
        } else {
            throw new JasperException(
                    Localizer.getMessage("jsp.error.include.exception", resourcePath));
        }

    }

    /**
     * 对字符串进行 URL 编码，基于提供的字符编码。
     * 此方法执行与 J2SDK1.4 中的 java.net.URLEncoder.encode 相同的功能。
     *
     * @param s 要进行 URL 编码的字符串
     * @param enc 字符编码
     * @return URL 编码后的字符串
     */
    public static String URLEncode(String s, String enc) {

        if (s == null) {
            return "null";
        }

        if (enc == null) {
            enc = "ISO-8859-1";        // The default request encoding
        }

        StringBuilder out = new StringBuilder(s.length());
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        OutputStreamWriter writer = null;
        try {
            writer = new OutputStreamWriter(buf, enc);
        } catch (java.io.UnsupportedEncodingException ex) {
            // Use the default encoding?
            writer = new OutputStreamWriter(buf);
        }

        for (int i = 0; i < s.length(); i++) {
            int c = s.charAt(i);
            if (c == ' ') {
                out.append('+');
            } else if (isSafeChar(c)) {
                out.append((char)c);
            } else {
                // convert to external encoding before hex conversion
                try {
                    writer.write(c);
                    writer.flush();
                } catch(IOException e) {
                    buf.reset();
                    continue;
                }
                byte[] ba = buf.toByteArray();
                for (byte b : ba) {
                    out.append('%');
                    // Converting each byte in the buffer
                    out.append(Character.forDigit((b >> 4) & 0xf, 16));
                    out.append(Character.forDigit(b & 0xf, 16));
                }
                buf.reset();
            }
        }
        return out.toString();
    }

    private static boolean isSafeChar(int c) {
        if (c >= 'a' && c <= 'z') {
            return true;
        }
        if (c >= 'A' && c <= 'Z') {
            return true;
        }
        if (c >= '0' && c <= '9') {
            return true;
        }
        if (c == '-' || c == '_' || c == '.' || c == '!' ||
            c == '~' || c == '*' || c == '\'' || c == '(' || c == ')') {
            return true;
        }
        return false;
    }


    /**
     * 开始缓冲标签体内容。
     * 为 BodyTag 设置 BodyContent 并调用 doInitBody()。
     *
     * @param pageContext 页面上下文
     * @param tag BodyTag 标签
     * @return BodyContent 对象
     * @throws JspException JSP 异常
     */
    public static JspWriter startBufferedBody(PageContext pageContext, BodyTag tag)
            throws JspException {
        BodyContent out = pageContext.pushBody();
        tag.setBodyContent(out);
        tag.doInitBody();
        return out;
    }


    /**
     * 释放标签实例。
     * 根据 reused 参数决定是否释放标签。
     *
     * @param tag 要释放的标签
     * @param instanceManager 实例管理器
     * @param reused 是否重用
     */
    public static void releaseTag(Tag tag, InstanceManager instanceManager, boolean reused) {
        // Caller ensures pool is non-null if reuse is true
        if (!reused) {
            releaseTag(tag, instanceManager);
        }
    }


    protected static void releaseTag(Tag tag, InstanceManager instanceManager) {
        try {
            tag.release();
        } catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            Log log = LogFactory.getLog(JspRuntimeLibrary.class);
            log.warn(Localizer.getMessage("jsp.warning.tagRelease", tag.getClass().getName()), t);
        }
        try {
            instanceManager.destroyInstance(tag);
        } catch (Exception e) {
            Throwable t = ExceptionUtils.unwrapInvocationTargetException(e);
            ExceptionUtils.handleThrowable(t);
            Log log = LogFactory.getLog(JspRuntimeLibrary.class);
            log.warn(Localizer.getMessage("jsp.warning.tagPreDestroy", tag.getClass().getName()), t);
        }

    }
}
