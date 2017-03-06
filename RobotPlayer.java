package peacefulplayer;
import battlecode.common.*;
import javafx.scene.shape.Arc;
import sun.reflect.generics.tree.Tree;

import java.util.ArrayList;
import java.util.Map;

/**
 * Created by Akhilan on 1/11/2017.
 */
public strictfp class RobotPlayer {
    private static RobotController rc;

    private static int MAPPING_CHANNELS = 15;
    private static int TREE_CLEARING_CHANNEL = 2000;
    private static int SPAWN_QUEUE_CHANNEL = 3000; //3000 to 3002

    private static int ROUND_NUM_CHANNEL = 999;
    private static int NUM_GARDENER_CHANNEL = 998;
    private static int NUM_SOLDIER_CHANNEL = 996;
    private static int NUM_SCOUT_CHANNEL = 995;
    private static int NUM_LUMBERJACK_CHANNEL = 994;
    private static int NUM_TANK_CHANNEL = 993;
    private static int NUM_TREES_CHANNEL = 1000;

    private static int LAST_GARDENER_CHANNEL = 992;
    private static int LAST_SOLDIER_CHANNEL = 990;
    private static int LAST_SCOUT_CHANNEL = 989;
    private static int LAST_LUMBERJACK_CHANNEL = 988;
    private static int LAST_TANK_CHANNEL = 987;
    private static int LAST_TREES_CHANNEL = 986;

    private static int STARTING_PHASE = 0;
    private static int ATTACK_PHASE = 1;
    private static int DONATE_PHASE = 2;

    private static int phase = STARTING_PHASE;
    private static float velScale = (float) .5;
    private static MapLocation goalPos = null;
    private static int waitingForLumberjackTurns = 0;
    private static boolean hasPlanted = false;
    private static int numInitialArchons = 0;
    private static int myArchonNum = 0;
    private static Direction wanderDir = null;
    private static int wanderTurns = 0;
    private static boolean respondingToCall = false;
    private static boolean treeConfigA = true;
    private static Direction gardenerPlantDir = null;
    private static boolean wanderMode = false;
    private static boolean DEBUG = false;
    private static int[] lastNext;
    private static int[] curNext;
    private static boolean[] changed;

    private static RobotType[] starterPack = {RobotType.GARDENER, RobotType.LUMBERJACK, RobotType.SOLDIER, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.GARDENER, RobotType.ARCHON, RobotType.SOLDIER, RobotType.ARCHON, RobotType.ARCHON, RobotType.LUMBERJACK, RobotType.ARCHON, RobotType.GARDENER, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.GARDENER, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.SCOUT};

    private static RobotType[] blankStarterPack = {RobotType.GARDENER, RobotType.ARCHON, RobotType.SOLDIER, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.GARDENER, RobotType.ARCHON, RobotType.SOLDIER, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.GARDENER, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.GARDENER, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.SCOUT};

    private static RobotType[] multiStarterPack = {RobotType.GARDENER, RobotType.LUMBERJACK, RobotType.ARCHON, RobotType.SOLDIER, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.GARDENER, RobotType.ARCHON, RobotType.SOLDIER, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.LUMBERJACK, RobotType.GARDENER, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.GARDENER, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.SCOUT};

    private static RobotType[] multiBlankStarterPack = {RobotType.GARDENER, RobotType.ARCHON, RobotType.SOLDIER, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.GARDENER, RobotType.ARCHON, RobotType.ARCHON, RobotType.SOLDIER, RobotType.ARCHON, RobotType.ARCHON, RobotType.GARDENER, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.GARDENER, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.SCOUT};

    public static void run(RobotController rc) throws GameActionException {
        RobotPlayer.rc = rc;
        switch (rc.getType()) {
            case ARCHON:
                runArchon();
                break;
            case GARDENER:
                runGardener();
                break;
            case SOLDIER:
                runSoldier();
                break;
            case LUMBERJACK:
                runLumberjack();
                break;
            case SCOUT:
                runScout();
                break;
            case TANK:
                runTank();
                break;
        }
    }

    private static void runArchon() throws GameActionException {
        while(true){
            try{
                if (phase == STARTING_PHASE && (rc.readBroadcast(LAST_TREES_CHANNEL) > 5 || rc.getRoundNum() > 2000)){
                    phase = ATTACK_PHASE;
                }
                TreeInfo[] trees = rc.senseNearbyTrees();
                RobotInfo[] robots = rc.senseNearbyRobots();
                BulletInfo[] bullets = rc.senseNearbyBullets();
                if (numInitialArchons == 0){
                    numInitialArchons = rc.getInitialArchonLocations(rc.getTeam()).length;
                    findMyArchonNum();
                    rc.broadcast(SPAWN_QUEUE_CHANNEL+6*myArchonNum, 1);
                    if (DEBUG){
                        System.out.println("Tree density: " + getTreeDensity(trees));
                    }
                    if (getTreeDensity(trees) < .01) {
                        if (numInitialArchons > 1){
                            addToSpawnQueue(multiBlankStarterPack, myArchonNum);
                        }
                        else {
                            addToSpawnQueue(blankStarterPack, myArchonNum);
                        }
                    }
                    else{
                        if (numInitialArchons > 1){
                            addToSpawnQueue(multiStarterPack, myArchonNum);
                        }
                        else {
                            addToSpawnQueue(starterPack, myArchonNum);
                        }
                    }
                    lastNext = new int[numInitialArchons+1];
                    curNext = new int[numInitialArchons+1];
                    changed = new boolean[numInitialArchons+1];
                    for (int i = 0; i <= numInitialArchons; i++){
                        lastNext[i] = 0;
                        curNext[i] = 1;
                        changed[i] = false;
                    }
                }
                updateBroadcasting(robots);

                shakeTrees(trees);
                dodge(bullets);
                move(robots, trees);
                if (DEBUG){
                    System.out.println("alive channel: " + rc.readBroadcast(SPAWN_QUEUE_CHANNEL+6*myArchonNum+3));
                }
                resetBroadcasting();
                census();

                if (DEBUG){
                    printQueue(myArchonNum);
                    System.out.println("Phase: " + phase);
                }

                if (DEBUG) {
                    System.out.println("my archon number: " + myArchonNum);
                    System.out.println("next in queue: " + viewNextInQueue(myArchonNum));
                }

                if(rc.hasRobotBuildRequirements(RobotType.GARDENER) && viewNextInQueue(myArchonNum) == 2 && (rc.getRoundNum()%(numInitialArchons+1)) == myArchonNum){
                    if (tryHiringGardener()){
                        pullFromQueue(myArchonNum);
                    }
                    else{
                        MapLocation myLoc = rc.getLocation();
                        for (TreeInfo t: trees) {
                            if (t.getTeam() != rc.getTeam() && myLoc.distanceTo(t.getLocation()) < 4) {
                                callForLumberjack(rc.getLocation(), robots);
                                break;
                            }
                        }
                    }
                }

                Clock.yield();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private static float getTreeDensity(TreeInfo[] trees) throws GameActionException{
        float area = 0;
        for (TreeInfo t: trees){
            area += Math.pow(t.getRadius(),2);
        }
        return area/(float)Math.pow(rc.getType().sensorRadius,2);
    }

    private static void findMyArchonNum() throws GameActionException{
        for (int i = 0; i < numInitialArchons; i++){
            int occupied = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+6*i+3);
            if (occupied == 0){
                myArchonNum=i;
                return;
            }
        }
        rc.broadcast(SPAWN_QUEUE_CHANNEL+6*myArchonNum+3, 2);
    }

    private static boolean tryHiringGardener() throws GameActionException{
        int tries = 12;
        for (int i = 0; i < tries; i++) {
            Direction spawnDir = new Direction((float)Math.PI*2*i/tries);
            if (rc.canHireGardener(spawnDir)) {
                rc.hireGardener(spawnDir);
                return true;
            }
        }
        return false;
    }

    private static void runGardener() throws GameActionException {
        while (true) {
            try {
                if (phase == STARTING_PHASE && (rc.readBroadcast(LAST_TREES_CHANNEL) > 5 || rc.getRoundNum() > 1500)) {
                    phase = ATTACK_PHASE;
                }
                TreeInfo[] trees = rc.senseNearbyTrees();
                RobotInfo[] robots = rc.senseNearbyRobots();
                BulletInfo[] bullets = rc.senseNearbyBullets();
                if (numInitialArchons == 0) {
                    numInitialArchons = rc.getInitialArchonLocations(rc.getTeam()).length;
                    findMyArchonNum(robots);
                    lastNext = new int[numInitialArchons+1];
                    curNext = new int[numInitialArchons+1];
                    changed = new boolean[numInitialArchons+1];
                    for (int i = 0; i <= numInitialArchons; i++){
                        lastNext[i] = 0;
                        curNext[i] = 1;
                        changed[i] = false;
                    }
                }

                updateBroadcasting(robots);
                shakeTrees(trees);

                resetBroadcasting();
                census(trees);
                MapLocation myLoc = rc.getLocation();

                int next = viewNextInQueue(myArchonNum);
                if (DEBUG) {
                    System.out.println("my archon number: " + myArchonNum);
                    System.out.println("next in queue: " + next);
                }

                boolean plant = (next == 7 && (rc.getRoundNum()%(numInitialArchons+1)) == myArchonNum);
                if (!hasPlanted) {
                    dodge(bullets);
                    if (goodPlantingSpot(robots, trees)){
                        wanderMode = false;
                    }
                    else{
                        wanderMode = true;
                    }
                    move(robots, trees);

                    if (plant && rc.hasTreeBuildRequirements() && goodPlantingSpot(robots, trees)) {
                        boolean didIPlant = false;
                        for (int i = 0; i < 12; i++) {
                            Direction dir = new Direction((float) ((i * Math.PI / 6)));
                            if (rc.canPlantTree(dir)){
                                rc.plantTree(dir);
                                didIPlant = true;
                                treeConfigA = ((i%2) != 0);
                                hasPlanted = true;
                                wanderMode = false;
                                break;
                            }
                        }
                        if (!didIPlant) {
                            for (TreeInfo t : trees) {
                                if (t.getTeam() != rc.getTeam() && myLoc.distanceTo(t.getLocation()) < 4) {
                                    callForLumberjack(rc.getLocation(), robots);
                                    break;
                                }
                            }
                        }
                    }
                }
                else {
                    if (rc.hasTreeBuildRequirements() && next == 7) {
                        boolean didIPlant = false;
                        float offset = 0;
                        if (treeConfigA){
                            offset = (float) Math.PI/6;
                        }
                        boolean first = false;
                        boolean second = false;
                        for (int i = 0; i < 6; i++) {
                            Direction dir = new Direction((float) ((offset+(i * Math.PI / 3))%(2*Math.PI)));
                            if (rc.canPlantTree(dir)) {
                                if (!first){
                                    first = true;
                                }
                                else if (!second){
                                    second = true;
                                }
                                else {
                                    rc.plantTree(dir);
                                    pullFromQueue(myArchonNum);
                                    didIPlant = true;
                                    break;
                                }
                            }
                        }
                        if (!didIPlant) {
                            for (TreeInfo t : trees) {
                                if (t.getTeam() != rc.getTeam() && myLoc.distanceTo(t.getLocation()) < 4) {
                                    callForLumberjack(rc.getLocation(), robots);
                                    break;
                                }
                            }
                        }
                    }
                }


                if (rc.getRoundNum()%(numInitialArchons+1) == myArchonNum){
                    if (rc.hasRobotBuildRequirements(RobotType.SCOUT) && next == 5) {
                        if (tryBuildingRobot(RobotType.SCOUT)) {
                            pullFromQueue(myArchonNum);
                        } else {
                            for (TreeInfo t : trees) {
                                if (t.getTeam() != rc.getTeam() && myLoc.distanceTo(t.getLocation()) < 4) {
                                    callForLumberjack(rc.getLocation(), robots);
                                    break;
                                }
                            }
                        }
                    } else if (rc.hasRobotBuildRequirements(RobotType.LUMBERJACK) && next == 4) {
                        if (tryBuildingRobot(RobotType.LUMBERJACK)) {
                            pullFromQueue(myArchonNum);
                        } else {
                            for (TreeInfo t : trees) {
                                if (t.getTeam() != rc.getTeam() && myLoc.distanceTo(t.getLocation()) < 4) {
                                    callForLumberjack(rc.getLocation(), robots);
                                    break;
                                }
                            }
                        }
                    } else if (rc.hasRobotBuildRequirements(RobotType.SOLDIER) && next == 3) {
                        if (tryBuildingRobot(RobotType.SOLDIER)) {
                            pullFromQueue(myArchonNum);
                        } else {
                            for (TreeInfo t : trees) {
                                if (t.getTeam() != rc.getTeam() && myLoc.distanceTo(t.getLocation()) < 4) {
                                    callForLumberjack(rc.getLocation(), robots);
                                    break;
                                }
                            }
                        }
                    } else if (rc.hasRobotBuildRequirements(RobotType.TANK) && next == 6) {
                        if (tryBuildingRobot(RobotType.TANK)) {
                            pullFromQueue(myArchonNum);
                        } else {
                            for (TreeInfo t : trees) {
                                if (t.getTeam() != rc.getTeam() && myLoc.distanceTo(t.getLocation()) < 4) {
                                    callForLumberjack(rc.getLocation(), robots);
                                    break;
                                }
                            }
                        }
                    }
                }


                trees = rc.senseNearbyTrees();
                if (trees.length != 0) {
                    float lowestHealth = 10000;
                    int lowestID = trees[0].ID;
                    for (TreeInfo t : trees) {
                        if (rc.canWater(t.ID) && rc.getTeam().equals(t.getTeam()) && (t.getHealth() < lowestHealth)) {
                            lowestHealth = t.getHealth();
                            lowestID = t.ID;
                        }
                    }
                    if (lowestID == trees[0].ID) {
                        if (rc.canWater(trees[0].ID)) {
                            rc.water(lowestID);
                        }
                    } else {
                        rc.water(lowestID);
                    }
                }
                Clock.yield();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean goodPlantingSpot(RobotInfo[] robots, TreeInfo[] trees){
        for (RobotInfo r : robots) {
            if (rc.getType().equals(RobotType.GARDENER) && rc.getTeam().equals(r.getTeam()) && r.getLocation().distanceTo(rc.getLocation()) < 5.3) {
                for (TreeInfo t: trees){
                    if (t.getTeam() == rc.getTeam() && t.getLocation().distanceTo(r.getLocation()) < 2.05){
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private static void findMyArchonNum(RobotInfo [] robotInfos) throws GameActionException{
        myArchonNum = numInitialArchons;
        MapLocation[] ArchonLocs = new MapLocation[numInitialArchons];
        boolean[] ArchonAlive = new boolean[numInitialArchons];
        for (int i = 0; i < numInitialArchons; i++){
            float xPos = Float.intBitsToFloat(rc.readBroadcast(SPAWN_QUEUE_CHANNEL+6*i+4));
            float yPos = Float.intBitsToFloat(rc.readBroadcast(SPAWN_QUEUE_CHANNEL+6*i+5));
            int alive = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+6*i+3);
            ArchonLocs[i] = new MapLocation(xPos,yPos);
            ArchonAlive[i] = (alive==2 || alive == 3);
            if (DEBUG){
                System.out.println("Archon " + i + " alive: " + alive);
            }
        }

        for (RobotInfo r: robotInfos){
            if (r.getType().equals(RobotType.ARCHON) && r.getTeam().equals(rc.getTeam())){
                MapLocation loc = r.getLocation();
                if (DEBUG) {
                    System.out.println("robot location: " + loc);
                }
                for (int i = 0; i < numInitialArchons; i++){
                    if (DEBUG) {
                        System.out.println("trying Archon Num: " + i);
                    }
                    if (ArchonAlive[i]){
                        if (DEBUG) {
                            System.out.println("archon location: " + loc);
                        }
                        if (ArchonLocs[i].distanceTo(loc) < RobotType.ARCHON.strideRadius+.1) {
                            myArchonNum = i;
                            return;
                        }
                    }
                }
            }
        }
    }

    private static boolean tryBuildingRobot(RobotType t) throws GameActionException{
        int tries = 12;
        for (int i = 0; i < tries; i++) {
            Direction spawnDir = new Direction((float)Math.PI*2*i/tries);
            if (rc.canBuildRobot(t, spawnDir)) {
                rc.buildRobot(t, spawnDir);
                return true;
            }
        }
        return false;
    }

    private static void runLumberjack() throws GameActionException {
        while(true){
            try{
                if (phase == STARTING_PHASE && (rc.readBroadcast(LAST_TREES_CHANNEL) > 5 || rc.getRoundNum() > 1500)){
                    phase = ATTACK_PHASE;
                }
                if (numInitialArchons == 0){
                    numInitialArchons = rc.getInitialArchonLocations(rc.getTeam()).length;
                    lastNext = new int[numInitialArchons+1];
                    curNext = new int[numInitialArchons+1];
                    changed = new boolean[numInitialArchons+1];
                    for (int i = 0; i <= numInitialArchons; i++){
                        lastNext[i] = 0;
                        curNext[i] = 1;
                        changed[i] = false;
                    }
                }
                TreeInfo[] trees = rc.senseNearbyTrees();
                RobotInfo[] robots = rc.senseNearbyRobots();
                BulletInfo[] bullets = rc.senseNearbyBullets();

                ArrayList<MapLocation> enemies = updateBroadcasting(robots);
                shakeTrees(trees);
                dodge(bullets);

                int allyNum = 0;
                int enemyNum = 0;
                for (RobotInfo r : robots) {
                    if (r.getLocation().distanceTo(rc.getLocation()) < 2 + r.getRadius()) {
                        if (r.getTeam().equals(rc.getTeam())) {
                            allyNum++;
                        } else {
                            enemyNum++;
                        }
                    }
                }
                if (phase == STARTING_PHASE && allyNum < enemyNum && rc.canStrike()){
                    rc.strike();
                }
                else if(rc.canStrike() && phase == ATTACK_PHASE && enemyNum > 1){
                    rc.strike();
                }
                else if(rc.canStrike() && phase == ATTACK_PHASE && enemyNum > 0){
                    rc.strike();
                }

                MapLocation specialTree = null;
                boolean chopped = false;
                for (TreeInfo t: trees){
                    int treeID = t.getID();
                    if (!t.getTeam().equals(rc.getTeam()) && rc.canChop(treeID)){
                        rc.chop(treeID);
                        chopped = true;
                    }
                    if (t.containedRobot != null && specialTree == null){
                        specialTree = t.getLocation();
                    }
                }
                if (specialTree != null) {
                    callForLumberjack(specialTree, robots);
                }

                if (!chopped && goalPos == null) {
                    if (specialTree == null) {
                        if (enemies.size() != 0) {
                            MapLocation myLocation = rc.getLocation();
                            float closest = Float.MAX_VALUE;
                            if (phase == STARTING_PHASE) {
                                closest = 5;
                            }
                            for (MapLocation e : enemies) {
                                if (e.distanceTo(myLocation) < closest) {
                                    closest = e.distanceTo(myLocation);
                                    goalPos = e;
                                }
                            }
                        }
                    }
                    else{
                        goalPos = specialTree;
                    }

                    if (goalPos == null) {
                        MapLocation myLoc = rc.getLocation();
                        float locationX = Float.intBitsToFloat(rc.readBroadcast(TREE_CLEARING_CHANNEL));
                        float locationY = Float.intBitsToFloat(rc.readBroadcast(TREE_CLEARING_CHANNEL+1));
                        if (locationX != 0 || locationY != 0) {
                            goalPos = new MapLocation(locationX, locationY);
                            respondingToCall = true;
                        }
                    }
                    if (goalPos == null) {
                        for (TreeInfo t : trees) {
                            if (t.getTeam() != rc.getTeam()){
                                goalPos = t.getLocation();
                            }
                        }
                    }
                }
                if (!chopped){
                    move(robots, trees);
                }

                resetBroadcasting();
                census();

                Clock.yield();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private static void runScout() throws GameActionException {
        while(true){
            try{
                if (phase == STARTING_PHASE && (rc.readBroadcast(LAST_TREES_CHANNEL) > 5 || rc.getRoundNum() > 1500)){
                    phase = ATTACK_PHASE;
                }
                if (numInitialArchons == 0){
                    numInitialArchons = rc.getInitialArchonLocations(rc.getTeam()).length;
                    lastNext = new int[numInitialArchons+1];
                    curNext = new int[numInitialArchons+1];
                    changed = new boolean[numInitialArchons+1];
                    for (int i = 0; i <= numInitialArchons; i++){
                        lastNext[i] = 0;
                        curNext[i] = 1;
                        changed[i] = false;
                    }
                }
                TreeInfo[] trees = rc.senseNearbyTrees();
                RobotInfo[] robots = rc.senseNearbyRobots();
                BulletInfo[] bullets = rc.senseNearbyBullets();

                ArrayList<MapLocation> enemies = updateBroadcasting(robots);
                shakeTrees(trees);
                dodge(bullets);

                if (enemies.size() != 0){
                    MapLocation myLocation = rc.getLocation();
                    float closest = Float.MAX_VALUE;
                    if (phase == STARTING_PHASE) {
                        closest = 5;
                    }
                    for (MapLocation e : enemies){
                        if (e.distanceTo(myLocation) < closest){
                            closest = e.distanceTo(myLocation);
                            goalPos = e;
                        }
                    }
                }
                move(robots, trees);

                resetBroadcasting();
                census();

                if (robots.length > 0){
                    MapLocation loc = null;
                    MapLocation myLoc = rc.getLocation();
                    for (RobotInfo r : robots){
                        if (!r.getTeam().equals(rc.getTeam())) {
                            Direction dirToBot = myLoc.directionTo(r.getLocation());
                            float dist = r.getLocation().distanceTo(rc.getLocation());
                            boolean fire = true;
                            for (TreeInfo t : trees) {
                                if (t.getTeam() != rc.getTeam().opponent() && Math.abs(myLoc.directionTo(t.getLocation()).degreesBetween(dirToBot)) < 10) {
                                    if (t.getLocation().distanceTo(myLoc) < dist) {
                                        fire = false;
                                        break;
                                    }
                                }
                            }
                            for (RobotInfo rb : robots) {
                                if (rb.getTeam() != rc.getTeam().opponent() && Math.abs(myLoc.directionTo(rb.getLocation()).degreesBetween(dirToBot)) < 10) {
                                    if (rb.getLocation().distanceTo(myLoc) < dist) {
                                        fire = false;
                                        break;
                                    }
                                }
                            }
                            if (fire) {
                                loc = r.getLocation();
                                break;
                            }
                        }
                    }
                    if (loc != null && rc.canFireSingleShot()){
                        rc.fireSingleShot(myLoc.directionTo(loc));
                    }
                }

                Clock.yield();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private static void runSoldier() throws GameActionException {
        while(true){
            try{
                if (phase == STARTING_PHASE && (rc.readBroadcast(LAST_TREES_CHANNEL) > 5 || rc.getRoundNum() > 1500)){
                    phase = ATTACK_PHASE;
                }
                if (numInitialArchons == 0){
                    numInitialArchons = rc.getInitialArchonLocations(rc.getTeam()).length;
                    lastNext = new int[numInitialArchons+1];
                    curNext = new int[numInitialArchons+1];
                    changed = new boolean[numInitialArchons+1];
                    for (int i = 0; i <= numInitialArchons; i++){
                        lastNext[i] = 0;
                        curNext[i] = 1;
                        changed[i] = false;
                    }
                }
                TreeInfo[] trees = rc.senseNearbyTrees();
                RobotInfo[] robots = rc.senseNearbyRobots();
                BulletInfo[] bullets = rc.senseNearbyBullets();
                ArrayList<MapLocation> enemies = updateBroadcasting(robots);
                shakeTrees(trees);
                dodge(bullets);

                if (enemies.size() != 0){
                    MapLocation myLocation = rc.getLocation();
                    float closest = Float.MAX_VALUE;
                    if (phase == STARTING_PHASE) {
                        closest = 5;
                    }
                    for (MapLocation e : enemies){
                        if (e.distanceTo(myLocation) < closest){
                            closest = e.distanceTo(myLocation);
                            goalPos = e;
                        }
                    }
                }

                move(robots, trees);

                resetBroadcasting();
                census();


                if (robots.length > 0){
                    MapLocation loc = null;
                    MapLocation myLoc = rc.getLocation();
                    for (RobotInfo r : robots){
                        if (!r.getTeam().equals(rc.getTeam())) {
                            Direction dirToBot = myLoc.directionTo(r.getLocation());
                            float dist = r.getLocation().distanceTo(rc.getLocation());
                            boolean fire = true;
                            for (TreeInfo t : trees) {
                                if (t.getTeam() != rc.getTeam().opponent() && Math.abs(myLoc.directionTo(t.getLocation()).degreesBetween(dirToBot)) < 30) {
                                    if (t.getLocation().distanceTo(myLoc) < dist) {
                                        fire = false;
                                        break;
                                    }
                                }
                            }
                            for (RobotInfo rb : robots) {
                                if (rb.getTeam() != rc.getTeam().opponent() && Math.abs(myLoc.directionTo(rb.getLocation()).degreesBetween(dirToBot)) < 30) {
                                    if (rb.getLocation().distanceTo(myLoc) < dist) {
                                        fire = false;
                                        break;
                                    }
                                }
                            }
                            if (fire) {
                                loc = r.getLocation();
                                break;
                            }
                        }
                    }
                    if (loc != null && rc.canFirePentadShot()){
                        rc.firePentadShot(myLoc.directionTo(loc));
                    }
                    else if (loc != null && rc.canFireTriadShot()){
                        rc.fireTriadShot(myLoc.directionTo(loc));
                    }
                    else if (loc != null && rc.canFireSingleShot()) {
                        rc.fireSingleShot(myLoc.directionTo(loc));
                    }
                    if (loc == null){
                        for (RobotInfo r : robots){
                            if (!r.getTeam().equals(rc.getTeam())) {
                                Direction dirToBot = myLoc.directionTo(r.getLocation());
                                float dist = r.getLocation().distanceTo(rc.getLocation());
                                boolean fire = true;
                                for (TreeInfo t : trees) {
                                    if (t.getTeam() != rc.getTeam().opponent() && Math.abs(myLoc.directionTo(t.getLocation()).degreesBetween(dirToBot)) < 20) {
                                        if (t.getLocation().distanceTo(myLoc) < dist) {
                                            fire = false;
                                            break;
                                        }
                                    }
                                }
                                for (RobotInfo rb : robots) {
                                    if (rb.getTeam() != rc.getTeam().opponent() && Math.abs(myLoc.directionTo(rb.getLocation()).degreesBetween(dirToBot)) < 20) {
                                        if (rb.getLocation().distanceTo(myLoc) < dist) {
                                            fire = false;
                                            break;
                                        }
                                    }
                                }
                                if (fire) {
                                    loc = r.getLocation();
                                    break;
                                }
                            }
                        }
                    }
                    if (loc != null && rc.canFireTriadShot()){
                        rc.fireTriadShot(myLoc.directionTo(loc));
                    }
                    else if (loc != null && rc.canFireSingleShot()) {
                        rc.fireSingleShot(myLoc.directionTo(loc));
                    }
                    if (loc == null){
                        for (RobotInfo r : robots){
                            if (!r.getTeam().equals(rc.getTeam())) {
                                Direction dirToBot = myLoc.directionTo(r.getLocation());
                                float dist = r.getLocation().distanceTo(rc.getLocation());
                                boolean fire = true;
                                for (TreeInfo t : trees) {
                                    if (t.getTeam() != rc.getTeam().opponent() && Math.abs(myLoc.directionTo(t.getLocation()).degreesBetween(dirToBot)) < 10) {
                                        if (t.getLocation().distanceTo(myLoc) < dist) {
                                            fire = false;
                                            break;
                                        }
                                    }
                                }
                                for (RobotInfo rb : robots) {
                                    if (rb.getTeam() != rc.getTeam().opponent() && Math.abs(myLoc.directionTo(rb.getLocation()).degreesBetween(dirToBot)) < 10) {
                                        if (rb.getLocation().distanceTo(myLoc) < dist) {
                                            fire = false;
                                            break;
                                        }
                                    }
                                }
                                if (fire) {
                                    loc = r.getLocation();
                                    break;
                                }
                            }
                        }
                    }
                    if (loc != null && rc.canFireSingleShot()) {
                        rc.fireSingleShot(myLoc.directionTo(loc));
                    }
                }

                Clock.yield();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private static void runTank() throws GameActionException {
        while(true){
            try{
                if (phase == STARTING_PHASE && (rc.readBroadcast(LAST_TREES_CHANNEL) > 5 || rc.getRoundNum() > 1500)){
                    phase = ATTACK_PHASE;
                }
                if (numInitialArchons == 0){
                    numInitialArchons = rc.getInitialArchonLocations(rc.getTeam()).length;
                    lastNext = new int[numInitialArchons+1];
                    curNext = new int[numInitialArchons+1];
                    changed = new boolean[numInitialArchons+1];
                    for (int i = 0; i <= numInitialArchons; i++){
                        lastNext[i] = 0;
                        curNext[i] = 1;
                        changed[i] = false;
                    }
                }
                TreeInfo[] trees = rc.senseNearbyTrees();
                RobotInfo[] robots = rc.senseNearbyRobots();
                BulletInfo[] bullets = rc.senseNearbyBullets();

                ArrayList<MapLocation> enemies = updateBroadcasting(robots);
                shakeTrees(trees);
                dodge(bullets);
                move(robots, trees);

                resetBroadcasting();
                census();

                if (enemies.size() != 0){
                    MapLocation myLocation = rc.getLocation();
                    float closest = Float.MAX_VALUE;
                    if (phase == STARTING_PHASE) {
                        closest = 5;
                    }
                    for (MapLocation e : enemies){
                        if (e.distanceTo(myLocation) < closest){
                            closest = e.distanceTo(myLocation);
                            goalPos = e;
                        }
                    }
                }
                move(robots, trees);

                if (robots.length > 0){
                    MapLocation loc = null;
                    MapLocation myLoc = rc.getLocation();
                    for (RobotInfo r : robots){
                        if (!r.getTeam().equals(rc.getTeam())) {
                            Direction dirToBot = myLoc.directionTo(r.getLocation());
                            float dist = r.getLocation().distanceTo(rc.getLocation());
                            boolean fire = true;
                            for (TreeInfo t : trees) {
                                if (t.getTeam() != rc.getTeam().opponent() && Math.abs(myLoc.directionTo(t.getLocation()).degreesBetween(dirToBot)) < 30) {
                                    if (t.getLocation().distanceTo(myLoc) < dist) {
                                        fire = false;
                                        break;
                                    }
                                }
                            }
                            for (RobotInfo rb : robots) {
                                if (rb.getTeam() != rc.getTeam().opponent() && Math.abs(myLoc.directionTo(rb.getLocation()).degreesBetween(dirToBot)) < 30) {
                                    if (rb.getLocation().distanceTo(myLoc) < dist) {
                                        fire = false;
                                        break;
                                    }
                                }
                            }
                            if (fire) {
                                loc = r.getLocation();
                                break;
                            }
                        }
                    }
                    if (loc != null && rc.canFirePentadShot()){
                        rc.firePentadShot(myLoc.directionTo(loc));
                    }
                    else if (loc != null && rc.canFireTriadShot()){
                        rc.fireTriadShot(myLoc.directionTo(loc));
                    }
                    else if (loc != null && rc.canFireSingleShot()) {
                        rc.fireSingleShot(myLoc.directionTo(loc));
                    }
                    if (loc == null){
                        for (RobotInfo r : robots){
                            if (!r.getTeam().equals(rc.getTeam())) {
                                Direction dirToBot = myLoc.directionTo(r.getLocation());
                                float dist = r.getLocation().distanceTo(rc.getLocation());
                                boolean fire = true;
                                for (TreeInfo t : trees) {
                                    if (t.getTeam() != rc.getTeam().opponent() && Math.abs(myLoc.directionTo(t.getLocation()).degreesBetween(dirToBot)) < 20) {
                                        if (t.getLocation().distanceTo(myLoc) < dist) {
                                            fire = false;
                                            break;
                                        }
                                    }
                                }
                                for (RobotInfo rb : robots) {
                                    if (rb.getTeam() != rc.getTeam().opponent() && Math.abs(myLoc.directionTo(rb.getLocation()).degreesBetween(dirToBot)) < 20) {
                                        if (rb.getLocation().distanceTo(myLoc) < dist) {
                                            fire = false;
                                            break;
                                        }
                                    }
                                }
                                if (fire) {
                                    loc = r.getLocation();
                                    break;
                                }
                            }
                        }
                    }
                    if (loc != null && rc.canFireTriadShot()){
                        rc.fireTriadShot(myLoc.directionTo(loc));
                    }
                    else if (loc != null && rc.canFireSingleShot()) {
                        rc.fireSingleShot(myLoc.directionTo(loc));
                    }
                    if (loc == null){
                        for (RobotInfo r : robots){
                            if (!r.getTeam().equals(rc.getTeam())) {
                                Direction dirToBot = myLoc.directionTo(r.getLocation());
                                float dist = r.getLocation().distanceTo(rc.getLocation());
                                boolean fire = true;
                                for (TreeInfo t : trees) {
                                    if (t.getTeam() != rc.getTeam().opponent() && Math.abs(myLoc.directionTo(t.getLocation()).degreesBetween(dirToBot)) < 10) {
                                        if (t.getLocation().distanceTo(myLoc) < dist) {
                                            fire = false;
                                            break;
                                        }
                                    }
                                }
                                for (RobotInfo rb : robots) {
                                    if (rb.getTeam() != rc.getTeam().opponent() && Math.abs(myLoc.directionTo(rb.getLocation()).degreesBetween(dirToBot)) < 10) {
                                        if (rb.getLocation().distanceTo(myLoc) < dist) {
                                            fire = false;
                                            break;
                                        }
                                    }
                                }
                                if (fire) {
                                    loc = r.getLocation();
                                    break;
                                }
                            }
                        }
                    }
                    if (loc != null && rc.canFireSingleShot()) {
                        rc.fireSingleShot(myLoc.directionTo(loc));
                    }
                }
                Clock.yield();
            }
            catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    private static Direction randomDirection() {
        return new Direction((float)(Math.random()*2*Math.PI));
    }

    private static boolean moveInDirection(Direction dir) throws GameActionException{
        float dist = rc.getType().strideRadius;
        if (rc.hasMoved()){
            return true;
        }
        int tries = 10;
        while (!rc.canMove(dir, dist) && tries > 0){
            dist = (float)(dist*.8);
            tries--;
        }
        if (tries != 0) {
            rc.move(dir, dist);
            return true;
        }
        return false;
    }

    private static boolean moveInDirection(Direction dir, float dist) throws GameActionException{
        if (rc.hasMoved()){
            return true;
        }
        int tries = 10;
        while (!rc.canMove(dir, dist) && tries > 0){
            dist = (float)(dist*.8);
            tries--;
        }
        if (tries != 0) {
            rc.move(dir, dist);
            return true;
        }
        return false;
    }

    private static float distanceFromWall(Direction dir) throws GameActionException{
        float sense = rc.getType().sensorRadius - (float).001;
        MapLocation loc = rc.getLocation();
        int tries = 20;
        while (!rc.onTheMap(loc.add(dir,sense)) && tries > 0){
            sense = (float)(sense*.9);
            tries--;
        }
        if (tries != 0) {
            float myRadius = rc.getType().bodyRadius;
            if (sense > myRadius){
                return sense-rc.getType().bodyRadius;
            }
            return (float) .001;
        }
        return rc.getType().bodyRadius;
    }

    private static void dodge(BulletInfo[] bullets) throws GameActionException{
        Direction dir = Direction.getNorth();
        float passing = 0;
        boolean right = false;
        boolean willBeHit = false;
        for (BulletInfo b : bullets){
            Direction bulletDir = b.getDir();
            MapLocation bulletLoc = b.getLocation();
            Direction toMeDir = bulletLoc.directionTo(rc.getLocation());
            float distanceToMe = bulletLoc.distanceTo(rc.getLocation());
            float angle = toMeDir.radiansBetween(bulletDir);
            float passingDistance = Math.abs(distanceToMe*((float) Math.sin(angle)));
            if (angle < Math.PI/2 && angle > -Math.PI/2 && passingDistance < rc.getType().bodyRadius){
                right = (angle > 0);
                dir = bulletDir;
                passing = passingDistance;
                willBeHit = true;
                break;
            }
        }
        if (!willBeHit){
            return;
        }
        if (right){
            if (rc.canMove(dir.rotateRightRads((float)Math.PI/2), (float)(rc.getType().bodyRadius-passing+.01))) {
                rc.move(dir.rotateRightRads((float) Math.PI / 2), (float) (rc.getType().bodyRadius-passing+.01));
            }
        }
        else{
            if (rc.canMove(dir.rotateLeftRads((float)Math.PI/2), (float)(rc.getType().bodyRadius-passing+.01))) {
                rc.move(dir.rotateLeftRads((float) Math.PI / 2), (float) (rc.getType().bodyRadius-passing+.01));
            }
        }

    }

    private static void shakeTrees(TreeInfo [] trees) throws GameActionException{
        for(TreeInfo t : trees){
            if (rc.canShake(t.getLocation())){
                rc.shake(t.getLocation());
                return;
            }
        }
    }

    private static void charity(int need) throws GameActionException{
        if (phase == STARTING_PHASE){
            return;
        }
        float VCDiff = rc.getOpponentVictoryPoints() - rc.getTeamVictoryPoints();
        float bullets = rc.getTeamBullets();
        float taxRate = 0;
        int numTrees = rc.readBroadcast(LAST_TREES_CHANNEL);
        if (numTrees > 15){
            phase = DONATE_PHASE;
        }

        if (phase == DONATE_PHASE) {
            taxRate = (float) 0.05;
            if (VCDiff > 0){
                taxRate = Math.min(taxRate + (float)0.002*VCDiff, (float)0.4);
            }
        }
        if (DEBUG){
            System.out.println("tax rate: " + taxRate);
            System.out.println("need: " + need);
            System.out.println("bullets: " + bullets);
        }
        if ((1-taxRate)*bullets > need){
            rc.donate(Math.round(bullets-need));
        }
        else{
            rc.donate(Math.round(taxRate*bullets));
        }
    }

    private static void move(RobotInfo[] robots, TreeInfo[] trees) throws GameActionException {
        if (DEBUG && goalPos != null){
            rc.setIndicatorDot(rc.getLocation(), 0, 255 , 0);
            rc.setIndicatorDot(goalPos, 255, 0 , 0);
        }
        if (goalPos == null || waitingForLumberjackTurns > 0) {
            passiveMove(robots, trees);
            if (waitingForLumberjackTurns > 0){
                waitingForLumberjackTurns--;
            }
        }
        else {
            if (rc.getLocation().distanceTo(goalPos) < rc.getType().bodyRadius + 1){
                if (DEBUG){
                    System.out.println("goalPos set to null");
                }
                if (rc.getType() == RobotType.LUMBERJACK && respondingToCall){
                    respondingToCall = false;
                    MapLocation loc = new MapLocation(Float.intBitsToFloat(rc.readBroadcast(TREE_CLEARING_CHANNEL)), Float.intBitsToFloat(rc.readBroadcast(TREE_CLEARING_CHANNEL+1)));
                    if (loc.equals(goalPos)) {
                        rc.broadcast(TREE_CLEARING_CHANNEL, Float.floatToIntBits(0));
                        rc.broadcast(TREE_CLEARING_CHANNEL + 1, Float.floatToIntBits(0));
                    }
                }
                moveInDirection(rc.getLocation().directionTo(goalPos));
                goalPos = null;
                return;
            }
            Direction dirToGoal = rc.getLocation().directionTo(goalPos);
            if (canMove(dirToGoal, trees)) {
                moveInDirection(dirToGoal);
                return;
            }
            int tries = 5;
            for (int i = 0; i < tries; i++) {
                if (canMove(dirToGoal.rotateRightRads((float) Math.PI * i / 2 / tries), trees)) {
                    moveInDirection(dirToGoal.rotateRightRads((float) Math.PI * i / 2 / tries));
                    return;
                }
                if (canMove(dirToGoal.rotateLeftRads((float) Math.PI * i / 2 / tries), trees)) {
                    moveInDirection(dirToGoal.rotateLeftRads((float) Math.PI * i / 2 / tries));
                    return;
                }
            }
            MapLocation myLoc = rc.getLocation();
            for (TreeInfo t: trees) {
                if ((Math.abs(myLoc.directionTo(t.getLocation()).degreesBetween(dirToGoal)) < 10) && rc.getType() != RobotType.LUMBERJACK) {
                    callForLumberjack(rc.getLocation().add(dirToGoal, rc.getType().strideRadius), robots);
                    break;
                }
            }
            moveInDirection(dirToGoal.rotateRightRads((float)Math.PI));
            waitingForLumberjackTurns = 10;
        }
    }

    private static boolean canMove(Direction dir, TreeInfo[] trees){
        if (rc.canMove(dir)){
            if (rc.getType() != RobotType.TANK) {
                return true;
            }
            else{
                for (TreeInfo t: trees){
                    if (t.getTeam().equals(rc.getTeam()) && t.getLocation().distanceTo(rc.getLocation().add(dir, RobotType.TANK.strideRadius)) < (t.getRadius()+RobotType.TANK.bodyRadius))
                    {
                        return false;
                    }
                }
                return true;
            }
        }
        return false;
    }

    private static void callForLumberjack(MapLocation loc, RobotInfo[]  robots) throws GameActionException{
        for (RobotInfo r: robots){
            if (r.getType() == RobotType.LUMBERJACK && r.getTeam() == rc.getTeam() && loc.distanceTo(r.getLocation()) < RobotType.LUMBERJACK.bodyRadius + 1){
                return;
            }
        }
        int channelToUse = TREE_CLEARING_CHANNEL;
        rc.broadcast(channelToUse, Float.floatToIntBits(loc.x));
        rc.broadcast(channelToUse+1, Float.floatToIntBits(loc.y));
    }

    private static void wander() throws GameActionException{
        if (wanderTurns == 0){
            wanderDir = null;
        }

        if (wanderDir == null) {
            int tries = 12;
            for (int i = 0; i < tries; i++) {
                Direction dir = randomDirection();
                if (rc.canMove(dir) && !rc.hasMoved()) {
                    wanderDir = dir;
                    moveInDirection(wanderDir);
                    wanderTurns = 10;
                    return;
                }
            }
        }
        else{
            moveInDirection(wanderDir);
            wanderTurns--;
        }
    }

    private static void passiveMove(RobotInfo[] robots, TreeInfo[] trees) throws GameActionException{
        if (rc.getType() == RobotType.ARCHON || rc.getType() == RobotType.SCOUT || wanderMode){
            wander();
            return;
        }

        MapLocation vector = new MapLocation(0,0);
        MapLocation loc = rc.getLocation();
        float myRadius = rc.getType().bodyRadius;
        for (RobotInfo r : robots){
            Direction dir = r.getLocation().directionTo(loc);
            float dist = r.getLocation().distanceTo(loc)-myRadius-r.getType().bodyRadius+(float).001;
            vector = vector.add(dir, (float)1/dist);
        }
        if (!rc.getType().equals(RobotType.SCOUT) && !rc.getType().equals(RobotType.TANK)  && !rc.getType().equals(RobotType.LUMBERJACK) ) {
            for (TreeInfo t : trees) {
                Direction dir = t.getLocation().directionTo(loc);
                float dist = t.getLocation().distanceTo(loc)-myRadius-t.getRadius()+(float).001;
                vector = vector.add(dir, (float)1/dist);
            }
        }
        vector = vector.add(Direction.getSouth(), ((float)1/distanceFromWall(Direction.getNorth())));
        vector = vector.add(Direction.getNorth(), ((float)1/distanceFromWall(Direction.getSouth())));
        vector = vector.add(Direction.getEast(), ((float)1/distanceFromWall(Direction.getWest())));
        vector = vector.add(Direction.getWest(), ((float)1/distanceFromWall(Direction.getEast())));

        float magnitude = velScale*vector.distanceTo(new MapLocation(0,0));
        if (magnitude == 0){
            return;
        }
        Direction dir = new Direction(vector.x, vector.y);
        if (magnitude >= rc.getType().strideRadius){
            moveInDirection(dir);
        }
        moveInDirection(dir, magnitude);
    }

    private static ArrayList<MapLocation> updateBroadcasting(RobotInfo[] robots) throws GameActionException{
        ArrayList<MapLocation> enemyLocations = new ArrayList<>();
        int [] mapCast = getMapCast();
        for (int i = 0; i < (MAPPING_CHANNELS)/3; i++){
            int info = mapCast[3*i];
            float locationX = Float.intBitsToFloat(mapCast[3*i+1]);
            float locationY = Float.intBitsToFloat(mapCast[3*i+2]);
            int fill = info%2;
            MapLocation loc = new MapLocation(locationX,locationY);
            if (fill == 1) {
                if (rc.canSenseLocation(loc)){
                    RobotInfo ri = rc.senseRobotAtLocation(loc);
                    if (ri == null || rc.senseRobotAtLocation(loc).getTeam().equals(rc.getTeam())){
                        rc.broadcast(3*i, info - fill);
                        mapCast[3*i] = info-fill;
                    }
                    else{
                        enemyLocations.add(loc);
                    }
                }
                enemyLocations.add(loc);
            }
        }

        for (int i = 0; i < Math.min(robots.length, MAPPING_CHANNELS/3); i++){
            RobotInfo r = robots[i];
            MapLocation rLoc = r.getLocation();
            if (!robots[i].getTeam().equals(rc.getTeam())) {
                int xPos = Float.floatToIntBits(rLoc.x);
                int yPos = Float.floatToIntBits(rLoc.y);
                addEnemyToBroadcasting(mapCast, xPos, yPos);
                enemyLocations.add(r.getLocation());
            }
        }
        return enemyLocations;
    }

    private static int[] getMapCast() throws GameActionException{
        int[] mapCast = new int[MAPPING_CHANNELS];
        for (int i = 0; i < MAPPING_CHANNELS; i++){
            mapCast[i] = rc.readBroadcast(i);
        }
        return mapCast;
    }

    private static void addEnemyToBroadcasting(int [] mapCast, int xPos, int yPos) throws GameActionException{
        int emptySpot = -1;
        for (int i = 0; i < (MAPPING_CHANNELS) / 3; i++) {
            if (mapCast[3 * i] % 2 == 0) {
                emptySpot = 3 * i;
            }
        }
        if (emptySpot != -1) {
            rc.broadcast(emptySpot, mapCast[emptySpot]+1);
            mapCast[emptySpot] = mapCast[emptySpot]+1;
            rc.broadcast(emptySpot + 1, xPos);
            mapCast[emptySpot+1] = xPos;
            rc.broadcast(emptySpot + 2, yPos);
            mapCast[emptySpot+2] = yPos;
        }
    }

    private static void resetBroadcasting() throws GameActionException{
        for (int i = 0; i <= numInitialArchons; i++) {
            changed[i] = (curNext[i] != viewNextInQueue(i));
        }
        if (rc.readBroadcast(ROUND_NUM_CHANNEL) != rc.getRoundNum()){
            int lastGardeners = rc.readBroadcast(LAST_GARDENER_CHANNEL);
            int lastSoldiers = rc.readBroadcast(LAST_SOLDIER_CHANNEL);
            int lastScouts = rc.readBroadcast(LAST_SCOUT_CHANNEL);
            int lastLumberjacks = rc.readBroadcast(LAST_LUMBERJACK_CHANNEL);
            int lastTanks = rc.readBroadcast(LAST_TANK_CHANNEL);
            int lastTrees = rc.readBroadcast(LAST_TREES_CHANNEL);

            int gardeners = rc.readBroadcast(NUM_GARDENER_CHANNEL);
            int soldiers = rc.readBroadcast(NUM_SOLDIER_CHANNEL);
            int scouts = rc.readBroadcast(NUM_SCOUT_CHANNEL);
            int lumberjacks = rc.readBroadcast(NUM_LUMBERJACK_CHANNEL);
            int tanks = rc.readBroadcast(NUM_TANK_CHANNEL);
            int trees = rc.readBroadcast(NUM_TREES_CHANNEL);
            rc.broadcast(LAST_GARDENER_CHANNEL, gardeners);
            rc.broadcast(LAST_SOLDIER_CHANNEL, soldiers);
            rc.broadcast(LAST_SCOUT_CHANNEL, scouts);
            rc.broadcast(LAST_LUMBERJACK_CHANNEL, lumberjacks);
            rc.broadcast(LAST_TANK_CHANNEL, tanks);
            rc.broadcast(LAST_TREES_CHANNEL, trees);

            rc.broadcast(NUM_GARDENER_CHANNEL, 0);
            rc.broadcast(NUM_SOLDIER_CHANNEL, 0);
            rc.broadcast(NUM_SCOUT_CHANNEL, 0);
            rc.broadcast(NUM_LUMBERJACK_CHANNEL, 0);
            rc.broadcast(NUM_TREES_CHANNEL, 0);
            rc.broadcast(NUM_TANK_CHANNEL, 0);
            rc.broadcast(ROUND_NUM_CHANNEL, rc.getRoundNum());

            for (int i = 0; i < numInitialArchons; i++){
                int last = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+6*i+3);
                rc.broadcast(SPAWN_QUEUE_CHANNEL+6*i+3, (last*2)%4);
            }

            if (rc.getRoundNum() % 10 == 0) {
                charity(gardeners*60-trees*15+soldiers*50+scouts*5+tanks*50+numArchonsAlive()*100);
                for (int i = 0; i <= numInitialArchons; i++) {
                    if (((rc.getTeamBullets() > 100 && curNext[i] != 6) || (rc.getTeamBullets() > 300))) {
                        lastNext[i] = curNext[i];
                        curNext[i] = viewNextInQueue(i);
                        if (!changed[i]){
                            pullFromQueue(i);
                            RobotType r = RobotType.SOLDIER;
                            switch (curNext[i]) {
                                case 2:
                                    r = RobotType.GARDENER;
                                    break;
                                case 3:
                                    r = RobotType.SOLDIER;
                                    break;
                                case 4:
                                    r = RobotType.LUMBERJACK;
                                    break;
                                case 5:
                                    r = RobotType.SCOUT;
                                    break;
                                case 6:
                                    r = RobotType.TANK;
                                    break;
                                case 7: //Archon means tree
                                    r = RobotType.ARCHON;
                                    break;
                            }
                            addToSpawnQueue(r, 2);
                        }
                        changed[i] = false;
                    }

                    double rand = Math.random();
                    if (phase == STARTING_PHASE) {
                        if (rand < .4 && i != numInitialArchons) {
                            RobotType[] addQueue = {RobotType.GARDENER, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON};
                            addToSpawnQueue(addQueue, i);
                        } else if (rand < .5) {
                            addToSpawnQueue(RobotType.SOLDIER, i);
                            addToSpawnQueue(RobotType.SOLDIER, i);
                        } else if (rand < .7) {
                            addToSpawnQueue(RobotType.LUMBERJACK, i);
                            addToSpawnQueue(RobotType.SOLDIER, i);
                        } else if (rand < .9) {
                            addToSpawnQueue(RobotType.TANK, i);
                            addToSpawnQueue(RobotType.SOLDIER, i);
                        } else {
                            addToSpawnQueue(RobotType.SCOUT, i);
                            addToSpawnQueue(RobotType.SOLDIER, i);
                        }
                    }
                    else{
                        if (rand < .2 && i != numInitialArchons) {
                            RobotType[] addQueue = {RobotType.GARDENER, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON, RobotType.ARCHON};
                            addToSpawnQueue(addQueue, i);
                        } else if (rand < .5) {
                            addToSpawnQueue(RobotType.TANK, i);
                            addToSpawnQueue(RobotType.TANK, i);
                        } else if (rand < .6) {
                            addToSpawnQueue(RobotType.LUMBERJACK, i);
                            addToSpawnQueue(RobotType.SOLDIER, i);
                        } else if (rand < .9) {
                            addToSpawnQueue(RobotType.TANK, i);
                            addToSpawnQueue(RobotType.SOLDIER, i);
                        } else {
                            addToSpawnQueue(RobotType.SCOUT, i);
                            addToSpawnQueue(RobotType.SOLDIER, i);
                        }
                    }
                }
            }

            int shortestQueue = getShortestQueue();
            if (DEBUG){
                System.out.println("trees: " + trees);
                System.out.println("lastTrees: " + lastTrees);
            }
            for (int i = 0; i < lastTrees-trees; i++){
                priorityAddToSpawnQueue(RobotType.ARCHON, shortestQueue);
            }
            for (int i = 0; i < lastScouts-scouts; i++){
                addToSpawnQueue(RobotType.SCOUT, shortestQueue);
            }
            for (int i = 0; i < lastLumberjacks-lumberjacks; i++){
                priorityAddToSpawnQueue(RobotType.LUMBERJACK, shortestQueue);
            }
            for (int i = 0; i < lastTanks-tanks; i++){
                priorityAddToSpawnQueue(RobotType.TANK, shortestQueue);
            }
            for (int i = 0; i < lastSoldiers-soldiers; i++){
                priorityAddToSpawnQueue(RobotType.SOLDIER, shortestQueue);
            }
            for (int i = 0; i < lastGardeners-gardeners; i++){
                priorityAddToSpawnQueue(RobotType.GARDENER, shortestQueue);
            }

            if (rc.getRoundNum() == 1){
                initGame();
            }
        }
        else {
            if (rc.getRoundNum() % 10 == 0) {
                for (int i = 0; i <= numInitialArchons; i++) {
                    if (((rc.getTeamBullets() > 100 && curNext[i] != 6) || (rc.getTeamBullets() > 300))) {
                        lastNext[i] = curNext[i];
                        curNext[i] = viewNextInQueue(i);
                        changed[i] = false;
                    }
                }
            }
        }
    }

    private static int getShortestQueue() throws GameActionException{
        int shortestQueue = -1;
        int shortestAliveQueue = -1;
        int shortestQueueLength = Integer.MAX_VALUE;
        int shortestAliveQueueLength = Integer.MAX_VALUE;
        for (int i = 0; i < numInitialArchons; i++){
            int len = queueLength(i);
            if (len < shortestQueueLength){
                shortestQueue = i;
                shortestQueueLength = len;
            }
            if (len < shortestAliveQueueLength && isArchonAlive(i)){
                shortestAliveQueueLength = len;
                shortestAliveQueue = i;
            }
        }
        if (shortestAliveQueue == -1){
            return shortestQueue;
        }
        return shortestAliveQueue;
    }

    private static boolean isArchonAlive(int queue) throws GameActionException{
        return rc.readBroadcast(SPAWN_QUEUE_CHANNEL+6*queue+3) > 1;
    }

    private static void initGame() throws  GameActionException{
        if (DEBUG){
            System.out.println("Initialize game");
        }
        MapLocation[] enemyArchon = rc.getInitialArchonLocations(rc.getTeam().opponent());
        int[] mapCast = getMapCast();
        for (MapLocation e : enemyArchon) {
            int xPos = Float.floatToIntBits(e.x);
            int yPos = Float.floatToIntBits(e.y);
            addEnemyToBroadcasting(mapCast, xPos, yPos);
        }
        rc.broadcast(SPAWN_QUEUE_CHANNEL+6*myArchonNum+3, 2);
    }

    private static void census() throws GameActionException{
        RobotType t = rc.getType();
        switch (t) {
            case SOLDIER:
                rc.broadcast(NUM_SOLDIER_CHANNEL, rc.readBroadcast(NUM_SOLDIER_CHANNEL)+1);
                break;
            case LUMBERJACK:
                rc.broadcast(NUM_LUMBERJACK_CHANNEL, rc.readBroadcast(NUM_LUMBERJACK_CHANNEL)+1);
                break;
            case SCOUT:
                rc.broadcast(NUM_SCOUT_CHANNEL, rc.readBroadcast(NUM_SCOUT_CHANNEL)+1);
                break;
            case TANK:
                rc.broadcast(NUM_TANK_CHANNEL, rc.readBroadcast(NUM_TANK_CHANNEL)+1);
                break;
            case ARCHON:
                int last = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+6*myArchonNum+3);
                if (DEBUG){
                    System.out.println("last broadcast: " + last);
                }
                rc.broadcast(SPAWN_QUEUE_CHANNEL+6*myArchonNum+3, (last+1)%4);
                MapLocation myLoc = rc.getLocation();
                int xPos = Float.floatToIntBits(myLoc.x);
                int yPos = Float.floatToIntBits(myLoc.y);
                rc.broadcast(SPAWN_QUEUE_CHANNEL+6*myArchonNum+4, xPos);
                rc.broadcast(SPAWN_QUEUE_CHANNEL+6*myArchonNum+5, yPos);
                break;
        }
    }

    private static void census(TreeInfo[] trees) throws GameActionException{
        if (!hasPlanted){
            return;
        }
        rc.broadcast(NUM_GARDENER_CHANNEL, rc.readBroadcast(NUM_GARDENER_CHANNEL)+1);
        int numTrees = 0;
        MapLocation myLoc = rc.getLocation();
        for (TreeInfo t: trees){
            if (t.getLocation().distanceTo(myLoc) < 2.05 && t.getTeam().equals(rc.getTeam())) {
                numTrees++;
            }
        }
        rc.broadcast(NUM_TREES_CHANNEL, rc.readBroadcast(NUM_TREES_CHANNEL)+numTrees);
    }

    private static void priorityAddToSpawnQueue(RobotType t, int queue) throws GameActionException{
        int spawnQueue1 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+queue*6);
        int spawnQueue2 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+1+queue*6);
        int spawnQueue3 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+2+queue*6);
        int[] info = queueToInfo(spawnQueue1, spawnQueue2, spawnQueue3);
        int start = 0;
        for(int j = 0; j < 30; j++){
            if (info[j] == 1){
                start = j;
                break;
            }
        }
        int data = 0;
        switch (t) {
            case GARDENER:
                data = 2;
                break;
            case SOLDIER:
                data = 3;
                break;
            case LUMBERJACK:
                data = 4;
                break;
            case SCOUT:
                data = 5;
                break;
            case TANK:
                data = 6;
                break;
            case ARCHON: //Tree
                data = 7;
                break;
        }
        if (info[(1+start)%30] == 2 && numArchonsAlive() > 0){ //Never go before a gardener
            info[start] = 2;
            info[(1+start)%30] = data;
            info[(29 + start) % 30] = 1;
        }
        else {
            info[start] = data;
            info[(29 + start) % 30] = 1;
        }
        infoIntoQueue(info, queue);
    }

    private static void addToSpawnQueue(RobotType[] types, int queue) throws GameActionException{
        int spawnQueue1 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+queue*6);
        int spawnQueue2 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+1+queue*6);
        int spawnQueue3 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+2+queue*6);
        int[] info = queueToInfo(spawnQueue1, spawnQueue2, spawnQueue3);
        int start = 0;
        for(int j = 0; j < 30; j++){
            if (info[j] == 1){
                start = j;
                break;
            }
        }

        for (RobotType t: types){
            int firstSpot = -1;
            for (int i = start+1; i < 30; i++){
                if (info[i] == 0){
                    firstSpot = i;
                    break;
                }
            }
            if (firstSpot == -1) {
                for (int i = 0; i < start; i++) {
                    if (info[i] == 0){
                        firstSpot = i;
                        break;
                    }
                }
            }

            if (firstSpot != -1) {
                int data = 0;
                switch (t) {
                    case GARDENER:
                        data = 2;
                        break;
                    case SOLDIER:
                        data = 3;
                        break;
                    case LUMBERJACK:
                        data = 4;
                        break;
                    case SCOUT:
                        data = 5;
                        break;
                    case TANK:
                        data = 6;
                        break;
                    case ARCHON: //Archon means tree
                        data = 7;
                        break;
                }
                info[firstSpot] = data;
            }
        }
        infoIntoQueue(info, queue);
    }

    private static void addToSpawnQueue(RobotType t, int queue) throws GameActionException{
        int spawnQueue1 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+queue*6);
        int spawnQueue2 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+1+queue*6);
        int spawnQueue3 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+2+queue*6);
        int[] info = queueToInfo(spawnQueue1, spawnQueue2, spawnQueue3);
        int start = 0;
        for(int j = 0; j < 30; j++){
            if (info[j] == 1){
                start = j;
                break;
            }
        }

        int firstSpot = -1;
        for (int i = start+1; i < 30; i++){
            if (info[i] == 0){
                firstSpot = i;
                break;
            }
        }
        if (firstSpot == -1) {
            for (int i = 0; i < start; i++) {
                if (info[i] == 0){
                    firstSpot = i;
                    break;
                }
            }
        }

        if (firstSpot != -1){
            int data = 0;
            switch (t) {
                case GARDENER:
                    data = 2;
                    break;
                case SOLDIER:
                    data = 3;
                    break;
                case LUMBERJACK:
                    data = 4;
                    break;
                case SCOUT:
                    data = 5;
                    break;
                case TANK:
                    data = 6;
                    break;
                case ARCHON: //Archon means tree
                    data = 7;
                    break;
            }
            info[firstSpot] = data;
            infoIntoQueue(info, queue);
        }
    }

    private static void pullFromQueue(int queue) throws GameActionException{
        int spawnQueue1 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+queue*6);
        int spawnQueue2 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+1+queue*6);
        int spawnQueue3 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+2+queue*6);
        int[] info = queueToInfo(spawnQueue1, spawnQueue2, spawnQueue3);
        int start = 0;
        for(int j = 0; j < 30; j++){
            if (info[j] == 1){
                start = j;
                break;
            }
        }
        info[start] = 0;
        info[(start+1)%30] = 1;
        infoIntoQueue(info, queue);
    }

    private static void infoIntoQueue(int[] info, int queue) throws GameActionException{
        int newQueue1 = 0;
        int newQueue2 = 0;
        int newQueue3 = 0;

        for (int i = 0; i < 10; i++){
            newQueue1 += info[i]*(int)Math.pow(8,i);
        }
        for (int i = 0; i < 10; i++){
            newQueue2 += info[i+10]*(int)Math.pow(8,i);
        }
        for (int i = 0; i < 10; i++){
            newQueue3 += info[i+20]*(int)Math.pow(8,i);
        }
        rc.broadcast(SPAWN_QUEUE_CHANNEL+queue*6, newQueue1);
        rc.broadcast(SPAWN_QUEUE_CHANNEL+1+queue*6, newQueue2);
        rc.broadcast(SPAWN_QUEUE_CHANNEL+2+queue*6, newQueue3);
    }

    private static int viewNextInQueue(int queue) throws GameActionException{
        int spawnQueue1 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+queue*6);
        int spawnQueue2 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+1+queue*6);
        int spawnQueue3 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+2+queue*6);
        int[] info = queueToInfo(spawnQueue1, spawnQueue2, spawnQueue3);
        int start = 0;
        for(int j = 0; j < 30; j++){
            if (info[j] == 1){
                start = j;
                break;
            }
        }
        return info[(start+1)%30];
    }

    private static int[] queueToInfo(int spawnQueue1, int spawnQueue2, int spawnQueue3) throws GameActionException{
        int[] info = new int[30];
        for (int i = 0; i < 10; i++){
            info[i] = (spawnQueue1/((int)Math.pow(8,i)))%8;
        }
        for (int i = 0; i < 10; i++){
            info[i+10] = (spawnQueue2/((int)Math.pow(8,i)))%8;
        }
        for (int i = 0; i < 10; i++){
            info[i+20] = (spawnQueue3/((int)Math.pow(8,i)))%8;
        }
        return info;
    }

    private static int queueLength(int queue) throws GameActionException{
        int spawnQueue1 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+queue*6);
        int spawnQueue2 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+1+queue*6);
        int spawnQueue3 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+2+queue*6);
        int[] info = queueToInfo(spawnQueue1, spawnQueue2, spawnQueue3);
        int length = 0;
        for (int i : info){
            if (i != 0){
                length++;
            }
        }
        return length;
    }

    private static int numArchonsAlive() throws GameActionException{
        int num = 0;
        for (int i = 0; i < numInitialArchons; i++){
            if (rc.readBroadcast(SPAWN_QUEUE_CHANNEL+6*i+3) > 1){
                num++;
            }
        }
        return num;
    }

    private static void printQueue(int queue) throws GameActionException{
        int spawnQueue1 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+queue*6);
        int spawnQueue2 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+1+queue*6);
        int spawnQueue3 = rc.readBroadcast(SPAWN_QUEUE_CHANNEL+2+queue*6);
        int[] info = queueToInfo(spawnQueue1, spawnQueue2, spawnQueue3);
        for (int i : info){
            System.out.println(i);
        }
    }

    private static void printInfo(int [] info) throws GameActionException{
        for (int i : info){
            System.out.println(i);
        }
    }
}
