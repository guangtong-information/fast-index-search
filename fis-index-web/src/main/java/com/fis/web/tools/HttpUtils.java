package com.fis.web.tools;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;

import javax.net.ssl.SSLContext;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 */
//Http请求的工具类
@Slf4j
public class HttpUtils {

    private static final int TIMEOUT_IN_MILLIONS = 30000;

    /**
     * @param url
     * @param params
     * @return
     */
    private static HttpPost postJson(String url, String params) {
        HttpPost httpost = new HttpPost(url);
        httpost.setHeader("Content-Type", "application/json;charset=UTF-8");
        StringEntity entity = new StringEntity(params, "utf-8");//解决中文乱码问题   ();
        entity.setContentEncoding("UTF-8");
        entity.setContentType("application/json");
        httpost.setEntity(entity);
        return httpost;
    }

    /**
     * Get请求，获得返回数据
     *
     * @param urlStr
     * @return
     * @throws Exception
     */
    public static String doGet(String urlStr, String cookie) {
        URL url = null;
        HttpURLConnection conn = null;
        InputStream is = null;
        ByteArrayOutputStream baos = null;
        try {
            url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(TIMEOUT_IN_MILLIONS);
            conn.setConnectTimeout(TIMEOUT_IN_MILLIONS);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("accept", "*/*");
            //cookie登录
            if (cookie != null)
                conn.setRequestProperty("cookie", cookie);
            conn.setRequestProperty("connection", "Keep-Alive");
            log.info("get url：{}", url);
            int rscode = conn.getResponseCode();
            if (rscode == 200) {
                is = conn.getInputStream();
                baos = new ByteArrayOutputStream();
                int len = -1;
                byte[] buf = new byte[128];

                while ((len = is.read(buf)) != -1) {
                    baos.write(buf, 0, len);
                }
                baos.flush();
                String result = baos.toString();
                log.info("get return body：{}", result);
                return result;
            } else {
                throw new RuntimeException("responseCode is " + rscode + ",error:" + conn.getErrorStream());
            }

        } catch (Exception e) {
            log.error("Get错误：", e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                log.error("is.close错误：", e);
            }
            try {
                if (baos != null) {
                    baos.close();
                }
            } catch (IOException e) {
                log.error("baos.close错误：", e);
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return null;
    }

    public static boolean download(String urlStr, String cookie, String fileDir, String fileName) {
        URL url = null;
        HttpURLConnection conn = null;
        InputStream is = null;
        try {
            url = new URL(urlStr);
            conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(TIMEOUT_IN_MILLIONS);
            conn.setConnectTimeout(TIMEOUT_IN_MILLIONS);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("accept", "*/*");
            //cookie登录
            if (cookie != null)
                conn.setRequestProperty("cookie", cookie);
            conn.setRequestProperty("connection", "Keep-Alive");
            log.info("get url：{}", url);
            int rscode = conn.getResponseCode();
            conn.getHeaderFields().get("Content-Type");
            if (rscode == HttpURLConnection.HTTP_OK) {
                is = conn.getInputStream();
                //文件对象
                File dir = new File(fileDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File file = new File(dir, fileName);//根据目录和文件名得到file对象
                FileOutputStream fos = new FileOutputStream(file);

                //字节流
                int len = -1;
                byte[] buf = new byte[1024 * 8];

                //下载
                while ((len = is.read(buf)) != -1) {
                    fos.write(buf, 0, len);
                }
                fos.flush();
                log.info("下载路径：{}", fileDir + fileName);
                return true;
            } else {
                throw new RuntimeException("responseCode is " + rscode + ",error:" + conn.getErrorStream());
            }

        } catch (Exception e) {
            log.error("下载错误：", e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (IOException e) {
                log.error("is.close错误：", e);
            }
            if (conn != null) {
                conn.disconnect();
            }
        }
        return false;
    }

    /**
     * 向指定 URL 发送POST方法的请求
     *
     * @param url        发送请求的 URL
     * @param param      请求参数，请求参数应该是 name1=value1&name2=value2 的形式。
     * @param headersMap 请求报文头
     * @return 所代表远程资源的响应结果
     * @throws Exception
     */
    public static String doPost(String url, String param, Map<String, String> headersMap) throws Exception {
        PrintWriter out = null;
        BufferedReader in = null;
        String result = "";
        try {
            log.info("post url：{}", url);
            log.info("post param：{}", param);
            URL realUrl = new URL(url);
            // 打开和URL之间的连接
            HttpURLConnection conn = (HttpURLConnection) realUrl
                    .openConnection();
            // 设置通用的请求属性
            conn.setRequestProperty("accept", "*/*");
            conn.setRequestProperty("connection", "Keep-Alive");
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8");
            conn.setRequestProperty("charset", "utf-8");
            conn.setUseCaches(false);
            // 发送POST请求必须设置如下两行
            conn.setDoOutput(true);
            conn.setDoInput(true);
            conn.setReadTimeout(TIMEOUT_IN_MILLIONS);
            conn.setConnectTimeout(TIMEOUT_IN_MILLIONS);

            if (headersMap != null) {
                for (String key : headersMap.keySet()) {
                    conn.setRequestProperty(key, headersMap.get(key));
                }
            }

            if (param != null && !"".equals(param.trim())) {
                // 获取URLConnection对象对应的输出流
                out = new PrintWriter(conn.getOutputStream());
                // 发送请求参数
                out.print(param);
                // flush输出流的缓冲
                out.flush();
            }
            // 定义BufferedReader输入流来读取URL的响应
            in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "UTF-8"));
            String line;
            while ((line = in.readLine()) != null) {
                result += line;
            }
        } catch (Exception e) {
            log.error("Post错误：", e);
            throw e;
        }
        // 使用finally块来关闭输出流、输入流
        finally {
            try {
                if (out != null) {
                    out.close();
                }
                if (in != null) {
                    in.close();
                }
            } catch (IOException e) {
                log.error("out/in.close错误：", e);
            }
        }
        JSONObject respJsonObject = JSONObject.parseObject(result);
        log.info("post return body：{}", respJsonObject.toJSONString());
        return respJsonObject.toJSONString();
    }

    /**
     * 发送HttpPost请求， @param url * 请求地址 * @param map * 请求参数 map * @return 返回字符串
     */
    public static String sendPost(String url, Map<String, String> map) throws Exception {
        CloseableHttpClient httpclient = getPostHttpclient();
        log.info("post url：{}", url);
        log.info("post param：{}", JSON.toJSONString(map));
        HttpPost httpPost = postForm(url, map);
        String body = invoke(httpclient, httpPost);
        log.info("post return body：{}", body);
        httpclient.getConnectionManager().shutdown();
        return body;
    }

    /**
     * 发送HttpPost请求， @param url * 请求地址 * @param json * 请求参数 json * @return 返回字符串
     */
    public static String postWithJson(String url, String json) throws Exception {
        CloseableHttpClient httpclient = getPostHttpclient();
        log.info("post url：{}", url);
        log.info("post param：{}", json);
        HttpPost post = postJson(url, json);
        String body = invoke(httpclient, post);
        log.info("post return body：{}", body);
        httpclient.getConnectionManager().shutdown();
        return body;
    }

    public static CloseableHttpClient getPostHttpclient() throws Exception {
        SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, new TrustStrategy() {
                    //信任所有
                    @Override
                    public boolean isTrusted(X509Certificate[] chain,
                                             String authType) throws CertificateException {
                        return true;
                    }
                }).build();
        SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(
                sslContext);
        CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).setConnectionTimeToLive(50000, TimeUnit.MILLISECONDS).build();
        return httpclient;
    }


