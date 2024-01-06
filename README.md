## Sandbox

Sandbox is a library that allows rendering Android UI directly on the JVM, without the need for an emulator or an Android device. This innovative tool provides a streamlined and efficient development experience for Android applications, enabling developers to render and test UI elements quickly and easily on their development machines. By eliminating the dependence on emulators or physical devices, Sandbox significantly speeds up the development and testing process, making it a valuable asset for Android developers looking to optimize their workflow..

## Getting Started

### Configure `build.gradle`

```kotlin
dependency {
  implementation("io.johnsonlee.playground:sandbox:1.2.0")
}
```

### Extract Dependent AARs

This process can be automated via [Gradle TransformAction](https://docs.gradle.org/current/dsl/org.gradle.api.artifacts.transform.TransformAction.html)

*NOTE: please ignore this step if your project doesn't depends on any AAR library*

### Setup Sandbox Environment

```kotlin
fun setup(extractedAarDir: File): Environment {
    val libraries = extractedAarDir.listFiles { aar ->
        aar.resolve(SdkConstants.FN_ANDROID_MANIFEST_XML).exists()
    }?.toList() ?: emptyList()

    return Environment(
        resourcePackageNames = libraries.map { aar ->
            AndroidManifestParser.parse(aar.resolve(SdkConstants.FN_ANDROID_MANIFEST_XML).toPath()).`package`
        },
        libraryResourceDirs = libraries.map { aar ->
            aar.resolve(SdkConstants.FD_RES)
        }.filter(File::exists).map(File::getPath),
        libraryAssetDirs = libraries.map { aar ->
            aar.resolve(SdkConstants.FD_ASSETS)
        }.filter(File::exists).map(File::getPath)
    )
)
```

### Render your UI

```kotlin
val environment = setup(extractedAarDir)
val sandbox = Sandbox(environment)

// ...

val result = sandbox.run(
    showLayoutBounds = false
) { context, parent ->
   // TODO: inflate the layout
}.getOrThrow()

// TODO: handle the rendering result
```
