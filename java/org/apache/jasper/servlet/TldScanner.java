package org.apache.jasper.servlet;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.servlet.ServletContext;
import javax.servlet.descriptor.JspConfigDescriptor;
import javax.servlet.descriptor.TaglibDescriptor;

import org.apache.jasper.compiler.JarScannerFactory;
import org.apache.jasper.compiler.Localizer;
import org.apache.juli.logging.Log;
import org.apache.juli.logging.LogFactory;
import org.apache.tomcat.Jar;
import org.apache.tomcat.JarScanType;
import org.apache.tomcat.JarScanner;
import org.apache.tomcat.JarScannerCallback;
import org.apache.tomcat.util.descriptor.tld.TaglibXml;
import org.apache.tomcat.util.descriptor.tld.TldParser;
import org.apache.tomcat.util.descriptor.tld.TldResourcePath;
import org.xml.sax.SAXException;

/**
 * TLD（标签库描述符）扫描器。
 * <p>
 * 用于扫描和加载 Web 应用程序中包含的标签库描述符（TLD）文件。
 * TLD 文件定义了 JSP 自定义标签库的信息，包括标签名称、处理器类等。
 * 扫描器按照 JSP 规范定义的位置进行扫描，包括平台定义的标签库、web.xml 中
 * 配置的标签库、/WEB-INF 目录下的资源、/WEB-INF/lib 中的 JAR 文件等。
 * </p>
 *
 * @author 您的姓名
 * @version 1.0
 */
public class TldScanner {
    // 日志对象，不能是静态的，确保每个实例都有自己的日志器
    private final Log log = LogFactory.getLog(TldScanner.class); // must not be static
    // 本地化消息资源的前缀
    private static final String MSG = "org.apache.jasper.servlet.TldScanner";
    // TLD 文件的扩展名
    private static final String TLD_EXT = ".tld";
    // WEB-INF 目录路径
    private static final String WEB_INF = "/WEB-INF/";
    // Servlet 上下文对象，用于访问 Web 应用资源
    private final ServletContext context;
    // TLD 解析器，用于解析 TLD 文件
    private final TldParser tldParser;
    // URI 到 TLD 资源路径的映射 Map
    private final Map<String, TldResourcePath> uriTldResourcePathMap = new HashMap<>();
    // TLD 资源路径到解析后的 TaglibXml 对象的映射 Map
    private final Map<TldResourcePath, TaglibXml> tldResourcePathTaglibXmlMap = new HashMap<>();
    // 扫描到的所有监听器类名列表
    private final List<String> listeners = new ArrayList<>();

    /**
     * 构造方法，使用应用程序的 ServletContext 初始化扫描器。
     *
     * @param context        应用程序的 ServletContext 对象，用于访问 Web 应用资源
     * @param namespaceAware 是否配置 XML 解析器为命名空间感知模式
     *                       （true 表示启用命名空间处理）
     * @param validation     是否配置 XML 解析器进行验证
     *                       （true 表示对 TLD 文件进行 XML 验证）
     * @param blockExternal  是否配置 XML 解析器阻止对外部实体的引用
     *                       （true 表示禁止访问外部实体，增强安全性）
     */
    public TldScanner(ServletContext context,
                      boolean namespaceAware,
                      boolean validation,
                      boolean blockExternal) {
        this.context = context;

        this.tldParser = new TldParser(namespaceAware, validation, blockExternal);
    }

    /**
     * 执行完整的 TLD 扫描操作。
     * <p>
     * 按照 JSP 规范定义的扫描顺序，扫描所有可能包含 TLD 的位置：
     * <ol>
     * <li>平台定义的标签库</li>
     * <li>web.xml 中 &lt;jsp-config&gt; 配置的标签库</li>
     * <li>/WEB-INF 目录下的资源文件</li>
     * <li>/WEB-INF/lib 目录中 JAR 文件内的 TLD</li>
     * <li>容器提供的额外条目</li>
     * </ol>
     * </p>
     *
     * @throws IOException  当扫描或加载 TLD 文件时发生 I/O 错误
     * @throws SAXException 当解析 TLD 文件时发生 XML 解析错误
     */
    public void scan() throws IOException, SAXException {
        scanPlatform();
        scanJspConfig();
        scanResourcePaths(WEB_INF);
        scanJars();
    }

    /**
     * 获取 URI 到 TLD 资源路径的映射 Map。
     * <p>
     * 该 Map 由扫描器构建，键是标签库的 URI，值是对应的 TLD 资源路径。
     * 该映射用于根据 URI 查找对应的 TLD 文件位置。
     * </p>
     *
     * @return URI 到 TldResourcePath 的映射 Map
     */
    public Map<String, TldResourcePath> getUriTldResourcePathMap() {
        return uriTldResourcePathMap;
    }

