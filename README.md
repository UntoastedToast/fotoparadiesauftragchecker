# 📸 Fotoparadies Auftrag Checker

📱 An Android application that helps you track your dm-Fotoparadies photo orders. Stay updated on your order status with automated background checks and notifications.

## ✨ Features

- 📦 Track multiple photo orders simultaneously
- 🔄 Real-time order status updates
- 🔔 Push notifications for status changes
- ⚡ Background order status checking
- 💾 Local order history storage
- ⚙️ Easy order management with add/delete functionality
- ⏰ Customizable settings for check intervals

## 🛠️ Technology Stack

- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel)
- **Database**: Room Persistence Library
- **Background Processing**: WorkManager
- **UI Components**: Android Material Design
- **API Integration**: Retrofit for dm-Fotoparadies API

## 📁 Project Structure

```
app/src/main/
├── java/com/app/fotoparadiesauftragchecker/
│   ├── adapter/          # RecyclerView adapters
│   ├── api/             # API service interfaces
│   ├── data/            # Database and data models
│   ├── notification/    # Notification handling
│   ├── viewmodel/       # ViewModels for UI
│   └── worker/         # Background workers
```

## 🚀 Setup

1. Clone the repository
2. Open the project in Android Studio
3. Sync project with Gradle files
4. Run the app on an emulator or physical device

## 📋 Requirements

- Android 6.0 (API level 23) or higher
- Internet permission for order status checks
- Notification permission for status updates

## 📱 Usage

1. Launch the app
2. Add a new photo order using the plus button
3. Enter your order number
4. The app will automatically check the status periodically
5. Receive notifications when your order status changes
6. View all your orders and their current status in the main screen
7. Delete orders when no longer needed
