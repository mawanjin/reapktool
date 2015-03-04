import java.io.File;

public class Main {

    public static void main(String[] args) {
        System.out.println("hi,i'm old ma.welcome!");

        String apk = "test.apk";
        String keyFile = "/Users/lala/Documents/workspace/keystore/android/paysdk/paysdk.jks";
        String keyName = "paysdk";
        String keyPasswd = "123456";
        String destDir = "";
        String vNumber="";

        boolean distribute = true;

        if (args.length == 7) {
            apk = args[0];
            keyFile = args[1];
            keyName = args[2];
            keyPasswd = args[3];
            distribute = Boolean.parseBoolean(args[4]);
            destDir = args[5];
            vNumber = args[6];

        }

        if (!new File(apk).exists()) {
            System.out.println("no apk specified.");
            System.exit(0);
        }

        SplitApk sp = new SplitApk(apk, keyFile, keyName, keyPasswd, distribute, destDir,vNumber);
        sp.mySplit();
    }
}
