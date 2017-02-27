import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

public class ImagePanel extends JPanel implements MouseListener {
    BufferedImage img = null;
    Graphics2D g2d = null;
    MipsPetriNetSimulator MIPSsim = null;
    public ImagePanel() {
        img = new BufferedImage(1200, 620, BufferedImage.TYPE_INT_ARGB);
        g2d = (Graphics2D) img.createGraphics();
        drawNet();
    }

    public void drawNet() {

        if(MIPSsim == null)
            return;

        drawRects();

        drawLines();

        drawCircles();

        drawTexts();

        drawTokens();

        repaint();
    }

    public void drawTokens() {
        drawINM();
        drawINB();
        drawADB();
        drawAIB();
        drawDAM();
        drawREB();
        drawRGF();
        drawLIB();
    }

    public void drawLine(int x, int y, int xf, int yf, boolean firing) {
        if(firing && MIPSsim.inProgress)
            g2d.setColor( Color.red );
        else
            g2d.setColor( Color.black );

        g2d.drawLine(x, y, xf, yf);
    }

    public void drawINM() {

        g2d.setFont(new Font("default", Font.BOLD, 10));
        g2d.setColor( new Color(0, 0, 0, 255)  );

        if(MIPSsim.instructionMemory != null && MIPSsim.instructionMemory.instructions.size() < 1) {
            return;
        }

        ArrayList<InstructionToken> instructions = MIPSsim.instructionMemory.instructions;

        int offset = 15;

        for(int m = 0; m < instructions.size(); m ++) {
            String s = instructions.get(m).getDataString();
            g2d.drawString(s, 53, 250 + offset * m + 28);
        }

    }

    public void drawINB() {

        g2d.setFont(new Font("default", Font.BOLD, 10));

        if(MIPSsim.instructionBuffer.instruction == null) {
            return;
        }

        InstructionToken instruction = MIPSsim.instructionBuffer.instruction;
        String s = instruction.getDataString();
        g2d.drawString(s, 242, 270 + 20);
    }

    public void drawLIB() {

        g2d.setFont(new Font("default", Font.BOLD, 10));

        if(MIPSsim.loadInstructionBuffer.loadInstruction == null) {
            return;
        }

        InstructionToken instruction = MIPSsim.loadInstructionBuffer.loadInstruction;
        String s = instruction.getDataString();
        g2d.drawString(s, 431, 80 + 30 + 20);

    }

    public void drawADB() {

        g2d.setFont(new Font("default", Font.BOLD, 11));

        if(MIPSsim.addressBuffer.loadInstruction == null) {
            return;
        }

        DataMemoryToken instruction = MIPSsim.addressBuffer.loadInstruction;
        String s = instruction.getDataString();
        g2d.drawString(s, 611 + 15, 110 + 20);
    }

    public void drawDAM() {

        g2d.setFont(new Font("default", Font.BOLD, 10));

        if(MIPSsim.dataMemory.memory == null) {
            return;
        }

        int offset = 15;
        for(int m = 0; m < MIPSsim.dataMemory.memory.length; m ++) {
            String s = (MIPSsim.dataMemory.getDataString(m));
            if(m % 2 == 0) {
                g2d.drawString(s, 639 + 20, 13 + offset * (m/2) + 18);
            }
            else
                g2d.drawString(s, 682 + 20, 13 + offset * (m/2) + 18);
        }
    }

    public void drawREB() {

        if(MIPSsim.resultBuffer.results.size() < 1) {
            return;
        }

        ArrayList<RegisterToken> results = MIPSsim.resultBuffer.results;

        int offset = 15;

        for(int m = 0; m < results.size(); m ++) {
            String s = results.get(m).getDataString();
            g2d.drawString(s, 845 + 23, 260 + offset * m + 24);
        }
    }

    public void drawAIB() {


        g2d.setFont(new Font("default", Font.BOLD, 10));

        if(MIPSsim.arithmeticInstructionBuffer.arithmeticInstruction == null) {
            return;
        }

        InstructionToken instruction = MIPSsim.arithmeticInstructionBuffer.arithmeticInstruction;
        String s = instruction.getDataString();
        g2d.drawString(s, 519, 270 + 20);
    }

    public void drawRGF() {

        g2d.setFont(new Font("default", Font.BOLD, 10));

        if(MIPSsim.registerFile.registers == null) {
            return;
        }

        int offset = 15;
        for(int m = 0; m < MIPSsim.registerFile.registers.length; m ++) {
            String s = (MIPSsim.registerFile.getDataString(m));
            if(m % 2 == 0)
                g2d.drawString(s, 515 + 5, 400 + offset * (m/2) + 38);
            else
                g2d.drawString(s, 559 + 5, 400 + offset * (m/2) + 38);
        }
    }