    private static String invoke(HttpClient httpclient,
                                 HttpUriRequest httpost) {
        HttpResponse response = sendRequest(httpclient, httpost);
        String body = paseResponse(response);
        return body;
    }

    private static String paseResponse(HttpResponse response) {
        String body = null;
        if (response != null) {
            HttpEntity entity = response.getEntity();
            try {
                body = EntityUtils.toString(entity);
            } catch (ParseException e) {
                log.error("ParseException错误：", e);
            } catch (IOException e) {
                log.error("IOException错误：", e);
            }
        }

        return body;
    }

    private static HttpResponse sendRequest(HttpClient httpclient,
                                            HttpUriRequest httpost) {
        HttpResponse response = null;
        try {
            response = httpclient.execute(httpost);
        } catch (ClientProtocolException e) {
            log.error("ClientProtocolException错误：", e);
        } catch (IOException e) {
            log.error("IOException错误：", e);
        }
        return response;
    }

    private static HttpPost postForm(String url, Map<String, String> params) {
        HttpPost httpost = new HttpPost(url);
        List<NameValuePair> nvps = new ArrayList<NameValuePair>();
        Set<String> keySet = params.keySet();
        for (String key : keySet) {
            nvps.add(new BasicNameValuePair(key, params.get(key)));
        }
        try {
            httpost.setEntity(new UrlEncodedFormEntity(nvps, HTTP.UTF_8));
        } catch (UnsupportedEncodingException e) {
            log.error("UnsupportedEncodingException错误：", e);
        }
        return httpost;
    }

