import java.util.*; import java.io.*;
public class P { public static void main(String[] a) throws Exception { Properties p = new Properties(); try (var in = new FileInputStream(a[0])) { p.load(in);} System.out.println(p.getProperty("dev.storePassword")); } }
