package Utilities;

/**
 * Simple ASCII table utility to replace the missing bethecoder ASCII table library
 */
public class SimpleASCIITable {
    private static final SimpleASCIITable instance = new SimpleASCIITable();
    
    public static SimpleASCIITable getInstance() {
        return instance;
    }
    
    /**
     * Prints a simple ASCII table with headers and data
     * @param headers Column headers
     * @param data Data rows
     */
    public void printTable(String[] headers, String[][] data) {
        if (headers == null || data == null) {
            return;
        }
        
        // Calculate column widths
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = headers[i].length();
        }
        
        for (String[] row : data) {
            for (int i = 0; i < Math.min(row.length, widths.length); i++) {
                if (row[i] != null) {
                    widths[i] = Math.max(widths[i], row[i].length());
                }
            }
        }
        
        // Print separator
        printSeparator(widths);
        
        // Print headers
        printRow(headers, widths);
        printSeparator(widths);
        
        // Print data
        for (String[] row : data) {
            printRow(row, widths);
        }
        
        printSeparator(widths);
    }
    
    private void printSeparator(int[] widths) {
        System.out.print("+");
        for (int width : widths) {
            for (int i = 0; i < width + 2; i++) {
                System.out.print("-");
            }
            System.out.print("+");
        }
        System.out.println();
    }
    
    private void printRow(String[] row, int[] widths) {
        System.out.print("|");
        for (int i = 0; i < widths.length; i++) {
            String cell = i < row.length && row[i] != null ? row[i] : "";
            System.out.printf(" %-" + widths[i] + "s |", cell);
        }
        System.out.println();
    }
}