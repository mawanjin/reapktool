
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

public class SplitApk {
    HashMap<String, String> qudao = new HashMap<String, String>();
    String curPath;
    String apkName;
    String keyFile;
    String keyName;
    String keyPasswd;

    public SplitApk(String apkName, String keyFile, String keyName,
                    String keyPasswd) {
        this.curPath = new File("").getAbsolutePath();
        this.apkName = apkName;
        this.keyFile = keyFile;
        this.keyName = keyName;
        this.keyPasswd = keyPasswd;
    }

    // 读取map文件
    private void setMapFile() {
        System.out.println("init map.txt");
        File f = new File("map.txt");
        if (f.exists() && f.isFile()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(f));
                String line = null;
                while ((line = br.readLine()) != null) {
                    String[] array = line.split("\t");
                    if (array.length == 2) {
                        qudao.put(array[0].trim(), array[1].trim());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        System.out.println("init completed.size = "+qudao.size());
    }

    public void runCmd(String cmd) {
        Runtime rt = Runtime.getRuntime();
        try {
            Process p = rt.exec(cmd);
            // p.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    p.getInputStream()));
            String msg = null;
            while ((msg = br.readLine()) != null) {
                System.out.println(msg);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // apktool解压apk，替换渠道值
    private void modifyQudao() throws Exception {

        // 解压
        String cmdUnpack = "cmd.exe /C java -jar apktool.jar  d -s " + apkName;
        runCmd(cmdUnpack);

        // 备份AndroidManifest.xml
        String dir = apkName.split(".apk")[0];
        File packDir = new File(dir);

        String f_mani = packDir.getAbsolutePath() + "\\AndroidManifest.xml";
        String f_mani_bak = curPath + "\\AndroidManifest.xml";
        File manifest = new File(f_mani);
        File manifest_bak = new File(f_mani_bak);

        System.out.println("bak AndroidManifest.xml: " + f_mani + "->"
                + f_mani_bak);

        manifest.renameTo(manifest_bak);

        for (int i = 0; i < 10; i++) {
            if (manifest_bak.exists()) {
                break;
            }
            Thread.sleep(1000);
        }

        File f = new File("apk");
        if (!f.exists()) {
            f.mkdir();
        }

		/*
		 * 遍历map，复制manifese进来，修改后打包，签名，存储在对应文件夹中
		 */
        for (Map.Entry<String, String> entry : qudao.entrySet()) {
            String id = entry.getKey();
            System.out.println("generate package: " + id + ":");
            BufferedReader br = new BufferedReader(new FileReader(manifest_bak));
            String line = null;
            StringBuffer sb = new StringBuffer();
            boolean firstTag = false,lastTag=false;
            while ((line = br.readLine()) != null) {
                if(line.contains("<application")&&!firstTag){
                    firstTag = true;
                }
                if(line.contains(">")&&firstTag&&!lastTag){
                    lastTag = true;
                    //写入
                    line = line+"\n"+"<meta-data android:value=\""+id+"\" android:name=\"myMsg\"></meta-data>";
                }
//                 if (line.contains("\"10000\"")) {
//                    System.out.println(line);
//                    line = line.replaceAll("10000", id);
//                    System.out.println(line);
//                }
                sb.append(line + "\n");
            }

            br.close();

            FileWriter fw = new FileWriter(f_mani);
            fw.write(sb.toString());
            fw.close();

            // 打包
            String unsignApk = id + "_" + dir + "_un.apk";
            String cmdPack = String.format(
                    "cmd.exe /C java -jar apktool.jar b %s %s", dir, unsignApk);
            System.out.println("start package.run["+cmdPack+"] ");
            runCmd(cmdPack);

            // 签名
            String signApk = "./apk/" + id + "_" + dir + ".apk";
            String cmdKey = String.format("cmd.exe /C jarsigner -verbose -keystore %s "+ " -storepass %s -signedjar %s  %s %s", keyFile,keyPasswd, signApk, unsignApk, keyName);
            System.out.println("start sign apk["+cmdKey+"]");
            runCmd(cmdKey);

            System.out.println("delete unsign apk["+unsignApk+"]");
            // 删除未签名的包
            File unApk = new File(unsignApk);
            unApk.delete();
        }

        System.out.println("OK");
    }

    // 读取当前文件夹中的文件
    private void moveFile() {
        File f = new File("apk");

        if (f.exists()) {
            File[] fileList = f.listFiles();
            for (File file : fileList) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    String id = fileName.split("_")[0];
                    String name = qudao.get(id);
                    File dir = new File(name);
                    if (!dir.exists()) {
                        dir.mkdir();
                    }
                    file.renameTo(new File(dir.getAbsoluteFile() + "/"
                            + fileName));
                }
            }
        }
    }

    public void mySplit() {
        setMapFile();
        try {
            modifyQudao();
        } catch (Exception e) {
            e.printStackTrace();
        }
        moveFile();
    }
}
