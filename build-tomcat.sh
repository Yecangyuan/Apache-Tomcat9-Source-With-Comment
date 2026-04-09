#!/bin/bash
################################################################################
# Apache Tomcat 9.0.90 一键构建脚本
# 用法: ./build-tomcat.sh [选项]
################################################################################

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目配置
TOMCAT_VERSION="9.0.90"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"
BUILD_DIR="${PROJECT_DIR}/output/build"
DEPENDENCIES_DIR="/Users/simley/SourceReading/Tomcat/tomcat-build-libs"

# 默认选项
CLEAN_BUILD=false
RUN_TESTS=false
SKIP_DOCS=false
VERBOSE=false

# 显示帮助信息
show_help() {
    cat << EOF
Apache Tomcat ${TOMCAT_VERSION} 一键构建脚本

用法: $0 [选项]

选项:
    -c, --clean         清理之前的构建输出
    -t, --test          构建完成后运行测试
    -s, --skip-docs     跳过文档构建
    -v, --verbose       显示详细输出
    -h, --help          显示此帮助信息

示例:
    $0                  # 标准构建
    $0 -c               # 清理并构建
    $0 -c -t            # 清理、构建并运行测试
    $0 -s               # 构建（跳过文档）

EOF
}

# 解析命令行参数
parse_args() {
    while [[ $# -gt 0 ]]; do
        case $1 in
            -c|--clean)
                CLEAN_BUILD=true
                shift
                ;;
            -t|--test)
                RUN_TESTS=true
                shift
                ;;
            -s|--skip-docs)
                SKIP_DOCS=true
                shift
                ;;
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            -h|--help)
                show_help
                exit 0
                ;;
            *)
                echo -e "${RED}错误: 未知选项 $1${NC}"
                show_help
                exit 1
                ;;
        esac
    done
}

# 打印带颜色的消息
log_info() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

log_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

log_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

log_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# 检查环境
check_environment() {
    log_info "检查构建环境..."
    
    # 检查 Java
    if ! command -v java &> /dev/null; then
        log_error "未找到 Java，请安装 JDK 11 或更高版本"
        exit 1
    fi
    
    JAVA_VERSION=$(java -version 2>&1 | awk -F '"' '/version/ {print $2}')
    log_info "Java 版本: $JAVA_VERSION"
    
    # 检查 Ant
    if ! command -v ant &> /dev/null; then
        log_error "未找到 Ant，请安装 Apache Ant 1.10.x 或更高版本"
        exit 1
    fi
    
    ANT_VERSION=$(ant -version 2>&1 | awk '/version/ {print $4}')
    log_info "Ant 版本: $ANT_VERSION"
    
    # 检查 build.properties
    if [[ ! -f "${PROJECT_DIR}/build.properties" ]]; then
        log_warning "build.properties 不存在，创建默认配置..."
        create_build_properties
    fi
    
    log_success "环境检查通过"
}

# 创建 build.properties
create_build_properties() {
    cat > "${PROJECT_DIR}/build.properties" << EOF
# Apache Tomcat 构建配置
# 生成时间: $(date)

# 依赖库下载目录
base.path=${DEPENDENCIES_DIR}

# 跳过 Windows 安装程序
skip.installer=true

# 禁用代码签名
do.codesigning=false

# 构建输出目录
tomcat.output=${PROJECT_DIR}/output

# 测试配置
execute.test.nio=true
execute.test.nio2=true
execute.test.apr=false
EOF
    log_success "已创建 build.properties"
}

# 清理构建输出
clean_build() {
    log_info "清理构建输出..."
    cd "${PROJECT_DIR}"
    
    if ant clean > /dev/null 2>&1; then
        log_success "清理完成"
    else
        log_warning "清理过程中出现问题，继续构建..."
    fi
}

# 执行构建
run_build() {
    log_info "开始构建 Apache Tomcat ${TOMCAT_VERSION}..."
    log_info "这可能需要几分钟时间，请耐心等待..."
    
    cd "${PROJECT_DIR}"
    
    # 构建参数
    local ant_args=""
    
    if [[ "$SKIP_DOCS" == true ]]; then
        ant_args="$ant_args -Dskip.docs=true"
        log_info "已设置: 跳过文档构建"
    fi
    
    # 开始计时
    local start_time=$(date +%s)
    
    # 执行构建
    if [[ "$VERBOSE" == true ]]; then
        ant $ant_args
    else
        ant $ant_args 2>&1 | tee "${PROJECT_DIR}/build.log" | while read line; do
            if [[ "$line" == *"BUILD SUCCESSFUL"* ]]; then
                log_success "$line"
            elif [[ "$line" == *"BUILD FAILED"* ]]; then
                log_error "$line"
            elif [[ "$line" == *"Compiling"* ]] || [[ "$line" == *"Building"* ]]; then
                echo -e "${YELLOW}  →${NC} $line"
            fi
        done
    fi
    
    local build_result=${PIPESTATUS[0]}
    local end_time=$(date +%s)
    local duration=$((end_time - start_time))
    
    if [[ $build_result -eq 0 ]]; then
        log_success "构建成功！耗时: ${duration} 秒"
        return 0
    else
        log_error "构建失败！请查看 build.log 了解详情"
        return 1
    fi
}

