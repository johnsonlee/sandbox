# Sandbox - CLAUDE.md

## Project Overview

Sandbox is a Kotlin library that enables rendering Android UI directly on the JVM without requiring an emulator or physical device. It leverages Android's official layoutlib to render layouts, views, and themes on desktop machines.

## Project Structure

```
sandbox/
├── src/main/
│   ├── java/
│   │   ├── android/              # Android framework mocks/delegates
│   │   └── com/android/          # Android resource handling
│   └── kotlin/io/johnsonlee/playground/
│       ├── databind/             # JSON serialization for rendering results
│       ├── sandbox/              # Core rendering engine
│       │   └── resources/        # Resource repository implementations
│       └── util/                 # Utility functions
├── gradle/libs.versions.toml     # Centralized dependency versions
├── build.gradle.kts              # Gradle build configuration
└── settings.gradle.kts           # Gradle settings
```

## Tech Stack

- **Language**: Kotlin (primary) + Java (Android compatibility layer)
- **Build System**: Gradle with Kotlin DSL
- **Java Version**: 17+
- **Key Dependencies**:
  - layoutlib 15.0.3 (Android official rendering engine)
  - layoutlib-api 31.1.2
  - android-tools 31.1.2
  - Jackson 2.13.4 (JSON serialization)
  - protobuf 3.19.3

## Build Commands

```bash
# Build project
./gradlew build

# Run tests
./gradlew check

# Compile only
./gradlew compileKotlin

# Publish to Maven local
./gradlew publishToMavenLocal
```

## Key Components

### Core Rendering

- **`Sandbox.kt`** - Main entry point, orchestrates the rendering pipeline
- **`Environment.kt`** - Configuration, SDK detection, resource initialization
- **`DeviceModel.kt`** - Device specifications (screen size, DPI, density)
- **`SessionParamsBuilder.kt`** - Builds rendering session parameters

### Resource Management

- **`MultiResourceRepository.kt`** - Aggregates resources with priority-based merging
- **`AppResourceRepository.kt`** - Project + library resources
- **`ProjectResourceRepository.kt`** - Local + module resources
- **`AarSourceResourceRepository.kt`** - Library AAR resources
- **`DynamicResourceIdManager.kt`** - Maps resources to Android resource IDs

### Parsing & Callbacks

- **`LayoutlibCallbackImpl.kt`** - Layoutlib callback implementation, resolves resources and loads custom views
- **`LayoutPullParser.kt`** - Custom XML parser for layout files
- **`ResourceParser.kt`** - XML resource parsing with namespace support

### Data & Serialization

- **`RenderData.kt`** - Rendering output (image, view tree)
- **`ViewInfoListSerializer.kt`** - Serializes view hierarchies to JSON
- **`BufferedImageSerializer.kt`** - Encodes images to Base64 PNG

## Usage Example

```kotlin
val environment = Environment()
val sandbox = Sandbox(environment)

val result = sandbox.run(showLayoutBounds = false) { context, parent ->
    // Inflate layout here
}.getOrThrow()

// Access: result.image, result.rootViews, result.systemViews
```

## Code Conventions

1. **Error Handling**: Uses Kotlin `Result<T>` for safe error propagation
2. **Lazy Initialization**: Properties computed only when needed
3. **Extension Functions**: Heavy use for utility methods
4. **Immutable Data Classes**: Configuration objects are immutable
5. **Reflection**: Used extensively to access internal layoutlib APIs

## Resource Resolution Priority

```
Framework Resources (Android)
    ↓
Project Resources (local + modules)
    ↓
Library Resources (AARs)
```

## Requirements

- Java 17+
- Android SDK with platform-31 (configurable via `COMPILE_SDK_VERSION` property)
- Gradle (wrapper included)
