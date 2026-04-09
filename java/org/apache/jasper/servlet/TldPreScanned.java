package org.apache.jasper.servlet;

import java.net.URI;
import java.net.URL;
import java.util.Collection;

import javax.servlet.ServletContext;

import org.apache.jasper.compiler.Localizer;
import org.apache.tomcat.util.descriptor.tld.TldResourcePath;

/**
 * 预扫描的 TLD 标签库扫描器。
 * 
 * <p>该类继承自 TldScanner，用于处理预先扫描好的 TLD 文件 URL 集合。
 * 与扫描整个类路径的 TldScanner 不同，此类只处理构造时传入的特定 TLD URL 列表，
 * 适用于在部署时已经确定 TLD 位置的场杢，可以提高启动性能。</p>
 *
 * @author 作者未注明
 * @version 9.0.90
 */
public class TldPreScanned extends TldScanner {

    /** 预扫描的 TLD 文件 URL 集合 */
    private final Collection<URL> preScannedURLs;

    /**
     * 构造预扫描的 TLD 扫描器。
     *
     * @param context Servlet 上下文，用于获取应用相关信息
     * @param namespaceAware 是否启用命名空间感知
     * @param validation 是否启用 XML 验证
     * @param blockExternal 是否阻止访问外部实体
     * @param preScannedTlds 预扫描的 TLD 文件 URL 集合
     */
    public TldPreScanned (ServletContext context, boolean namespaceAware, boolean validation,
            boolean blockExternal, Collection<URL> preScannedTlds) {
        super(context, namespaceAware, validation, blockExternal);
        preScannedURLs = preScannedTlds;
    }

    /**
     * 扫描 JAR 文件中的 TLD 标签库。
     * 
     * <p>遍历预扫描的 URL 集合，解析每个 jar: 协议 URL，提取 jar 文件路径和
     * TLD 条目路径，然后调用 parseTld 方法解析标签库定义。</p>
     * 
     * <p>URL 格式示例：jar:file:/path/to/app.jar!/META-INF/taglib.tld</p>
     */
    @Override
    public void scanJars() {
        for (URL url : preScannedURLs){
            String str = url.toExternalForm();
            int a = str.indexOf("jar:");
            int b = str.indexOf("!/");
            if (a >= 0 && b> 0) {
                String fileUrl = str.substring(a + 4, b);
                String path = str.substring(b + 2);
                try {
                    parseTld(new TldResourcePath(new URI(fileUrl).toURL(), null, path));
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            } else {
                throw new IllegalStateException(Localizer.getMessage("jsp.error.tld.url", str));
            }
        }
    }
}
