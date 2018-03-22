import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PackFile
{
  private static String configFileName = "config.txt";
  private static String tomcatProjectPath = "";
  private static String projetSvnName = "icap2.0";

  public static void main(String[] args) {
    long opCount = 0L;
    System.out.println("本地编码格式：" + Charset.defaultCharset() + "\n");
    System.out.println(++opCount + "======读取当前文件路径======");
    String resourcePath = PackFile.class.getResource("").getPath();
    try {
      resourcePath = URLDecoder.decode(resourcePath, "utf-8");
    } catch (UnsupportedEncodingException e) {
      System.out.println("资源路径转码失败");
      e.printStackTrace();
      return;
    }
    File currentF = new File(resourcePath);
    String currentPath = currentF.getPath();
    System.out.println("当前文件路径: " + currentPath);

    System.out.println("\n" + ++opCount + "======读取配置文件" + configFileName + "======");
    String copyIsoFileConfigPath = currentPath + File.separator + configFileName;
    List<String> configList = readFileByLines(copyIsoFileConfigPath);
    System.out.println("config rows size :" + configList.size());

    List<Map<String,String>> configMapList = new ArrayList<Map<String,String>>();
    for (String configStr : configList) {
      Map<String,String> m = null;
      if ((configStr != null) && (configStr.length() > 0)) {
        String[] config = configStr.split("\\|");
        if (config != null) {
          m = new HashMap<String,String>();
          String filePath = config[0];
          if ((!isNotEmpty(filePath)) || 
            (filePath.contains("#"))) {
            continue;
          }
          if (filePath.startsWith("projetSvnName=")) {
            projetSvnName = filePath.replace("projetSvnName=", "");
          } else if (filePath.startsWith("tomcatProjectPath=")) {
            tomcatProjectPath = filePath.replace("tomcatProjectPath=", "");
          } else {
            boolean isInClasss = filePath.contains(projetSvnName + "/src");

            if (isNotEmpty(projetSvnName)) {
              filePath = filePath.replace(projetSvnName + "/src/", "").replace(projetSvnName + "/WebRoot/", "");
            }
            if ((filePath.contains(".java")) || (filePath.endsWith(".xml"))) {
              if (filePath.startsWith("/")) {
                filePath = filePath.replaceFirst("/", "");
              }
              if (filePath.contains(".java")) {
                filePath = filePath.replace(".java", ".class");
              }
              isInClasss = true;
            }
            if (isInClasss) {
              filePath = "/WEB-INF/classes/" + filePath;
            }
            filePath = filePath.replace("/", File.separator).replace("\\", File.separator);
            if (!filePath.startsWith(File.separator)) {
              filePath = File.separator + filePath;
            }
            m.put("sourceFilePath", filePath);
            configMapList.add(m);
          }
        }
      }
    }
    System.out.println("tomcatProjectPath: " + tomcatProjectPath);
    System.out.println("projetSvnName: " + projetSvnName);
    if ((tomcatProjectPath == null) || (tomcatProjectPath.length() < 1)) {
      System.out.println("\n本地tomcat工程路径为空！(tomcatProjectPath)");
      return;
    }

    File tomcatProject = new File(tomcatProjectPath);
    if (!tomcatProject.isDirectory()) {
      System.out.println("本地tomcat工程路径不存在(" + tomcatProjectPath + ")！");
      return;
    }
    String copyFileResult = "";
    StringBuffer copyFileResultSuccessSbf = new StringBuffer();
    StringBuffer copyFileResultFailSbf = new StringBuffer();

    long successCount = 0L;
    long failCount = 0L;
    System.out.println("\n" + ++opCount + "======复制文件======");

    String newFileDir = currentPath + File.separator + "发布包Pack" + getDateFormat("yyyyMMddHHmm") + File.separator + "ROOT";
    for (Map<String,String> m : configMapList) {
      boolean copySuccess = false;
      String errorMsg = "";
      String v = (String)m.get("sourceFilePath");
      String oldFilePath = tomcatProjectPath + v;
      String newFilePath = newFileDir + v;
      if (fileExists(oldFilePath)) {
        File oldFile = new File(oldFilePath);
        if (!oldFile.isDirectory()) {
          File newFile = createFile(newFilePath);
          if (newFile.exists()) {
            if (copyFile(oldFile, newFile)){
            	if( oldFile.length()==newFile.length()){
            		copySuccess = true;
            	}else{
            		errorMsg = "文件大小不一致("+oldFile.length()+"-->"+newFile.length()+")";
            	}
            }else{
            	errorMsg = "文件复制失败";
            }
          }
          else{
            errorMsg = "新文件创建失败";
          }
        }
        else {
          copySuccess = true;
          System.out.println("文件目录：" + oldFilePath);
        }
      } else {
        errorMsg = "源文件不存在";
      }
      StringBuffer sbf = new StringBuffer();
      sbf.append((String)m.get("sourceFilePath"));
      if (!copySuccess) {
        sbf.append("|").append("失败：" + errorMsg);
      }
      sbf.append("\r\n");
      if (copySuccess) {
        copyFileResultSuccessSbf.append(sbf.toString());
        successCount++;
      } else {
        copyFileResultFailSbf.append(sbf.toString());
        failCount++;
      }
    }
    copyFileResult = copyFileResult + "\r\n\r\n===========================================失败" + failCount + "===========================================\r\n";
    copyFileResult = copyFileResult + copyFileResultFailSbf.toString();
    copyFileResult = copyFileResult + "\r\n\r\n===========================================成功" + successCount + "===========================================\r\n";
    copyFileResult = copyFileResult + copyFileResultSuccessSbf.toString();

    System.out.println("\n" + ++opCount + "======写入文件复制结果======");
    String copyFileResultPath = currentPath + File.separator + "打包结果(" + getDateFormat("yyyyMMddHHmm") + ").txt";
    writeFile(copyFileResultPath, copyFileResult, false);
    System.out.println("复制文件完成！成功：" + successCount + "; 失败：" + failCount);
  }

  private static boolean isNotEmpty(String str)
  {
    return (str != null) && (str.length() > 0);
  }

  private static boolean copyFile(File oldFile, File newFile)
  {
    boolean result = false;
    FileOutputStream outputStream = null;
    FileInputStream inputStream = null;
    try {
      outputStream = new FileOutputStream(newFile);//true
      inputStream = new FileInputStream(oldFile);
      byte[] buff = new byte[1024];
      int length = inputStream.read(buff);
      while (length != -1) {
        outputStream.write(buff, 0, length);
        length = inputStream.read(buff);
      }
      result = true;
    } catch (Exception e) {
      System.out.println("复制文件失败！");
      e.printStackTrace();
    }
    finally
    {
      try
      {
    	  if (inputStream != null){
              inputStream.close();
            }
        if (outputStream != null) {
          outputStream.close();
        }
        
      }
      catch (IOException e)
      {
        e.printStackTrace();
      }
    }
    return result;
  }

  public static File createFile(String filePath) {
    File file = new File(filePath);
    try {
      System.out.print("创建文件：");
      if (!file.exists()) {
        if (!file.getParentFile().exists()) {
          file.getParentFile().mkdirs();
        }
        file.createNewFile();
        System.out.print("成功");
      } else {
        System.out.print("文件已存在");
      }
      System.out.println(" " + filePath);
    } catch (Exception e) {
      System.out.println("创建文件出错");
      e.printStackTrace();
    }
    return file;
  }

  private static boolean fileExists(String filePath) {
    boolean result = false;
    File file = new File(filePath);
    result = file.exists();
    return result;
  }

  public static void writeFile(String fileNamePath, String str, boolean append)
  {
    Writer writer = null;
    try
    {
      File file = createFile(fileNamePath);
      writer = new OutputStreamWriter(new FileOutputStream(file, append), "UTF-8");
      writer.write(str);
      writer.close();
      System.out.println("写入文件成功！ " + fileNamePath);
    } catch (Exception e) {
      System.out.println("写入文件出错！");
      e.printStackTrace();

      if (writer != null) {
        try {
          writer.close();
        } catch (Exception ex) {
          System.out.println("写入文件，关闭流出错！");
          ex.printStackTrace();
        }

      }

      if (writer != null)
        try {
          writer.close();
        } catch (Exception ex) {
          System.out.println("写入文件，关闭流出错！");
          e.printStackTrace();
        }
    }
    finally
    {
      if (writer != null)
        try {
          writer.close();
        } catch (Exception e) {
          System.out.println("写入文件，关闭流出错！");
          e.printStackTrace();
        }
    }
  }

  public static List<String> readFileByLines(String fileNamePath)
  {
    File file = new File(fileNamePath);
    BufferedReader reader = null;
    List<String> list = new ArrayList<String>();
    try
    {
      reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
      String tempString = null;
      int line = 1;

      while ((tempString = reader.readLine()) != null)
      {
        list.add(tempString);
        line++;
      }
      reader.close();
    } catch (Exception e) {
      System.out.println("读取文件出错！");
      e.printStackTrace();

      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e1) {
          System.out.println("读取文件，关闭流出错！");
          e1.printStackTrace();
        }

      }

      if (reader != null)
        try {
          reader.close();
        } catch (IOException e1) {
          System.out.println("读取文件，关闭流出错！");
          e1.printStackTrace();
        }
    }
    finally
    {
      if (reader != null) {
        try {
          reader.close();
        } catch (IOException e1) {
          System.out.println("读取文件，关闭流出错！");
          e1.printStackTrace();
        }
      }
    }
    return list;
  }

  public static String getDateFormat(String format)
  {
    SimpleDateFormat sdf = new SimpleDateFormat(format);
    return sdf.format(new Date());
  }
}