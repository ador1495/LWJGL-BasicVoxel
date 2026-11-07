# Downloads LWJGL 3.3.3 and JOML dependencies into lib/
$version = "3.3.3"
$jomlVersion = "1.10.8"
$baseUrl = "https://repo1.maven.org/maven2/org/lwjgl"

New-Item -ItemType Directory -Force -Path "lib" | Out-Null

$jars = @(
    "$baseUrl/lwjgl/$version/lwjgl-$version.jar",
    "$baseUrl/lwjgl/$version/lwjgl-$version-natives-windows.jar",
    "$baseUrl/lwjgl-glfw/$version/lwjgl-glfw-$version.jar",
    "$baseUrl/lwjgl-glfw/$version/lwjgl-glfw-$version-natives-windows.jar",
    "$baseUrl/lwjgl-opengl/$version/lwjgl-opengl-$version.jar",
    "$baseUrl/lwjgl-opengl/$version/lwjgl-opengl-$version-natives-windows.jar",
    "$baseUrl/lwjgl-stb/$version/lwjgl-stb-$version.jar",
    "$baseUrl/lwjgl-stb/$version/lwjgl-stb-$version-natives-windows.jar",
    "https://repo1.maven.org/maven2/org/joml/joml/$jomlVersion/joml-$jomlVersion.jar"
)

foreach ($url in $jars) {
    $filename = Split-Path $url -Leaf
    $dest = "lib\$filename"
    if (-not (Test-Path $dest)) {
        Write-Host "Downloading $filename..."
        curl.exe -sSL $url -o $dest
    }
}
Write-Host "All libraries ready in lib/"

