package edu.knox.minecraft.serverturtle;

import java.util.Map;
import java.util.Stack;

import edu.knoxcraft.turtle3d.KCTCommand;
import edu.knoxcraft.turtle3d.KCTScript;
import edu.knoxcraft.turtle3d.TurtleCommandException;
import net.canarymod.api.world.World;
import net.canarymod.api.world.blocks.BlockType;
import net.canarymod.api.world.position.Direction;
import net.canarymod.api.world.position.Position;
import net.canarymod.chat.MessageReceiver;
import net.canarymod.logger.Logman;

public class Turtle {

    //POSITION VARIABLES

    //in world position of player at turtle on --> (0,0,0) for Turtle's relative coord system.
    private Position originPos;

    //current relative position
    private Position relPos;

    //true current position/direction in game coords(made by combining relative and real)
    private Position gamePos;
    private Direction dir;

    //OTHER VARIABLES
    private boolean bp = true;  //Block Place on/off
    private BlockType bt = BlockType.Stone;  //default turtle block type 
    private World world;  //World in which all actions occur
    private MessageReceiver sender;  //player to send messages to
    private Stack<BlockRecord> oldBlocks;  //original pos/type of all bricks laid by this turtle for undoing
    private Logman logger;

    ///////////////////////////////////////////////////////////////////////////////
    public Turtle(Logman logger) {
        this.logger=logger;
    }

    /**
     * Initialize the turtle.  Called when executing a script (or turning command line turtle on) 
     * @param sender
     */
    public void turtleInit(MessageReceiver sender)  {
        //initialize undo buffer
        oldBlocks = new Stack<BlockRecord>();

        //record sender
        this.sender = sender;

        //GET WORLD
        world = sender.asPlayer().getWorld();

        //Set up positions

        //Get origin Position and Direction
        originPos = sender.asPlayer().getPosition();
        dir = sender.asPlayer().getCardinalDirection();

        //Make the Relative Position
        relPos = new Position(0,0,0);

        //Update game position
        gamePos = new Position(); 
        updateGamePos();
    }       

    /**
     * Output a message to the player console.
     * 
     * @param sender
     * @param args
     */
    public void turtleConsole(String msg)
    {
        sender.message(msg); 
    }  

    /**
     * Turn block placement mode on/off.
     * 
     * @param sender
     * @param args
     */
    public void turtleSetBlockPlace(boolean mode)
    {
        bp = mode;
        turtleBlockPlaceStatus();  //alert user about change
    }

    /**
     * Reports whether block placement mode is on.
     * @param sender
     * @param args
     */
    public void turtleBlockPlaceStatus()
    {
        if(bp)  {
            turtleConsole("Block placement mode on.");
        }  else {
            turtleConsole("Block placement mode off.");
        }
    }

    /**
     * Set turtle position (relative coords)
     * @param sender
     * @param args
     */
    public void turtleSetRelPosition(int x, int y, int z)
    {     
        relPos = new Position(x, y, z);
    }

    /**
     * Set turtle direction.  Number based.
     * 
     * @param sender
     * @param args
     */
    public void turtleSetDirection(int dir)
    {
        //update direction
        // 0 = NORTH
        // 1 = NORTHEAST
        // 2 = EAST
        // 3 = SOUTHEAST
        // 4 = SOUTH
        // 5 = SOUTHWEST
        // 6 = WEST
        // 7 = NORTHWEST
        // Else = ERROR 
        this.dir = (Direction.getFromIntValue(dir));
    }

    /**
     * Report current position (relative)
     */
    public void turtleReportPosition()
    {
        turtleConsole("" + relPos);
    }

    /**
     * Report current position of Turtle in game coords
     */
    public void turtleReportGamePosition()
    {
        turtleConsole("" + gamePos);
    }

    /**
     * Report position of relative origin (Player's pos at Turtle on) in game coords
     */
    public void turtleReportOriginPosition()
    {
        turtleConsole("" + originPos);
    }

