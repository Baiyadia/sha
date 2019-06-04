package com.kaiqi.osprey.web.service;

import com.kaiqi.osprey.web.AbstractTest;
import com.kaiqi.osprey.web.domain.User;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.Resource;
import java.util.List;

/**
 * @author wangs
 * @title: UserServiceTest
 * @package com.kaiqi.osprey.mapper.service
 * @description: TODO
 * @date 2019-06-03 17:03
 */
public class UserServiceTest extends AbstractTest {

    @Resource
    private UserService userService;

    @Test
    public void testGet() {
        List<User> all = userService.getAll();
        Assert.assertNotNull(all);
    }

}