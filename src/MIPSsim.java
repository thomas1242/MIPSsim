import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;
import java.util.Scanner;

public class MIPSsim {
    public static void main(String[] args) {

        MIPSsimulation sim = new MIPSsimulation();

    }
}

class MIPSsimulation {

    View frame = null;

    public MIPSsimulation() {

        // SwingUtilities.invokeLater causes the Runnable to be executed asynchronously on the Event Dispatch Thread:
        // It queues up a task (GUI update) on the EDT and instantly returns.
        // Used to prevent long tasks from freezing up the GUI
        SwingUtilities.invokeLater( new Runnable() {
            public void run() {
                createAndShowGUI();
            }
        } );
    }

    // Create the GUI and show it.
    private void createAndShowGUI()
    {
        frame = new View( 1200, 630);                                 // setup new frame
        frame.setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE );    
        frame.setVisible( true ); // make the frame visible

        MipsPetriNetSimulator s = new MipsPetriNetSimulator( frame );  
        frame.giveModel( s );
        frame.redraw();
    }
}

class View extends JFrame
{
    ImagePanel imagePanel = null;
    JPanel controls = null;
    MipsPetriNetSimulator MIPSsim = null;
    JSlider slider = null;

    public View( int width, int height)
    {
        this.setTitle( "MIPSsim" );
        this.setSize( width, height );
        addMenu();

        imagePanel = new ImagePanel();
        this.add( imagePanel );

        slider = new JSlider(0, 8, 4);
        slider.setMinorTickSpacing(1);
        slider.setMajorTickSpacing(2);
        slider.setPaintTicks(true);
        slider.setSnapToTicks(true);
        slider.setPaintLabels(true);
        
        controls = new JPanel();
        controls.add( new JLabel("clock rate (Hz)"), BorderLayout.EAST );
        controls.add(slider, BorderLayout.WEST );

        JButton start = new JButton("RUN");
        start.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                new Thread(new Runnable() {
                    public void run() {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                MIPSsim.registerFile = null;
                                MIPSsim.dataMemory = null;
                                MIPSsim.instructionMemory = null;
                                MIPSsim.initSystem();
                                MIPSsim.simulate();
                                repaint();
                            }
                        });

                    }
                }).start();
            }
        });

        controls.add( start, BorderLayout.SOUTH );
        this.add(controls, BorderLayout.SOUTH);

        repaint();
        this.setVisible( true );
    }

    public void giveModel( MipsPetriNetSimulator s ) {
        this.MIPSsim = s;
        imagePanel.giveModel( s );
    }

    public void redraw() {
        imagePanel.redraw();
        repaint();
    }


    private void addMenu()
    {                       	   // addMenu() method used to setup a frame's menu bar
        JMenu fileMenu = new JMenu( "File" );
        JMenuItem exitItem = new JMenuItem( "Exit" );
        exitItem.addActionListener( new ActionListener() 	// define what happens when this menu item is selected
        {
            public void actionPerformed( ActionEvent event )
            {
                System.exit( 0 );
            }
        } );
        fileMenu.add( exitItem );

        JMenuBar menuBar = new JMenuBar();                  // create a new menu bar
        menuBar.add( fileMenu );                           	// add the "File" menu to the menu bar
        this.setJMenuBar( menuBar );                        // attach the menu bar to this frame
    }

}

class MipsPetriNetSimulator {

    DataMemory dataMemory = null;
    RegisterFile registerFile = null;
    InstructionMemory instructionMemory = null;
    InstructionBuffer instructionBuffer = null;
    LoadInstructionBuffer loadInstructionBuffer = null;
    ArithmeticInstructionBuffer arithmeticInstructionBuffer = null;
    AddressBuffer addressBuffer = null;
    ResultBuffer resultBuffer = null;

    boolean inProgress = false;

    private static int step = 0;
    View view;

    public MipsPetriNetSimulator(View frame) {

        // The attributes are Places
        // The methods are Transitions
        this.view = frame;

        instructionBuffer = new InstructionBuffer();
        loadInstructionBuffer = new LoadInstructionBuffer();
        arithmeticInstructionBuffer = new ArithmeticInstructionBuffer();
        addressBuffer = new AddressBuffer();
        resultBuffer = new ResultBuffer();

        initSystem();   // initialize register file, instruction memory, and data memory
    }

    public void initSystem() {

        String instructions = "instructions.txt";
        String registers    = "registers.txt";
        String datamemory   = "datamemory.txt";

        dataMemory = new DataMemory( datamemory );
        instructionMemory = new InstructionMemory( instructions );
        registerFile = new RegisterFile( registers );

        registerFile.initRegisters();
        instructionMemory.initInstructions();
        dataMemory.initMemory();
    }