    public static void main(String[] args) {
        String url = "http://chuanbo.weiboyi.com/hworder/weixin/filterlist/source/all?price_list=top%2Csecond%2Cother%2Csingle&snbt_exponent_sort=DESC&start=20&limit=20";
        String cookie = "_gscu_867320846=62838523461vve19; Hm_lvt_29d7c655e7d1db886d67d7b9b3846aca=1563076673,1563155330,1563277325,1563328779; Hm_lvt_b96f95878b55be2cf49fb3c099aea393=1563076674,1563155330,1563277325,1563328779; aLastLoginTime=1563353380; loginHistoryRecorded=0; Hm_lvt_a659e18331d9e50368afe428a4366751=1563155363,1563270136,1563328789,1563353383; TRACK_DETECTED=1.0.1; Hm_lvt_5ff3a7941ce54a1ba102742f48f181ab=1563270136,1563279577,1563328789,1563353384; next_notify=1563353682; is_alert=0; notify_data=%7B%22yy%22%3A%220%22%2C%22jd%22%3A%220%22%2C%22zjtk%22%3A%220%22%2C%22pjgl%22%3A%220%22%2C%22total%22%3A%220%22%2C%22is_alert%22%3A%220%22%7D; username=zhongsuo; contactMain=1; PHPSESSID=alov6d7qg8rhcv7gegal6ng0l5; web_image_site=http%3A%2F%2Fimg.weiboyi.com; TY_SESSION_ID=debc66c5-c4e6-4eb5-a2d8-c994dc7b904a; _gscbrs_867320846=1; Hm_lpvt_a659e18331d9e50368afe428a4366751=1563355265; TRACK_USER_ID=469981; TRACK_IDENTIFY_AT=2019-07-17T08%3A49%3A44.019Z; TRACK_SESSION_ID=4a9b0f0f039b8d62ee63e089ae46f407; Hm_lpvt_5ff3a7941ce54a1ba102742f48f181ab=1563355266";

        String json = doGet(url, cookie);
        System.out.println(json);

        JSONObject jo = JSON.parseObject(json);
        JSONObject data = jo.getJSONObject("data");
        JSONArray jsonArray = data.getJSONArray("rows");
        JSONObject row = null;
        for (int i = 0; i < jsonArray.size(); i++) {
            row = jsonArray.getJSONObject(i);
            String id = row.get("id") == null ? "" : row.get("id").toString();
            JSONObject cells = row.getJSONObject("cells");
            String original_weibo_name = cells.get("original_weibo_name") == null ? "" : cells.get("original_weibo_name").toString();
            String face_url = cells.get("face_url") == null ? "" : cells.get("face_url").toString();
            boolean ok = download(face_url, cookie, "E:\\工作项目\\省广众烁\\data\\", "xx.jpg");

            System.out.println(id + "." + original_weibo_name);
        }
    }

}
