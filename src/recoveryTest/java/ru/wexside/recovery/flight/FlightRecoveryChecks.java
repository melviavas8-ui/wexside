package ru.wexside.recovery.flight;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import ru.wexside.recovery.flight.FireworkBoostBehavior.Speeds;
import ru.wexside.recovery.flight.FireworkBoostBehavior.Vector;
import ru.wexside.recovery.flight.ElytraMotionBehavior.Target;

public final class FlightRecoveryChecks {
    private static int checks;

    public static void main(String[] args) {
        float[] custom = {1.5f, 1.6f, 1.7f, 1.8f, 1.9f, 2.0f, 2.1f, 2.2f, 2.3f};
        float[][] expected = {
                {1.60f,1.63f,1.65f,1.71f,1.85f,1.84f,1.93f,2.04f,2.04f},
                {1.60f,1.61f,1.60f,1.63f,2.01f,1.84f,1.92f,2.00f,2.01f},
                {1.60f,1.63f,1.66f,1.71f,1.73f,1.81f,1.81f,1.81f,1.81f},
                {1.60f,1.62f,1.60f,1.62f,1.64f,1.68f,1.75f,1.90f,2.16f}
        };
        for (int preset = 0; preset < 2; preset++) {
            String mode = preset == 0 ? FireworkBoostBehavior.B_HVH : FireworkBoostBehavior.RW;
            FireworkBoostBehavior behavior = new FireworkBoostBehavior(true, mode, custom, custom);
            for (int index = 0; index < 9; index++) {
                Speeds yaw = behavior.updateSpeeds(true, index * 5, 0, new Speeds(1.5f, 1.5f));
                Speeds pitch = behavior.updateSpeeds(true, 0, index * 5, new Speeds(1.5f, 1.5f));
                exact(yaw.horizontal(), expected[preset * 2][index], "decoded yaw table");
                exact(pitch.vertical(), expected[preset * 2 + 1][index], "decoded pitch table");
            }
        }
        FireworkBoostBehavior rw = new FireworkBoostBehavior(true, "RW", custom, custom);
        Speeds apex = rw.updateSpeeds(true, 0, 40, new Speeds(1.5f,1.5f));
        exact(apex.horizontal(),2.16f,"vertical speed raises horizontal speed");
        exact(apex.vertical(),2.16f,"RW vertical apex");
        exact(rw.updateSpeeds(true, 400, 0, new Speeds(1.5f,1.5f)).horizontal(),1.81f,"wrapped yaw 400 ->40");
        exact(FireworkBoostBehavior.foldAngle(-135),45,"fold negative quadrant");
        exact(FireworkBoostBehavior.foldAngle(180),0,"fold half rotation");
        exact(FireworkBoostBehavior.foldAngle(90),0,"fold right angle");
        require(FireworkBoostBehavior.bucketIndex(9,4.999f)==0,"lower bucket boundary");
        require(FireworkBoostBehavior.bucketIndex(9,5)==1,"exact bucket boundary");
        require(FireworkBoostBehavior.bucketIndex(9,45)==8,"upper bucket clamps");
        FireworkBoostBehavior fallback = new FireworkBoostBehavior(true,"explicit unknown selection",custom,custom);
        exact(fallback.updateSpeeds(true,10,0,new Speeds(1.5f,1.5f)).horizontal(),1.7f,"unknown selection follows original custom fallback");
        require(!fallback.customSettingsVisible(),"custom visibility only for exact Custom label");
        require(new FireworkBoostBehavior(true,"Custom",custom,custom).customSettingsVisible(),"Custom visibility");
        Speeds incoming = new Speeds(1.25f,1.75f);
        require(rw.updateSpeeds(false,0,0,incoming)==incoming,"no-player event remains untouched");
        require(new FireworkBoostBehavior(false,"RW",custom,custom).updateSpeeds(true,0,0,incoming)==incoming,"disabled event untouched");
        Vector vanilla = new Vector(0.7,0.8,0.9);
        require(FireworkBoostBehavior.boostedVelocity(true,true,new Vector(0,0,1),new Vector(0,0,0),vanilla,new Speeds(1.5f,1.5f))==vanilla,"vanilla fast path");
        Vector result = FireworkBoostBehavior.boostedVelocity(true,true,new Vector(0.6,0.8,0),new Vector(0.2,-0.3,0.4),vanilla,new Speeds(2,1.8f));
        // Independent simplified affine form of original dadd/dsub arithmetic: v/2 + look*(0.1 + speed/2).
        near(result.x(),0.76,"horizontal affine boost");
        near(result.y(),0.65,"vertical affine boost");
        near(result.z(),0.2,"old lateral velocity damped");
        elytraChecks();
        System.out.println("FlightRecoveryChecks PASS: " + checks + " assertions");
    }

