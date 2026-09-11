# Dafit Android v1.2 — Java

Versi ini membuang Kotlin sepenuhnya untuk mengelakkan masalah Kotlin/JVM.

Fungsi:
- BLE scan
- Connect / Disconnect
- GATT Services
- GATT Characteristics
- READ
- NOTIFY / INDICATE
- Log HEX / ASCII

GitHub Actions akan membina `app-debug.apk`.

## Struktur
app/
build.gradle
settings.gradle
gradle.properties
.github/workflows/build-apk.yml

## GitHub
Upload semua kandungan projek ini ke ROOT repository.
Kemudian:
Actions -> Build Dafit APK -> Run workflow.