    /**
     * Report current direction (relative)
     */
    public void turtleReportDirection()
    {
        turtleConsole("" + dir);
    }

    /**
     * Set block type (int based)
     * @param int
     */
    public void turtleSetBlockType(int blockType)
    {
        if (!bp)  //don't allow if block placement mode isn't on
            return;

        bt = BlockType.fromId(blockType);      
    }

    /**
     * set Block type (string/BlockType based)
     * @param sender
     * @param args
     */
    public void turtleSetBlockType(String blockType)
    {
        if (!bp)  //don't allow if block placement mode isn't on
            return;

        bt = BlockType.fromString(blockType);      
    }
    /**
     * Report current block type
     * @param sender
     * @param args
     */
    public void turtleReportBlockType()
    {
        if (!bp)  //don't allow if block placement mode isn't on
            return;

        //report current BT of turtle   
        turtleConsole("" + bt);
    }

    /**
     * Move (forward/back)
     * 
     * @param dist
     */
    public void turtleMove(int dist)
    {
        boolean fd = false;  //flipped direction (for moving backward) 

        //check if distance is negative (going backward)
        if (dist < 0){  
            //if so, reverse turtle direction
            dist = Math.abs(dist);
            flipDir();
            fd = true;
        }

        for (int i = dist; i > 0; i--){

            //update turtle position
            relPos = calculateMove(relPos, dir, false, false);
            updateGamePos();

            //Place block if block placement mode on
            if (bp) {

                //keep track of original block to undo
                oldBlocks.push(new BlockRecord(world.getBlockAt(gamePos), world));

                //place new block
                world.setBlockAt(gamePos, bt);
            }
        }

        //if reversed turtle direction, reset to original
        if (fd == true){
            flipDir();
        }
    }

    /**
     * Moves turtle up/down
     * @param dist
     */
    public void turtleUpDown(int dist)
    {
        boolean up = true;  //default direction is up

        //check if distance is negative (going down)
        if (dist < 0 ){  
            //if so, reverse turtle direction
            dist = Math.abs(dist);
            up = false;
        }

        for (int i = dist; i > 0; i--){

            //update turtle position
            relPos = calculateMove(relPos, dir, up, !up); 
            updateGamePos();

            //Place block if block placement mode on
            if (bp) {

                //keep track of original block to undo
                oldBlocks.push(new BlockRecord(world.getBlockAt(gamePos), world));

                //place new block
                world.setBlockAt(gamePos, bt);
            }
        }
    }

    /**
     * Turn right/left.
     * 
     * @param left
     * @param deg
     */ 
    public void turtleTurn(boolean left, int deg)
    {
        //turn turtle
        dir = calculateTurn(dir, left, deg);
    }

    /**
     * Return whether block placement mode is on.  Called by TurtleTester.
     * 
     * @return the value of bp
     */
    public boolean getBP()  {
        return bp;
    }

    /**
     * Return a copy of the stack of blocks replaced by this turtle (for undoing)
     * @return
     */
    public Stack<BlockRecord> getOldBlocks()  {
        Stack<BlockRecord> retVal = new Stack<BlockRecord>();
        Stack<BlockRecord> temp = new Stack<BlockRecord>();

        //populate reverse stack
        while(!oldBlocks.empty())  {
            temp.push(oldBlocks.pop());
        }

        //restore and copy
        while(!temp.empty())  {
            BlockRecord block = temp.pop();
            retVal.push(block);
            oldBlocks.push(block);
        }        

        return retVal;
    }    

