package org.adbcj.mysql.codec;

import java.math.BigInteger;
import java.util.Date;

import junit.framework.Assert;
import junit.framework.TestCase;

import org.testng.annotations.Test;


public class MysqlPreparedStatementUnitTest extends TestCase {
    @Test
    public void test_normal() throws Exception
    {
        String format = "yyyy-MM-dd HH:mm:ss";
        FastDateFormat fdf = FastDateFormat.getInstance(format);
        
        String sql = "select * from table where id = ?";
        String after = null;
        after = MysqlPreparedStatement.replacePattern(sql,fdf, 1);
        Assert.assertEquals("select * from table where id = 1", after);
        
        after = MysqlPreparedStatement.replacePattern(sql,fdf, BigInteger.valueOf(1l));
        Assert.assertEquals("select * from table where id = 1", after);
        
        after = MysqlPreparedStatement.replacePattern(sql,fdf, 1.1f);
        Assert.assertEquals("select * from table where id = 1.1", after);
       
        after = MysqlPreparedStatement.replacePattern(sql,fdf, 2.2d);
        Assert.assertEquals("select * from table where id = 2.2", after);
       
        sql = "select * from table where id = ? and b = ?";
        after = MysqlPreparedStatement.replacePattern(sql,fdf, 1,"'asdf?'");
        Assert.assertEquals("select * from table where id = 1 and b = 'asdf?'", after);
        
        after = MysqlPreparedStatement.replacePattern(sql,fdf, 1,new Date(10000000l));
        Assert.assertEquals("select * from table where id = 1 and b = '1970-01-01 10:46:40'", after);
        
        after = MysqlPreparedStatement.replacePattern(sql,fdf, 1,"asdf?");
        Assert.assertEquals("select * from table where id = 1 and b = 'asdf?'", after);
        
    }
    
    @Test
    public void test_exception() throws Exception
    {
        String format = "yyyy-MM-dd HH:mm:ss";
        FastDateFormat fdf = FastDateFormat.getInstance(format);
        
        String sql = "select * from table where id = ? and b = ?";
//        String after = null;
       
        try {
            String after  = MysqlPreparedStatement.replacePattern(sql, fdf, 1);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("params' size is less than question mark in sql", e.getMessage());
        }
    }
}
