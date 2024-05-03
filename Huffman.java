import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Huffman {
    public static void main(String[] args) {
        if (args.length > 0) {
            String file = null;
            if (args.length >= 2) {
                file = args[1];
            } else {
                System.err.println("Please Specify File");
                System.exit(1);
            }

            if (args[0].equalsIgnoreCase("compress")) {
                try {
                    compress(file);
                } catch (IOException e) {
                    System.err.println("Compression failed");
                }
            }

            if (args[0].equalsIgnoreCase("decompress")) {
                try {
                    decompress(file);
                } catch (IOException e) {
                    System.err.println("Decompression failed");
                }
            }
        }
    }

    public static void sort(ArrayList<Node> nodes) {
        for (int i = 0; i < nodes.size()-1; i++) {
            for (int j = i+1; j < nodes.size(); j++) {
                if (nodes.get(i).getValue() > nodes.get(j).getValue() || nodes.get(i).getValue() == nodes.get(j).getValue() && nodes.get(i).getChar() > nodes.get(j).getChar()) {
                    Node temp = nodes.get(j);
                    nodes.set(j, nodes.get(i));
                    nodes.set(i, temp);
                }
            }
        }
    }

    public static Node[] getSmallestPair(ArrayList<Node> nodes) {
        int n = 0;
        int sum = 0;
        int currentSum;
        for (int i = 0; i < nodes.size()-1; i++) {
            currentSum = nodes.get(i).getValue() + nodes.get(i+1).getValue();
            if (i == 0 || currentSum < sum) {
                n = i;
                sum = currentSum;
            }
        }
        return new Node[]{nodes.get(n), nodes.get(n+1)};
    }

    public static void writeTable(FileOutputStream out, HashMap<Integer, String> table) {
        try {
            out.write(table.size()-1);
            StringBuffer bitBuffer = new StringBuffer();
            for (int c : table.keySet()) {
                bitBuffer.append(intToBin(c));
                bitBuffer.append(intToBin(table.get(c).length()));
                bitBuffer.append(table.get(c));
                while (bitBuffer.length() >= 8) {
                    out.write(binToInt(bitBuffer.substring(0,8)));
                    bitBuffer.delete(0,8);
                }
            }
            if (bitBuffer.length() > 0) {
                while (bitBuffer.length() < 8) {
                    bitBuffer.append(0);
                }
                out.write(binToInt(bitBuffer.toString()));
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static HashMap<String, Integer> parseTable(FileInputStream in) {
        HashMap<String, Integer> table = new HashMap<String, Integer>();
        try {
            int tableSize = in.read() + 1;
            StringBuffer bitBuffer = new StringBuffer();
            for (int i = 0; i < tableSize; i++) {
                if (bitBuffer.length() < 8) {
                    bitBuffer.append(intToBin(in.read()));
                }
                char c = (char) binToInt(bitBuffer.substring(0,8));
                bitBuffer.delete(0, 8);
                if (bitBuffer.length() < 8) {
                    bitBuffer.append(intToBin(in.read()));
                }
                int codeSize = binToInt(bitBuffer.substring(0,8));
                bitBuffer.delete(0, 8);
                while (bitBuffer.length() < codeSize) {
                    bitBuffer.append(intToBin(in.read()));
                }
                table.put(bitBuffer.substring(0, codeSize), (int) c);
                bitBuffer.delete(0, codeSize);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return table;
    }


    public static void compress(String file) throws IOException{
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            System.err.println("Cannot Find File");
            System.exit(1);
        }

        HashMap<Integer, Integer> frequencies = new HashMap<Integer, Integer>();
        int c;
        while ((c = in.read()) != -1) {
            if (frequencies.containsKey(c)) {
                frequencies.put(c, frequencies.get(c) + 1);
            } else {
                frequencies.put(c, 1);
            }
        }

        in.close();

        ArrayList<Node> nodes = new ArrayList<>();
        for (int i : frequencies.keySet()) {
            nodes.add(new Node(frequencies.get(i), (char) i));
        }

        sort(nodes);
        ArrayList<Node> leaves = new ArrayList<>(nodes);

        Node[] smallestPair;
        while (nodes.size() > 1) {
            smallestPair = getSmallestPair(nodes);
            Node parent = new Node(smallestPair[0].getValue() + smallestPair[1].getValue());
            nodes.add(nodes.indexOf(smallestPair[0]), parent);
            for (int i = 0; i < smallestPair.length; i++) {
                smallestPair[i].setParent(parent, i);
                nodes.remove(smallestPair[i]);
            }
        }

        HashMap<Integer, String> table = new HashMap<Integer, String>();
        String currentPath;
        for (Node leaf : leaves) {
            if ((currentPath = leaf.getPath()) != null) {
                table.put((int) leaf.getChar(), currentPath);
            }
        }


        FileOutputStream out = new FileOutputStream(file + ".comp");
        writeTable(out, table);
        StringBuilder sb = new StringBuilder();

        try {
            in = new FileInputStream(file);
        } catch (FileNotFoundException e) {
            System.err.println("Cannot Find File");
            System.exit(1);
        }

        int b;
        while ((b = in.read()) != -1) {
            if (table.containsKey(b)) {
                sb.append(table.get(b));
            }

            while (sb.length() >= 8) {
                out.write(binToInt(sb.substring(0, 8)));
                sb.delete(0, 8);
            }
        }

        if (sb.length() > 0) {
            while (sb.length() < 8) {
                sb.append("0");
            }
        }

        out.write((byte) binToInt(sb.toString()));
        in.close();
        out.close();
    }

    public static void decompress(String file) throws IOException {
        StringBuilder bitBuffer = new StringBuilder();
        String outPath = String.format("decomp-%s", file.substring(0, file.lastIndexOf(".comp")));
        FileInputStream in = null;
        FileOutputStream out = null;
        try {
            in = new FileInputStream(file);
            out = new FileOutputStream(outPath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        HashMap<String, Integer> table = parseTable(in);

        bitBuffer.append(intToBin(in.read()));

        while (bitBuffer.length() > 0) {
            StringBuilder currentCode = new StringBuilder();
            while (!table.containsKey(currentCode.toString())) {
                try{
                    if (bitBuffer.length() < 1) {
                        bitBuffer.append(intToBin(in.read()));
                    }
                    currentCode.append(bitBuffer.charAt(0));
                    bitBuffer.deleteCharAt(0);
                } catch (StringIndexOutOfBoundsException e2) {
                    currentCode = null;
                    break;
                }
            }

            if (currentCode != null) {
                out.write(table.get(currentCode.toString()));
                if (bitBuffer.length() < 1) {
                    bitBuffer.append(intToBin(in.read()));
                }
            }
            
            if (bitBuffer.length() < 8) {
                bitBuffer.append(intToBin(in.read()));
            }
        }
        in.close();
        out.close();
    }

    public static int binToInt(String bin) {
        int total = 0;
        for (int i = 0; i < bin.length(); i++) {
            total += Math.pow(2, i) * (bin.charAt(i) % '0');
        }
        return total;
    }

    public static String intToBin(int num) {
        StringBuilder binary = new StringBuilder();

        if (num == 0) {
            binary.append(0);
        }
    
        while (num > 0) {
            int remainder = num % 2;
            binary.append(remainder);
            num /= 2;
        }
    
        while (binary.length() % 8 != 0) {
            binary.append('0');
        }
    
        return binary.toString();
    }

}

class Node {
    private int value;
    private Node parent;
    private int position;
    private char c;

    public Node(int value) {
        this.value = value;
    }

    public Node (int value, char c) {
        this.c = c;
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public char getChar() {
        return c;
    }

    public void setParent(Node parent, int position) {
        this.parent = parent;
        this.position = position;
    }

    public String toString() {
        return String.format("%d %c", value, c);
    }

    public String getPath() {
        return getPath(new StringBuilder());
    }

    private String getPath(StringBuilder path) {
        if (parent == null) {
            return path.toString();
        }

        path.insert(0, position);
        return parent.getPath(path);
    }

}