import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Administrator on 2016/3/2 0002.
 */
public class Comm {
    public static String getData() {
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");//设置日期格式
        return df.format(new Date());
    }
}
