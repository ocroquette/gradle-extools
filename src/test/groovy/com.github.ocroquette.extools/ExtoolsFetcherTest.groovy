package com.github.ocroquette.extools

import com.github.ocroquette.extools.ExtoolsFetcher
import com.github.tomakehurst.wiremock.junit.WireMockRule
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification
import static com.github.tomakehurst.wiremock.client.WireMock.*
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

class ExtoolsFetcherTest extends Specification {
//    WireMockServer wireMockServer = new WireMockServer(options().port(8888))
//    WireMock wireMock = new WireMock("localhost", 8888)

    static final EXTOOL_NAME = "some-tools-2.0"
    static final EXTOOL_FILENAME = EXTOOL_NAME + ".ext"
    static final EXTOOL_FILE_CONTENT = "Content of " + EXTOOL_FILENAME

    static final EXTOOL_SUBDIR = "subdir"

    @Rule
    TemporaryFolder temporaryFolder

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(wireMockConfig().dynamicPort().dynamicHttpsPort());

    File targetDirectory

    ExtoolsFetcher fetcher

    def setup() {
        targetDirectory = temporaryFolder.newFolder()
        fetcher = new ExtoolsFetcher(new URL("http://localhost:" + wireMockRule.port()), targetDirectory)
        wireMockRule.start()
    }

    def cleanup() {
        wireMockRule.stop()
    }

    def "must download remote files"() {
        given:
        wireMockRule.stubFor(get(urlEqualTo("/$EXTOOL_FILENAME" ))
                .willReturn(aResponse()
                .withHeader("Content-Type", "text/plain")
                .withBody(EXTOOL_FILE_CONTENT)));

        when:
        fetcher.fetch(EXTOOL_NAME)

        then: "the file has been downloaded and has the expected content"
        with(new File(targetDirectory, EXTOOL_FILENAME)) {
            exists()
            text == EXTOOL_FILE_CONTENT
        }
    }

    def "must download remote files in sub-directories"() {
        given:
        wireMockRule.stubFor(get(urlEqualTo("/$EXTOOL_SUBDIR/$EXTOOL_FILENAME"))
                .willReturn(aResponse()
                .withHeader("Content-Type", "text/plain")
                .withBody(EXTOOL_FILE_CONTENT)));

        when:
        fetcher.fetch("$EXTOOL_SUBDIR/$EXTOOL_NAME")

        then: "the file has been downloaded and has the expected content"
        with(new File(targetDirectory, "$EXTOOL_SUBDIR/$EXTOOL_FILENAME")) {
            exists()
            text == EXTOOL_FILE_CONTENT
        }
    }

    def "must throw exception on missing remote files"() {
        given:
        wireMockRule.stubFor(get(urlEqualTo("/" + EXTOOL_FILENAME))
                .willReturn(aResponse()
                .withStatus(404)));

        when:
        fetcher.fetch(EXTOOL_NAME)

        then: "the file has been downloaded and has the expected content"
        with(new File(targetDirectory, EXTOOL_FILENAME)) {
            !exists()
        }
        thrown(FileNotFoundException)
    }
}