    public void simulate() {



        view.giveModel( this );

        BufferedWriter buff = null;
        try {
            buff = new BufferedWriter(new FileWriter("../resources/simulation.txt"));
        }
        catch(Exception e) {}

        final BufferedWriter b = buff;

        new Thread(new Runnable() {
            @Override
            public void run() {

                inProgress = true;

                while( isFirable() ) {              // WHILE THERE EXIST FIRE-ABLE TRANSITIONS

                    while(view.slider.getValue() == 0) {
                        // do nothing, lol
                        try{
                            Thread.sleep(100);
                        }
                        catch(Exception e) {}
                    }

                    logSystemState( b );             // log system state

                    if(writeFirable())                  // fire transitions
                        write();
                    if(loadFirable())
                        load();
                    if(addrFirable())
                        addr();
                    if(aluFirable())
                        alu();
                    if(issue2Firable())
                        issue2();
                    if(issue1Firable())
                        issue1();
                    if(decodeFirable())
                        decode();

                    view.redraw();

                    try{
                        Thread.sleep(2000 / view.slider.getValue());
                    }
                    catch(Exception e) {}

                    step++;                             // increment step
                }
                logSystemState( b );
                inProgress = false;
            }
        }).start();


    }

    public void decode() {
        InstructionToken topInstruction = instructionMemory.getTopInstruction();
        int val1 = read( topInstruction.getSoureOp1() );
        int val2 = read( topInstruction.getSoureOp2() );
        InstructionToken decodedInstruction = new InstructionToken(topInstruction.getOpcode(), topInstruction.getDestReg(), val1, val2, topInstruction.id);
        instructionBuffer.receiveToken( decodedInstruction );
    }

    public int read(String sourceOp) {
        return registerFile.registers[ Integer.valueOf( sourceOp.substring( sourceOp.indexOf('R') + 1 )  ) ];
    }

    public void issue1() {
        arithmeticInstructionBuffer.receiveToken( instructionBuffer.instruction );
        instructionBuffer.consumeToken();
    }

    public void issue2() {
        loadInstructionBuffer.receiveToken( instructionBuffer.instruction );
        instructionBuffer.consumeToken();
    }

    public void alu() {
        InstructionToken instruct = arithmeticInstructionBuffer.arithmeticInstruction;
        arithmeticInstructionBuffer.consumeToken();
        // perform operation, put result in registerToken, pass to result buffer
        int result = 0;
        String opcode = instruct.getOpcode();
        int source1 = Integer.valueOf( instruct.getSoureOp1() );
        int source2 = Integer.valueOf( instruct.getSoureOp2() );

        if(opcode.contains("ADD")) {
            result = source1 + source2;
        }
        else if (opcode.contains("SUB") ) {
            result = source1 - source2;
        }
        else if (opcode.contains("AND") ) {
            result = source1 & source2;
        }
        else if (opcode.contains("OR") ) {
            result = source1 | source2;
        }

        RegisterToken res = new RegisterToken(instruct.getDestReg(), result, instruct.id);
        resultBuffer.results.add( res );
    }

    public void addr() {
        InstructionToken instruct = loadInstructionBuffer.loadInstruction;
        loadInstructionBuffer.consumeToken();
        // perform address calc, put result in DataToken, pass to address buffer
        int source1 = Integer.valueOf( instruct.getSoureOp1() );
        int source2 = Integer.valueOf( instruct.getSoureOp2() );
        int address = source1 + source2;
        DataMemoryToken res = new DataMemoryToken(instruct.getDestReg(), address, instruct.id);
        addressBuffer.loadInstruction = res;
    }

    public void load() {
        DataMemoryToken instruct = addressBuffer.loadInstruction;
        addressBuffer.consumeToken();
        // get data from data memory for corresponding address, put resulting reigstertoken in result buffer
        int valueAtAddress = dataMemory.memory[ instruct.getAddress() ];
        RegisterToken res = new RegisterToken(instruct.getName(), valueAtAddress, instruct.id);
        resultBuffer.results.add( res );
    }

    public void write() {
        RegisterToken toWrite = null;
        if(resultBuffer.results.size() <= 0) {
            return;
        }
        else {
            int minId = InstructionToken.instructionCount + 1;
            for(int i = 0; i < resultBuffer.results.size(); i++){
                if(resultBuffer.results.get(i).id < minId  ) {
                    minId = resultBuffer.results.get(i).id;
                    toWrite = resultBuffer.results.get(i);
                }
            }

            int index = Integer.valueOf( toWrite.getName().substring(1)  );
            int value = Integer.valueOf( toWrite.getValue()  );
            registerFile.updateRegister(index, value);
            resultBuffer.results.remove( toWrite );
        }
    }

