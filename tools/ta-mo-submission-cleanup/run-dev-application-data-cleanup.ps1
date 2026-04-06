# 在仓库根执行双端清理（需已安装 Maven）
$ErrorActionPreference = 'Stop'
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$root = Resolve-Path (Join-Path $here '..\..')
Set-Location $root
$extra = $args -join ' '
if ($extra) {
    mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.bupt.tarecruit.tools.DevApplicationDataCleanupTool -Dexec.args="$extra"
} else {
    mvn -q -DskipTests compile exec:java -Dexec.mainClass=com.bupt.tarecruit.tools.DevApplicationDataCleanupTool
}
