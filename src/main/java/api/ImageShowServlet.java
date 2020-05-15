package api;

import dao.Image;
import dao.ImageDao;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;

public class ImageShowServlet extends HttpServlet {
    static private HashSet<String> whiteList = new HashSet<>();

    static {
        whiteList.add("http://127.0.0.1:8081/java_image_sever/index.html");
    }

    /**
     * 实现基于白名单方式的防盗链
     * 通过 HTTP 中的 refer 字段判定是否是指定网站请求图片.
     * HashSet用于存储允许的referer
     * referer表示当前请求的上一个页面的地址，看它在不在HashSet中
     * 优化磁盘存取空间，用md5
     * 如果两个图片内容完全一样，就只在磁盘上存储一份
     * @param req
     * @param resp
     * @throws ServletException
     * @throws IOException
     */
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String referer = req.getHeader("Referer");
        if (!whiteList.contains(referer)) {
            resp.setContentType("application/json; charset: utf-8");
            resp.getWriter().write("{ \"ok\": false, \"reason\": \"未授权的访问\" }");
            return;
        }

        // 1. 解析出 imageId
        String imageId = req.getParameter("imageId");
        if (imageId == null || imageId.equals("")) {
            resp.setContentType("application/json; charset: utf-8");
            resp.getWriter().write("{ \"ok\": false, \"reason\": \"imageId 解析失败\" }");
            return;
        }
        // 2. 根据 imageId 查找数据库, 得到对应的图片属性信息(需要知道图片存储的路径)
        ImageDao imageDao = new ImageDao();
        Image image = imageDao.selectOne(Integer.parseInt(imageId));
        // 3. 根据路径打开文件, 读取其中的内容, 写入到响应对象中
        resp.setContentType(image.getContentType());
        File file = new File(image.getPath());
        // 由于图片是二进制文件, 应该使用字节流的方式读取文件
        OutputStream outputStream = resp.getOutputStream();
        FileInputStream fileInputStream = new FileInputStream(file);
        byte[] buffer = new byte[1024];
        while (true) {
            int len = fileInputStream.read(buffer);
            if (len == -1) {
                // 文件读取结束
                break;
            }
            // 此时已经读到一部分数据, 放到 buffer 里, 把 buffer 中的内容写到响应对象中
            outputStream.write(buffer);
        }
        fileInputStream.close();
        outputStream.close();
    }
}