    public boolean isFirable() {
        if(writeFirable())
            return true;
        else if(loadFirable())
            return true;
        else if(addrFirable())
            return true;
        else if(aluFirable())
            return true;
        else if(issue2Firable())
            return true;
        else if(issue1Firable())
            return true;
        else if(decodeFirable())
            return true;
        else
            return false;
    }

    public void logSystemState(BufferedWriter buff) {
        try {
            String stepString = "STEP " + step + ":";
            buff.write(stepString);
            buff.newLine();
            buff.write( "INM:" );
            buff.write( instructionMemory.getState() );
            buff.newLine();
            buff.write( "INB:" );
            buff.write( instructionBuffer.getState() );
            buff.newLine();
            buff.write( "AIB:" );
            buff.write( arithmeticInstructionBuffer.getState() );
            buff.newLine();
            buff.write( "LIB:" );
            buff.write( loadInstructionBuffer.getState() );
            buff.newLine();
            buff.write( "ADB:" );
            buff.write( addressBuffer.getState() );
            buff.newLine();
            buff.write( "REB:" );
            buff.write( resultBuffer.getState() );
            buff.newLine();
            buff.write( "RGF:" );
            buff.write( registerFile.getState() );
            buff.newLine();
            buff.write( "DAM:" );
            buff.write( dataMemory.getState() );
            buff.newLine();
            buff.newLine();
            buff.flush();
        }
        catch(Exception e) {}
    }

    public boolean decodeFirable() {
        if(instructionMemory.instructions.size() > 0)
            return true;
        else
            return false;
    }

    public boolean issue1Firable() {

        if(instructionBuffer.hasToken() && !instructionBuffer.getInstruction().getOpcode().contains( "LD" )  )
            return true;
        else
            return false;
    }

    public boolean issue2Firable() {
        if(instructionBuffer.hasToken() && instructionBuffer.getInstruction().getOpcode().contains( "LD" )  )
            return true;
        else
            return false;
    }

    public boolean readFirable() {
        if(registerFile.registers != null)
            return true;
        else
            return false;
    }

    public boolean addrFirable() {
        if( loadInstructionBuffer.hasToken() )
            return true;
        else
            return false;
    }

    public boolean aluFirable() {
        if( arithmeticInstructionBuffer.hasToken() )
            return true;
        else
            return false;
    }

    public boolean loadFirable() {
        if( addressBuffer.hasToken() )
            return true;
        else
            return false;
    }

    public boolean writeFirable() {
        if( resultBuffer.hasToken() )
            return true;
        else
            return false;
    }

}

// ======= Important Places =======

interface TokenHolder {
    public boolean hasToken();
}

class RegisterFile {

    int[] registers = null;
    String fileName = null;

    public RegisterFile(String registerFile) {
        fileName = registerFile;
        registers = new int[8];
    }

    public void initRegisters() {
        Scanner in = null;
        try {
            in = new Scanner( new FileReader( "../resources/registers.txt"  ) );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String s;
        while ( in.hasNext() ) {
            s = in.next();
            updateRegister( getRegisterIndex( getRegisterName( s ) ), getRegisterValue( s ) );
        }
    }

    public String getState() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (int value : registers) {
            sb.append("<");
            sb.append("R" + i++ + "," + value );
            sb.append(">");
            if(i < registers.length) sb.append(",");
        }
        return sb.toString();
    }

    private String getRegisterName(String s) {
        return s.substring(s.indexOf('<') + 1, s.indexOf(','));
    }

    private int getRegisterValue(String s) {
        return Integer.parseInt( s.substring(s.indexOf(',') + 1, s.indexOf('>')) );
    }

    private int getRegisterIndex(String registerName) {
        return Integer.parseInt( registerName.substring(1) );
    }

    public void updateRegister(int index, int val) {
        registers[index] = val;
    }

    public String getDataString(int i) {
        StringBuilder sb = new StringBuilder();
        sb.append("<R");
        sb.append( Integer.toString(i) );
        sb.append(",");
        sb.append( Integer.toString( registers[i] ) );
        sb.append(">");
        return sb.toString();
    }

    public void print() {
        int i = 0;
        for (int value : registers) {
            System.out.println("R" + i++ + ": " + value );
        }
    }
}

class DataMemory {

    public int[] memory;
    String fileName = null;

