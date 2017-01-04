package com.orctom.gradle.archetype.util

import com.orctom.gradle.archetype.ConflictResolutionStrategy
import groovy.io.FileType
import groovy.text.GStringTemplateEngine
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.regex.Matcher

import static com.orctom.gradle.archetype.ConflictResolutionStrategy.FAIL
import static com.orctom.gradle.archetype.ConflictResolutionStrategy.OVERWRITE

class FileUtils {

  static final Logger log = Logging.getLogger(getClass().name)
  static engine = new GStringTemplateEngine()

  /** Provides collection of all files from the source directory.
   *
   * @param sourceDir source directory, i.e. directory with the templates(s)
   * @return list of all files in the source directory, including sub-directories
   */
  static List<File> getTemplates(File sourceDir) {
    log.info("source dir: '{}'", sourceDir.name)

    List<File> sourceFiles = []
    sourceDir.eachFileRecurse(FileType.ANY) { file ->
      sourceFiles << file
    }

    sourceFiles
  }

  /** Provides collection of all non-templates files from the source directory.
   *
   * @param sourceDir source directory, i.e. directory with the templates(s)
   * @param defaultNonTemplatesFile location of the default non-templates files
   *
   * @return set of all non-templates files in the source directory, including sub-directories
   */
  static Set<String> getNonTemplates(defaultNonTemplatesFile, sourceDir) {
    Set<String> nonTemplates = []

    // TODO: allow template-specific settings overriding the default
    Set<String> nonTemplatesWildcards = defaultNonTemplatesFile.readLines() as Set
    FileNameFinder finder = new FileNameFinder()
    nonTemplatesWildcards.each {
      def files = finder.getFileNames(sourceDir.path, it)
      nonTemplates.addAll(files)
    }

    nonTemplates
  }

  static void generate(
      List<File> templates,
      Map binding,
      File sourceDir,
      File targetDir,
      Set<String> nonTemplates,
      ConflictResolutionStrategy strategy) {

    log.info("target dir: '{}'", targetDir.name)

    if (targetDir.exists() && strategy == ConflictResolutionStrategy.SWEEP ) {
      log.warn("removing existing target dir: '{}'", targetDir.absolutePath)
      targetDir.deleteDir()
    }

    targetDir.mkdirs()

    logBindings(binding)

    // list of written files
    // currently used only for cleaning up when there is a conflict and ConflictResolutionStrategy is FAIL
    List<File> filesWritten = new ArrayList<>();

    templates.each {
      source ->

        // apply variable substitution to path
        File target = new File(targetDir, resolvePaths(getRelativePath(sourceDir, source)))
        // replace delimiters with escaped ones
        String quotedPath = target.path.replaceAll(File.separator,Matcher.quoteReplacement(File.separator))
        String path = engine.createTemplate(quotedPath).make(binding)
        target = new File(path)

        if (source.isFile()) {

          // ensure ancestor dirs exist
          target.parentFile.mkdirs();

          if (isNonTemplate(source, nonTemplates)) {
            writeNonTemplate(target, source, strategy, filesWritten)
          } else {
            writeTemplate(target, source, strategy, binding, filesWritten)
          }
        }
    }

    log.info('Done')
  }

  // handle templates
  private static void writeTemplate(File target, File source, ConflictResolutionStrategy strategy,
                                    Map binding, List<File> filesWritten ) {
    try {

      if (target.exists()) {
        switch (strategy) {

          case OVERWRITE:
            log.info("Overwriting file '{}'.", target.absolutePath)
            target.delete()
            target << resolve(source.text, binding)
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
            break

          case FAIL:
            log.error("File already exists '{}'.", target.absolutePath)
            log.info("Stopping the generation, deleting generated files.")
            // remove generated files
            filesWritten.each { file -> file.delete() }
            System.exit(1)
            break
        }
      } else {
        target << resolve(source.text, binding)
      }

      filesWritten.add(target)

    } catch (Exception ex) {
      log.error("Failed to resolve variables in: '{}]", source.path)
      log.error(ex.getMessage())
      Files.copy(source.toPath(), target.toPath())
    }
  }

  // handle non-templates
  private static void writeNonTemplate(File target, File source, ConflictResolutionStrategy strategy, List<File> filesWritten ) {

    if (target.exists()) {
      switch (strategy) {

        case OVERWRITE:
          log.info("Overwriting file '{}'.", target.absolutePath)
          Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
          break

        case FAIL:
          log.error("File already exists '{}'.", target.absolutePath)
          log.info("Stopping the generation, deleting generated files.")
          // remove generated files
          filesWritten.each { file -> file.delete() }
          System.exit(1)
          break
      }
    } else {
      Files.copy(source.toPath(), target.toPath())
    }

    filesWritten.add(target)
  }

  static logBindings(Map map) {
    map.each {
      k, v -> log.info("variable: {}='{}'", k, v)
    }
  }

  static boolean isNonTemplate(File source, Set<String> nonTemplates) {
    source.path in nonTemplates
  }

  static String resolve(String text, Map binding) {
    String escaped = text.replaceAll('\\$', '__DOLLAR__')
    String ready = escaped.replaceAll('@(\\w+)@', '\\$\\{$1\\}')
    String resolved = engine.createTemplate(ready).make(binding)
    String done = resolved.replaceAll('__DOLLAR__', '\\$')
    done
  }

  static String getRelativePath(File sourceDir, File file) {
    sourceDir.toURI().relativize(file.toURI()).toString()
  }

  /** Applies variable substitution to provided path. */
  static String resolvePaths(String pathAsString) {

    if (!pathAsString.contains('__')) {
      pathAsString
    }

    String path = '';
    pathAsString.split(Matcher.quoteReplacement(File.separator)).each {
      if (it.contains('__')) {
        path += resolvePath(it)
      } else {
        path += it
      }

      path += File.separator
    }

    path
  }

  // replaces "__variable__" (used in directory/file names) with "${variable}"
  static String resolvePath(String path) {
    path.replaceAll('(.*)__(\\w+)__(.*)', '$1\\$\\{$2\\}$3')
  }

}