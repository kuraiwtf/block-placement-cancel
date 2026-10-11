plugins {
  java
}

repositories {
  mavenCentral()
  maven("https://repo.codemc.io/repository/maven-releases/")
  maven("https://repo.codemc.io/repository/maven-snapshots/")
  maven("https://hub.spigotmc.org/nexus/content/repositories/public/")
  maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
}

dependencies {
  compileOnly(libs.spigot)
  compileOnly(libs.packetevents.spigot)
  compileOnly(libs.lombok)
  annotationProcessor(libs.lombok)
}