    public DataMemory(String datamemoryFile) {
        fileName = datamemoryFile;
        memory = new int[8];
    }

    public void initMemory() {
        Scanner in = null;
        try {
            in = new Scanner( new FileReader( "../resources/datamemory.txt"  ) );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String s;
        while ( in.hasNext() ) {
            s = in.next();
           updateMemory( getDataAddress( s ), getDataValue( s ) );
        }
    }

    public String getState() {
        int i = 0;
         StringBuilder sb = new StringBuilder();
         for(int value : memory) {
            sb.append("<");
            sb.append( Integer.toString(i) );
            sb.append(",");
            sb.append( Integer.toString( memory[i] ) );
            sb.append(">");
            if(++i < memory.length) sb.append(",");
         }
         return sb.toString();
    }

    private int getDataAddress(String s) {
        return Integer.parseInt( s.substring(s.indexOf('<') + 1, s.indexOf(',')) );
    }

    private int getDataValue(String s) {
        return Integer.parseInt( s.substring(s.indexOf(',') + 1, s.indexOf('>')) );
    }

    private void updateMemory(int index, int val) {
        memory[index] = val;
    }

    public String getDataString(int i) {
        StringBuilder sb = new StringBuilder();
            sb.append("<");
            sb.append( Integer.toString(i) );
            sb.append(",");
            sb.append( Integer.toString( memory[i] ) );
            sb.append(">");
        return sb.toString();
    }

    public void print() {
        int i = 0;
        for (int value : memory) {
            System.out.println("M" + i++ + ": " + value );
        }
    }

}

class InstructionMemory implements TokenHolder {

    String fileName = null;
    ArrayList<InstructionToken> instructions = null;

    public InstructionMemory(String instructionFile) {
        fileName = instructionFile;
        instructions = new ArrayList<>();
    }

    public void initInstructions() {

        Scanner in = null;
        try {
            in = new Scanner( new FileReader( "../resources/instructions.txt"  ) );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        String instructionString;
        while ( in.hasNext() ) {
            instructionString = in.next();
            instructions.add( extractInstructionToken( instructionString ) );
        }

    }

    public InstructionToken getTopInstruction() {
        if(instructions.size() == 0) {
            return null;
        }
        return instructions.remove(0);
    }

    public String getState() {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for(int m = 0; m < instructions.size(); m ++) {
            String s = instructions.get(m).getDataString();
            sb.append(s);
            if( m < instructions.size() - 1 ) sb.append(",");
        }
        return sb.toString();
    }

    private InstructionToken extractInstructionToken( String instruction ) {
        int firstComma = instruction.indexOf(',');
        int secondComma = instruction.indexOf(',', firstComma + 1);
        int thirdComma = instruction.indexOf(',', secondComma + 1);

        String opcode = instruction.substring( instruction.indexOf('<') + 1, instruction.indexOf(',') );
        String dest = instruction.substring( firstComma + 1, secondComma );
        String sourceOp1 = instruction.substring( secondComma + 1, thirdComma );
        String sourceOp2 = instruction.substring( thirdComma + 1, instruction.indexOf('>') );
        InstructionToken token = new InstructionToken(opcode, dest, sourceOp1, sourceOp2);

        return token;
    }

    public boolean hasToken() {
        return instructions.isEmpty();
    }

    public void print() {
        for(Object token : instructions.toArray()) {
            ( (InstructionToken)token ).print();
            System.out.println();
        }
    }

}

class InstructionBuffer implements TokenHolder {

    InstructionToken instruction = null;

    public InstructionBuffer() {
    }

    public boolean hasToken() {
        return (instruction != null);
    }

    public String getState() {
        StringBuilder sb = new StringBuilder();
        sb.append("");
        if(instruction != null)
            sb.append( instruction.getDataString() );

        return sb.toString();
    }

    public InstructionToken getInstruction() {
        return  instruction;
    }

    public void receiveToken( InstructionToken instruction) {
        this.instruction = instruction;
    }
    public void consumeToken() {
        instruction = null;
    }
}

class LoadInstructionBuffer implements TokenHolder {

    InstructionToken loadInstruction = null;

    public LoadInstructionBuffer() {
    }

    public boolean hasToken() {
        return loadInstruction != null;
    }

    public String getState() {
        StringBuilder sb = new StringBuilder();
        sb.append("");
        if(loadInstruction != null)
            sb.append( loadInstruction.getDataString() );

        return sb.toString();
    }

    public void receiveToken( InstructionToken instruction) {
        this.loadInstruction = instruction;
    }
    public void consumeToken() {
        loadInstruction = null;
    }
}

class ArithmeticInstructionBuffer implements TokenHolder {

