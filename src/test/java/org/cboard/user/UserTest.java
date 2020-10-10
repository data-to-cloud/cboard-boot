package org.cboard.user;


import org.cboard.dao.UserDao;
import org.cboard.pojo.DashboardUser;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
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
}
