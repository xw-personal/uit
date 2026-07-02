#!/bin/bash
set -euo pipefail

function_url="http://ftp.aoya.net/software/develop/aoyasomp2/install/aoyaserver-2.0-function-develop.bin"
integ_url="http://ftp.aoya.net/software/develop/aoyasomp2/install/aoyaserver-2.0.bin"
server_file1="/root/aoyaserver-2.0.bin"
server_file2="/root/aoyaserver-2.0-function-develop.bin"
aoya="/opt/aoya"

# ----- 清理旧文件 -----
if [[ -f "$server_file1" ]]; then
    echo "删除旧集成测试包：$server_file1"
    rm -rf "$server_file1"
fi
if [[ -f "$server_file2" ]]; then
    echo "删除旧功能测试包：$server_file2"
    rm -rf "$server_file2"
fi

# ----- 卸载旧服务端 -----
if [[ -d "$aoya" ]]; then
    echo "开始卸载服务端"
    if [[ -f "$aoya/uninstall.sh" ]]; then
        "$aoya/uninstall.sh"
    else
        echo "警告：未找到卸载脚本 $aoya/uninstall.sh"
    fi
fi

# 清理其他残留
rm -rf /root/install.log /root/install.sh /root/mysql.log
echo 1 > /proc/sys/vm/drop_caches

cd /root

# ----- 获取两个远程文件的 Last-Modified 时间 -----
echo "获取远程文件修改时间..."
function_time=$(curl -sI "$function_url" | grep -i '^Last-Modified:' | sed 's/^[^:]*: //' | tr -d '\r' || true)
integ_time=$(curl -sI "$integ_url" | grep -i '^Last-Modified:' | sed 's/^[^:]*: //' | tr -d '\r' || true)

# ----- 兜底：如果任何一个时间获取失败，直接下集成测试包并安装 -----
if [[ -z "$function_time" || -z "$integ_time" ]]; then
    echo "无法获取所有远程文件时间，直接下载集成测试包作为默认..."
    curl -fL -o "$server_file1" "$integ_url"
    chmod +x "$server_file1"
    "$server_file1" -D -m
    cd /root && cat mysql.log
    docker ps -a
    exit 0
fi

# ----- 转换为时间戳并比较 -----
remote_timestamp_fun=$(date -d "$function_time" +%s 2>/dev/null)
remote_timestamp_int=$(date -d "$integ_time" +%s 2>/dev/null)

# 如果转换失败，也走兜底
if [[ -z "$remote_timestamp_fun" || -z "$remote_timestamp_int" ]]; then
    echo "时间转换失败，直接下载集成测试包..."
    curl -fL -o "$server_file1" "$integ_url"
    chmod +x "$server_file1"
    "$server_file1" -D -m
    cd /root && cat mysql.log
    docker ps -a
    exit 0
fi

# ----- 根据时间戳决定下载哪个包 -----
downloaded_file=""   # 记录下载的文件路径
if [[ $remote_timestamp_fun -gt $remote_timestamp_int ]]; then
    echo "功能测试包更新，下载 $function_url ..."
    curl -fL -o "$server_file2" "$function_url"
    downloaded_file="$server_file2"
else
    echo "集成测试包更新，下载 $integ_url ..."
    curl -fL -o "$server_file1" "$integ_url"
    downloaded_file="$server_file1"
fi

# ----- 安装下载的包 -----
if [[ -n "$downloaded_file" && -f "$downloaded_file" ]]; then
    chmod +x "$downloaded_file"
    "$downloaded_file" -D -m
else
    echo "下载失败或文件丢失，终止！" >&2
    exit 1
fi

# 后续检查
cd /root && cat mysql.log
docker ps -a