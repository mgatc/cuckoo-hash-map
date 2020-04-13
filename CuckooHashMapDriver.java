package cuckoohashmapdriver;

import java.util.*;

public class CuckooHashMapDriver {
    public static void main(String[] args) {
        classDriver();
        
        

    }
    public static void classDriver() {
        CuckooHashMap<String, String> test1 = new CuckooHashMap<>(16, 10009);

        System.out.println("Number of entries in test1: \t" + test1.size());
        System.out.println("Load factor of test1: \t\t" + test1.loadFactor());

        System.out.println("Adding some entries with duplicate keys");
        String matching = "Justin";
        
        test1.put("Nicki", "Minaj");
        test1.put("Amy", "Winehouse");
        test1.put(matching, "ONE");
        test1.put("Jay", "Z"); 
                                // rehash here
        test1.put("Paul", "McCartney");
        test1.put("Freddie", "Mercury"); 
                               // rehash here
        test1.put("John", "Legend");
        test1.put("Elvis", "Presley");
        test1.put("50", "Cent");
                                
        test1.put("John", "Harrison");
        test1.put("Elton", "John");
                                // rehash here
        test1.put("John", "Lennon");
        test1.put(matching, "TWO");
        
        /*
*/
        System.out.println("Number of entries in test1: \t" + test1.size());
        System.out.println("Load factor of test1: \t\t" + test1.loadFactor());    

        ArrayList<Entry<String, String>> entries = (ArrayList<Entry<String, String>>) test1.entrySet();
        ArrayList<String> keys = (ArrayList<String>) test1.keySet();
        ArrayList<String> values = (ArrayList<String>) test1.valueSet();

        for (String v: values) {
            System.out.print(v + ", ");
        }
        System.out.println();
        for (String k: keys) {
            System.out.print(k + ", ");
        }
        System.out.println();

        for (Entry<String, String> e: entries) {
            System.out.println(e.getKey() + " => " + test1.get(e.getKey()));
            System.out.println("Removing ... " + test1.remove(e.getKey()));
        }

        System.out.println("Number of entries in test1: \t" + test1.size());
        System.out.println("Load factor of test1: \t\t" + test1.loadFactor());
      
    }
    
}
