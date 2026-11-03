# Windows (PowerShell). Requiere mariadb.exe en el PATH
# (p. ej. C:\Program Files\MariaDB 11.8\bin). Uso: .\database\instalar.ps1
$ErrorActionPreference = "Stop"
Set-Location $PSScriptRoot
$clave = Read-Host "Clave de root de MariaDB"
foreach ($f in "01_esquema.sql", "02_objetos.sql", "03_datos.sql", "00_usuario_app.sql") {
  Write-Host "-> $f"
  Get-Content $f -Raw -Encoding UTF8 | mariadb -u root "-p$clave" --default-character-set=utf8mb4
}
Write-Host "Listo. Base frutimarket_bd instalada."
