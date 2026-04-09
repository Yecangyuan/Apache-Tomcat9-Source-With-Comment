package org.apache.jasper.runtime;

import java.util.Map;

/**
 * 用于跟踪源文件依赖的接口，目的是编译过期页面。这适用于：
 * 1) 被页面指令（page directives）包含的文件
 * 2) 被 jsp:config 中的 include-prelude 和 include-coda 包含的文件
 * 3) 被引用的标签文件（tag files）
 * 4) 被引用的 TLD 文件
 */

public interface JspSourceDependent {

   /**
    * 返回当前页面所依赖的源文件的文件名和最后修改时间的映射。
    * @return 依赖资源的映射表，键为文件名，值为最后修改时间戳
    */
    Map<String,Long> getDependants();

}