    public void drawTriangle(int xf, int yf, int theta) {
        int size = 6;

        AffineTransform at = new AffineTransform();
        at.rotate(Math.toRadians(theta), xf - 3, yf - 3  );

        int[] x1 = new int[]{xf, xf + size, (int)(xf + 0.5 * size)};
        int[] y1 =  new int[]{yf, yf, (int)(yf - size*0.86)};

        int[] rx1 = new int[x1.length];
        int[] ry1 =  new int[y1.length];

        for(int i = 0; i < x1.length; i++) {
            Point p = new Point(x1[i], y1[i]);
            at.transform(p, p);
            rx1[i] = p.x;
            ry1[i] = p.y;
        }

        g2d.drawPolygon(rx1, ry1, 3);
    }

    public void drawTexts(){
        int width = 23;
        int height = 110;

        int charSpace = 15;
        g2d.setFont(new Font("default", Font.BOLD, 14));
        g2d.setColor( new Color(0, 0, 0, 255)  );
        g2d.drawString("D", 181, 245 + 20);
        g2d.drawString("E", 181, 245 + charSpace + 20);
        g2d.drawString("C", 181, 245 + charSpace*2 + 20);
        g2d.drawString("O", 181, 245 + charSpace*3 + 20);
        g2d.drawString("D", 181, 245 + charSpace*4 + 20);
        g2d.drawString("E", 181, 245 + charSpace*5 + 20);

        g2d.drawString("I", 368, 85 + 20);
        g2d.drawString("S", 368, 85 + charSpace + 20);
        g2d.drawString("S", 368, 85 + charSpace*2 + 20);
        g2d.drawString("U", 368, 85 + charSpace*3 + 20);
        g2d.drawString("E", 368, 85 + charSpace*4 + 20);
        g2d.drawString("2", 368, 85 + charSpace*5 + 20);

        g2d.drawString("A", 555, 85 + charSpace + 20);
        g2d.drawString("D", 555, 85 + charSpace*2 + 20);
        g2d.drawString("D", 555, 85 + charSpace*3 + 20);
        g2d.drawString("R", 555, 85 + charSpace*4 + 20);

        g2d.drawString("L", 766, 85 + 20);
        g2d.drawString("O", 766, 85 + charSpace + 20);
        g2d.drawString("A", 766, 85 + charSpace*2 + 20);
        g2d.drawString("D", 766, 85 + charSpace*3 + 20);

        g2d.drawString("W", 981, 245 + 20);
        g2d.drawString("R", 981, 245 + charSpace + 20);
        g2d.drawString("I", 981, 245 + charSpace*2 + 20);
        g2d.drawString("T", 981, 245 + charSpace*3 + 20);
        g2d.drawString("E", 981, 245 + charSpace*4 + 20);

        g2d.drawString("I", 443, 245 + 20);
        g2d.drawString("S", 443, 245 + charSpace + 20);
        g2d.drawString("S", 443, 245 + charSpace*2 + 20);
        g2d.drawString("U", 443, 245 + charSpace*3 + 20);
        g2d.drawString("E", 443, 245 + charSpace*4 + 20);
        g2d.drawString("1", 443, 245 + charSpace*5 + 20);

        g2d.drawString("A", 646, 245 + charSpace*2 + 20);
        g2d.drawString("L", 646, 245 + charSpace*3 + 20);
        g2d.drawString("U", 646, 245 + charSpace*4 + 20);

        g2d.drawString("R", 368, 405 + 20);
        g2d.drawString("E", 368, 405 + charSpace + 20);
        g2d.drawString("A", 368, 405 + charSpace*2 + 20);
        g2d.drawString("D", 368, 405 + charSpace*3 + 20);

        g2d.setFont(new Font("default", Font.BOLD, 14));
        g2d.setColor( new Color(10, 10, 10, 150)  );
        g2d.drawString("INM", 40, 250);
        g2d.drawString("INB", 237, 240 + 20);
        g2d.drawString("LIB", 424, 80 + 20);
        g2d.drawString("ADB", 611, 80 + 20);
        g2d.drawString("DAM", 634, 11);
        g2d.drawString("REB", 845, 240 + 20);
        g2d.drawString("AIB", 515, 240 + 20);
        g2d.drawString("RGF", 510 , 390 + 20);
    }

