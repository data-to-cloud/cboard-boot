package org.cboard.user;


import lombok.extern.slf4j.Slf4j;
import org.cboard.dao.UserDao;
import org.cboard.pojo.DashboardUser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;

import java.sql.*;
import java.util.LinkedList;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class UserTest {

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    UserDao userDao;

    @Test
    public void initUser(){
       String password =  passwordEncoder.encode("Abcd!234");
        DashboardUser user =new DashboardUser();
        user.setUserPassword(password);
        user.setUserId("1");
        user.setLoginName("admin");
        user.setUserName("Administrator");
        Assert.assertEquals(userDao.update(user),1);
    }


    @Test
    public void initJdbc(){

        List<String[]> list = new LinkedList<>();
        try{
            //1.导入jar包
            //2.注册驱动
            Class.forName("com.mysql.cj.jdbc.Driver");
            //3.获取数据库连接对象
            String url = "jdbc:mysql://121.41.4.2:3307/anan?useSSL=false&serverTimezone=UTC&useUnicode=true&characterEncoding=UTF-8";
            String user = "root";
            String password = "123456";
            Connection conn = DriverManager.getConnection(url, user, password);
            //4.定义sql语句
            String sql = "SELECT province_id, city_id ,COUNT(id)  \n" +
                    " FROM (\n" +
                    "select id,province_id,city_id,district_id from f_user_address\n" +
                    ") cb_view \n" +
                    " WHERE province_id IN ('河南省') \n" +
                    " GROUP BY province_id, city_id ";
            //5.获取执行sql的对象 Statement
            Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery(sql);
                ResultSetMetaData metaData = rs.getMetaData();
                int columnCount = metaData.getColumnCount();
                while (rs.next()) {
                    String[] row = new String[columnCount];
                    for (int j = 0; j < columnCount; j++) {
                        int columType = metaData.getColumnType(j + 1);
                        switch (columType) {
                            case java.sql.Types.DATE:
                                row[j] = rs.getDate(j + 1).toString();
                                break;
                            default:
                                row[j] = rs.getString(j + 1);
                                break;
                        }
                    }
                    list.add(row);
                }
                log.info("list.size is ",list.size());
        }catch (Exception e){
            log.error(e.getMessage());
        }
    }


}
