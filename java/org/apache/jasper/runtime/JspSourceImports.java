package org.apache.jasper.runtime;

import java.util.Set;

/**
 * EL引擎需要访问JSP页面中使用的导入类来配置ELContext。
 * 导入类在编译时可用，但ELContext是在每个页面延迟创建的。
 * 该接口在运行时暴露导入类，以便在创建ELContext时可以将它们添加进去。
 */
public interface JspSourceImports {

    /**
     * 获取包导入集合。
     * 
     * @return 导入的包名集合
     */
    Set<String> getPackageImports();

    /**
     * 获取类导入集合。
     * 
     * @return 导入的类全限定名集合
     */
    Set<String> getClassImports();
}
