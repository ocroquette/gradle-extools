package com.github.ocroquette.extools

import com.github.ocroquette.extools.internal.utils.PathResolver
import com.github.ocroquette.extools.testutils.Os
import spock.lang.IgnoreIf
import spock.lang.Specification

class PathResolverTest extends Specification{
    static final DIR = new File("src/test/resources/pathresolver")

    // Skip the Unix test on Windows, because canExecute() is broken and PathResolver will therefore find "not_executable"
    @IgnoreIf({ Os.isWindows() })
    def "find the proper file in a single directory on Unix"() {
        given:
        def list = ["unix/dir1"].collect {new File(DIR, it)}
        def resolver = new PathResolver(list)
        resolver.forceOperatingSystem(PathResolver.OperatingSystem.UNIX)

        expect:
        resolver.find(requested) == ( expected == null ? null : new File(DIR, expected) )

        where:
        requested           | expected
        "executable"        | "unix/dir1/executable"
        "Executable"        | null                      // case sensitive
        "not_executable"    | null                      // not executable
        "file.exe"          | null                      // not executable
        "file"              | null                      // wrong name
    }

    // Skip the Unix test on Windows, because canExecute() is broken and PathResolver will therefore find "not_executable"
    @IgnoreIf({ Os.isWindows() })
    def "find the proper file in multiple directories on Unix"() {
        given:
        def list = ["unix/dir1", "unix/dir2"].collect {new File(DIR, it)}
        def resolver = new PathResolver(list)
        resolver.forceOperatingSystem(PathResolver.OperatingSystem.UNIX)

        expect:
        resolver.find(requested) == ( expected == null ? null : new File(DIR, expected) )

        where:
        requested           | expected
        "executable"        | "unix/dir1/executable"    // dir1 has precedence
        "not_executable"    | null                      // not executable
        "file.exe"          | "unix/dir2/file.exe"      // executable in dir2
        "file"              | null                      // wrong name
    }


    def "find the proper file in single directory on Windows"() {
        given:
        def list = ["windows/dir1"].collect {new File(DIR, it)}
        def resolver = new PathResolver(list)
        resolver.forceOperatingSystem(PathResolver.OperatingSystem.WINDOWS)
        resolver.PATHEXT = ".COM;.EXE;.BAT"

        expect:
        resolver.find(requested) == ( expected == null ? null : new File(DIR, expected) )

        where:
        requested           | expected
        "file1"             | "windows/dir1/file1.exe"  // file1.exe must have precedence over file1.bat
        "FILE1"             | "windows/dir1/file1.exe"
        "FILE1.exe"         | "windows/dir1/file1.exe"
        "file1.bat"         | "windows/dir1/file1.bat"
        "FILE1.bat"         | "windows/dir1/file1.bat"
        "file2"             | null                      // .CMD is not in PATHEXT
        "file2.cmd"         | null                      // .CMD is not in PATHEXT
        "subdir.exe"        | null                      // it's a directory
    }

    def "find the proper file in multiple directories on Windows"() {
        given:
        def list = ["windows/dir1", "windows/dir2"].collect {new File(DIR, it)}
        def resolver = new PathResolver(list)
        resolver.forceOperatingSystem(PathResolver.OperatingSystem.WINDOWS)
        resolver.PATHEXT = ".COM;.EXE;.BAT"

        expect:
        resolver.find(requested) == ( expected == null ? null : new File(DIR, expected) )

        where:
        requested           | expected
        "file1"             | "windows/dir1/file1.exe"  // file1.exe must have precedence over dir2/file1.exe
        "FILE3"             | "windows/dir2/file3.exe"
        "FILE4"             | "windows/dir2/file4.com"
        "FILE4.com"         | "windows/dir2/file4.com"
    }

}
