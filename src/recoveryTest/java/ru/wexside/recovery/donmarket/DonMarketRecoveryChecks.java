package ru.wexside.recovery.donmarket;
import java.util.*;
import java.util.function.*;
import org.joml.Matrix4f;
import static ru.wexside.recovery.donmarket.DonMarketRunnerBehavior.*;
public final class DonMarketRecoveryChecks {
 static int assertions;
 static void eq(Object expected,Object actual) { assertions++; if(!Objects.equals(expected,actual))throw new AssertionError(expected+" != "+actual); }
 static final class Enabled implements BooleanValue { boolean value; public boolean get(){return value;} public void set(boolean v){value=v;} }
 static final class Runtime implements Controller { final List<String> trace; int ticks,pauses,resets;Runtime(List<String> t){trace=t;} public void tick(){ticks++;trace.add("controller.tick");}public void pause(){pauses++;}public void reset(){resets++;} }
 static final class Ui implements Overlay { final List<String> trace;int clicks,types,keys,renders;Matrix4f matrix;Ui(List<String> t){trace=t;}public void tick(){trace.add("overlay.tick");}public boolean click(int b){clicks++;return b==7;}public boolean type(char c){types++;return c=='x';}public void render(Matrix4f m){renders++;matrix=m;}public boolean input(int k){keys++;return k==9;} }
 static final class Fixture {
  final List<String> trace=new ArrayList<>();final Enabled enabled=new Enabled();final Ui ui=new Ui(trace);final Runtime runtime=new Runtime(trace);int sessionReads,coordinatorReads;boolean sessionReady,coordinatorReady;BooleanSupplier active;Supplier<Rule> rules;IntSupplier delay,budget;Runnable completion;int rate=17,units=3;
  final DonMarketRunnerBehavior<String,String> behavior;
  Fixture(boolean existing,boolean sessionReady,boolean coordinatorReady,boolean paused) {
   this.sessionReady=sessionReady;this.coordinatorReady=coordinatorReady;
   behavior=new DonMarketRunnerBehavior<>(new Metadata("required-id","required-name","required-description","required-category",new String[0]),new Settings(enabled,()->rate,()->units,()->33,()->44),()->{trace.add("session");return this.sessionReady?"s"+(++sessionReads):null;},()->{trace.add("coordinator");return this.coordinatorReady?"c"+(++coordinatorReads):null;},(s,c,a,r,d,b,done)->{eq("s2",s);eq("c2",c);trace.add("create");active=a;rules=r;delay=d;budget=b;completion=done;return runtime;},ui,new InitialState(paused,existing?runtime:null));
  }
 }
 // Independent interpretation of the original flattened state's branch table and successors.
 static List<String> originalTrace(boolean existing,boolean session,boolean coordinator) {
  var trace=new ArrayList<String>();int state=-173892271;
  while(true) switch(state) {
   case -173892271 -> {trace.add("overlay.tick");state=existing?-989111262:-116216789;}
   case -116216789 -> {trace.add("session");state=session?-1230445645:-2099036492;}
   case -1230445645 -> {trace.add("coordinator");state=coordinator?-143796791:-2099036492;}
   case -143796791 -> {trace.add("session");trace.add("coordinator");trace.add("create");state=-989111262;}
   case -989111262 -> {trace.add("controller.tick");return trace;}
   case -2099036492 -> {return trace;}
   default -> throw new AssertionError(state);
  }
 }
 public static void main(String[] args) {
  for(boolean existing:new boolean[]{false,true}) for(boolean session:new boolean[]{false,true}) for(boolean coordinator:new boolean[]{false,true}) {
   var f=new Fixture(existing,session,coordinator,true);f.behavior.tick();eq(originalTrace(existing,session,coordinator),f.trace);
  }
  installInstance(null);eq(false,click(7));eq(false,type('x'));eq(false,input(9));render(new Matrix4f());
  var f=new Fixture(false,true,true,true);installInstance(f.behavior);eq(false,click(7));eq(true,type('x'));eq(true,input(9));render(new Matrix4f());eq(0,f.ui.renders);
  f.behavior.tick();eq(1,f.runtime.ticks);eq(2,f.sessionReads);eq(2,f.coordinatorReads);eq(false,f.active.getAsBoolean());eq(33,f.delay.getAsInt());eq(44,f.budget.getAsInt());
  eq(new Rule(17L,3,Set.of()),f.rules.get());f.rate=-4;f.units=8;eq(new Rule(-4L,8,Set.of()),f.rules.get());
  f.behavior.startOrTogglePause();eq(true,f.enabled.get());eq(false,f.behavior.isPaused());eq(true,f.active.getAsBoolean());eq(0,f.runtime.pauses);
  f.behavior.startOrTogglePause();eq(true,f.behavior.isPaused());eq(1,f.runtime.pauses);eq(false,f.active.getAsBoolean());
  f.behavior.togglePause();eq(false,f.behavior.isPaused());eq(1,f.runtime.pauses);eq(true,f.active.getAsBoolean());
  f.behavior.togglePause();f.completion.run();eq(false,f.enabled.get());eq(true,f.behavior.isPaused());
  f.behavior.stopAndReset();eq(false,f.enabled.get());eq(false,f.behavior.isPaused());eq(1,f.runtime.resets);
  eq(true,click(7));eq(false,click(8));eq(false,type('y'));eq(false,input(8));var matrix=new Matrix4f();render(matrix);eq(matrix,f.ui.matrix);eq(1,f.ui.renders);
  f.trace.clear();f.behavior.tick();eq(List.of("overlay.tick","controller.tick"),f.trace);
  final Runnable[] listener={null};f.behavior.registerTick(r->listener[0]=r);listener[0].run();eq(3,f.runtime.ticks);
  var recipe=f.behavior.hiddenNumber(r->r,"a","b",-3,9,4,.25);eq("a",recipe.id());eq("b",recipe.label());eq("b",recipe.description());eq(-3d,recipe.minimum());eq(9d,recipe.maximum());eq(4d,recipe.initialValue());eq(.25d,recipe.steps());eq(0,recipe.originalIntegerOption());eq(false,recipe.visible().get());
  var notReady=new Fixture(false,false,false,false);notReady.behavior.stopAndReset();eq(0,notReady.runtime.resets);notReady.behavior.togglePause();eq(true,notReady.behavior.isPaused());eq(0,notReady.runtime.pauses);
  installInstance(null);System.out.println("PASS "+assertions+" assertions");
 }
}