    /**
     * Execute a KCTScript.
     */
    public void executeScript(KCTScript script) {

        //empty the undo buffer of previous scripts' blocks
        while (!oldBlocks.empty())  {
            oldBlocks.pop();
        }

        //execute each command of the new script
        for (KCTCommand c : script.getCommands())  {
            try {
                executeCommand(c);
            } catch (TurtleCommandException e) {
                sender.asPlayer().message(e.getMessage());
                sender.asPlayer().message("Unable to execute Turtle program "+script.getScriptName());
                return;
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    //PRIVATE HELPER FUNCTIONS

    /**
     * Update game pos
     */
    private void updateGamePos() {
        //get origin coords
        int xo = originPos.getBlockX();
        int yo = originPos.getBlockY();
        int zo = originPos.getBlockZ();     

        //get relative coords
        int xr = relPos.getBlockX();
        int yr = relPos.getBlockY();
        int zr = relPos.getBlockZ();

        //update game position
        //gamePos = originPos + relPos;
        gamePos.setX(xo+xr);
        gamePos.setY(yo+yr);
        gamePos.setZ(zo+zr);
    }

    /**
     * Reverses relative direction (turn 180 degrees).  Used when moving backward.
     */
    private void flipDir(){
        //get current direction (N, NE, ... , S --> 0, 1, ... , 7)
        int dirInt = dir.getIntValue();  

        //calculate new direction
        dirInt = (dirInt + 4) % 8;

        //update relDir
        dir = Direction.getFromIntValue(dirInt);
    }

    /**
     * Calculate move.  Returns new relative position of Turtle.
     * 
     * @param p Initial relative position of turtle
     * @param d forward direction of turtle
     * @param up Is this move going up?
     * @param down Is this move going down?
     * @return New relative position of turtle.
     */
    private Position calculateMove (Position p, Direction d, boolean up, boolean down){ 

        int dn = d.getIntValue();  //get direction number

        //check if vertical motion
        if (up || down ){
            if (up)  {  //moving up
                //add y +1
                p.setY(p.getBlockY() + 1);

            }else  {  //otherwise moving down
                //subtract y -1
                p.setY(p.getBlockY() - 1);
            }

        }  else  {  //2D motion
            if(dn == 0){ //NORTH
                //subtract z -1
                p.setZ(p.getBlockZ() - 1);

            }else if(dn == 1){//NORTHEAST
                //subtract z -1
                //add x +1
                p.setZ(p.getBlockZ() - 1);
                p.setX(p.getBlockX() + 1);

            }else if(dn == 2){//EAST
                //add x +1
                p.setX(p.getBlockX() + 1);

            }else if(dn == 3){//SOUTHEAST
                //add z +1
                //add x +1
                p.setZ(p.getBlockZ() + 1);
                p.setX(p.getBlockX() + 1);

            }else if(dn == 4){//SOUTH
                //add z +1
                p.setZ(p.getBlockZ() + 1);

            }else if(dn == 5){//SOUTHWEST
                //add z +1
                //subtract x -1
                p.setZ(p.getBlockZ() + 1);
                p.setX(p.getBlockX() - 1);

            }else if(dn == 6){//WEST
                //subtract x -1
                p.setX(p.getBlockX() - 1);

            }else if(dn == 7){//NORTHWEST
                //subtract z -1
                //subtract x -1
                p.setZ(p.getBlockZ() - 1);
                p.setX(p.getBlockX() - 1);

            }else {
                //BAD STUFF
                //Not one of the 8 main directions.  
                //Will require more math, but maybe we don't want to worry about this case.
            }
        }
        return p;  //return updated position
    }

    /**
     *  Calculate turn.  Returns new relative direction of Turtle.
     *  
     * @param d  Initial relative direction of turtle.
     * @param left Is this turn going left?  (False -> turning right)
     * @param deg  number of degrees to turn in specified direction
     * @return New relative direction of turtle.
     */
    private Direction calculateTurn(Direction d, boolean left, int deg)  {

        //get current direction (N, NE, ... , S --> 0, 1, ... , 7)
        int dirInt = d.getIntValue();  

        //calculate new direction            
        //This currently only works correctly for 45 deg intervals.  It may be okay to leave it that way.

        int turns = deg/45;  //desired number of eighth turns

        if (left)  {  //turning left
            dirInt -= turns;
        }  else  {  //turning right
            dirInt += turns;
        }

        dirInt = dirInt % 8;

        //update direction and return
        d = Direction.getFromIntValue(dirInt);
        return d;
    }

    private static int toInt(Object o) {
        return (int)((Long)o).longValue();
    }
    
    /*
     * Execute a KCTCommand.
     */
    private void executeCommand(KCTCommand c) throws TurtleCommandException {
        
        Map<String, Object> m = c.getArguments();
        String commandName = c.getCommandName();

        if (commandName.equals(KCTCommand.FORWARD)) {
            // go forward
            int dist;
            if (!m.containsKey(KCTCommand.DIST)){ //TODO VERIFY!!!
                dist = 1; //default
            }else{
                dist = toInt(m.get(KCTCommand.DIST));
            }
            turtleMove(dist); 
            
        }  else if (commandName.equals(KCTCommand.BACKWARD)) {
            // go backward
            int dist;
            if (!m.containsKey(KCTCommand.DIST)){ //TODO VERIFY!!!
                dist = -1; //default
            }else{
                dist = -toInt(m.get(KCTCommand.DIST));
            }
            turtleMove(dist); 
            
        }else if (commandName.equals(KCTCommand.TURNRIGHT)) {
            // turn right
            //Parse for amount to turn
            int ang;
            if (!m.containsKey(KCTCommand.DEGREES)){ //TODO VERIFY!!!
                ang = 90; //default
            }else{
                ang = toInt(m.get(KCTCommand.DEGREES));
            }
            turtleTurn(false, ang);
            
        } else if (commandName.equals(KCTCommand.TURNLEFT)) {
            // turn left
            int ang;
            if (!m.containsKey(KCTCommand.DEGREES)){ //TODO VERIFY!!!
                ang = 90; //default
            }else{
                ang = toInt(m.get(KCTCommand.DEGREES)); //Magic hand wavey stuff
            }
            //Parse for amount to turn
            turtleTurn(true, ang);
            
        } else if (commandName.equals(KCTCommand.PLACEBLOCKS)) {
            // place blocks on/off 
            //only changes mode if arg map contains valid arg
            if (m.containsKey(KCTCommand.BLOCKPLACEMODE)){ //TODO VERIFY!!!
                boolean mode = (boolean)m.get(KCTCommand.BLOCKPLACEMODE);  //TODO:  does this work?
                turtleSetBlockPlace(mode);
            }
            
        } else if (commandName.equals(KCTCommand.SETPOSITION)) {
            // TODO set position
            turtleSetRelPosition(0, 0, 0);
            
        } else if (commandName.equals(KCTCommand.UP)) {
            // go up
            int dist;
            if (!m.containsKey(KCTCommand.DIST)){ //TODO VERIFY!!!
                dist = 1; //default
            }else{
                dist = toInt(m.get(KCTCommand.DIST)); //Magic hand wavey stuff)
            }
            turtleUpDown(dist);
            
        } else if (commandName.equals(KCTCommand.DOWN)) {
            // go down
            int dist;
            if (!m.containsKey(KCTCommand.DIST)){ //TODO VERIFY!!!
                dist = -1; //default
            }else{
                dist = -toInt(m.get(KCTCommand.DIST)); //Magic hand wavey stuff)
            }
            turtleUpDown(dist);
            
        } else if (commandName.equals(KCTCommand.SETBLOCK)) {
            // Set block type
            int type;
            String strType = "";
            if (!m.containsKey(KCTCommand.BLOCKTYPE)){ //Default
                type = 1; //default is Stone
                turtleSetBlockType(type);
            }else{
                Object o = m.get(KCTCommand.BLOCKTYPE);
                if (o instanceof String) {//STR vs INT
                    strType = (String)o;
                    turtleSetBlockType(strType);
                } else{ // Otherwise its an int
                    type = toInt(o);
                    turtleSetBlockType(type);
                }
            }
            
        } else {
            // TODO: Handle an unknown command. Is Runtime Exception the correct exception?
            // Are there better ways to handle this?
            String msg=String.format("Unknown command: %s", commandName);
            logger.error(msg);
            throw new TurtleCommandException(msg);
        }
    }
}