package com.orctom.gradle.archetype

import com.google.common.io.Files
import org.apache.commons.io.FileUtils
import org.junit.Before
import org.junit.Test

import static junit.framework.TestCase.assertTrue
import static org.hamcrest.core.StringContains.containsString
import static org.junit.Assert.assertFalse
import static org.junit.Assert.assertThat

public class ArchetypeGenerateHelperTest {
  private String templatePath;
  private File projectDir;

  ArchetypeGenerateHelperTest() {
    URL templateUrl = getClass().getResource("/sample")
    this.templatePath = new File(templateUrl.toURI()).getAbsolutePath()

    System.setProperty("group","com.arctom");
    System.setProperty("name","myProject");
    System.setProperty("version","1.0-SNAPSHOT");
    System.setProperty("templates", templatePath);
  }

  @Before
  public void init() {
    this.projectDir = Files.createTempDir();
  }

  @Test
  public void testOptionalFilesCreatedWhenIfTrue() throws URISyntaxException {
    System.setProperty("com.orctom.gradle.archetype.binding.myClassName","ReallyNiceClass");
    System.setProperty("com.orctom.gradle.archetype.binding.isClassApplicable","Y");
    System.setProperty("com.orctom.gradle.archetype.binding.isSet","Y");
    System.setProperty("com.orctom.gradle.archetype.binding.isDirOption","Y");
    new ArchetypeGenerateHelper().run(projectDir)

    def optionalFile = new File(this.projectDir,
            "generated/src/main/resources/templates/myProject-model/src/main/java/com/arctom/myProject/model/ReallyNiceClass.java")

    assertTrue("Optional file should have been created", optionalFile.exists())

    def niceClassContent = FileUtils.readFileToString(optionalFile)
    assertThat(niceClassContent, containsString("public class ReallyNiceClass"))

    def optionalFileNotNamedUsingBinding = new File(this.projectDir,
            "generated/src/main/resources/templates/myProject-model/src/main/java/com/arctom/myProject/model/ExampleOptional.java")

    assertTrue("Optional file should have been created", optionalFileNotNamedUsingBinding.exists())
  }

  @Test
  public void testOptionalFilesNotCreatedWhenIfFalse() throws URISyntaxException {
    System.setProperty("com.orctom.gradle.archetype.binding.myClassName","ReallyNiceClass");
    System.setProperty("com.orctom.gradle.archetype.binding.isClassApplicable","N");
    System.setProperty("com.orctom.gradle.archetype.binding.isSet","N");
    new ArchetypeGenerateHelper().run(projectDir)

    def optionalFile = new File(this.projectDir,
            "generated/src/main/resources/templates/myProject-model/src/main/java/com/arctom/myProject/model/ReallyNiceClass.java")

    assertFalse("Optional file should not have been created", optionalFile.exists())

    def optionalFileNotNamedUsingBinding = new File(this.projectDir,
            "generated/src/main/resources/templates/myProject-model/src/main/java/com/arctom/myProject/model/ExampleOptional.java")

    assertFalse("Optional file should not have been created", optionalFileNotNamedUsingBinding.exists())
  }

  @Test
  public void testOptionalDirectoriesCreatedWhenIfTrue() throws URISyntaxException {
    System.setProperty("com.orctom.gradle.archetype.binding.myClassName","ReallyNiceClass")
    System.setProperty("com.orctom.gradle.archetype.binding.isClassApplicable","Y")
    System.setProperty("com.orctom.gradle.archetype.binding.isSet","Y")
    System.setProperty("com.orctom.gradle.archetype.binding.isDirOption","Y");

    new ArchetypeGenerateHelper().run(projectDir)

    def optionalDir = new File(this.projectDir,
            "generated/src/main/resources/templates/myProject-model/src/main/java/com/arctom/myProject/model/optional")

    assertTrue("Optional directory should have been created", optionalDir.exists())

    def fileInOptionalDir = new File(this.projectDir,
            "generated/src/main/resources/templates/myProject-model/src/main/java/com/arctom/myProject/model/optional/shouldExistWhenDirDoes")

    assertTrue("File in optional directory should have been created", fileInOptionalDir.exists())

  }

  @Test
  public void testOptionalDirectoriesNotCreatedWhenIfFalse() throws URISyntaxException {
    System.setProperty("com.orctom.gradle.archetype.binding.myClassName","ReallyNiceClass");
    System.setProperty("com.orctom.gradle.archetype.binding.isClassApplicable","N");
    System.setProperty("com.orctom.gradle.archetype.binding.isSet","N");
    System.setProperty("com.orctom.gradle.archetype.binding.isDirOption","N");

    new ArchetypeGenerateHelper().run(projectDir)

    def optionalDir = new File(this.projectDir,
            "generated/src/main/resources/templates/myProject-model/src/main/java/com/arctom/myProject/model/optional")

    assertFalse("Optional directory should not have been created", optionalDir.exists())
  }
}
