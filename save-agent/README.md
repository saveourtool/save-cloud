# Building
Read the [official docs](https://github.com/JetBrains/kotlin/tree/master/kotlin-native#kotlinnative) and install all dependencies listed in [Prerequisites](https://github.com/JetBrains/kotlin/tree/master/kotlin-native#building-from-source) section.

On Windows you'll also need to install msys2 and run `pacman -S mingw-w64-x86_64-curl` to have libcurl for ktor-client.
On ubuntu install `libcurl4-openssl-dev` for ktor client.

`save-agent` also requires `unzip` to be present on `$PATH`.