# 运行测试
run_tests() {
    log_info "运行单元测试..."
    cd "${PROJECT_DIR}"
    
    if ant test 2>&1 | tee "${PROJECT_DIR}/test.log"; then
        log_success "测试完成"
        log_info "测试报告位置: output/build/logs"
    else
        log_warning "部分测试可能失败，请查看 test.log"
    fi
}

# 验证构建结果
verify_build() {
    log_info "验证构建结果..."
    
    if [[ ! -d "$BUILD_DIR" ]]; then
        log_error "构建输出目录不存在: $BUILD_DIR"
        return 1
    fi
    
    # 检查关键文件
    local required_files=(
        "bin/catalina.sh"
        "bin/startup.sh"
        "bin/shutdown.sh"
        "lib/catalina.jar"
        "conf/server.xml"
    )
    
    local missing_count=0
    for file in "${required_files[@]}"; do
        if [[ ! -f "$BUILD_DIR/$file" ]]; then
            log_error "缺少文件: $file"
            ((missing_count++))
        fi
    done
    
    if [[ $missing_count -gt 0 ]]; then
        log_error "验证失败，缺少 $missing_count 个关键文件"
        return 1
    fi
    
    # 显示版本信息
    log_info "Tomcat 版本信息:"
    java -cp "$BUILD_DIR/lib/catalina.jar" org.apache.catalina.util.ServerInfo 2>/dev/null | while read line; do
        echo -e "  ${GREEN}✓${NC} $line"
    done
    
    # 显示构建统计
    local jar_count=$(find "$BUILD_DIR/lib" -name "*.jar" | wc -l)
    local webapp_count=$(find "$BUILD_DIR/webapps" -maxdepth 1 -type d | wc -l)
    local total_size=$(du -sh "$BUILD_DIR" | cut -f1)
    
    log_info "构建统计:"
    echo -e "  ${GREEN}✓${NC} JAR 文件数量: $jar_count"
    echo -e "  ${GREEN}✓${NC} Web 应用数量: $((webapp_count - 1))"
    echo -e "  ${GREEN}✓${NC} 总大小: $total_size"
    
    log_success "构建验证通过"
    return 0
}

# 显示使用说明
show_usage() {
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
    log_success "Apache Tomcat ${TOMCAT_VERSION} 构建完成！"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    echo "📂 构建输出位置:"
    echo "   $BUILD_DIR"
    echo ""
    echo "🚀 启动 Tomcat:"
    echo "   cd $BUILD_DIR"
    echo "   ./bin/startup.sh"
    echo ""
    echo "📝 前台运行（查看日志）:"
    echo "   cd $BUILD_DIR"
    echo "   ./bin/catalina.sh run"
    echo ""
    echo "🛑 停止 Tomcat:"
    echo "   cd $BUILD_DIR"
    echo "   ./bin/shutdown.sh"
    echo ""
    echo "📊 日志文件位置:"
    echo "   $BUILD_DIR/logs/"
    echo ""
    echo "═══════════════════════════════════════════════════════════════"
}

# 主函数
main() {
    echo "═══════════════════════════════════════════════════════════════"
    echo "  Apache Tomcat ${TOMCAT_VERSION} 一键构建脚本"
    echo "═══════════════════════════════════════════════════════════════"
    echo ""
    
    # 解析参数
    parse_args "$@"
    
    # 检查环境
    check_environment
    
    # 清理（如果需要）
    if [[ "$CLEAN_BUILD" == true ]]; then
        clean_build
    fi
    
    # 执行构建
    if ! run_build; then
        exit 1
    fi
    
    # 验证构建结果
    if ! verify_build; then
        exit 1
    fi
    
    # 运行测试（如果需要）
    if [[ "$RUN_TESTS" == true ]]; then
        run_tests
    fi
    
    # 显示使用说明
    show_usage
}

# 执行主函数
main "$@"
