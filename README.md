# Extools plugin for Gradle
## Introduction
Groovy and Gradle are very good at downloading JVM based dependencies from jcenter or Maven central, but they come short when it comes to download external tools like compilers.

The "extools" plugin for Gradle provides a convenient way to do so. 

## Basic usage

Let's assume you want to call the program "myclitool" in your build, and that "myclitool" is provided as part of "mytoolkit", and you want it to be downloaded and used automatically when required. Just add the following lines to your build.gradle:

```
// Apply the extools plugin
plugins {
    id 'extools'
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

Currently, the plugin itself is not available in any central repositories, so you need to download the source and build it:

```
gradlew jar
```

Then copy the JAR file to the project where you want to use the plugin, and add it to the class path:

```
buildscript {
    repositories {
        flatDir dirs: "gradle/libs"
    }
    dependencies {
        classpath "fr.ocroquette:gradle-extools:1.2-SNAPSHOT"
    }
}
```

## Creating extools packages and repositories

There is no central, public repository, and there will probably never be any, so you will have to create and maintain your own.

Creating a package is pretty easy. Just put the put all the content you need in a directory "dir", and add a text file called "extools.conf":

```
dir/bin/myclitool
dir/extools.conf
```

The file "extools.conf" allows to extend the environment of the host when the extool is used. In this simple case, ```extools.conf``` should contain the following line:

```
append;env;path;PATH;bin
```

When this extool will be used, the "bin" subdirectory will be added to the PATH variable, allowing to find "myclitools".

We now need to package the extool. It is very easy, since the ZIP format is used. Just make sure that "extools.conf" is at the root of the content of the ZIP file, and that the level "dir" is not used. For instance, when using zip on the command line on a Unix like system:
```
cd dir
zip -r ../mytoolkit.ext .
```

As you can see, the extension ".ext" is used for extools packages.

Creating a repository is very easy, just put the file ```mytoolkit.ext``` in a directory in the local file system or an HTTP server, and set ```extools.repositoryUrl``` accordingly.

Check out also [gradle-extools-recipes](https://github.com/ocroquette/gradle-extools-recipes), where you can find some recipes for some common tools.

## Additional features
### Explicit tool list
With the following task definition, all extools spsec
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
The example above will work, but if the minor number of the version is changing regularly, it is not nice to have it hard-coded in the task definition. In this case, you can define an alias for the extools in the main configuration:

```
extools {
    tools "mytoolkit-v1" : "mytoolkit-v1.3",
          "mytoolkit-v2" : "mytoolkit-v2.6"
}

task execMyCliTool(type:ExtoolsExec) {
    usingExtools "mytoolkit-v1"
    commandLine "myclitool"
}
```

### Organizing and structuring packages

Structure of packages are supported:
```
extools {
    tools "compiler/gcc-v7.1",
          "lang/perl-5.26",
          "lang/python-2.7.14"
}
```

### Customizing the execution environment

```ExtoolsExec``` extends the standard ```Exec``` Gradle task, so all the features of the later are available, for instance:

```
task execMyCliTool(type:Exec) {
	workingDir '../tomcat/bin'

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
append;env;path;PATH;bin

# Extend the CMAKE_PREFIX_PATH environment variable with the lib/cmake sub-directory
append;env;path;CMAKE_PREFIX_PATH;lib/cmake
```

The separator inserted betweeen the different paths in the variable value is the standard separator used by the operating system for the PATH variable, e.g. ';' on Windows and ':' on Linux, macOS and Unix.

To set an environment variable to a fixed string:

```
# Set environment variable SOME_VAR with the string "Value of SOME_VAR"
set;env;string;SOME_VAR;Value of SOME_VAR
```

### Local cache and extract directory

By default, the plugin will use the global ```.gradle``` directory to store downloaded packages and extract them, so that they can be reused among workspaces and save time. You can specify other directories if required as properties:

```
extools.localCache=<localpath>
extools.extractDir=<localpath>
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

If you need the variable value only within Gradle and not as an environment variables in the child processes, use ```var``` instead of ```env``` in the ```extools.conf``` file:

```
# ExtoolsExec will set the environment variable MY_VARIABLE to "Value of MY_VARIABLE"
set;env;string;MY_VARIABLE;Value of MY_VARIABLE

# ExtoolsExec will NOT set the environment variable MYVAR,
# but you can still access the value with getValue()
set;var;string;MY_VARIABLE;Value of MY_VARIABLE
```
