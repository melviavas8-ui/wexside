import java.nio.file.*;
import java.net.*;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/** Resolve actual member references without initializing application classes. */
public class LinkageAudit implements Opcodes {
 static Map<String,ClassNode> headers=new HashMap<>();
 static URLClassLoader dependencies;
 static List<String> errors=new ArrayList<>();
 static int references;
 record Found(String owner,int flags){}
 static ClassNode header(String name)throws Exception {
  if(name.startsWith("["))name="java/lang/Object";
  if(headers.containsKey(name))return headers.get(name);
  try(var input=dependencies.getResourceAsStream(name+".class")){
   if(input==null)return null;
   ClassNode c=new ClassNode();new ClassReader(input).accept(c,ClassReader.SKIP_CODE|ClassReader.SKIP_DEBUG|ClassReader.SKIP_FRAMES);headers.put(name,c);return c;
  }
 }
 static Found resolve(String owner,String name,String desc,boolean method,Set<String> visited)throws Exception {
  if(!visited.add(owner))return null;
  ClassNode c=header(owner);if(c==null)return null;
  if(method){for(MethodNode m:c.methods)if(m.name.equals(name)&&(m.desc.equals(desc)||((c.name.equals("java/lang/invoke/MethodHandle")||c.name.equals("java/lang/invoke/VarHandle"))&&(m.access&(ACC_VARARGS|ACC_NATIVE))==(ACC_VARARGS|ACC_NATIVE))))return new Found(c.name,m.access);}
  else for(FieldNode f:c.fields)if(f.name.equals(name)&&f.desc.equals(desc))return new Found(c.name,f.access);
  if(name.equals("<init>"))return null;
  if(method&&c.superName!=null){Found result=resolve(c.superName,name,desc,true,visited);if(result!=null)return result;}
  for(String iface:c.interfaces){Found result=resolve(iface,name,desc,method,visited);if(result!=null)return result;}
  if(!method&&c.superName!=null)return resolve(c.superName,name,desc,false,visited);
  if(method&&(c.access&ACC_INTERFACE)!=0)return resolve("java/lang/Object",name,desc,true,visited);
  return null;
 }
 static void check(String caller,String owner,String name,String desc,boolean method,boolean isStatic)throws Exception {
  references++;
  Found found=resolve(owner,name,desc,method,new HashSet<>());
  if(found==null)errors.add(caller+"\tMISSING\t"+owner+"."+name+desc);
  else if(((found.flags&ACC_STATIC)!=0)!=isStatic)errors.add(caller+"\tSTATIC_MISMATCH\t"+owner+"."+name+desc);
 }
 static void handle(String caller,Handle h)throws Exception {
  int tag=h.getTag();check(caller,h.getOwner(),h.getName(),h.getDesc(),tag>=H_INVOKEVIRTUAL,tag==H_INVOKESTATIC||tag==H_GETSTATIC||tag==H_PUTSTATIC);
 }
 static void constant(String caller,Object value)throws Exception {
  if(value instanceof Handle h)handle(caller,h);
  else if(value instanceof ConstantDynamic d){handle(caller,d.getBootstrapMethod());for(int i=0;i<d.getBootstrapMethodArgumentCount();i++)constant(caller,d.getBootstrapMethodArgument(i));}
 }
 public static void main(String[] args)throws Exception {
  if(args.length!=3){System.err.println("Usage: LinkageAudit CLASSES DEPENDENCY_DIRECTORY REPORT_TSV");System.exit(2);}
  if(!Files.isDirectory(Path.of(args[0])))throw new IllegalArgumentException("CLASSES must be an existing directory, not a JAR");
  List<URL> urls=new ArrayList<>();urls.add(Path.of(args[1]).toUri().toURL());try(var paths=Files.walk(Path.of(args[1]))){for(Path p:paths.filter(p->p.toString().endsWith(".jar")).toList())urls.add(p.toUri().toURL());}
  dependencies=new URLClassLoader(urls.toArray(URL[]::new),ClassLoader.getPlatformClassLoader());
  List<ClassNode> classes=new ArrayList<>();try(var paths=Files.walk(Path.of(args[0]))){for(Path p:paths.filter(p->p.toString().endsWith(".class")).toList()){ClassNode c=new ClassNode();new ClassReader(Files.readAllBytes(p)).accept(c,ClassReader.SKIP_FRAMES);classes.add(c);headers.put(c.name,c);}}
  if(classes.isEmpty())throw new IllegalArgumentException("No classfiles found");
  for(ClassNode c:classes)for(MethodNode m:c.methods){String caller=c.name+"."+m.name+m.desc;for(AbstractInsnNode n:m.instructions){
   if(n instanceof MethodInsnNode call)check(caller,call.owner,call.name,call.desc,true,call.getOpcode()==INVOKESTATIC);
   else if(n instanceof FieldInsnNode f)check(caller,f.owner,f.name,f.desc,false,f.getOpcode()==GETSTATIC||f.getOpcode()==PUTSTATIC);
   else if(n instanceof InvokeDynamicInsnNode i){handle(caller,i.bsm);for(Object value:i.bsmArgs)constant(caller,value);}
   else if(n instanceof LdcInsnNode l)constant(caller,l.cst);
  }}
  Path report=Path.of(args[2]).toAbsolutePath();Files.createDirectories(report.getParent());Files.write(report,errors);System.out.println("Classes="+classes.size()+" MemberReferences="+references+" Failures="+errors.size());dependencies.close();if(!errors.isEmpty())System.exit(1);
 }
}