    /**
     * 获取 TLD 资源路径到解析后的 TaglibXml 对象的映射 Map。
     * <p>
     * 该 Map 由扫描器构建，键是 TLD 资源路径，值是解析后的 TaglibXml 对象。
     * 该映射用于获取已解析的 TLD 内容信息。
     * </p>
     *
     * @return TldResourcePath 到解析后的 TaglibXml 的映射 Map
     */
    public Map<TldResourcePath,TaglibXml> getTldResourcePathTaglibXmlMap() {
        return tldResourcePathTaglibXmlMap;
    }

    /**
     * 获取扫描到的所有监听器类名列表。
     * <p>
     * TLD 文件中可以声明监听器，这些监听器需要在 Web 应用启动时注册。
     * 该方法返回所有扫描到的 TLD 中声明的监听器类名。
     * </p>
     *
     * @return 监听器类名的列表
     */
    public List<String> getListeners() {
        return listeners;
    }

    /**
     * 设置用于创建对象的类加载器。
     * <p>
     * 该方法设置 Digester 解析器在解析 TLD 时使用的类加载器。
     * 通常仅在 JspC（JSP 预编译器）场景下才需要显式设置。
     * </p>
     *
     * @param classLoader 用于在解析 TLD 时创建新对象的类加载器
     */
    public void setClassLoader(ClassLoader classLoader) {
        tldParser.setClassLoader(classLoader);
    }

    /**
     * 扫描平台规范要求的 TLD。
     * <p>
     * 该方法用于扫描由平台规范定义的标签库。
     * 当前实现为空，由子类根据需要重写。
     * </p>
     */
    protected void scanPlatform() {
    }

    /**
     * 扫描 web.xml 中 &lt;jsp-config&gt; 配置定义的 TLD。
     * <p>
     * 从 ServletContext 获取 JSP 配置描述符，遍历其中配置的每个标签库描述符。
     * 根据配置的资源路径加载并解析对应的 TLD 文件，将结果存储到内部映射中。
     * </p>
     *
     * @throws IOException  读取资源时发生错误
     * @throws SAXException XML 解析时发生错误
     */
    protected void scanJspConfig() throws IOException, SAXException {
        JspConfigDescriptor jspConfigDescriptor = context.getJspConfigDescriptor();
        if (jspConfigDescriptor == null) {
            return;
        }

        Collection<TaglibDescriptor> descriptors = jspConfigDescriptor.getTaglibs();
        for (TaglibDescriptor descriptor : descriptors) {
            String taglibURI = descriptor.getTaglibURI();
            String resourcePath = descriptor.getTaglibLocation();
            // 注意：虽然 Servlet 2.4 DTD 暗示位置必须是以 '/' 开头的上下文相对路径，
            // 但 JSP.7.3.6.1 明确说明了如何处理不以 '/' 开头的路径
            if (!resourcePath.startsWith("/")) {
                resourcePath = WEB_INF + resourcePath;
            }
            if (uriTldResourcePathMap.containsKey(taglibURI)) {
                log.warn(Localizer.getMessage(MSG + ".webxmlSkip",
                        resourcePath,
                        taglibURI));
                continue;
            }

            if (log.isTraceEnabled()) {
                log.trace(Localizer.getMessage(MSG + ".webxmlAdd",
                        resourcePath,
                        taglibURI));
            }

            URL url = context.getResource(resourcePath);
            if (url != null) {
                TldResourcePath tldResourcePath;
                if (resourcePath.endsWith(".jar")) {
                    // 如果路径指向 JAR 文件，则假定 TLD 在 JAR 内部的 META-INF/taglib.tld
                    tldResourcePath = new TldResourcePath(url, resourcePath, "META-INF/taglib.tld");
                } else {
                    tldResourcePath = new TldResourcePath(url, resourcePath);
                }
                // 解析 TLD，但使用描述符中提供的 URI 作为键存储
                TaglibXml tld = tldParser.parse(tldResourcePath);
                uriTldResourcePathMap.put(taglibURI, tldResourcePath);
                tldResourcePathTaglibXmlMap.put(tldResourcePath, tld);
                if (tld.getListeners() != null) {
                    listeners.addAll(tld.getListeners());
                }
            } else {
                log.warn(Localizer.getMessage(MSG + ".webxmlFailPathDoesNotExist",
                        resourcePath,
                        taglibURI));
                continue;
            }
        }
    }

