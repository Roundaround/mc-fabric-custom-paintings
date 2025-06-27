import me.roundaround.gradle.extension.library.LibModule

plugins {
  id("roundalib-gradle") version "1.0.0"
}

roundalib {
  library {
    local = true
<<<<<<< HEAD
    version = "3.1.0"
=======
    version = "3.2.0"
>>>>>>> 3.0.2+1.21.6
    modules.addAll(LibModule.CONFIG_GUI, LibModule.NETWORK)
  }
}
