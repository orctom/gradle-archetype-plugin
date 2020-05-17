package com.orctom.gradle.archetype

import com.orctom.gradle.archetype.util.ConsoleUtils
import com.orctom.gradle.archetype.util.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.TaskAction

import java.util.regex.Pattern

class ArchetypeGenerateTask extends DefaultTask {

  static final Logger LOGGER = Logging.getLogger(ArchetypeGenerateTask.class)

  private static final Pattern PATTERN_NON_ALPHA_NUMERIC = Pattern.compile('[^0-9a-zA-Z]')
  private static final Pattern PATTERN_DOUBLE_SLASHES = Pattern.compile('//')
  private static final Pattern PATTERN_DOUBLE_DOTS = Pattern.compile('..')

  ArchetypeGenerateTask() {
    ext {

      // This property allows the consuming plugin to configure this task with a closure that will be
      // invoked just prior to performing the actual generation.  It gives the project a chance to
      // add/modify the bindings as needed.

      bindingProcessor = {} // Default processor does nothing
      bindingsToPrompt = []
    }
  }

  @TaskAction
  create() {
    String projectGroup = getParam('group', 'Please enter the group name')
    String projectName = getParam('name', 'Please enter the project name')
    String projectVersion = getParam('version', 'Please enter the version name', '1.0-SNAPSHOT')

    String templatePath = System.getProperty('templates', ArchetypePlugin.DIR_TEMPLATES)

    Map binding = [
        'group'          : projectGroup,
        'groupId'        : projectGroup,
        'name'           : projectName,
        'projectName'    : projectName,
        'artifactId'     : projectName,
        'version'        : projectVersion,
        'project.version': projectVersion
    ]

    binding << bindingsToPrompt.collectEntries { key, defaultValue -> 
      [ (key): this.class.getParam(key, "Please enter ${key}", defaultValue) ]
    }

    extendedBinding(binding)

    // Now that all the bindings have been resolved, let the consuming project perform any customizations they need
    bindingProcessor.call(binding)

    logBindings(binding)

    FileUtils.generate(project.projectDir, templatePath, binding, isFailIfFileExist())
  }

  private static void extendedBinding(Map binding) {
    addCommandLinePropertiesToBinding(binding)
    addPropertyScopedBindings(binding)

    String name = binding.get('name')
    String group = binding.get('group')

    !binding.containsKey('namePackage') &&
      binding.put('namePackage', replaceAllNonAlphaNumericWith(name, "."))

    String namePath = replaceAllNonAlphaNumericWith(name, '/')
    !binding.containsKey('namePath') &&
      binding.put('namePath', namePath)

    String groupPath = replaceAllNonAlphaNumericWith(group, '/')
    !binding.containsKey('groupPath') &&
      binding.put('groupPath', groupPath)

    String packagePath = replaceDoubleSlashesWithSingleOne(
        replaceAllNonAlphaNumericWith(groupPath + '/' + namePath, '/')
    )
    !binding.containsKey('packagePath') &&
      binding.put('packagePath', packagePath)
    !binding.containsKey('packageName') &&
      binding.put('packageName', replaceAllNonAlphaNumericWith(packagePath, '.'))
  }

  private static String replaceAllNonAlphaNumericWith(String name, String replacement) {
    return PATTERN_NON_ALPHA_NUMERIC.matcher(name).replaceAll(replacement)
  }

  private static String replaceDoubleSlashesWithSingleOne(String name) {
    return PATTERN_DOUBLE_SLASHES.matcher(name).replaceAll("/")
  }

  private static String replaceDoubleDotsWithSingleOne(String name) {
    return PATTERN_DOUBLE_DOTS.matcher(name).replaceAll(".")
  }

  private static void addCommandLinePropertiesToBinding(binding) {
    String extraProperties = System.getProperty("sun.java.command")
    if (null != extraProperties) {
      extraProperties.split('\\s+').each { item ->
        int equalSignIndex
        if (item.startsWith("-D") && (equalSignIndex = item.indexOf('=')) > 2) {
          String key = item.substring(2, equalSignIndex)
          String value = item.substring(equalSignIndex + 1, item.length())
          binding.put(key, value)
        }
      }
    }
  }

  private static void addPropertyScopedBindings(binding) {
    final String propertyBinding = "com.orctom.gradle.archetype.binding"
    final int scopeSize = propertyBinding.length() + 1
    System.getProperties().findAll { p -> p.key.startsWith(propertyBinding) }
        .each { p -> binding.put(p.key.substring(scopeSize), p.value) }
  }

  private static String getParam(String paramName, String prompt, String defaultValue = null) {
    String value = System.getProperty(paramName)

    if (!value) {
      value = ConsoleUtils.prompt(prompt, defaultValue)
    }

    if (!value) {
      throw new IllegalArgumentException("Parameter required: $paramName")
    }

    return value
  }

  private static void logBindings(Map map) {
    LOGGER.info('Variables:')
    map.each { k, v -> LOGGER.info("  {}='{}'", k.padRight(25), v) }
  }

  private static boolean isFailIfFileExist() {
    String value = System.getProperty('failIfFileExist', 'y').trim().toLowerCase().charAt(0)
    'y' == value || 't' == value || '1' == value
  }

  @Override
  String getGroup() {
    'Archetype'
  }

  @Override
  String getDescription() {
    'Generates project(s) from template(s)'
  }
}
