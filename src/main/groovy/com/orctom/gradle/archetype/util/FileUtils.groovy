package com.orctom.gradle.archetype.util

import com.orctom.gradle.archetype.ArchetypePlugin
import groovy.io.FileType
import groovy.text.GStringTemplateEngine
import org.apache.commons.lang.StringUtils
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.regex.Matcher
import java.util.regex.Pattern

class FileUtils {

  static final Logger LOGGER = Logging.getLogger(FileUtils.class)
  private static final String TRUTH_REGEX = "^[Yy]"
  private static final Pattern TRUTH_PATTERN = Pattern.compile(TRUTH_REGEX, Pattern.CASE_INSENSITIVE)
  private static final String DELETED_MARKER = "<DELETED>"

  static engine = new GStringTemplateEngine()

  private static File getResourceFile(File projectDir, String path) {
    if (Paths.get(path).absolute) {
      return new File(path)
    }
    new File(projectDir, path)
  }

  private static List<File> getTemplates(File templateDir) {
    LOGGER.info('Using template in: {}', templateDir.path)

    List<File> sourceFiles = []
    templateDir.eachFileRecurse(FileType.ANY) { file ->
      sourceFiles << file
    }

    sourceFiles
  }

  private static Set<String> getNonTemplates(File projectDir, File templateDir) {
    File templateSpecificNonTemplatesFile = new File(templateDir, '.nontemplates')
    if (templateSpecificNonTemplatesFile.exists()) {
      return readNonTemplates(templateDir.path, templateSpecificNonTemplatesFile)
    }

    File defaultNonTemplatesFile = new File(projectDir, 'src/main/resources/.nontemplates')
    if (defaultNonTemplatesFile.exists()) {
      return readNonTemplates(templateDir.path, defaultNonTemplatesFile)
    }

    return []
  }

  private static Set<String> readNonTemplates(String templateDirPath, File nonTemplatesFile) {
    LOGGER.info('Using non-template list in file: {}', nonTemplatesFile)
    Set<String> nonTemplates = []

    Set<String> nonTemplatesWildcards = nonTemplatesFile.readLines() as Set
    FileNameFinder finder = new FileNameFinder()
    nonTemplatesWildcards.each {
      if (null == it || 0 == it.trim().length() || it.startsWith("#")) {
        return
      }
      def files = finder.getFileNames(templateDirPath, it)
      nonTemplates.addAll(files)
    }

    nonTemplates
  }

  static void generate(File projectDir, String templatePath, Map binding, boolean failIfFileExist) {
    File templateDir = getResourceFile(projectDir, templatePath)
    List<File> templates = getTemplates(templateDir)
    File targetDir = getResourceFile(projectDir, ArchetypePlugin.DIR_TARGET)
    Set<String> nonTemplates = getNonTemplates(projectDir, templateDir)

    targetDir.mkdirs()

    List<File> generatedFiles = []
    Path sourceDirPath = templateDir.toPath()
    templates.stream().each { source ->
      Optional<File> potentialTarget = getTargetFile(sourceDirPath, targetDir, source, binding)

      if (source.isDirectory() || !potentialTarget.isPresent()) {
        return
      }

      File target = potentialTarget.get();

      LOGGER.debug('Processing {} -> {}', source, target)

      failTheBuildIfFileExist(target, failIfFileExist, generatedFiles)

      target.parentFile.mkdirs()

      if (isNonTemplate(source, nonTemplates)) {
        generateFromNonTemplate(source, target)
      } else {
        generateFromTemplate(source, target, binding)
      }
      generatedFiles.add(target)
    }

    LOGGER.info('Done')
  }

  private static void failTheBuildIfFileExist(File target, boolean failIfFileExist, List<File> generatedFiles) {
    if (target.exists() && failIfFileExist) {
      LOGGER.error("File already exists '{}'.", target.absolutePath)
      LOGGER.info("Stopping the generation, deleting generated files.")
      deleteNewlyGeneratedFiles(generatedFiles)
      throw new RuntimeException("failIfFileExist=true and the target file already exists.")
    }
  }

  private static Optional<File> getTargetFile(Path sourceDirPath, File targetDir, File source, Map binding) {
    Path sourcePath = sourceDirPath.relativize(source.toPath())
    String rawTargetPath = new File(targetDir, resolvePaths(sourcePath)).path
    rawTargetPath = rawTargetPath.replaceAll("\\\\", "/")
    String resolvedTargetPath = engine.createTemplate(rawTargetPath).make(binding)
    if (resolvedTargetPath.contains(DELETED_MARKER)) {
      Optional.empty()
    } else {
      Optional.of(new File(resolvedTargetPath))
    }
  }

  private static void generateFromNonTemplate(File source, File target) {
    Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
  }

