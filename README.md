# Extools plugin for Gradle
## Introduction
Groovy and Gradle are very good at downloading JVM based dependencies from jcenter or Maven central, but they come short when it comes to download external tools (like compilers or installer generators).

The "extools" plugin for Gradle provides a convenient way to do so. It is basically a portable app manager within Gradle. It automatically downloads the tools requires by a Gradle build, extracts them and make them available for execution, without any installation or changes to the host system.

## Prerequisites

The extools plugin have no special dependencies apart from:
* Gradle 4
* Any Java runtime supported by Gradle

You need also basic understanding of Gradle. More precisely, you need to be able to start a new Gradle project. The recommended way is to use the wrapper, which provides two major benefits to your users: automatic download of Gradle itself (no local installation required), and consistency of the Gradle version used.

## Basic usage

Let's assume that you have a Gradle project and you want to call the program ```myclitool``` from your build, and that ```myclitool``` is provided as part of ```mytoolkit```. Just add the following lines to your build.gradle:

```
// Apply the extools plugin (please check and use the latest version)
plugins {
    id 'com.github.ocroquette.extools' version '1.7'
}

// Configure the plugin
extools {
    // Define a dependency to the extool called "mytoolkit"
    tool "mytoolkit"
}

// Import the definition of the custom task type
import com.github.ocroquette.extools.ExtoolsExec

// Define a task that uses the external tools
task execMyCliTool(type:ExtoolsExec) {
    commandLine "myclitool"
}

```

For this to work, you will need to set the URL of the  extool repository a property, typically in gradle.properties:

```
extools.repositoryUrl=file:/...
```

or

```
extools.repositoryUrl=http://
```

## Creating extools packages and repositories

There is no central, public extools repository, and there will probably never be any, so you will have to create and maintain your own.

Creating an extool package is pretty easy. Just put the put all the content you need in a directory ```dir```, and add a text file called ```extools.conf```. Here is the file structure you should have at this point:

```
dir/bin/myclitool
dir/extools.conf
```

The file ```extools.conf``` allows to extend the environment of the host when the extool is used. In this simple case, ```extools.conf``` should contain the following line:

```
prepend;env;path;PATH;bin
```

When this extool will be used, the ```bin``` subdirectory will be added at the beginning of the ```PATH``` variable, allowing to find "myclitools".

We now need to package the extool. It is very easy, since the ZIP format is used. Just make sure that ```extools.conf``` is at the root of the content of the ZIP file, and that the level "dir" is not used. For instance, when using zip on the command line on a Unix like system:
```
cd dir
zip -r ../mytoolkit.ext .
```

As you can see, the extension ```.ext``` is used for extools packages.

Creating a repository is very easy, just put the file ```mytoolkit.ext``` in a directory in the local file system or on an HTTP server, and set ```extools.repositoryUrl``` accordingly.

