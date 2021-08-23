package ru.example;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PostServer {

  // задаем директорию и порт сервера как глобальные переменные
  public static String fileDir;
  public static int serverPort;
  // формат даты
  private static final DateFormat DF = new SimpleDateFormat(
    "yyyyMMdd_HHmmss_SSS"
  );

  public static void main(String[] args) throws IOException {
    // основной класс
    Map<String, String> actualArgs = argsHash(args); // берет список аргум
    serverPort = Integer.parseInt(actualArgs.get("port"));
    fileDir = actualArgs.get("dir");
    System.out.println(
      "Server is listening on port " +
      serverPort +
      " and write files to " +
      fileDir
    );
    HttpServer server = HttpServer.create(new InetSocketAddress(serverPort), 0);
    server.createContext("/", new MyHandler());
    server.setExecutor(null); // creates a default executor
    server.start();
  }

  static class MyHandler implements HttpHandler {

    public void handle(HttpExchange t) throws IOException {
      String fileNamePrefix = fileDir + "post-server-file-";
      // проверка, что пришел POST запрос
      if ("POST".equals(t.getRequestMethod())) {
        // получаем body, записываем в буфер и далее в файл
        InputStreamReader isr = new InputStreamReader(
          t.getRequestBody(),
          "utf-8"
        );
        BufferedReader br = new BufferedReader(isr);
        int b;
        StringBuilder buf = new StringBuilder(512);
        while ((b = br.read()) != -1) {
          buf.append((char) b);
        }

        br.close();
        isr.close();
        try {
          BufferedWriter writer = new BufferedWriter(
            new FileWriter(fileNamePrefix + DF.format(new Date()))
          );
          writer.write(buf.toString());
          writer.close();
          // ответ 200 кодом если запись произошла успешно
          t.sendResponseHeaders(200, -1);
        } catch (IOException e) {
          System.out.println(
            "Can't write to " +
            fileNamePrefix +
            DF.format(new Date()) +
            "\n Mistake: " +
            e.getLocalizedMessage()
          );
          // ответ 500 кодом если запись не удалась
          t.sendResponseHeaders(500, -1);
        }
      } else {
        // ответ с 405 кодом если не post запрос
        t.sendResponseHeaders(405, -1);
      }
    }
  }

  // сдела в таком виде, а не взял какие то готовые библиотеки, потому что regex хотел потрогать в java
  public static Map<String, String> argsHash(String[] args) {
    // Принимает список аргументов командной строки, ищет --port и --dir. Возвращает
    // дефолтные значение если не найдены подходящие в аргументах
    Map<String, String> argsValues = new HashMap<String, String>();
    Pattern portRegexPattern = Pattern.compile("^--port=(\\d{1,5})$");
    Pattern dirRegexPattern = Pattern.compile("^^--dir=(/|(/[\\w-]+)+/)$");
    argsValues.put("port", "8080");
    argsValues.put("dir", "./");
    // проверяет количество аргументов и если оно не нулевое, то происходит их обработка через regex
    if (args.length != 0) {
      for (String arg : args) {
        if (arg.startsWith("--port=")) {
          Matcher portRegexMatcher = portRegexPattern.matcher(arg);
          try {
            portRegexMatcher.find();
            argsValues.put("port", portRegexMatcher.group(1));
          } catch (java.lang.IllegalStateException e) {
            throw new IllegalStateException(
              "Mistake in arg - have to be valid port. Mistake: " +
              e.getMessage()
            );
          }
        } else if ((arg.startsWith("--dir="))) {
          Matcher dirRegexMatcher = dirRegexPattern.matcher(arg);
          try {
            dirRegexMatcher.find();
            argsValues.put("dir", dirRegexMatcher.group(1));
          } catch (java.lang.IllegalStateException e) {
            throw new IllegalStateException(
              "Mistake in arg - have to be valid linux dir path. Mistake: " +
              e.getMessage()
            );
          }
        } else {
          throw new IllegalArgumentException(
            "Args have to be '--port=' or '--dir'"
          );
        }
      }
    }
    return argsValues;
  }
}