    InstructionToken arithmeticInstruction = null;

    public ArithmeticInstructionBuffer() {
    }

    public boolean hasToken() {
        return arithmeticInstruction != null;
    }
    public String getState() {
        StringBuilder sb = new StringBuilder();
        sb.append("");
        if(arithmeticInstruction != null)
            sb.append( arithmeticInstruction.getDataString() );

        return sb.toString();
    }

    public void receiveToken( InstructionToken instruction) {
        this.arithmeticInstruction = instruction;
    }
    public void consumeToken() {
        arithmeticInstruction = null;
    }
}

class AddressBuffer implements TokenHolder {

    DataMemoryToken loadInstruction = null;

    public AddressBuffer() {
    }

    public boolean hasToken() {
        return loadInstruction != null;
    }
    public String getState() {
        StringBuilder sb = new StringBuilder();
        sb.append("");
        if(loadInstruction != null)
            sb.append( loadInstruction.getDataString() );

        return sb.toString();
    }

    public void receiveToken( DataMemoryToken instruction) {
        this.loadInstruction = instruction;
    }
    public void consumeToken() {
        loadInstruction = null;
    }
}

class ResultBuffer implements TokenHolder {

    ArrayList<RegisterToken> results = null;

    public ResultBuffer() {
        results = new ArrayList<>();
    }

    public boolean hasToken() {
        return results.size() > 0;
    }
    public String getState() {

        StringBuilder sb = new StringBuilder();
        sb.append("");
        for(int i = 0; i < results.size(); i++) {
         sb.append( results.get(i).getDataString() );
         if(i < results.size() - 1) sb.append(",");
        }
        return sb.toString();
    }
    public void receiveToken( RegisterToken res) {
        results.add( res );
    }
}

// ======= Tokens =======
abstract class Token implements Comparable{
    private String type;

    public abstract void print();
    public abstract String getDataString();

    public void setType(String type) {
        this.type = type;
    }
    public String getType() {
        return this.type;
    }
}

class InstructionToken extends Token {
    private String opcode;
    private String destReg;
    private String[] sourceOps;
    int id;
    static int instructionCount = 0;

    public InstructionToken(String o, String d, String s1, String s2) {
        setType( "INSTRUCTION" );
        opcode = o;
        destReg = d;
        sourceOps = new String[]{s1, s2};
        id = ++instructionCount;
    }
    public InstructionToken(String o, String d, int s1, int s2, int id) {
        setType( "INSTRUCTION" );
        opcode = o;
        destReg = d;
        sourceOps = new String[]{Integer.toString(s1), Integer.toString(s2)};
        this.id = id;
    }
    public String getOpcode() {
        return  opcode;
    }
    public String getDestReg() {
        return destReg;
    }
    public String getSoureOp1() {
        return sourceOps[0];
    }
    public String getSoureOp2() {
        return sourceOps[1];
    }
    public void print() {
        System.out.print("<" + opcode + " " + destReg + " " + getSoureOp1() + " " + getSoureOp2() + ">");
    }
    public String getDataString() {
        return "<" + opcode + "," + destReg + "," + getSoureOp1() + "," + getSoureOp2() + ">";
    }
    @Override
    public int compareTo(Object o) {
        return 0;
    }
}

class RegisterToken extends Token {
    private String name;
    private int value;
    int id;

    public RegisterToken(String name, int value) {
        setType( "REGISTER" );
        this.name = name;
        this.value = value;
        id = 0;
    }
    public RegisterToken(String name, int value,  int id) {
        setType( "REGISTER" );
        this.name = name;
        this.value = value;
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public int getValue() {
        return value;
    }
    public void print() {
        System.out.print("<" + name + " " + value + ">");
    }
    public String getDataString() {
        return "<" + name + "," + value + ">";
    }
    @Override
    public int compareTo(Object o) {
        return 0;
    }
}

class DataMemoryToken extends Token {
    private String name;
    private int address;
    int id;

    public DataMemoryToken(String name, int address) {
        setType( "DATA" );
        this.name = name;
        this.address = address;
        id = 0;
    }
    public DataMemoryToken(String name, int address, int id) {
        setType( "DATA" );
        this.name = name;
        this.address = address;
        this.id = id;
    }
    public String getName() {
        return name;
    }
    public int getAddress() {
        return address;
    }
    public void print() {
        System.out.print("<" + name + " " + address + ">");
    }
    public String getDataString() {
        return "<" + name + "," + address + ">";
    }
    @Override
    public int compareTo(Object o) {
        return 0;
    }
}
