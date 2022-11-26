<p align="center">
<h3 align="center">Colliders</h3>

------

<p align="center">
Implementation of non-traditional minecraft colliders. It is useful for people with basic understanding of java, gradle, workflow and is designed for lazy people
</p>

<p align="center">
<img alt="License" src="https://img.shields.io/github/license/CKATEPTb-minecraft/Colliders">
<a href="#Download"><img alt="Sonatype Nexus (Snapshots)" src="https://img.shields.io/nexus/s/dev.ckateptb.minecraft/Colliders?label=repo&server=https://repo.animecraft.fun/"></a>
<img alt="Publish" src="https://img.shields.io/github/workflow/status/CKATEPTb-minecraft/Colliders/Publish/production">
<a href="https://docs.gradle.org/7.5/release-notes.html"><img src="https://img.shields.io/badge/Gradle-7.5-brightgreen.svg?colorB=469C00&logo=gradle"></a>
<a href="https://discord.gg/P7FaqjcATp" target="_blank"><img alt="Discord" src="https://img.shields.io/discord/925686623222505482?label=discord"></a>
</p>

------

# Versioning

We use [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html) to manage our releases.

# Features

- [X] Easy to use
- [X] Implements the following collision systems
  - [X] AxisAlignedBoundingBoxCollider
  - [X] SphereBoundingBoxCollider
  - [X] OrientedBoundingBoxCollider
  - [X] CombinedBoundingBoxCollider
  - [X] DiskBoundingBoxCollider
- [X] ThreadSafe
- [ ] Documented

# Download

Download from our repository or depend via Gradle:

```kotlin
repositories {
    maven("https://repo.animecraft.fun/repository/maven-snapshots/")
}
dependencies {
    implementation("dev.ckateptb.minecraft:Colliders:<version>")
}
```

# How To

* Import the dependency [as shown above](#Download)
* Add Colliders as a dependency to your `plugin.yml`
```yaml
name: ...
version: ...
main: ...
depend: [ Colliders ]
authors: ...
description: ...
```
* Use Colliders class for create custom colliders
* See example in CollidersCommand
* Start work