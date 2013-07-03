package org.adbcj.mysql.netty;

import junit.framework.Assert;

import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;
import org.adbcj.DbSessionFuture;
import org.adbcj.Result;
import org.adbcj.ResultSet;
import org.testng.annotations.Test;


public class ExceptionTest {

    public static ConnectionManager cm   = ConnectionManagerProvider.createConnectionManager("adbcj:mysqlnetty://localhost/unit_test",
                                             "root",
                                             "");
    public static final String      DATE = "'1986-03-22 05:33:12'";

    @Test
    public void test_duplicate_exception() throws Exception {
        Connection connection = cm.connect().get();
        DbSessionFuture<Result> result = connection.executeUpdate("delete from type_test");
        Assert.assertTrue(result.get().getAffectedRows() > -1);
        String sql = "insert into type_test (" + "pk,varcharr,charr,blobr,integerr,integerr_unsigned,tinyintr,tinyintr_unsigned,"
                     + "smallintr,smallintr_unsigned,mediumintr,mediumintr_unsigned,bitr,bigintr,bigintr_unsigned,floatr,doubler,"
                     + "decimalr,dater,timer,datetimer,timestampr,yearr) values (" + "0,'varch','char','lob',100,100,4,4,"
                     + "1,1,100,100,b'0',1000000,1000000,1.1,2.2,1000.1," + DATE + "," + DATE + "," + DATE + "," + DATE + "," + DATE
                     + ")";
        result = connection.executeUpdate(sql);
        Assert.assertTrue(result.get().getAffectedRows() > -1);
        
        sql = "insert into type_test (" + "pk,varcharr,charr,blobr,integerr,integerr_unsigned,tinyintr,tinyintr_unsigned,"
                     + "smallintr,smallintr_unsigned,mediumintr,mediumintr_unsigned,bitr,bigintr,bigintr_unsigned,floatr,doubler,"
                     + "decimalr,dater,timer,datetimer,timestampr,yearr) values (" + "0,'varch','char','lob',100,100,4,4,"
                     + "1,1,100,100,b'0',1000000,1000000,1.1,2.2,1000.1," + DATE + "," + DATE + "," + DATE + "," + DATE + "," + DATE
                     + ")";
        result = connection.executeUpdate(sql);
        try {
            Assert.assertTrue(result.get().getAffectedRows() > -1);
            Assert.fail();
        } catch (Exception e) {
            Assert.assertEquals("org.adbcj.mysql.codec.MysqlException: 23000Duplicate entry '0' for key 'PRIMARY'\n", e.getMessage());
            
        }
        
        //make sure the connection can execute other request
        sql = "select * from type_test";
        DbSessionFuture<ResultSet> queryResult = connection.executeQuery(sql);
        Assert.assertTrue(queryResult.get().size()>0);
        connection.close(true);
    }
}
