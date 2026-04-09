#!/bin/bash
################################################################################
# Apache Tomcat 快速构建脚本
# 用于快速重新构建（不清理依赖，只重新编译）
################################################################################

set -e

cd "$(dirname "$0")"

echo "═══════════════════════════════════════════════════════════════"
echo "  Apache Tomcat 9.0.90 快速构建"
echo "═══════════════════════════════════════════════════════════════"
echo ""

# 检查 build.properties
if [[ ! -f "build.properties" ]]; then
    echo "[INFO] 创建 build.properties..."
    cat > build.properties << 'EOF'
base.path=/Users/simley/SourceReading/Tomcat/tomcat-build-libs
skip.installer=true
do.codesigning=false
EOF
fi

echo "[INFO] 开始快速构建..."
echo "[INFO] 执行: ant"
echo ""

# 执行构建
ant

echo ""
echo "═══════════════════════════════════════════════════════════════"
echo "  ✅ 构建成功！"
echo "═══════════════════════════════════════════════════════════════"
echo ""
echo "📂 输出位置: $(pwd)/output/build"
echo "🚀 启动命令: ./output/build/bin/startup.sh"
echo ""
