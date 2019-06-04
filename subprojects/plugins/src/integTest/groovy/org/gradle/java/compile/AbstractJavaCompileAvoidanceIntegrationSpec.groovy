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

package org.gradle.java.compile

import org.gradle.integtests.fixtures.CompiledLanguage
import org.gradle.language.fixtures.HelperProcessorFixture

abstract class AbstractJavaCompileAvoidanceIntegrationSpec extends AbstractJavaGroovyCompileAvoidanceIntegrationSpec {
    CompiledLanguage language = CompiledLanguage.JAVA

    def "doesn't recompile when private inner class changes"() {
        given:
        // Groovy doesn't produce real private inner classes - the generated bytecode has no ACC_PRIVATE
        buildFile << """
            project(':b') {
                dependencies {
                    compile project(':a')
                }
            }
        """
        def sourceFile = file("a/src/main/${language.name}/ToolImpl.${language.name}")
        sourceFile << """
            public class ToolImpl {
                private class Thing { }
            }
        """
        file("b/src/main/${language.name}/Main.${language.name}") << """
            public class Main { ToolImpl t = new ToolImpl(); }
        """

        when:
        succeeds ":b:${language.compileTaskName}"

        then:
        executedAndNotSkipped ":a:${language.compileTaskName}"
        executedAndNotSkipped ":b:${language.compileTaskName}"

        when:
        // ABI change of inner class
        sourceFile.text = """
            public class ToolImpl {
                private class Thing {
                    public long v;
                }
            }
"""

        then:
        succeeds ":b:${language.compileTaskName}"
        executedAndNotSkipped ":a:${language.compileTaskName}"
        skipped ":b:${language.compileTaskName}"

        when:
        // Remove inner class
        sourceFile.text = """
            public class ToolImpl {
            }
"""

        then:
        succeeds ":b:${language.compileTaskName}"
        executedAndNotSkipped ":a:${language.compileTaskName}"
        skipped ":b:${language.compileTaskName}"

        when:
        // Anonymous class
        sourceFile.text = """
            public class ToolImpl {
                private Object r = new Runnable() { public void run() { } };
            }
"""

        then:
        succeeds ":b:${language.compileTaskName}"
        executedAndNotSkipped ":a:${language.compileTaskName}"
        skipped ":b:${language.compileTaskName}"

        when:
        // Add classes
        sourceFile.text = """
            public class ToolImpl {
                private Object r = new Runnable() {
                    public void run() {
                        class LocalThing { }
                    }
                };
                private static class C1 {
                }
                private class C2 {
                    public void go() {
                        Object r2 = new Runnable() { public void run() { } };
                    }
                }
            }
"""

        then:
        succeeds ":b:${language.compileTaskName}"
        executedAndNotSkipped ":a:${language.compileTaskName}"
        skipped ":b:${language.compileTaskName}"
    }

    def "recompiles source when annotation processor implementation on annotation processor classpath changes"() {
        given:
        settingsFile << "include 'c'"

        buildFile << """
            project(':b') {
                dependencies {
                    compile project(':a')
                }
            }
            project(':c') {
                configurations {
                    processor
                }
                dependencies {
                    compile project(':a')
                    processor project(':b')
                }
                ${language.compileTaskName}.options.annotationProcessorPath = configurations.processor
                task run(type: JavaExec) {
                    main = 'TestApp'
                    classpath = sourceSets.main.runtimeClasspath
                }
            }
        """

        def fixture = new HelperProcessorFixture()

        // The annotation
        fixture.writeApiTo(file("a"))

        // The processor and library
        fixture.writeSupportLibraryTo(file("b"))
        fixture.writeAnnotationProcessorTo(file("b"))

        // The class that is the target of the processor
        file("c/src/main/${language.name}/TestApp.${language.name}") << '''
            @Helper
            class TestApp {
                public static void main(String[] args) {
                    System.out.println(new TestAppHelper().getValue()); // generated class
                }
            }
'''

        when:
        run(':c:run')

        then:
        executedAndNotSkipped(":a:${language.compileTaskName}")
        executedAndNotSkipped(":b:${language.compileTaskName}")
        executedAndNotSkipped(":c:${language.compileTaskName}")
        outputContains('greetings')

        when:
        run(':c:run')

        then:
        skipped(":a:${language.compileTaskName}")
        skipped(":b:${language.compileTaskName}")
        skipped(":c:${language.compileTaskName}")
        outputContains('greetings')

        when:
        // Update the library class
        fixture.message = 'hello'
        fixture.writeSupportLibraryTo(file("b"))

        run(':c:run')

        then:
        skipped(":a:${language.compileTaskName}")
        executedAndNotSkipped(":b:${language.compileTaskName}")
        executedAndNotSkipped(":c:${language.compileTaskName}")
        outputContains('hello')

        when:
        run(':c:run')

        then:
        skipped(":a:${language.compileTaskName}")
        skipped(":b:${language.compileTaskName}")
        skipped(":c:${language.compileTaskName}")
        outputContains('hello')

        when:
        // Update the processor class
        fixture.suffix = 'world'
        fixture.writeAnnotationProcessorTo(file("b"))

        run(':c:run')

        then:
        skipped(":a:${language.compileTaskName}")
        executedAndNotSkipped(":b:${language.compileTaskName}")
        executedAndNotSkipped(":c:${language.compileTaskName}")
        outputContains('hello world')
    }

    def "ignores annotation processor implementation when included in the compile classpath but annotation processing is disabled"() {
        given:
        settingsFile << "include 'c'"

        buildFile << """
            project(':b') {
                dependencies {
                    compile project(':a')
                }
            }
            project(':c') {
                dependencies {
                    compile project(':b')
                }
                ${language.compileTaskName}.options.annotationProcessorPath = files()
            }
        """

        def fixture = new HelperProcessorFixture()

        fixture.writeSupportLibraryTo(file("a"))
        fixture.writeApiTo(file("b"))
        fixture.writeAnnotationProcessorTo(file("b"))

        file("c/src/main/${language.name}/TestApp.${language.name}") << '''
            @Helper
            class TestApp {
                public static void main(String[] args) {
                }
            }
'''

        when:
        run(":c:${language.compileTaskName}")

        then:
        executedAndNotSkipped(":a:${language.compileTaskName}")
        executedAndNotSkipped(":b:${language.compileTaskName}")
        executedAndNotSkipped(":c:${language.compileTaskName}")

        when:
        run(":c:${language.compileTaskName}")

        then:
        skipped(":a:${language.compileTaskName}")
        skipped(":b:${language.compileTaskName}")
        skipped(":c:${language.compileTaskName}")

        when:
        // Update the library class
        fixture.message = 'hello'
        fixture.writeSupportLibraryTo(file("a"))

        run(":c:${language.compileTaskName}")

        then:
        executedAndNotSkipped(":a:${language.compileTaskName}")
        skipped(":b:${language.compileTaskName}")
        skipped(":c:${language.compileTaskName}")

        when:
        // Update the processor class
        fixture.suffix = 'world'
        fixture.writeAnnotationProcessorTo(file("b"))

        run(":c:${language.compileTaskName}")

        then:
        skipped(":a:${language.compileTaskName}")
        executedAndNotSkipped(":b:${language.compileTaskName}")
        skipped(":c:${language.compileTaskName}")
    }
}
