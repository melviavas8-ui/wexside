import java.net.*;
import java.nio.file.*;
import java.util.*;
import java.io.*;
import org.objectweb.asm.*;
import org.objectweb.asm.util.*;

public final class VerifyRecovered {
    public static void main(String[] args)throws Exception {
        if(args.length==0){System.err.println("Usage: VerifyRecovered CLASSES [DEPENDENCY_JAR_OR_DIRECTORY ...] [--report-dir NEW_DIRECTORY]");System.exit(2);}
        Path root=Path.of(args[0]);if(!Files.isDirectory(root))throw new IllegalArgumentException("CLASSES must be an existing directory, not a JAR");
        Path reports=null;List<Path> dependencies=new ArrayList<>();for(int i=1;i<args.length;i++){if(args[i].equals("--report-dir")){if(++i>=args.length)throw new IllegalArgumentException("Missing report directory");if(reports!=null)throw new IllegalArgumentException("Duplicate report directory");reports=Path.of(args[i]);}else dependencies.add(Path.of(args[i]));}
        List<URL> urls=new ArrayList<>();urls.add(root.toUri().toURL());
        for(Path p:dependencies) {
            if(Files.isDirectory(p)){urls.add(p.toUri().toURL());try(var paths=Files.walk(p)){for(Path jar:paths.filter(x->x.toString().endsWith(".jar")).toList())urls.add(jar.toUri().toURL());}}
            else urls.add(p.toUri().toURL());
        }
        int classes=0,passed=0,modules=0;List<String> failures=new ArrayList<>();
        try(URLClassLoader loader=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader());var paths=Files.walk(root)) {
            for(Path file:paths.filter(x->x.toString().endsWith(".class")).toList()) {
                String name=root.relativize(file).toString().replace('\\','.').replace('/','.').replaceAll("\\.class$","");
                if(name.equals("module-info")){new ClassReader(Files.readAllBytes(file)).accept(new CheckClassAdapter(new ClassWriter(0),false),0);modules++;continue;}classes++;
                try {
                    Class<?> c=Class.forName(name,false,loader);c.getDeclaredMethods();c.getDeclaredFields();c.getDeclaredConstructors();
                    StringWriter errors=new StringWriter();CheckClassAdapter.verify(new ClassReader(Files.readAllBytes(file)),loader,false,new PrintWriter(errors));
                    if(!errors.toString().isBlank())throw new IllegalStateException(errors.toString());
                    passed++;
                }catch(Throwable error){failures.add(name+"\t"+error.toString().replace('\n',' ').replace('\r',' '));}
            }
        }
        if(classes==0&&modules==0)throw new IllegalArgumentException("No classfiles found");
        if(reports==null)reports=Files.createTempDirectory(root.toAbsolutePath().getParent(),"verification-");
        else Files.createDirectory(reports);
        Files.write(reports.resolve("bytecode-failures.tsv"),failures);
        String report="Classes="+classes+" Passed="+passed+" Failures="+failures.size()+" ModuleDescriptors="+modules+"; JVM loading and reflection without initialization, ASM typed data-flow";
        Files.writeString(reports.resolve("bytecode-statistics.txt"),report+"\n");System.out.println(report);
        System.out.println("Reports="+reports.toAbsolutePath());
        if(!failures.isEmpty())System.exit(1);
    }
}