    public void  drawRects() {
        int width = 23;
        int height = 110;

        g2d.setColor( Color.black );
        g2d.drawRect(175, 225 + 20, width, height);          // DECODE
        g2d.drawRect(362, 65 + 20, width, height);           // ISSUE 2
        g2d.drawRect(549, 65 + 10 + 20, width, 85);           // ADDR
        g2d.drawRect(760, 65  + 20, width, 80);            // LOAD
        g2d.drawRect(975, 225 + 20, width, 85);           // WRITE
        g2d.drawRect(437, 225 + 20, width, height);      // ISSUE 1
        g2d.drawRect(640, 225 + 20 + 20, width, 80);      // ALU
        g2d.drawRect(362, 385 + 20, width, 75);           // READ

        g2d.setColor( new Color(220, 220, 220, 100) );
        g2d.fillRect(175, 225 + 20, width, height);          // DECODE
        g2d.fillRect(362, 65 + 20, width, height);           // ISSUE 2
        g2d.fillRect(549, 65 + 10 + 20, width, 85);           // ADDR
        g2d.fillRect(760, 65  + 20, width, 80);            // LOAD
        g2d.fillRect(975, 225 + 20, width, 85);           // WRITE
        g2d.fillRect(437, 225 + 20, width, height);      // ISSUE 1
        g2d.fillRect(640, 225 + 20 + 20, width, 80);      // ALU
        g2d.fillRect(362, 385 + 20, width, 75);           // READ
    }

    public void drawCircles() {
        int width = 23;
        int height = 110;

        int circlesize = 80;
        int charSpace = 15;

        g2d.setColor( new Color(0x0A00CAFF) );
        g2d.fillOval(40, 230 + 20, circlesize + 30, circlesize + 30);    // INM
        g2d.fillOval(237, 240 + 20, circlesize, circlesize);    // INB
        g2d.fillOval(424, 80 + 20, circlesize, circlesize);    // LIB
        g2d.fillOval(611, 80 + 20, circlesize, circlesize);    // ADB
        g2d.fillOval(650, 2, circlesize + 20, circlesize + 20);    // DAM
        g2d.fillOval(850, 240 + 20, circlesize, circlesize);    // REB
        g2d.fillOval(515, 240 + 20, circlesize, circlesize);    // AIB
        g2d.fillOval(510 , 390 + 20, circlesize + 20, circlesize + 20);    // RGF

        g2d.setColor( Color.black );
        g2d.drawOval(40, 230 + 20, circlesize + 30, circlesize + 30);    // INM
        g2d.drawOval(237, 240 + 20, circlesize, circlesize);    // INB
        g2d.drawOval(424, 80 + 20, circlesize, circlesize);    // LIB
        g2d.drawOval(611, 80 + 20, circlesize, circlesize);    // ADB
        g2d.drawOval(650, 2, circlesize + 20, circlesize + 20);    // DAM
        g2d.drawOval(850, 240 + 20, circlesize, circlesize);    // REB
        g2d.drawOval(515, 240 + 20, circlesize, circlesize);    // AIB
        g2d.drawOval(510 , 390 + 20, circlesize + 20, circlesize + 20);    // RGF
    }

