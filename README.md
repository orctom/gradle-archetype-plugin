# [LOOK FOR NEW MAINTAINER]

## gradle-archetype-plugin [![Build Status](https://travis-ci.org/orctom/gradle-archetype-plugin.svg)](https://travis-ci.org/orctom/gradle-archetype-plugin)

Maven archetype like plugin for Gradle.
Generating projects from local template.

### Install
https://plugins.gradle.org/plugin/com.orctom.archetype

### Tasks
 * `cleanArchetype`: cleans the generated folders and files.
 * `generate`: generates projects from the template.

### Interactive Mode:
```
gradle cleanArch generate -i
```

### Batch Mode:
```
gradle cleanArch generate -i -Dtarget=generated -Dgroup=com.xxx.yyy -Dname=dummy-service -Dversion=1.0-SNAPSHOT
```

### Parameters
#### Prompted
Following parameters will be asked, if not available in system properties

| Param           | Description                                         | Default                        |
| --------------- | ----------------------------------------------------| ------------------------------ |
| group           | group name in Gradle or Maven, *Mandatory*          |                                |
| name            | name in Gradle, of artifactId in Maven, *Mandatory* |                                |
| version         | version in Gradle or Maven, *Mandatory*             | 1.0-SNAPSHOT                   |

#### Won't Be Prompted
Following parameters will NOT be prompted, if not available in system properties.

| Param           | Description                                         | Default                        |
| --------------- | ----------------------------------------------------| ------------------------------ |
| templates       | The folder path where template locates, *Mandatory* | `src/main/resources/templates` |
| failIfFileExist | Fail if there are files with the same name exist in the `generated` folder; otherwise overwrite | `true` |

#### System Properties
Parameters will firstly been searched in System Properties, which includes:

 * gradle.properties: systemProp.param1=value1
 * settings.properties: systemProp.param1=value1
 * ~/.gradle/gradle.properties (not suggested for this plugin)
 * Command line: -Dparam1=value1 -Dparam2=value2 -Dparam3=value3

### Variables:
Variables that can be used in template files.

| name         | description                                        | sample                |
| ------------ | -------------------------------------------------- | --------------------- |
| group        | project.group                                      | com.xxx.yyy           |
| name         | project.name                                       | dummy-app             |
| version      | project.version                                    | 1.0-SNAPSHOT          |
| projectName  | project.name                                       | dummy-app             |
| namePackage  | replaced non-characters with '.' in name           | dummy.app             |
| namePath     | replaced non-characters with '/' in name           | dummy/app             |
| groupPath    | replaced '.' with '/' in group                     | com/xxx/yyy           |
| packageName  | (group + name) replaced non-characters with '.'    | com.xxx.yyy.dummy.app |
| packagePath  | replaced '.' with '/' in packageName               | com/xxx/yyy/dummy/app |

#### Adding Custom variables
Extra variables can be added via command line or programmatically with the
`com.orctom.gradle.archetype.binding` prefix.

Command line :
```
-Dparam1=value1 -Dparam2=value2 -Dparam3=value3 ...
```

Property prefix :
```
System.setProperty('com.orctom.gradle.archetype.binding.param1', value1)
```

#### Prompt for custom variables
Extra variables (bindings) can be prompted if missing, just like group or name.

Provide `bindingsToPrompt` map where key is binding name and value is default value for variable. If none default should
be available, just put empty `String`

Example:
```groovy
generate {
   bindingsToPrompt = ["apiVersion" : "1", "createdBy" : ""]
}
```


#### Programmatic Customization of Bindings
Often, additional variables (bindings) need to be created based on the values of existing variables after they have
 been resolved (e.g., when they are entered in interactive mode), but prior to the start of the actual generation
 process.

The `generate` task can be configured with a processor that is called just prior to the actual file generation, but
after all other variables have been resolved. The processor is just a closure that accepts a single argument, the
current binding configuration as a `Map`.  The processor is specified by setting the `bindingProcessor` property
of the `generate` task.

For example:
```groovy
generate {
    bindingProcessor = { bindings ->
        bindings.capitalizedName = bindings.name.capitalize()
    }
}
```

### Token Format
 * In code: `@variable@`
 * In file name: `__variable__`

Additional GString expressions can be defined between the `@` and `__` tokens :
 * `@variable.capitalize()@`
 * `__new Date()__`

See [GStringTemplateEngine](http://docs.groovy-lang.org/latest/html/api/groovy/text/GStringTemplateEngine.html)

### Generated Project(s) Folder
Fixed to: `generated`.

### Non-templates:
Files that will not be resoled by variables, as they would fail if tried to resolve.
Put the non-template lists to `.nontemplates` file,
and put the file to template folder (such as `src/main/resources/templates`) or `src/main/resources/`.

Sample:
```
# comments
**/*.jar
**/*.bat
**/*.sh
**/*.zip
**/*.gz
**/*.xz
**/*.tar
**/*.7z

gradle/
.gradle/
gradlew
gradlew.bat
```

It follows ant style. The tailing slash for directory is mandatory.

### Sample
https://github.com/orctom/gradle-archetype-plugin/tree/master/src/test/resources/sample

### Known Issues
 * Doesn't work with property files that have such escapes: key=https`\`://aaa.bbb.ccc/xxx, remove the `\` escape to have it work.
 * In interactive mode, the prompt text got truncated sometimes.

### Change Logs
#### 1.4.8
 * [#29](https://github.com/orctom/gradle-archetype-plugin/pull/29) Add custom variable prompting functionality

#### 1.4.6.3
 * Fixed issue [#19](https://github.com/orctom/gradle-archetype-plugin/pull/19) blank lines and comments support in `.nontemplate`

#### 1.4.5
 * Fixed issue [#17](https://github.com/orctom/gradle-archetype-plugin/pull/17)

#### 1.4.4
 * Fixed issue [#16](https://github.com/orctom/gradle-archetype-plugin/pull/17) (introduced in `1.4.3`)

#### 1.4.3
 * Fixed issue for windows.[#15](https://github.com/orctom/gradle-archetype-plugin/pull/15)

#### 1.4.2
 * Added escape for `@`.[#14](https://github.com/orctom/gradle-archetype-plugin/pull/14)

#### 1.4.1
 * Adding ability to programmatically add bindings. #12
 * Do not override properties if already defined. #13

#### 1.4
 * Renamed `clean` task to `cleanArchetype`, as _"Declaring custom check, **clean**, build or assemble tasks is not allowed anymore when using the lifecycle plugin."_ (https://docs.gradle.org/3.0/release-notes)
 * Allowing full GString expressions to be passed on to parser

#### 1.3.1.1
 * Fixed issue in 1.3.1, `packagePath` and `namePath` not working as expected.

#### 1.3.1
 * Added variables: `namePackage` and `namePath` (**NOTICE: do NOT use this buggy version**).

#### 1.3
 * The target folder where the generated project(s) locates is not changeable, fixed to `generated`.
 * The generation will fail by default, if there are files with the same name exist in the `generated` folder.
 * Added `clean` task that will have `generated` folder recreated.
 * Changed `print` and `println` to logger, so please append `-i` args to have the log printed out.
