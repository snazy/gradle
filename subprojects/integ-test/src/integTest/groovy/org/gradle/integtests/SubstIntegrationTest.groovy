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
import org.gradle.util.Requires
import org.gradle.util.TestPrecondition

@Requires(TestPrecondition.WINDOWS)
class SubstIntegrationTest extends AbstractIntegrationSpec {
    final SUBST_TARGET_DRIVE = 'X:'.toUpperCase()

    def setup() {
        assert !File.listRoots().any { "${it}".toUpperCase().startsWith(SUBST_TARGET_DRIVE) }
        ['subst', SUBST_TARGET_DRIVE, temporaryFolder.root.absolutePath].execute().waitForProcessOutput()
    }

    def cleanup() {
        ['subst', '/d', SUBST_TARGET_DRIVE].execute()
    }

    def "up to date check works from filesystem's root"() {
        def substRoot = temporaryFolder.createDir("root").createDir()
        substRoot.file("input.txt") << "content"

        when:
        buildScript """
            class CustomTask extends DefaultTask {
                @InputFile File input
                @OutputFile File output
                
                @TaskAction def execute() {
                    outputFile.text = inputFile.text
                }
            }
            
            task custom(type: CustomTask) {
                input = file(${SUBST_TARGET_DRIVE})
            }
        """

        then:
        succeeds 'custom'
    }
}