    /**
     * 递归扫描 Web 应用资源中的 TLD 文件。
     * <p>
     * 从指定起始路径开始，递归扫描目录结构中的 TLD 文件。
     * 根据 JSP 规范：
     * - 跳过 /WEB-INF/classes/ 和 /WEB-INF/lib/ 目录
     * - 在 /WEB-INF/tags/ 目录下只考虑 implicit.tld 文件
     * - 其他位置的所有 .tld 文件都会被解析
     * </p>
     *
     * @param startPath 要扫描的目录资源路径
     * @throws IOException  当扫描或加载 TLD 时发生 I/O 错误
     * @throws SAXException 当解析 TLD 时发生 XML 解析错误
     */
    protected void scanResourcePaths(String startPath)
            throws IOException, SAXException {

        boolean found = false;
        Set<String> dirList = context.getResourcePaths(startPath);
        if (dirList != null) {
            for (String path : dirList) {
                if (path.startsWith("/WEB-INF/classes/")) {
                    // 跳过：JSP.7.3.1 规范要求
                } else if (path.startsWith("/WEB-INF/lib/")) {
                    // 跳过：JSP.7.3.1 规范要求
                } else if (path.endsWith("/")) {
                    scanResourcePaths(path);
                } else if (path.startsWith("/WEB-INF/tags/")) {
                    // JSP 7.3.1：在 /WEB-INF/tags 目录下只考虑 implicit.tld 文件
                    if (path.endsWith("/implicit.tld")) {
                        found = true;
                        parseTld(path);
                    }
                } else if (path.endsWith(TLD_EXT)) {
                    found = true;
                    parseTld(path);
                }
            }
        }
        if (found) {
            if (log.isTraceEnabled()) {
                log.trace(Localizer.getMessage("jsp.tldCache.tldInResourcePath", startPath));
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace(Localizer.getMessage("jsp.tldCache.noTldInResourcePath", startPath));
            }
        }
    }

    /**
     * 扫描 /WEB-INF/lib 目录中 JAR 文件的 TLD。
     * <p>
     * 使用 JarScanner 扫描 Web 应用的 JAR 文件，查找其中 META-INF 目录下的 TLD 文件。
     * 扫描完成后，如果没有找到任何 TLD，会记录一条信息日志。
     * </p>
     */
    public void scanJars() {
        JarScanner scanner = JarScannerFactory.getJarScanner(context);
        TldScannerCallback callback = new TldScannerCallback();
        scanner.scan(JarScanType.TLD, context, callback);
        if (callback.scanFoundNoTLDs()) {
            log.info(Localizer.getMessage("jsp.tldCache.noTldSummary"));
        }
    }

    /**
     * 解析指定资源路径的 TLD 文件。
     *
     * @param resourcePath TLD 文件的资源路径
     * @throws IOException  读取资源时发生错误
     * @throws SAXException XML 解析时发生错误
     */
    protected void parseTld(String resourcePath) throws IOException, SAXException {
        TldResourcePath tldResourcePath =
                new TldResourcePath(context.getResource(resourcePath), resourcePath);
        parseTld(tldResourcePath);
    }

    /**
     * 解析指定的 TLD 资源路径。
     * <p>
     * 使用 TldParser 解析 TLD 文件，将解析结果存储到内部映射中。
     * 如果 TLD 已作为 web.xml 处理结果被解析过，则跳过重复解析。
     * </p>
     *
     * @param path TLD 资源路径对象
     * @throws IOException  读取资源时发生错误
     * @throws SAXException XML 解析时发生错误
     */
    protected void parseTld(TldResourcePath path) throws IOException, SAXException {
        TaglibXml tld = tldParser.parse(path);
        String uri = tld.getUri();
        if (uri != null) {
            if (!uriTldResourcePathMap.containsKey(uri)) {
                uriTldResourcePathMap.put(uri, path);
            }
        }

        if (tldResourcePathTaglibXmlMap.containsKey(path)) {
            // TLD 已经作为 web.xml 处理结果被解析过了
            return;
        }

        tldResourcePathTaglibXmlMap.put(path, tld);
        if (tld.getListeners() != null) {
            listeners.addAll(tld.getListeners());
        }
    }

    /**
     * JAR 扫描回调实现类。
     * <p>
     * 实现了 JarScannerCallback 接口，用于处理 JAR 扫描过程中发现的 TLD 文件。
     * 支持处理 JAR 文件、文件系统中的目录以及 WEB-INF/classes 目录中的 TLD。
     * </p>
     */
    class TldScannerCallback implements JarScannerCallback {
        // 标记是否发现没有 TLD 的 JAR 文件
        private boolean foundJarWithoutTld = false;
        // 标记是否发现没有 TLD 的普通文件目录
        private boolean foundFileWithoutTld = false;


