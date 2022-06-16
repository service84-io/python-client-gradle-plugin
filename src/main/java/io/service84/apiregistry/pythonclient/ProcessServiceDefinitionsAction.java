/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.service84.apiregistry.pythonclient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.tasks.util.PatternFilterable;
import org.gradle.api.tasks.util.PatternSet;
import org.openapitools.codegen.ClientOptInput;
import org.openapitools.codegen.Generator;
import org.openapitools.codegen.config.CodegenConfigurator;

public class ProcessServiceDefinitionsAction implements Action<Task> {
  private Project project;

  public ProcessServiceDefinitionsAction(Project project) {
    this.project = project;
  }

  @Override
  public void execute(Task task) {
    String rootDir = project.getRootDir().getAbsolutePath();
    Configuration pythonClientConfiguration =
        project.getConfigurations().getAt(PluginImpl.PythonClient);

    pythonClientConfiguration.forEach(
        dependency -> {
          String dependencyName = dependency.getName();
          String serviceName = dependencyName.split("-")[0];
          System.out.println("Generating Python client for " + serviceName);
          String serviceDefinitionDirectory = rootDir + "/service_definitions/";
          String serviceDefinitionFile = serviceDefinitionDirectory + serviceName + ".yaml";
          String serviceClientDirectory = rootDir + "/" + serviceName + "_client";
          project.mkdir(serviceDefinitionDirectory);
          project.delete(serviceDefinitionFile);
          project.delete(serviceClientDirectory);
          PatternFilterable pattern = new PatternSet();
          pattern.include(serviceName + ".yaml");
          project
              .zipTree(dependency.getAbsolutePath())
              .matching(pattern)
              .forEach(
                  sdf -> {
                    try {
                      Files.copy(
                          Path.of(sdf.getAbsolutePath()),
                          Path.of(serviceDefinitionFile),
                          StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                      throw new Error(e);
                    }
                  });
          Generator generator = new org.openapitools.codegen.DefaultGenerator();
          ClientOptInput opts =
              new CodegenConfigurator()
                  .setInputSpec(serviceDefinitionFile)
                  .setOutputDir(rootDir)
                  .setGeneratorName("python")
                  .setLibrary("urllib3")
                  .setPackageName(serviceName + "_client")
                  .toClientOptInput();
          generator.opts(opts).generate();
        });
  }
}
