import java.security.*; import java.io.*;
public class K { public static void main(String[] a) throws Exception {
 KeyStore ks = KeyStore.getInstance("PKCS12"); ks.load(new FileInputStream(a[0]), a[1].toCharArray());
 try { System.out.println(ks.getKey("dev", a[2].toCharArray()) != null ? "key ok with "+a[2] : "null"); } catch (Exception e) { System.out.println("FAIL with "+a[2]+": "+e); }
}}
