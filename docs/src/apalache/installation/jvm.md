# Running in Java Virtual Machine

You have to download and install a Java Virtual Machine first. For instance,
[AdoptOpenJDK](https://adoptopenjdk.net/) should work (we tried Apalache with
OpenJDK 11 and OpenJDK 15).

Once you have installed Java, download the [latest
release](https://github.com/informalsystems/apalache/releases) and unpack into
a directory of your choice. Depending on your OS, you have two options.

*Option 1: Linux, MacOS.* You can run the script `./bin/apalache-mc`, or,
better, add the `./bin` directory to your `PATH` and run `apalache-mc`.

*Option 2: Windows.* You have to run Java directly with

  ```
  java.exe -jar ./lib/apalache.jar <args>
  ```

  The arguments `<args>` are explained in [Running the Tool](../running.md).

If you would like to contribute a command-line script for running Apalache in
Windows, please [open a pull
request](https://github.com/informalsystems/apalache/blob/unstable/CONTRIBUTING.md#making-a-pull-request).