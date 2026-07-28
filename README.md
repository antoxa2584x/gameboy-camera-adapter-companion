# GBCam Companion

A modern Android companion application for the [RP2040-based GameBoy Camera Adapter](https://github.com/antoxa2584x/gameboy-camera-adapter) (my fork of the original project). This app allows you to preview, capture, and manage photos from your GameBoy Camera directly on your Android device via USB-Serial — and print photos (including any image from your phone) on a real GameBoy Printer.

## ✨ Key Features

- **Live Photo Capture**: Automatically receives and decodes images transmitted via the GameBoy Printer protocol from the RP2040 adapter.
- **Dynamic Image Decoding**: Support for various print sizes (x2, x3 height) and high-quality rendering using optimized tile-to-bitmap conversion.
- **Built-in Gallery**: View, save, and manage your captured photos in a retro-styled grid.
- **Custom Color Palettes**: Apply classic Pocket Camera palettes to your photos before saving.
- **GameBoy Printer Printing**:
    - Print captured photos back to a real GameBoy Printer connected to the adapter.
    - Print **any photo from your device**: pick an image, crop it manually (drag & pinch) inside the 160×144 GB frame, and tune the print darkness with an exposure slider.
    - Pixel-exact 4-shade preview of what will come out of the printer.
    - Requires adapter firmware newer than **v2.0.2**.
- **Hardware Integration**:
    - Real-time LED status monitoring.
    - Remote LED color configuration (RGB mode support).
    - Mobile compatibility mode switch (Android USB / iOS web-only).
    - Automatic device discovery and connection management.
- **Smart Saving**:
    - High-quality JPEG export with custom EXIF metadata (Make, Model, Software, and applied Color Scheme).
    - Automatic integration with the Android Gallery/Camera Roll (DCIM/GBCamAdapter).
    - Support for both Scoped Storage (Android 10+) and legacy storage permissions.
- **Update Notifications**: Checks GitHub releases for adapter-firmware updates. App updates are delivered through Google Play's in-app update flow.
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
- An **RP2040-based GameBoy Camera Adapter** running [https://github.com/antoxa2584x/gameboy-camera-adapter](https://github.com/antoxa2584x/gameboy-camera-adapter) firmware 1.4.8 or later.
- A USB OTG cable to connect the adapter to your Android device.
- For printing to a real GameBoy Printer: firmware **newer than 2.0.2** (firmware updates are flashed from a PC) and the printer attached to the adapter's link cable port.

## 📦 Installation

1. Download the latest APK from the [Releases](https://github.com/antoxa2584x/gameboy-camera-adapter-companion/releases/) page.
2. Install the APK on your Android device (Minimum SDK: 24 / Android 7.0).
3. Alternatively, you can clone this repository and build the project in **Android Studio (Ladybug or newer)**.

### Building from source

The app uses Firebase (Analytics + Crashlytics), and `app/google-services.json` is
deliberately not committed. The build fails without it, so before building you need to
add your own Android app with package name `ua.retrogaming.gcac` to a
[Firebase project](https://console.firebase.google.com/), then download its
`google-services.json` into `app/`.

## 🎮 How to Use

### Capturing photos

1. Connect your RP2040 adapter to your phone via USB OTG.
2. Grant the app permission to access the USB device.
3. Use your GameBoy Camera to "Print" a photo.
4. The app will automatically catch the data and display the "Printing..." status.
5. Once received, the photo will appear in your gallery.
6. Open any photo to apply a color palette or save it to your device's camera roll.

### Printing to a real GameBoy Printer

1. Connect a GameBoy Printer to the adapter's link cable port (adapter firmware newer than v2.0.2 required).
2. To print a captured photo, open it from the gallery and tap **Print**.
3. To print any photo from your device, open the menu button and tap **Print**, then:
    - Pick an image from your phone.
    - Drag and pinch to frame the crop inside the 160×144 print area.
    - Adjust the exposure slider — the preview shows the print result.
    - Tap **Print**.

## 🤝 Credits & Acknowledgments

- **Font**: "Press Start 2P" by CodeMan38.

---
*Developed with ❤️ for retro gaming enthusiasts.*
