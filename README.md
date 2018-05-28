# Extools plugin for Gradle
## Introduction
Groovy and Gradle are very good at downloading Java based dependencies from jcenter or Maven central, but they come short when it comes to download external tools (like compilers or installer generators).

The "extools" plugin for Gradle provides a convenient way to do so. It is basically a portable app manager within Gradle. It automatically downloads the tools required by a Gradle build, extracts them and makes them available for execution on the fly, without any installation or changes to the host system.

## Prerequisites

The extools plugin has no special dependencies apart from:

* Gradle 4
* Any Java runtime supported by Gradle. The Java 9 is not recommended because it displays warnings with Groovy code.

You also need a basic understanding of Gradle. More precisely, you need to be able to start a new Gradle project. The recommended way is to use the wrapper, which provides two major benefits to your users: automatic download of Gradle itself (no local installation required), and consistency of the Gradle version used.

## Basic usage

Let's assume that you have a Gradle project and you want to call the program ```myclitool``` from your build, and that ```myclitool``` is provided as part of ```mytoolkit```. Just [check for the lastest version available](https://plugins.gradle.org/plugin/com.github.ocroquette.extools	), and add the following lines to your build.gradle:

```
// Apply the extools plugin (using the plugin syntax introduced in Gradle 2.1)
plugins {
    id 'com.github.ocroquette.extools' version '1.11'
}

// Configure the plugin, assuming a repo URL has already been set as a property (see below)
extools {
    // Define a dependency to the extool called "mytoolkit"
    tool "mytoolkit"
}

// Define a task similar to Gradle's standard "Exec" task, but that one uses some external tools
import com.github.ocroquette.extools.tasks.ExtoolExec
task execMyCliTool(type:ExtoolExec) {
    usingExtools "mytoolkit"
    commandLine "myclitool"
}

// You can also execute an extool from any task, similarly to Gradle's standard "exec {}" statement,
// but in this case, you need to define a dependency to the task "extoolsLoad", which
// loads the required tools metadata
task doStuff {
    dependsOn "extoolsLoad"
    dolast {
        extoolexec {
            usingExtools "mytoolkit"
            commandLine "myclitool", "CLI parameter for first run"
        }
        extoolexec {
            usingExtools "mytoolkit"
            commandLine "myclitool", "CLI parameter for the second run"
        }
    }
}
```

For this to work, you will need to set the URL of the extools repository a property, typically in gradle.properties:

```
extools.repositoryUrl=file:/...
```

or

```
extools.repositoryUrl=http://
```

## Creating extools packages and repositories

There is no central, public extools repository, and there will probably never be any, so you have to create and maintain your own.

Creating an extool package is pretty easy. Just put all the content you need in a directory ```dir```, and add a text file called ```extools.conf```. Here is an example:

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

Whithin the ZIP file, the file structure should like this:

```
bin/myclitool
extools.conf
```

Creating a repository is very easy, just put the file ```mytoolkit.ext``` in a directory in the local file system or on an HTTP server, and set ```extools.repositoryUrl``` accordingly.

