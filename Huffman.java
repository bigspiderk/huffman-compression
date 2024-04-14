import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

public class Huffman {
    public static void main(String[] args) {
        // String s = "AAAAAABBBCCCCCCCD";
        String s = null;
        try {
            FileInputStream in = new FileInputStream("test.webp");
            s = new String(in.readAllBytes());
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        CompressedText ct = compress(s);
        
        if (decompress(ct).equals(s)) {
            System.out.println("Decompression Worked!");
            System.out.printf("Size of compressed text is %.2f%s of the original\n", ((double) ct.getLength()/s.length()) * 100, "%");
        } else {
            System.out.println("Decompression Failed");
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

    public static CompressedText compress(String s) {
        HashMap<Character, Integer> frequencies = new HashMap<Character, Integer>();
        for (char c : s.toCharArray()) {
            if (frequencies.containsKey(c)) {
                frequencies.put(c, frequencies.get(c) + 1);
            } else {
                frequencies.put(c, 1);
            }
        }

        ArrayList<Node> nodes = new ArrayList<>();
        for (char i : frequencies.keySet()) {
            nodes.add(new Node(frequencies.get(i), (char) i));
        }

        sort(nodes);
        ArrayList<Node> leaves = new ArrayList<>(nodes);

        Node[] smallestPair;
        while (nodes.size() > 1) {
            smallestPair = getSmallestPair(nodes);
            Node parent = new Node(smallestPair[0].getValue() + smallestPair[1].getValue());
            nodes.set(nodes.indexOf(smallestPair[0]), parent);
            for (int i = 0; i < smallestPair.length; i++) {
                smallestPair[i].setParent(parent, i);
                nodes.remove(smallestPair[i]);
            }
        }

        HashMap<Character, String> table = new HashMap<Character, String>();
        String currentPath;
        for (Node leaf : leaves) {
            if ((currentPath = leaf.getPath()) != null) {
                table.put(leaf.getChar(), currentPath);
            }
        }

        ByteArrayOutputStream compressedBytes = new ByteArrayOutputStream();
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (table.containsKey(c)) {
                sb.append(table.get(c));
            }
            if (sb.length() >= 8) {
                compressedBytes.write(binToInt(sb.substring(0, 8)));
                sb.delete(0, 8);
            }
        }

        int ignoredBits = 8-sb.length();
        for (int i = 0; i < ignoredBits; i++) {
            sb.append("0");
        }

        compressedBytes.write((byte) binToInt(sb.toString()));

        return new CompressedText(table, compressedBytes.toByteArray(), ignoredBits);
    }

    public static String decompress(CompressedText ct) {
        StringBuilder bitBuffer = new StringBuilder();
        StringBuilder decompressedText = new StringBuilder();
        byte[] compressedText = ct.getBytes();
        
        for (int i = 0; i < compressedText.length; i++) {
            int endIndex = 8-(ct.getBitsIgnored() * (i == compressedText.length-1 ? 1 : 0));
            bitBuffer.append(intToBin((int) compressedText[i] & 0xff).substring(0, endIndex));
            for (int j = 0; j < bitBuffer.length()+1; j++) {
                if (ct.getTable().containsKey(bitBuffer.substring(0, j))) {
                    decompressedText.append(ct.getTable().get(bitBuffer.substring(0, j)));
                    bitBuffer.delete(0, j);
                    j = 0;
                }
            }
        }

        return decompressedText.toString();
    }

    public static int binToInt(String bin) {
        int total = 0;
        char[] bits = bin.toCharArray();
        for (int i = bits.length-1; i >= 0; i--) {
            total += Math.pow(2, i) * ((int)bits[bin.length()-i-1] % 48);
        }
        return total;
    }

    public static String intToBin(int num) {
        int remainder = num;
        StringBuilder bin = new StringBuilder();
        for (int i = 7; i >= 0; i--) {
            if (Math.pow(2, i) <= remainder) {
                remainder -= Math.pow(2, i);
                bin.append(1);
            } else {
                bin.append(0);
            }
        }

        return bin.toString();
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

class CompressedText {
    private HashMap<String, Character> table;
    private byte[] text;
    private int bitsIgnored;

    public CompressedText(HashMap<Character, String> table, byte[] text, int bitsIgnored) {
        this.text = text;
        this.bitsIgnored = bitsIgnored;
        this.table = new HashMap<String, Character>();
        for (char c : table.keySet()) {
            this.table.put(table.get(c), c);
        }
    }

    public int getBitsIgnored() {
        return bitsIgnored;
    }

    public byte[] getBytes() {
        return text;
    }

    public HashMap<String, Character> getTable() {
        return table;
    }

    public int getLength() {
        return text.length;
    }

    public String toString() {
        return new String(text);
    }
}