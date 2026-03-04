# GameBoy Camera Adapter Companion (GCAC)

A modern Android companion application for the [RP2040-based GameBoy Camera Adapter](https://github.com/antoxa2584x/gameboy-camera-adapter) (my fork of the original project). This app allows you to preview, capture, and manage photos from your GameBoy Camera directly on your Android device via USB-Serial.

## ✨ Key Features

- **Live Photo Capture**: Automatically receives and decodes images transmitted via the GameBoy Printer protocol from the RP2040 adapter.
- **Dynamic Image Decoding**: Support for various print sizes (x2, x3 height) and high-quality rendering using optimized tile-to-bitmap conversion.
- **Built-in Gallery**: View, save, and manage your captured photos in a retro-styled grid.
- **Custom Color Palettes**: Apply classic Pocket Camera palettes to your photos before saving.
- **Hardware Integration**:
    - Real-time LED status monitoring.
    - Remote LED color configuration (RGB mode support).
    - Automatic device discovery and connection management.
- **Smart Saving**:
    - High-quality JPEG export with custom EXIF metadata (Make, Model, Software, and applied Color Scheme).
    - Automatic integration with the Android Gallery/Camera Roll (DCIM/GBCamAdapter).
    - Support for both Scoped Storage (Android 10+) and legacy storage permissions.
- **Multi-language Support**: Available in **English** and **Ukrainian**.
- **Retro Aesthetic**: Pixel-art UI inspired by the GameBoy era, featuring the "Press Start 2P" font.

## 🛠️ Technical Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/compose) for a modern, reactive user interface.
- **Dependency Injection**: [Koin](https://insert-koin.io/) for lightweight and efficient DI.
- **Serial Communication**: [usb-serial-for-android](https://github.com/mik3y/usb-serial-for-android) for robust USB connectivity.
- **State Management**: [Kotpref](https://github.com/chibatching/Kotpref) for persistent shared preferences.
- **Image Processing**: [Coil](https://coil-kt.github.io/coil/) with custom transformations for real-time palette application.
- **Architecture**: Layered Clean Architecture (Core, Data, UI, DI, Util).

## 🚀 Hardware Requirements

To use this app, you need a compatible hardware adapter:
- An **RP2040-based GameBoy Camera Adapter** running [https://github.com/antoxa2584x/gameboy-camera-adapter](https://github.com/antoxa2584x/gameboy-camera-adapter) firmware 1.4.7 or later.
- A USB OTG cable to connect the adapter to your Android device.

## 📦 Installation

1. Clone this repository.
2. Open the project in **Android Studio (Ladybug or newer)**.
3. Build and install the APK on your Android device (Minimum SDK: 24 / Android 7.0).

## 🎮 How to Use

1. Connect your RP2040 adapter to your phone via USB OTG.
2. Grant the app permission to access the USB device.
3. Use your GameBoy Camera to "Print" a photo.
4. The app will automatically catch the data and display the "Printing..." status.
5. Once received, the photo will appear in your gallery.
6. Open any photo to apply a color palette or save it to your device's camera roll.

## 🤝 Credits & Acknowledgments

- **Font**: "Press Start 2P" by CodeMan38.

---
*Developed with ❤️ for retro gaming enthusiasts.*
