/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.gradle.integtests

import org.gradle.integtests.fixtures.AbstractIntegrationSpec
import org.gradle.test.fixtures.file.TestFile
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition

// TODO
@Requires(TestPrecondition.WINDOWS)
class SubstIntegrationTest extends AbstractIntegrationSpec {
    final SUBST_TARGET_DRIVE = 'X:'.toUpperCase()

    def "up to date check works from filesystem's root"() {
        def substRoot = getRoot()

        def (inputFileName, outputFileName) = ["input.txt", "output.txt"]
        def input = substRoot.file(inputFileName)
        input << 'content'
        def output = substRoot.file(outputFileName)

        def taskName = 'inputFromFilesystemRoot'
        def script = /*language=Gradle*/ """
            class InputToOutputDelegatingAction extends DefaultTask {
                @InputFile File input
                @OutputFile File output
                
                @TaskAction def execute() {
                    output.text = input.text
                }
            }
            
            task ${taskName}(type: InputToOutputDelegatingAction) {
                input = file("${SUBST_TARGET_DRIVE}\\\\${inputFileName}") // TODO slash?
                output = file("${SUBST_TARGET_DRIVE}\\\\${outputFileName}")
            }
        """
        when:
        buildScript script

        then:
        succeeds taskName
        outputContains taskName
        output.text == input.text

        cleanup:
        cleanupSubst()
    }

    private TestFile getRoot() {
        assert !File.listRoots().any { "${it}".toUpperCase().startsWith(SUBST_TARGET_DRIVE) }: "Drive ${SUBST_TARGET_DRIVE} already in use!"
        def substRoot = temporaryFolder.createDir("root").createDir()
        ['subst', SUBST_TARGET_DRIVE, substRoot].execute().waitForProcessOutput()
        substRoot
    }

    private Process cleanupSubst() {
        ['subst', '/d', SUBST_TARGET_DRIVE].execute()
    }
}
