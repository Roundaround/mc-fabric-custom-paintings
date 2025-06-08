import me.roundaround.gradle.extension.library.LibModule

plugins {
  id("roundalib-gradle") version "1.0.0"
}

roundalib {
  library {
    local = true
    version = "3.1.0"
    modules.addAll(LibModule.CONFIG_GUI, LibModule.NETWORK)
  }
}
