public class Main {

    public static void main(String[] args) {
        System.out.println("hi,i'm old ma.welcome!");

//        if(args.length != 4) {
//            System.out.println("usage:java -jar RyApkTool.jar apkName keyFile keyName  keyPasswd");
//            System.out.println("Example: java -jar RyApkTool.jar test.apk myAndroidkey rydiy 123456");
//            System.out.println("invalid numbers of parameter,needs 4.");
//            return;
//        }

//        String apk = args[0];
//        String keyFile = args[1];
//        String keyName = args[2];
//        String keyPasswd = args[3];
//        System.out.println("apk="+apk+";keyFile="+keyFile+";keyName="+keyName+";keyPasswd="+keyPasswd);

        String apk = "test.apk";
        String keyFile = "F:/workspace/keystore/paysdk.jks";
        String keyName = "paysdk";
        String keyPasswd = "123456";

        SplitApk sp = new SplitApk(apk, keyFile, keyName, keyPasswd);
        sp.mySplit();
    }
}
