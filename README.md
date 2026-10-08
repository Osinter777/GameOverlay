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
