package com.orctom.gradle.archetype

import com.orctom.gradle.archetype.util.FileUtils
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

import static com.orctom.gradle.archetype.ArchetypePlugin.DIR_TARGET

class ArchetypeCleanTask extends DefaultTask {

  static final Logger LOGGER = Logging.getLogger(ArchetypeCleanTask.class)

  @TaskAction
  create() {
    LOGGER.info('Cleaning...')
    File targetDir = FileUtils.getResourceFile(project.projectDir, DIR_TARGET)
    if (targetDir.exists()) {
      targetDir.deleteDir()
      LOGGER.info('Deleted {}', targetDir.getAbsolutePath())
    } else {
      LOGGER.info('Skipped, not existing: {}', targetDir.getAbsolutePath())
    }
  }

  @Override
  @Internal
  String getGroup() {
    'Archetype'
  }

  @Override
  @Internal
  String getDescription() {
    'Cleans generated project(s)'
  }
}