  private static void generateFromTemplate(File source, File target, Map binding) {
    try {
      target.delete()
      target << resolve(source.text, binding)
    } catch (Exception e) {
      LOGGER.error("Failed to resolve variables in: '{}]", source.path)
      LOGGER.error(e.getMessage())
      Files.copy(source.toPath(), target.toPath())
    }
  }

  private static List<File> deleteNewlyGeneratedFiles(List<File> generatedFiles) {
    generatedFiles.each { file ->
      file.delete()
      LOGGER.debug("Deleted: {}", file)
    }
  }

  static boolean isNonTemplate(File source, Set<String> nonTemplates) {
    source.path in nonTemplates
  }

  static String resolve(String text, Map binding) {
    String latestText = escape(text)

    String previouslyExpanded

    while(!latestText.equals(previouslyExpanded)) {
      previouslyExpanded = latestText
      latestText = expandInlineIfBlocks(latestText, binding)
      latestText = expandMultilineIfBlocks(latestText, binding)
      latestText = expandVariables(latestText, binding)
    }
    latestText = engine.createTemplate(latestText).make(binding).toString()
    unescape(latestText)
  }

  private static String expandVariables(String text, Map bindings) {
    text.replaceAll('@([^{}/\\\\@\\n,\\s]+)@', '\\$\\{$1\\}')
  }

  private static String expandInlineIfBlocks(String text, Map bindings) {
    Pattern p = Pattern.compile("@IF\\s*\\(\\s*([^{}/\\\\@\\n,\\s]+)\\s*\\)@([^\\n]*)@ENDIF@", Pattern.DOTALL)
    expandIfBlocks(p, text, bindings)
  }

  private static String expandMultilineIfBlocks(String text, Map bindings) {
    Pattern p = Pattern.compile("@IF\\s*\\(\\s*([^{}/\\\\@\\n,\\s]+)\\s*\\)@\\n?(.*)@ENDIF@\\n?", Pattern.DOTALL)
    expandIfBlocks(p, text, bindings)
  }

  private static String expandIfBlocks(Pattern p, String text, Map bindings) {
    Matcher m = p.matcher(text)

    final StringBuffer ifBlockExpanded = new StringBuffer()
    int lastMatchPos = 0
    while (m.find()) {
      ifBlockExpanded.append(text.substring(lastMatchPos, m.start()))
      String keyForIfBlock = m.group(1)

      if (evaluateIfClause(keyForIfBlock, bindings)) {
        String ifBlockText = m.group(2)
        ifBlockExpanded.append(ifBlockText)
      }
      lastMatchPos = m.end()
    }
    if (lastMatchPos != text.length()) {
      ifBlockExpanded.append(text.substring(lastMatchPos))
    }
    ifBlockExpanded.toString()
  }

  private static boolean evaluateIfClause(String ifClause, Map bindings) {
    String ifClauseBinding = bindings.get(ifClause.trim())

    if (StringUtils.isNotEmpty(bindings.get(ifClause.trim()))) {
      return ifClauseBinding.matches(TRUTH_PATTERN)
    } else if (ifClause.startsWith('!') && StringUtils.isNotEmpty(bindings.get(ifClause.trim().substring(1)))) {
      return !bindings.get(ifClause.trim().substring(1)).matches(TRUTH_PATTERN)
    } else {
      try {
        def evaluationClause = '${' + ifClause + ' ? \'true\' : \'\'}'
        def evaluation = engine.createTemplate(evaluationClause).make(bindings).toString()
        return ('true' == evaluation)
      } catch (MissingPropertyException e) {
        return false
      }
    }
  }

  private static String escape(String text) {
    String escaped = text.replaceAll('\\$', '__DOLLAR__')
    escaped = escaped.replaceAll('@@', '__AT__')
    escaped
  }

  private static String unescape(String resolved) {
    String unescaped = resolved.replaceAll('__DOLLAR__', '\\$')
    unescaped = unescaped.replaceAll('__AT__', '@')
    unescaped
  }

  // Applies variable substitution to provided path.
  static String resolvePaths(Path path) {
    if (!path.toString().contains('__')) {
      path.toString()
    }

    path.collect {
      resolvePath(it.toString())
    }.join(File.separator)
  }

  // replaces "__variable__" (used in directory/file names) with "${variable}"
  static String resolvePath(String path) {
    path = path.replaceAll('(.*)__IF_(\\w+)__(.*)', '$1\\${$2.matches(\'' + TRUTH_REGEX +
            '\') ? \'\' : \'' + DELETED_MARKER + '\'}$3')
    path.replaceAll('(.*)__([^{}/\\\\@\\n,]+)__(.*)', '$1\\$\\{$2\\}$3')
  }
}