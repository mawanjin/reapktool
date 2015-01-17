
import org.apache.commons.io.FileUtils;

import java.io.*;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class SplitApk {
    HashMap<String, String> qudao = new HashMap<String, String>();
    String curPath;
    String apkName;
    String keyFile;
    String keyName;
    String keyPasswd;
    boolean distribute;
    String folderDistribute = "distribute";
    File fDistribute;
    String destDir;
    String  prefix = "mg";
    String suffix ;

    public SplitApk(String apkName, String keyFile, String keyName,
                    String keyPasswd,boolean distribute,String destDir) {
        this.curPath = new File("").getAbsolutePath();
        this.apkName = apkName;
        this.keyFile = keyFile;
        this.keyName = keyName;
        this.keyPasswd = keyPasswd;
        this.distribute = distribute;
        this.destDir = destDir;
        Calendar calendar = Calendar.getInstance();
        String month = (calendar.get(Calendar.MONTH)+1)+"";

        if(Integer.parseInt(month)<10){
            month="0"+month;
        }
        suffix = (calendar.get(Calendar.YEAR)+"").substring(2)+month;
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
        System.out.println("init completed.size = " + qudao.size());
    }

    public void runCmd(String cmd) {
        Runtime rt = Runtime.getRuntime();
        try {
            Process p = rt.exec(cmd);

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


    public void execShell(String shell) {
        Runtime rt = Runtime.getRuntime();
        try {
            rt.exec(shell);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    boolean isRunning = false;

     public void runShell(String shell) {
        Process process;
        try {
            process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", shell}, null, null);
            InputStreamReader ir = new InputStreamReader(process.getInputStream());
            LineNumberReader input = new LineNumberReader(ir);
            String line;


            while ((line = input.readLine()) != null) {
                System.out.println(line);
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    // apktool解压apk，替换渠道值
    private void modifyQudao() throws Exception {
        String dir = apkName.split(".apk")[0];
        System.out.println("delete previous apk package.");
        FileUtils.forceDelete(new File(dir));

        System.out.println("run proc :: java -jar apktool.jar d " + apkName);



        runShell("java -jar apktool.jar d " + apkName);
        System.out.println("run proc complete,ready to backup AndroidManifest.xml");

        // 备份AndroidManifest.xml

        File packDir = new File(dir);

        String f_mani = packDir.getAbsolutePath() + "/AndroidManifest.xml";
        String f_mani_bak = curPath + "/AndroidManifest.xml";
        File manifest = new File(f_mani);
        File manifest_bak = new File(f_mani_bak);
        if(manifest_bak.exists())FileUtils.forceDelete(manifest_bak);

        System.out.println("bak AndroidManifest.xml: " + f_mani + "->"
                + f_mani_bak);

        FileUtils.copyFile(manifest, manifest_bak);

        File f = new File("apk");
        fDistribute = new File(folderDistribute);
        if (!f.exists()) {
            f.mkdir();
        }else{
            FileUtils.deleteDirectory(f);
            f.mkdir();
        }
        if(fDistribute.exists()){
            FileUtils.deleteDirectory(fDistribute);

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
            boolean firstTag = false, lastTag = false;
            boolean written = false;

            while ((line = br.readLine()) != null) {
                if (line.contains("<application") && !firstTag) {
                    firstTag = true;
                }
                if (line.contains(">") && firstTag && !lastTag && !written) {
                    lastTag = true;
                    //写入
                    line = line + "\n" + "<meta-data android:value=\"" + id + "\" android:name=\"qd_code\"></meta-data>";
                    written = true;
                    firstTag = false;
                }
                sb.append(line + "\n");
            }

            br.close();

            FileWriter fw = new FileWriter(f_mani);
            fw.write(sb.toString());
            fw.flush();
            fw.close();

            // 打包
            String unsignApk = "apk/" + dir + "_" + id + "_un.apk";
            String cmdPack = "java -jar apktool.jar b -o " + unsignApk + " " + dir;
            System.out.println("start package.run[" + cmdPack + "] ");
            runShell(cmdPack);

            // 签名
            String signApk = "apk/"+prefix + "_" + dir + "_" + suffix + "_" + id + ".apk";
            String cmdKey = String.format("jarsigner -verbose -keystore %s " + " -storepass %s -signedjar %s -digestalg SHA1 -sigalg MD5withRSA %s %s", keyFile, keyPasswd, signApk, unsignApk, keyName);
            System.out.println("start sign apk[" + cmdKey + "]");
            runShell(cmdKey);
            System.out.println("delete unsign apk[" + unsignApk + "]");

            // 删除未签名的包
            File unApk = new File(unsignApk);
            unApk.delete();
            //还原AndroidManifest.xml
            FileUtils.forceDelete(new File(f_mani));
            FileUtils.copyFile(manifest_bak, manifest);
        }

        System.out.println("OK");
    }

    private void toDestDir(){
        if(destDir==null||destDir.equals(""))return;
        System.out.println("move APKS to "+destDir);
        File f = new File("apk");
        runShell("mv "+f.getAbsolutePath()+"/*.apk "+destDir);
        System.out.println("move APKS done.");
    }

    // 读取当前文件夹中的文件
    private void moveFile() {
        File f = new File("apk");
        fDistribute = new File(folderDistribute);
        if(!fDistribute.exists())fDistribute.mkdir();

        if (f.exists()) {
            File[] fileList = f.listFiles();
            for (File file : fileList) {
                if (file.isFile()) {
                    String fileName = file.getName();
                    String id = fileName.split("_")[1].split(".apk")[0];
                    String name = qudao.get(id);
                    File dir = new File(folderDistribute+"/"+ name);
                    if (!dir.exists()) {
                        dir.mkdir();
                    }
                    try {
                        File targetF = new File(folderDistribute+"/"+name+"/"+fileName);
                        System.out.println("move ["+targetF.getAbsolutePath()+"] to ["+targetF.getAbsolutePath()+"]");
                        FileUtils.copyFile(file, targetF);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

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
        if(distribute)
            moveFile();

        toDestDir();
    }
}
