/*
 * Copyright 2010 the original author or authors.
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
import org.gradle.integtests.fixtures.TestResources
import org.junit.Rule
import spock.lang.Issue

class ExecIntegrationTest extends AbstractIntegrationSpec {
    @Rule
    public final TestResources testResources = new TestResources(testDirectoryProvider)

    def 'can execute java'() {
        given:
        buildFile << '''
            apply plugin: 'java'

            task javaexecTask(type: JavaExec) {
                ext.testFile = file("$buildDir/$name")
                classpath(sourceSets.main.output.classesDirs)
                main = 'org.gradle.TestMain'
                args projectDir, testFile
                doLast {
                    assert testFile.exists()
                }
                assert delegate instanceof ExtensionAware
            }

            task javaexecByMethod() {
                ext.testFile = file("$buildDir/$name")
                doFirst {
                    javaexec {
                        assert !(delegate instanceof ExtensionAware)
                        classpath(sourceSets.main.output.classesDirs)
                        main 'org.gradle.TestMain'
                        args projectDir, testFile
                    }
                }
                doLast {
                    assert testFile.exists()
                }
            }
        '''.stripIndent()

        expect:
        succeeds 'javaexecTask', 'javaexecByMethod'

    }

    def 'can execute commands'() {
        given:
        buildFile << '''
            import org.gradle.internal.jvm.Jvm

            apply plugin: 'java'

            task execTask(type: Exec) {
                dependsOn sourceSets.main.runtimeClasspath
                ext.testFile = file("$buildDir/$name")
                executable = Jvm.current().getJavaExecutable()
                args '-cp', sourceSets.main.runtimeClasspath.asPath, 'org.gradle.TestMain', projectDir, testFile
                doLast {
                    assert testFile.exists()
                }
                assert delegate instanceof ExtensionAware
            }

            task execByMethod {
                dependsOn sourceSets.main.runtimeClasspath
                ext.testFile = file("$buildDir/$name")
                doFirst {
                    exec {
                        executable Jvm.current().getJavaExecutable()
                        args '-cp', sourceSets.main.runtimeClasspath.asPath, 'org.gradle.TestMain', projectDir, testFile
                        assert !(delegate instanceof ExtensionAware)
                    }
                }
                doLast {
                    assert testFile.exists()
                }
            }
        '''.stripIndent()

        expect:
        succeeds 'execTask', 'execByMethod'
    }

    @Issue("GRADLE-3528")
    def "when the user declares outputs it becomes incremental"() {
        given:
        buildFile << '''
            apply plugin: 'java'

            task run(type: Exec) {
                inputs.files sourceSets.main.runtimeClasspath
                ext.testFile = file("$buildDir/out.txt")
                outputs.file testFile
                executable = org.gradle.internal.jvm.Jvm.current().getJavaExecutable()
                args '-cp', sourceSets.main.runtimeClasspath.asPath, 'org.gradle.TestMain', projectDir, testFile
                doLast {
                    assert testFile.exists()
                }
            }
        '''.stripIndent()

        when:
        run "run"

        then:
        executedAndNotSkipped(":run")

        when:
        run "run"

        then:
        skipped(":run")

        when:
        file('build/out.txt').delete()

        and:
        run "run"

        then:
        executedAndNotSkipped(":run")
    }

    def "arguments can be passed by using argument providers"() {
        given:
        buildFile << '''
            apply plugin: 'java'

            class JavaTestCommand implements CommandLineArgumentProvider {
                @Internal
                File expectedWorkingDir
                
                @Input
                String getExpectedWorkingDirPath() {
                    return expectedWorkingDir.absolutePath
                }
                
                @Classpath
                FileCollection classPath
                
                @OutputFile
                File outputFile
            
                @Override
                Iterable<String> asArguments() {
                    ['-cp', classPath.asPath, 'org.gradle.TestMain', expectedWorkingDirPath, outputFile.absolutePath]
                }
            }
            
            task run(type: Exec) {
                ext.testFile = file("$buildDir/out.txt")
                argumentProviders << new JavaTestCommand(
                    expectedWorkingDir: projectDir,
                    classPath: sourceSets.main.runtimeClasspath,
                    outputFile: testFile 
                )
                executable = org.gradle.internal.jvm.Jvm.current().getJavaExecutable()
                doLast {
                    assert testFile.exists()
                }
            }
        '''

        when:
        run "run"
        then:
        executedAndNotSkipped ":run"

        when:
        run "run"
        then:
        skipped ":run"

        when:
        file('build/out.txt').delete()
        and:
        run "run"
        then:
        executedAndNotSkipped ":run"
    }
}