    private static void elytraChecks() {
        ElytraMotionBehavior behavior = new ElytraMotionBehavior(true,true,10,false,0);
        List<Boolean> keyStates = new ArrayList<>();
        AtomicInteger attempts = new AtomicInteger();
        Optional<Target> near = Optional.of(new Target(false,9.99f));
        behavior.tick(true,true,499,()->near,keyStates::add,attempts::incrementAndGet);
        require(behavior.isFrozen(),"near non-gliding target freezes local travel");
        require(behavior.shouldCancelTravel(true)&&!behavior.shouldCancelTravel(false),"only local travel is cancelled");
        require(keyStates.equals(List.of(false)),"freeze suppresses controlled key");
        require(attempts.get()==0,"rocket before 500ms blocked");
        behavior.tick(true,true,500,()->near,keyStates::add,attempts::incrementAndGet);
        require(attempts.get()==1&&behavior.lastRocketAttemptMillis()==500,"rocket at 500ms fires and timestamps");
        behavior.tick(true,true,999,()->near,keyStates::add,attempts::incrementAndGet);
        require(attempts.get()==1,"next rocket waits whole interval");
        behavior.tick(true,true,1000,()->Optional.of(new Target(true,1)),keyStates::add,attempts::incrementAndGet);
        require(!behavior.isFrozen()&&keyStates.get(keyStates.size()-1),"gliding target releases freeze and presses key");
        require(!behavior.check(true,true,Optional.of(new Target(false,10))),"distance threshold strict");
        require(!behavior.check(true,true,Optional.empty()),"missing target does not freeze");
        behavior.tick(true,false,1500,()->near,keyStates::add,attempts::incrementAndGet);
        require(!behavior.isFrozen()&&attempts.get()==2,"not-gliding resets freeze without rocket use");
        ElytraMotionBehavior noPlayer = new ElytraMotionBehavior(true,false,10,true,123);
        noPlayer.tick(false,false,1000,Optional::empty,keyStates::add,attempts::incrementAndGet);
        require(noPlayer.isFrozen()&&noPlayer.lastRocketAttemptMillis()==123,"no-player return preserves state");
        ElytraMotionBehavior disabled = new ElytraMotionBehavior(false,true,10,true,123);
        disabled.tick(true,true,1000,()->near,keyStates::add,attempts::incrementAndGet);
        require(!disabled.isFrozen()&&disabled.lastRocketAttemptMillis()==123,"disabled clears freeze only");
        require(behavior.isGlidingTarget(true,true,near,false),"nonstrict ignores target glide flag");
        require(!behavior.isGlidingTarget(true,true,near,true),"strict requires both gliding");

        RocketSpy inventory = new RocketSpy();
        inventory.available=false;
        require(!ElytraMotionBehavior.requestRocket(inventory)&&inventory.calls.isEmpty(),"unavailable inventory returns");
        inventory.available=true;inventory.offhand=true;
        require(ElytraMotionBehavior.requestRocket(inventory),"offhand rocket chosen");
        require(inventory.calls.equals(List.of("offhand:elytra_motion:HIGH:FAST:SKIP_IF_BUSY")),"decoded owner and enum selectors passed intact");
        inventory.calls.clear();inventory.offhand=false;inventory.slot=-1;
        require(!ElytraMotionBehavior.requestRocket(inventory)&&inventory.calls.isEmpty(),"missing rocket produces no action");
        inventory.slot=17;
        require(ElytraMotionBehavior.requestRocket(inventory),"inventory rocket chosen");
        require(inventory.calls.equals(List.of("slot:17:40:elytra_motion:HIGH:FAST:SKIP_IF_BUSY")),"retained inventory operands preserved at explicit port");
    }

    private static final class RocketSpy implements ElytraMotionBehavior.RocketInventory {
        boolean available=true, offhand;
        int slot=-1;
        final List<String> calls=new ArrayList<>();
        public boolean isAvailable(){return available;}
        public boolean offhandHasFirework(){return offhand;}
        public int findFireworkSlot(){return slot;}
        public void useOffhand(String owner,String priority,String timing,String conflict){calls.add("offhand:"+owner+":"+priority+":"+timing+":"+conflict);}
        public void useInventorySlot(int slot,int operand,String owner,String priority,String timing,String conflict){calls.add("slot:"+slot+":"+operand+":"+owner+":"+priority+":"+timing+":"+conflict);}
    }

    private static void exact(float actual,float expected,String message){checks++;if(Float.floatToIntBits(actual)!=Float.floatToIntBits(expected))throw new AssertionError(message+": "+actual+" != "+expected);}
    private static void near(double actual,double expected,String message){checks++;if(Math.abs(actual-expected)>1e-7)throw new AssertionError(message+": "+actual+" != "+expected);}
    private static void require(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}
