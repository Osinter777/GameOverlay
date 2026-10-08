# GameOverlay

Android-приложение для захвата экрана с оверлеем поверх других приложений и распознаванием объектов через TensorFlow Lite.

## Что делает

- Захват экрана в реальном времени через MediaProjection API.
- Распознавание объектов на кадре через нейросеть SSD MobileNet.
- Отрисовка боксов поверх других приложений через WindowManager.
- Работает без рута.
- Поддержка Shizuku для расширенных разрешений.

## Возможности

- ESP: боксы поверх распознанных объектов.
- Авто-наведение через AccessibilityService.
- Настройки FPS, прозрачности, толщины линий, цвета.
- Настройка FOV и скорости наведения.
- Публикация в Telegram-бот (опционально).

## Стек

- Kotlin
- Android SDK 34
- TensorFlow Lite 2.14
- Shizuku API
- Gradle 8.11.1

## Сборка

### Через GitHub Actions

1. Залей проект в репозиторий.
2. Убедись, что `.github/workflows/build.yml` на месте.
3. Actions → Build APK → Run workflow.
4. Скачай APK из Artifacts.

### Локально

```bash
git clone https://github.com/Osinter777/GameOverlay.git
cd GameOverlay
./gradlew assembleDebug
```

APK появится в app/build/outputs/apk/debug/app-debug.apk.

Установка

1. Разреши установку из неизвестных источников.
2. Установи APK.
3. Открой приложение.
4. Разреши оверлей.
5. Разреши захват экрана.
6. Включи AccessibilityService в настройках.
7. Нажми «Запустить оверлей».

Требования

· Android 8.0+ (API 26+).
· 4 ГБ RAM рекомендуется.
· Разрешения: SYSTEM_ALERT_WINDOW, FOREGROUND_SERVICE, BIND_ACCESSIBILITY_SERVICE.

Структура

```
app/
├── src/main/
│   ├── java/com/fox/gameoverlay/
│   │   ├── MainActivity.kt
│   │   ├── OverlayService.kt
│   │   ├── OverlayView.kt
│   │   ├── ScreenCaptureService.kt
│   │   ├── TFLiteHelper.kt
│   │   ├── AimAccessibilityService.kt
│   │   └── SettingsActivity.kt
│   ├── res/
│   │   ├── layout/
│   │   ├── values/
│   │   └── xml/
│   ├── assets/
│   │   └── ssd_mobilenet.tflite
│   └── AndroidManifest.xml
└── build.gradle.kts
```

Важно

Проект создан в образовательных целях. Автор не несёт ответственности за использование в нарушение правил игровых платформ.

Лицензия

MIT
