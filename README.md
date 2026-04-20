# Reminder Voice App (Starter Project)

Starter project Android native (Kotlin + Jetpack Compose) untuk reminder harian dengan:
- exact alarm
- notifikasi
- mode suara biasa
- mode suara "ngomong" pakai TextToSpeech
- mode file audio custom
- snooze 5 menit
- reschedule ulang setelah reboot

## Fitur inti
1. Simpan reminder harian berdasarkan jam & menit
2. Pilih mode suara per reminder:
   - **Notif biasa**
   - **Suara ngomong**
   - **File audio custom**
3. Exact alarm memakai `AlarmManager.setExactAndAllowWhileIdle()`
4. Receiver reboot untuk menjadwalkan ulang reminder aktif
5. Penyimpanan lokal pakai Room

## Catatan penting
- Di Android 13+ notifikasi perlu izin **POST_NOTIFICATIONS**
- Di Android 12+ exact alarm perlu akses **Alarms & reminders**
- Di Android 8.0+ perilaku suara notifikasi dikontrol oleh **notification channel**
- Karena channel bersifat kaku, mode TTS dan audio custom di proyek ini diputar oleh **foreground service**, bukan semata-mata sound notifikasi

## Struktur file penting
- `MainActivity.kt` → UI utama
- `ui/ReminderViewModel.kt` → logika tambah / hapus / aktif-nonaktif
- `alarm/AlarmScheduler.kt` → pasang exact alarm
- `alarm/AlarmReceiver.kt` → alarm dipicu
- `alarm/AlarmPlaybackService.kt` → putar TTS / audio custom
- `alarm/RescheduleReceiver.kt` → pasang ulang alarm setelah reboot
- `data/*` → Room database

## Hal yang bisa kamu upgrade berikutnya
- pilih hari tertentu (Senin–Minggu)
- edit reminder
- full-screen ringing screen
- volume custom
- import/export data
- rekaman suara sendiri per tugas
- bahasa TTS pria/wanita jika engine HP mendukung

## Langkah buka project
1. Extract zip
2. Buka folder project di Android Studio
3. Sync Gradle
4. Jalankan ke device Android
5. Izinkan notifikasi
6. Aktifkan exact alarm bila diminta

## Catatan kejujuran
Project ini saya siapkan sebagai **starter project yang realistis**, tetapi saya **belum menjalankan build Android penuh di environment ini**, jadi mungkin masih ada penyesuaian kecil saat pertama kali sync/build di Android Studio.

## Update v1.3

- Pengaturan volume alarm paksa sekarang tampil di aplikasi dan bisa diubah 10-100%.
- Jeda ulang alarm bisa diatur 1-15 detik. Catatan: jeda dihitung setelah audio/TTS selesai, bukan sejak mulai bicara.
- Reminder tersimpan dipindah ke tab kedua agar layar utama lebih rapi.
- Dropdown model suara TTS sekarang punya search/filter.
- TTS mendukung jeda eksplisit:
  - Enter/baris baru = jeda pendek.
  - `|` = jeda pendek.
  - `[jeda 2]` = jeda 2 detik. Angka bisa diganti, contoh `[jeda 1.5]`.
