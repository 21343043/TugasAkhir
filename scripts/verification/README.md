# Pengujian tambahan untuk pengembang

Folder ini berisi pemeriksaan migrasi SQLite berbasis Python. Skrip membaca SQL
migrasi dan seed dari kode produksi, kemudian mengujinya pada database sementara.
Skrip tidak menjadi bagian APK dan tidak menggantikan pengujian Room pada Android.

Dari root proyek:

```bash
python3 -m unittest discover -s scripts/verification -v
```

Pengujian parser dan ringkasan keuangan berada di `app/src/test`, mengikuti
struktur paket produksi. Runner Java lama di `tools/` dihapus karena hanya mampu
mengompilasi Java. Jalankan pengujian unit melalui Gradle:

```bash
./gradlew testDebugUnitTest
```

Di Windows gunakan `gradlew.bat`. Untuk pengujian Room, foto, dan Compose pada
emulator/perangkat gunakan `gradlew.bat connectedDebugAndroidTest`.