        /**
         * 扫描 JAR 文件中的 TLD。
         * <p>
         * 遍历 JAR 文件中的所有条目，查找 META-INF 目录下以 .tld 结尾的文件。
         * 对每个找到的 TLD 文件进行解析。
         * </p>
         *
         * @param jar       JAR 文件对象
         * @param webappPath Web 应用路径
         * @param isWebapp  是否为 Web 应用
         * @throws IOException 读取或解析 JAR 时发生错误
         */
        @Override
        public void scan(Jar jar, String webappPath, boolean isWebapp) throws IOException {
            boolean found = false;
            URL jarFileUrl = jar.getJarFileURL();
            jar.nextEntry();
            for (String entryName = jar.getEntryName();
                entryName != null;
                jar.nextEntry(), entryName = jar.getEntryName()) {
                if (!(entryName.startsWith("META-INF/") &&
                        entryName.endsWith(TLD_EXT))) {
                    continue;
                }
                found = true;
                TldResourcePath tldResourcePath =
                        new TldResourcePath(jarFileUrl, webappPath, entryName);
                try {
                    parseTld(tldResourcePath);
                } catch (SAXException e) {
                    throw new IOException(e);
                }
            }
            if (found) {
                if (log.isTraceEnabled()) {
                    log.trace(Localizer.getMessage("jsp.tldCache.tldInJar", jarFileUrl.toString()));
                }
            } else {
                foundJarWithoutTld = true;
                if (log.isTraceEnabled()) {
                    log.trace(Localizer.getMessage(
                            "jsp.tldCache.noTldInJar", jarFileUrl.toString()));
                }
            }
        }

        /**
         * 扫描文件目录中的 TLD。
         * <p>
         * 遍历指定目录的 META-INF 子目录，查找所有以 .tld 结尾的文件并解析。
         * 使用文件树遍历算法递归查找所有 TLD 文件。
         * </p>
         *
         * @param file       要扫描的目录文件
         * @param webappPath Web 应用路径
         * @param isWebapp   是否为 Web 应用
         * @throws IOException 读取或解析文件时发生错误
         */
        @Override
        public void scan(File file, final String webappPath, boolean isWebapp)
                throws IOException {
            File metaInf = new File(file, "META-INF");
            if (!metaInf.isDirectory()) {
                return;
            }
            foundFileWithoutTld = false;
            final Path filePath = file.toPath();
            Files.walkFileTree(metaInf.toPath(), new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(Path file,
                                                 BasicFileAttributes attrs)
                        throws IOException {
                    Path fileName = file.getFileName();
                    if (fileName == null || !fileName.toString().toLowerCase(
                            Locale.ENGLISH).endsWith(TLD_EXT)) {
                        return FileVisitResult.CONTINUE;
                    }

                    foundFileWithoutTld = true;
                    String resourcePath;
                    if (webappPath == null) {
                        resourcePath = null;
                    } else {
                        String subPath = file.subpath(
                                filePath.getNameCount(), file.getNameCount()).toString();
                        if ('/' != File.separatorChar) {
                            subPath = subPath.replace(File.separatorChar, '/');
                        }
                        resourcePath = webappPath + "/" + subPath;
                    }

                    try {
                        URL url = file.toUri().toURL();
                        TldResourcePath path = new TldResourcePath(url, resourcePath);
                        parseTld(path);
                    } catch (SAXException e) {
                        throw new IOException(e);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            if (foundFileWithoutTld) {
                if (log.isTraceEnabled()) {
                    log.trace(Localizer.getMessage("jsp.tldCache.tldInDir",
                            file.getAbsolutePath()));
                }
            } else {
                if (log.isTraceEnabled()) {
                    log.trace(Localizer.getMessage("jsp.tldCache.noTldInDir",
                            file.getAbsolutePath()));
                }
            }
        }

        /**
         * 扫描 WEB-INF/classes 目录中的 TLD。
         * <p>
         * 当 scanAllDirectories 启用且某些 JAR 被解压到 WEB-INF/classes 目录时使用
         * （如某些 IDE 的情况）。扫描 classes/META-INF 目录下的 TLD 文件。
         * </p>
         *
         * @throws IOException 读取或解析文件时发生错误
         */
        @Override
        public void scanWebInfClasses() throws IOException {
            // 当 scanAllDirectories 启用且一个或多个 JAR 被解压到 WEB-INF/classes 时使用
            // （如某些 IDE 的情况）

            Set<String> paths = context.getResourcePaths(WEB_INF + "classes/META-INF");
            if (paths == null) {
                return;
            }

            for (String path : paths) {
                if (path.endsWith(TLD_EXT)) {
                    try {
                        parseTld(path);
                    } catch (SAXException e) {
                        throw new IOException(e);
                    }
                }
            }
        }


        /**
         * 检查扫描是否没有发现任何 TLD。
         *
         * @return 如果扫描过程中发现没有 TLD 的 JAR 文件，则返回 true
         */
        boolean scanFoundNoTLDs() {
            return foundJarWithoutTld;
        }
    }
}
