import me.roundaround.gradle.extension.library.LibModule

plugins {
  id("roundalib-gradle") version "1.0.0"
}

roundalib {
  library {
    local = true
    version = "3.4.1"
    modules.addAll(LibModule.CORE, LibModule.GUI, LibModule.CONFIG, LibModule.CONFIG_GUI, LibModule.NETWORK, LibModule.OBSERVABLES)
  }
}