Check out also [gradle-extools-recipes](https://github.com/ocroquette/gradle-extools-recipes), where you will find some recipes for some common tools.

## Additional features
### Explicit tool list

Let's assume you need too major, incompatible versions of the tool kit and the following script:

```
extools {
    tools "mytoolkit-v1.3", "mytoolkit-v2.6"
}

task execMyCliTool(type:ExtoolsExec) {
    commandLine "myclitool"
}
```

By default, the ExtoolsExec task will use all extools specified in the ```extools {}``` block, but in this case, you should specify which exact tool to use:

```
task execMyCliTool(type:ExtoolsExec) {
    usingExtools "mytoolkit-v1.3"
    commandLine "myclitool"
}
```

### Tool aliases
The example above will work, but it is not nice to have the version number specified twice (once in the ```extools {}``` block, and once in the task definition). To avoid this, you can define an alias for the extools in the main configuration:

```
extools {
    // "mytoolkit-v1.3" will be loaded from the repository, and made available
    // under the alias "mytoolkit-v1" within the build
    tools "mytoolkit-v1" : "mytoolkit-v1.3",
          "mytoolkit-v2" : "mytoolkit-v2.6"
}

task execMyCliTool(type:ExtoolsExec) {
    usingExtools "mytoolkit-v1"
    commandLine "myclitool"
}
```

### Organizing and structuring packages

You can structure the extools in the repository in subfolders if you want. They must be refered to with the complete relative path in the build script:

```
extools {
    tools "compiler/gcc-v7.1",
          "lang/perl-5.26",
          "lang/python-2.7.14"
}
```

You can use aliases as described above to remove or change the naming structure within a given build. 

### Customizing the execution environment

```ExtoolsExec``` extends the standard ```Exec``` Gradle task, so all the features of the later are available, for instance:

```
task execMyCliTool(type:Exec) {
	workingDir "../some/folder"

	environment "VAR": "VALUE"

	commandLine 'myclitool'

 	standardOutput = new ByteArrayOutputStream()

 	ext.output = {
		return standardOutput.toString()
	}
}
```

### Setting and extending environment variables

So far, we only extended the PATH variable in ```extools.conf```, but it is possible to extend any environment variable with a path:

```
# Extend the PATH environment variable with the bin/ sub-directory
prepend;env;path;PATH;bin

# Extend the CMAKE_PREFIX_PATH environment variable with the lib/cmake sub-directory
prepend;env;path;CMAKE_PREFIX_PATH;lib/cmake
```

The separator inserted betweeen the different paths in the variable value is the standard separator used by the operating system for the PATH variable, e.g. ';' on Windows and ':' on Linux, macOS and Unix.

To set an environment variable to a fixed string:

```
# Set the environment variable called SOME_VAR to "Value of SOME_VAR"
set;env;string;SOME_VAR;Value of SOME_VAR
```

### Local cache and extract directory

By default, the plugin will use the global ```.gradle``` directory to store downloaded packages and extract them, so that they can be reused among workspaces and save time. You can specify other directories if required as properties on the command line or in a ```gradle.properties``` file.

```
extools.localCache=<localpath>
extools.extractDir=<localpath>
```

### Getting current configuration

The plugin adds a task called ```extoolsInfo``` that will dump on the console all global settings like the paths used and all referenced extools, with their variables, in YAML format:

```
$ gradlew extoolsInfo
...
globalconfig:
  repositoryUrl: http://...
  localCache: ...
  extractDir: ...
tools:
  -
    alias: alias_1
    realname: dummy_1
    variables:
      CMAKE_PREFIX_PATH: ...
      DUMMY1_DIR: ...
      DUMMY1_STRING: Value of DUMMY1_STRING
      DUMMY1_VAR: Value of DUMMY1_VAR
      DUMMY_STRING: Value of DUMMY_STRING from dummy_1
      PATH: ...
    variablesToSetInEnv:
      - DUMMY1_DIR
      - DUMMY1_STRING
      - DUMMY_STRING
    variablesToPrependInEnv:
      - CMAKE_PREFIX_PATH
      - PATH
  -
    alias: alias_2
    realname: dummy_1
    ...
```



### Retrieving extool variable values from other tasks

You can retrieve the values set in the ```extools.conf``` files from Gradle tasks using ```getValue()```:

```
task accessVariable {
    dependsOn "extoolsLoad"

    doLast {
        println project.extensions.extools.getValue("toolalias", "MY_VARIABLE")
    }
}
```

Be aware of the build phases, the task definition will be executed by Gradle during the configuration phase. At this point, the task ```extoolsLoad``` task didn't run, and the extools configuration is not available. In this case, use Groovy's lazy string evaluation:

```
${->project.extensions.extools.getValue("toolalias", "MY_VARIABLE")}
```

### Using internal variables, not exported to the environment

If you need the variable value only within Gradle and not as an environment variables in the child processes, use ```var``` instead of ```env``` in the ```extools.conf``` file:

```
# ExtoolsExec will set the environment variable MY_VARIABLE to "Value of MY_VARIABLE"
set;env;string;MY_VARIABLE;Value of MY_VARIABLE

# ExtoolsExec will NOT set the environment variable MYVAR,
# but you can still access the value with getValue()
set;var;string;MY_VARIABLE;Value of MY_VARIABLE
```
