package org.apache.jasper.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.CodeSource;
import java.security.PermissionCollection;

import org.apache.jasper.Constants;

/**
 * 用于加载Servlet类文件（对应JSP文件）和标签处理器类文件（对应标签文件）的类加载器。
 * <p>
 * JasperLoader 继承自 URLClassLoader，专门为JSP页面编译后的Servlet类提供服务。
 * 它处理与JSP相关的类加载逻辑，包括安全权限检查、类加载委托策略等。
 * 对于非 org.apache.jsp 包下的类，会委托给父类加载器加载。
 * </p>
 *
 * @author Anil K. Vijendran
 * @author Harish Prabandham
 */
public class JasperLoader extends URLClassLoader {

    /** 权限集合，用于安全沙箱控制 */
    private final PermissionCollection permissionCollection;
    /** 安全管理器实例 */
    private final SecurityManager securityManager;

    /**
     * 构造JasperLoader实例。
     *
     * @param urls 用于查找类文件的URL数组
     * @param parent 父类加载器
     * @param permissionCollection 权限集合，用于控制代码执行权限
     */
    public JasperLoader(URL[] urls, ClassLoader parent,
                        PermissionCollection permissionCollection) {
        super(urls, parent);
        this.permissionCollection = permissionCollection;
        this.securityManager = System.getSecurityManager();
    }

    /**
     * 加载指定名称的类。此方法以与 <code>loadClass(String, boolean)</code>
     * 相同的方式搜索类，但将第二个参数设置为 <code>false</code>。
     *
     * @param name 要加载的类的完全限定名
     * @return 加载的Class对象
     * @throws ClassNotFoundException 如果找不到该类
     */
    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return loadClass(name, false);
    }

    /**
     * 加载指定名称的类，使用以下算法进行搜索直到找到并返回类。
     * 如果找不到类，则抛出 <code>ClassNotFoundException</code>。
     * <ul>
     * <li>调用 <code>findLoadedClass(String)</code> 检查类是否已加载。
     *     如果已加载，返回相同的 <code>Class</code> 对象。</li>
     * <li>进行安全管理器权限检查（如果启用了SecurityManager）。</li>
     * <li>如果类名不以 org.apache.jsp 开头，委托给父类加载器加载。</li>
     * <li>否则，调用 <code>findClass()</code> 在本地仓库中查找类。</li>
     * </ul>
     * 如果使用上述步骤找到类，且 <code>resolve</code> 标志为 <code>true</code>，
     * 则此方法将调用 <code>resolveClass(Class)</code> 来解析类。
     *
     * @param name 要加载的类的完全限定名
     * @param resolve 如果为 <code>true</code>，则解析类
     * @return 加载的Class对象
     * @throws ClassNotFoundException 如果找不到该类
     */
    @Override
    public synchronized Class<?> loadClass(final String name, boolean resolve)
        throws ClassNotFoundException {

        Class<?> clazz = null;

        // (0) 检查已加载的类缓存
        clazz = findLoadedClass(name);
        if (clazz != null) {
            if (resolve) {
                resolveClass(clazz);
            }
            return clazz;
        }

        // (.5) 使用SecurityManager时检查访问权限
        if (securityManager != null) {
            int dot = name.lastIndexOf('.');
            if (dot >= 0) {
                try {
                    // 默认情况下授予 org.apache.jasper.runtime 包的访问权限
                    if (!"org.apache.jasper.runtime".equalsIgnoreCase(name.substring(0,dot))){
                        securityManager.checkPackageAccess(name.substring(0,dot));
                    }
                } catch (SecurityException se) {
                    String error = "Security Violation, attempt to use " +
                        "Restricted Class: " + name;
                    se.printStackTrace();
                    throw new ClassNotFoundException(error);
                }
            }
        }

        if( !name.startsWith(Constants.JSP_PACKAGE_NAME + '.') ) {
            // 类不在 org.apache.jsp 包中，委托给父类加载器加载
            clazz = getParent().loadClass(name);
            if( resolve ) {
                resolveClass(clazz);
            }
            return clazz;
        }

        return findClass(name);
    }


    /**
     * 获取指定名称资源的输入流。
     * <p>
     * 首先尝试从父类加载器获取资源流，如果失败，
     * 则在本地URL中查找资源并打开流。
     * </p>
     *
     * @param name 资源名称
     * @return 资源的输入流，如果找不到则返回 null
     * @see java.lang.ClassLoader#getResourceAsStream(String)
     */
    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream is = getParent().getResourceAsStream(name);
        if (is == null) {
            URL url = findResource(name);
            if (url != null) {
                try {
                    is = url.openStream();
                } catch (IOException e) {
                    // 忽略异常
                }
            }
        }
        return is;
    }


    /**
     * 获取指定代码源的权限集合。
     * <p>
     * 由于此ClassLoader仅用于Web应用上下文中的JSP页面，
     * 我们直接返回预设的PermissionCollection。
     * </p>
     *
     * @param codeSource 代码加载来源
     * @return 权限集合
     */
    @Override
    public final PermissionCollection getPermissions(CodeSource codeSource) {
        return permissionCollection;
    }
}
