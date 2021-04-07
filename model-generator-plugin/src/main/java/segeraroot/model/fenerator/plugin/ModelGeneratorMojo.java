package segeraroot.model.fenerator.plugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;
import segararoot.generator.ast.AST;
import segararoot.generator.parser.ParseResultFailure;
import segararoot.generator.parser.ParseResultSuccess;
import segararoot.generator.parser.Parser;
import segararoot.model.generator.dto.CompilationUnit;
import segararoot.model.generator.dto.DtoGenerator;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

@Mojo(name = "generate-model", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ModelGeneratorMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", required = true, readonly = true)
    MavenProject project;

    @Parameter(property = "generateDto", defaultValue = "true")
    boolean generateDto;
    @Parameter(property = "packageName", required = true)
    String packageName;
    @Parameter(property = "modelFile", required = true)
    File modelFile;
    @Parameter(property = "outputFolder", defaultValue = "${project.build.directory}/generated-sources/model")
    File outputFolder;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("executing ModelGeneratorMojo.execute()");
        getLog().info("packageName: " + packageName);
        getLog().info("modelFile: " + modelFile);

        System.out.println("++++++++++++++++++++++++++++++++");
        try {
            Enumeration<URL> resources = MavenProject.class.getClassLoader().getResources("/");
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                System.out.println(url);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("++++++++++++++++++++++++++++++++");

        AST ast = parse();

        var allUnits = new ArrayList<CompilationUnit>();
        if (generateDto) {
            var units = new DtoGenerator(packageName).generate(ast);
            allUnits.addAll(units);
        }

        for (CompilationUnit allUnit : allUnits) {
            save(allUnit);
        }

        project.addCompileSourceRoot(outputFolder.getPath());
    }

    private void save(CompilationUnit compilationUnit) throws MojoExecutionException {
        File folder = new File(outputFolder, packageToPath(compilationUnit.packageName()));
        File file = new File(folder, compilationUnit.name() + ".java");

        folder.mkdirs();
        try {
            new FileOutputStream(file).write(compilationUnit.body().getBytes());
        } catch (IOException e) {
            getLog().debug(e);
            throw new MojoExecutionException("Error while writing file: " + e.getMessage());
        }
    }

    private String packageToPath(String packageName) {
        return String.join(File.separator, packageName.split("\\."));
    }

    private AST parse() throws MojoExecutionException {
        var parseResult = Parser.parse(readFile(modelFile));
        if (parseResult instanceof ParseResultFailure) {
            getLog().error("Parsing error:" + ((ParseResultFailure) parseResult).error());
            throw new MojoExecutionException("Unable to parse file");
        }

        return ((ParseResultSuccess) parseResult).ast();
    }

    private String readFile(File modelFile) throws MojoExecutionException {
        try {
            return FileUtils.fileRead(modelFile);
        } catch (IOException e) {
            getLog().debug(e);
            throw new MojoExecutionException("Error while reading file: " + e.getMessage());
        }
    }
}