It is recommended to automate the generation of the packages. Gradle itself is the perfect tool since it provides all the required features like unzipping, zipping, file manipulation... See [gradle-extools-recipes](https://github.com/ocroquette/gradle-extools-recipes) for sample recipes.


## Additional features
### Execution options

When executing an extool as ```ExtoolExec``` task or with the ```extoolexec``` statement, the following options are supported.

Options common to Gradle's standard ```exec``` mechanism:

* ```commandLine```: the full command line with parameters, as a list
* ```executable```: the name of the executable
* ```args```: a list of arguments to provide to the executable 
* ```environment```: a map containing the environment variables to set in the child process
* ```standardOutput```: the output stream to use for the error stream of the child process
* ```errorOutput```: the output stream to use for the error stream
* ```standardInput```: the input stream to use for the input stream of the child process
* ```ignoreExitValue```: a boolean that indicates if the non zero exit values from the child process must be ignored
* ```workingDir```: a file or path to set as working directory for the child process

Additional options:

* ```usingExtools```: the list of aliases of the extools to use for the execution
* ```runInBackground```: a boolean to indicate if the tool should be run synchronously (false, default) or asynchronously in the background (true)

### Using tools implicitly

In case you need some tools to be available implicitly in all executions, use the ```usingExtools``` statement in the global configuration:

```
extools {
    tools "mytoolkit"
    usingExtools "mytoolkit" // "mytoolkit" will be available in all extools executions
}

task execMyCliTool(type:ExtoolExec) {
    // No need to specify a dependency to "mytoolkit"
    commandLine "myclitool"
}
```

### Tool aliases
Usually, the real name of the extools will have a version number in it. When you update the tool, you will have to update the name everywhere it is used, in the global ```extools {}``` block and in the task definitions. To avoid this, you can define aliases in the main configuration:

```
extools {
    // "mytoolkit-v1.3" will be loaded from the repository, and made available
    // under the alias "mytoolkit" within the build
    tools "mytoolkit": "mytoolkit-v1.3",
          "othertoolkit" : "othertoolkit-v2.6-rc3"
}

task execMyCliTool(type:ExtoolExec) {
    usingExtools "mytoolkit"
    commandLine "myclitool"
}
```

### Organizing and structuring packages

You can structure the extools in the repository into subfolders if you want. In the build script, they must be refered to with their complete relative path:

```
extools {
    tools "compiler/gcc-v7.1",
          "lang/perl-5.26",
          "lang/python-2.7.14"
}
```

You can use aliases as described above to remove or change the naming structure within a given build:

```
extools {
    tools "dev/gcc":     "compiler/gcc-v7.1",
          "misc/perl":   "lang/perl-5.26",
          "misc/python": "lang/python-2.7.14"
}
```


### Customizing the execution environment

```ExtoolExec``` and ```extoolexec``` extend the standard ```Exec``` Gradle task, so all the features of the latter are available. Additionally, you can prepend paths to environment variables using prependEnvPaths:


```
task execMyCliTool(type:Exec) {
	workingDir "../some/folder"

	environment "VAR": "VALUE"
	prependEnvPaths "PATH": new File(workingDir, "subdir")

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

The separator inserted between the different paths in the variable value is the standard separator used by the operating system for the PATH variable, e.g. ';' on Windows and ':' on Linux, macOS and Unix.

To set an environment variable to a fixed string:

```
# Set the environment variable called SOME_VAR to "Value of SOME_VAR"
set;env;string;SOME_VAR;Value of SOME_VAR
```


### Using internal variables, not exported to the environment

If you need the variable value only within Gradle and not as an environment variables in the child processes, use ```var``` instead of ```env``` in the ```extools.conf``` file:

```
# ExtoolExec will set the environment variable MY_VARIABLE to "Value of MY_VARIABLE"
set;env;string;MY_VARIABLE;Value of MY_VARIABLE

# The plugin will NOT set the environment variable MYVAR,
# but you can still access the value with getValue()
set;var;string;MY_VARIABLE;Value of MY_VARIABLE
```


### Local cache and extract directory

By default, the plugin will store downloaded packages and extract them in the user directory ```.gradle/extools```, so that they can be reused, saving time and space. This also allows for offline work. You can specify other directories if required as properties on the command line or in a ```gradle.properties``` file.

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
        println project.extensions.extools.getValueWithDefault("toolalias", "MY_VARIABLE", "Default value")
    }
}
```

Be aware of the build phases, the task definition will be executed by Gradle during the configuration phase. At this point, the task ```extoolsLoad``` task didn't run, and the extools configuration is not available. In this case, use Groovy's lazy string evaluation:

```
${->project.extensions.extools.getValue("toolalias", "MY_VARIABLE")}
```

### Getting the list of loaded aliases

The ```getLoadedAliases()``` returns a sorted array containing the aliases of all loaded tools:

```
task getLoadedAliases {
    dependsOn "extoolsLoad"

    doLast {
        println project.extensions.extools.getLoadedAliases()
    }
}
```

getLoadedAliases

### Resolving aliases

You can resolve an alias by using the ```resolvealias()``` function:

```
task resolveAlias {
    dependsOn "extoolsLoad"

    doLast {
        println project.extensions.extools.resolveAlias("toolalias")
    }
}
```

### Generating scripts

You can generate scripts containing the environment variables of tool by using the ``generateEnvironmentScript()```
function. On Windows, it might return a string like:

```
set VAR1=value1
set PATH=...;%PATH%
```

The ```generateEnvironmentScript()``` will generate CMD scripts when running on Windows and shell scripts on other
platforms, but you can force a syntax by using ```generateEnvironmentScriptCmd()``` or ```generateEnvironmentScriptShell()```

### Interactive use

It is possible to run commands using extools without modifying the build script. The task called "extoolsExec" can be used for that:

```
gradlew extoolsExec "-PcommandLine=..." "-PusingExtools=..."
```

The command line parameters are separated by spaces. If you need spaces within the arguments, use a file based script.

The list of extools is comma separated.

The result is equivalent to the following task in the build script:

```
task someTaskName(type:ExtoolExec) {
    usingExtools ...
    commandLine ...
}
```



