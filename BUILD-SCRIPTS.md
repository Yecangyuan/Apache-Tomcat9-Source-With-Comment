# Apache Tomcat 9.0.90 一键构建脚本

本目录包含多个便捷的构建脚本，帮助你快速构建 Apache Tomcat。

## 📜 脚本列表

| 脚本 | 平台 | 说明 |
|------|------|------|
| `build-tomcat.sh` | Linux/macOS | 功能完整的一键构建脚本 |
| `quick-build.sh` | Linux/macOS | 快速重新构建（简洁版） |
| `build-tomcat.bat` | Windows | Windows 平台一键构建脚本 |

---

## 🚀 快速开始

### Linux / macOS

```bash
# 方式1: 功能完整版（推荐）
./build-tomcat.sh

# 方式2: 快速构建（简洁版）
./quick-build.sh
```

### Windows

```cmd
# 双击运行或在命令行执行
build-tomcat.bat
```

---

## 📋 build-tomcat.sh 详细用法

### 基本用法

```bash
./build-tomcat.sh [选项]
```

### 选项

| 选项 | 说明 |
|------|------|
| `-c, --clean` | 清理之前的构建输出 |
| `-t, --test` | 构建完成后运行单元测试 |
| `-s, --skip-docs` | 跳过文档构建 |
| `-v, --verbose` | 显示详细输出 |
| `-h, --help` | 显示帮助信息 |

### 示例

```bash
# 标准构建
./build-tomcat.sh

# 清理并重新构建
./build-tomcat.sh -c

# 清理、构建并运行测试
./build-tomcat.sh -c -t

# 构建（跳过文档，加快速度）
./build-tomcat.sh -s

# 显示详细输出
./build-tomcat.sh -v
```

---

## 🪟 build-tomcat.bat 详细用法 (Windows)

### 基本用法

```cmd
build-tomcat.bat [选项]
```

### 选项

| 选项 | 说明 |
|------|------|
| `-c, --clean` | 清理之前的构建输出 |
| `-t, --test` | 构建完成后运行单元测试 |
| `-h, --help` | 显示帮助信息 |

### 示例

```cmd
# 标准构建
build-tomcat.bat

# 清理并重新构建
build-tomcat.bat -c

# 清理、构建并运行测试
build-tomcat.bat -c -t
```

---

## ⚙️ 环境要求

- **Java**: JDK 11 或更高版本
- **Apache Ant**: 1.10.x 或更高版本

### 检查环境

```bash
# 检查 Java
java -version

# 检查 Ant
ant -version
```

---

## 📂 构建输出

构建完成后，可运行的 Tomcat 位于：

```
output/build/
├── bin/          # 启动/停止脚本
├── conf/         # 配置文件
├── lib/          # JAR 库
├── logs/         # 日志目录
├── temp/         # 临时目录
└── webapps/      # Web 应用
```

---

## 🎯 启动 Tomcat

### Linux / macOS

```bash
cd output/build

# 启动（后台运行）
./bin/startup.sh

# 前台运行（查看日志）
./bin/catalina.sh run

# 停止
./bin/shutdown.sh
```

### Windows

```cmd
cd output\build

# 启动（后台运行）
bin\startup.bat

# 前台运行（查看日志）
bin\catalina.bat run

# 停止
bin\shutdown.bat
```

---

## 🔧 自定义配置

脚本会自动创建 `build.properties` 文件，你可以手动编辑它以自定义构建：

```properties
# 依赖库下载目录
base.path=/path/to/dependencies

# 跳过 Windows 安装程序
skip.installer=true

# 禁用代码签名
do.codesigning=false

# 构建输出目录
tomcat.output=/path/to/output
```

---

## 🐛 故障排除

### 构建失败

1. **检查环境**
   ```bash
   java -version
   ant -version
   ```

2. **清理并重新构建**
   ```bash
   ./build-tomcat.sh -c
   ```

3. **查看详细日志**
   ```bash
   ./build-tomcat.sh -v
   # 或查看 build.log
   cat build.log
   ```

### 常见问题

| 问题 | 解决方案 |
|------|----------|
| 找不到 Java | 设置 JAVA_HOME 环境变量 |
| 找不到 Ant | 将 Ant 添加到 PATH 环境变量 |
| 下载依赖失败 | 检查网络连接，或配置代理 |
| 权限不足 | 使用 `chmod +x build-tomcat.sh` 添加执行权限 |

---

## 📝 构建日志

- **构建日志**: `build.log`（如果使用 `-v` 选项）
- **测试日志**: `test.log`（如果运行测试）
- **Tomcat 日志**: `output/build/logs/`

---

## 🤝 其他构建命令

如果需要更精细的控制，可以直接使用 Ant：

```bash
# 清理
ant clean

# 编译
ant compile

# 构建 JAR
ant jar

# 完整构建
ant deploy

# 生成文档
ant javadoc

# 运行测试
ant test
```