    public void drawLines() {

        int width = 23;
        int height = 110;

        int circlesize = 80;
        int charSpace = 15;

        // INM -> DECODE
        drawLine(50 + circlesize + 11, 240+35 + 20, 175 - 6, 240+35 + 20, MIPSsim.decodeFirable());
        drawTriangle(175, 240+35 + 17, 90 );

        // DECODE -> INB
        drawLine(199, 240+35 + 20, 237 - 6, 240+35 + 20, MIPSsim.decodeFirable());
        drawTriangle(237, 240+35 + 17, 90);

        // INB -> ISSUE2
        drawLine(237 + (int)(circlesize/1.5), 240 + 20 + 20, 356, 90 + 30 + 17, MIPSsim.issue2Firable());
        drawTriangle(355, 90 + 30 + 14, 25);

        // INB -> ISSUE1
        drawLine(237 + circlesize + 1, 235 + circlesize/2 + 20, 430, 240 + 35 + 20, MIPSsim.issue1Firable());
        drawTriangle(437, 240 + 35 + 17, 90);

        // INB -> READ
        drawLine(234 + circlesize/2 + 17 , 240 + circlesize + 2 + 23, 362, 400 + 15 + 20, MIPSsim.decodeFirable());
        drawTriangle(234 + circlesize/2 + 13 , 240 + circlesize + 3 + 25, -35);

        // ISSUE2 -> LIB
        drawLine(362 + 24, 90 + 30 + 20, 424 - 7, 90 + 30 + 20, MIPSsim.issue2Firable());
        drawTriangle(423, 90 + 30 + 20 - 3 , 90);

        // LIB -> ADDR
        drawLine(424 + circlesize, 90 + 30 + 20, 541, 90 + 30 + 20, MIPSsim.addrFirable());
        drawTriangle(548, 90 + 30 + 17, 90);

        // ADDR -> ADB
        drawLine(549 + 24, 90 + 30 + 20, 611 - 8, 90 + 30 + 20, MIPSsim.addrFirable());
        drawTriangle(609, 90 + 30 + 20 - 3 , 90);

        // ADB -> LOAD
        drawLine(612 + circlesize, 90 + 30 + 20, 760 - 10, 90 + 30 + 20, MIPSsim.loadFirable());
        drawTriangle(757, 90 + 30 + 17 , 90);

        // DAM -> LOAD
        drawLine(642 + circlesize - 4, 10 + circlesize/2 + 10 + 20, 760-8, 90 + 14, MIPSsim.loadFirable());
        drawTriangle(763, 90 + 16, 135);

        // LOAD -> REB
        drawLine(760 + 24, 110 + 20, 850 + circlesize/2 - 7, 240 + 16, MIPSsim.loadFirable());
        drawTriangle(852 + circlesize/2, 240 + 15, 125);

        // REB -> WRITE
        drawLine(850 + circlesize + 1, 240 + circlesize/2 + 20, 969, 240 + circlesize/2 + 20, MIPSsim.writeFirable());
        drawTriangle(975, 240 + circlesize/2 + 17, 90);

        // WRITE -> RGF
        drawLine(975 + 24, 240 + circlesize/2 + 20, 975 + 24 + 40, 240 + circlesize/2 + 20, MIPSsim.writeFirable());
        drawLine(975 + 24 + 40, 240 + circlesize/2 + 20, 975 + 24 + 40, 240 + 30 + 60 + 20, MIPSsim.writeFirable());
        drawLine(975 + 24 + 40, 240 + 30 + 60 + 20, 515 + circlesize + 26 , 400 + circlesize/2 + 20, MIPSsim.writeFirable());
        drawTriangle(514 + circlesize + 27 , 400 + circlesize/2 + 30, -110);


        // RGF -> READ
        drawLine(515 , 400 + circlesize/2 + 20, 362 + 24 + 9, 400 + circlesize/2 + 20, MIPSsim.decodeFirable());
        drawTriangle(362 + 24 + 7, 400 + circlesize/2 + 20 + 8, -90);

        // ISSUE1 -> AIB
        drawLine(437 + 24, 240 + circlesize/2 + 20, 509, 240 + circlesize/2 + 20, MIPSsim.issue1Firable());
        drawTriangle(515, 240 + circlesize/2 + 17, 90);

        // AIB -> ALU
        drawLine(515 + circlesize, 240 + circlesize/2 + 20, 634, 240 + circlesize/2 + 20, MIPSsim.aluFirable());
        drawTriangle(640, 240 + circlesize/2 + 17 , 90);

        // ALU -> REB
        drawLine(640 + 24, 240 + circlesize/2 + 20, 844, 240 + circlesize/2 + 20, MIPSsim.aluFirable());
        drawTriangle(850, 240 + circlesize/2 + 17, 90);

    }

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(img, 0, 20, this); // see javadoc for more info on the parameters
    }

    public void giveModel( MipsPetriNetSimulator s ) {
        this.MIPSsim = s;
    }

    public void mouseClicked(MouseEvent e) {
        System.out.println(e.getX() + " , " + e.getY());

    }

    public void mousePressed(MouseEvent e) {
        System.out.println(e.getX() + " , " + e.getY());
    }

    public void mouseReleased(MouseEvent e) {

    }

    public void mouseEntered(MouseEvent e) {
        System.out.println(e.getX() + " , " + e.getY());
    }

    public void mouseExited(MouseEvent e) {

    }

    public void redraw() {
        g2d.setStroke( new BasicStroke(2.0f) );
        g2d.setColor( new Color(0xFFFFFFFF) );
        g2d.fillRect( 0, 0, img.getWidth(), img.getHeight() );
        drawNet();
        repaint();
    }
}