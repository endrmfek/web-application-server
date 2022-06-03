package http;

import lombok.extern.slf4j.Slf4j;
import util.HttpRequestUtils;
import util.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class HttpRequest {

    private Map<String, String> headers = new HashMap<>();
    private Map<String, String> params = new HashMap<>();
    private RequestLine requestLine;

    public HttpRequest(InputStream in) {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            String line = br.readLine();

            if (line == null) {
                return;
            }

            requestLine = new RequestLine(line);

            line = br.readLine();
            while(!line.equals("")){
                log.debug("header : {} ", line);
                String[] tokens = line.split(":");
                headers.put(tokens[0].trim() , tokens[1].trim()); // 헤더 이름 , 값
                line = br.readLine();
            }

            if(requestLine.getMethod().isPost()) {
                String body = IOUtils.readData(br, Integer.parseInt(headers.get("Content-Length")));
                params = HttpRequestUtils.parseQueryString(body);
            } else {
                params= requestLine.getParams();
            }

        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }



//    private void processRequestLine(String requestLine) {
//        log.debug("request line : {} ", requestLine);
//        String[] tokens = requestLine.split(" ");
//        method = tokens[0];
//
//        if ("POST".equals(method)) { //post 일때
//            path = tokens[1];
//            return;
//        }
//
//        int index = tokens[1].indexOf("?"); //get일때
//        if (index == -1) {
//            path = tokens[1];
//        } else {
//            path = tokens[1].substring(0, index);
//            params = HttpRequestUtils.parseQueryString(tokens[1].substring(index+1));
//        }
//    }

    public HttpMethod getMethod() {
        return requestLine.getMethod();
    }

    public String getPath() {
        return requestLine.getPath();
    }

    public String getHeader(String name) {
        return headers.get(name);
    }

    public String getParameter(String name) {
        return params.get(name);
    }
}
