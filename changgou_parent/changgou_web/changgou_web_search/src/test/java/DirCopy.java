import org.junit.Test;

import java.io.File;

public class DirCopy {

    @Test
    public void test(){
        File dirname=new File("D:\\resoure\\就业班作业和讲义\\就业班13天讲义");

        String[] names = dirname.list();
        for (String name : names) {
            File newDir=new File("E:\\工作资料\\"+name);
            boolean mkdir = newDir.mkdir();
            if(!mkdir){
                return;
            }
        }
    }
}
