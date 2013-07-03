package org.adbcj.mysql.netty;

import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Blob;
import java.sql.Date;
import java.sql.Time;
import java.sql.Timestamp;

import junit.framework.Assert;

import org.adbcj.Connection;
import org.adbcj.ConnectionManager;
import org.adbcj.ConnectionManagerProvider;
import org.adbcj.DbSessionFuture;
import org.adbcj.Result;
import org.adbcj.ResultSet;
import org.adbcj.Row;
import org.adbcj.Value;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestType {

    // public static ConnectionManager cm =
    // ConnectionManagerProvider.createConnectionManager("adbcj:mysqlnetty://10.232.31.25:3309/unitTest",
    // "test",
    // "test");

    public static ConnectionManager cm   = ConnectionManagerProvider.createConnectionManager("adbcj:mysqlnetty://localhost/unit_test",
                                             "root",
                                             "");
    public static final String      DATE = "'1986-03-22 05:33:12'";

    @BeforeMethod
    public void prepare() throws Exception {
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
        connection.close(true);
    }

    public void tear() throws Exception {

    }

    @Test
    public void testType() throws Exception {
        Connection connection = cm.connect().get();
        DbSessionFuture<ResultSet> result = connection.executeQuery("select pk,varcharr,charr,blobr,integerr,integerr_unsigned,tinyintr,tinyintr_unsigned,"
                                                                    + "smallintr,smallintr_unsigned,mediumintr,mediumintr_unsigned,bitr,bigintr,bigintr_unsigned,floatr,doubler,"
                                                                    + "decimalr,dater,timer,datetimer,timestampr,yearr from type_test");
        ResultSet r = result.get();
        Row row = r.iterator().next();
        Value[] values = row.getValues();
        // pk
        Assert.assertEquals(0, values[0].getValue());
        // varcharr varch
        Assert.assertEquals("varch", values[1].getValue());
        // charr 'char'
        Assert.assertEquals("char", values[2].getValue());
        // blobr 'lob'
        InputStream instream = ((Blob) values[3].getValue()).getBinaryStream();
        byte[] b = new byte[instream.available()];
        instream.read(b);
        String str = new String(b);
        Assert.assertEquals("lob", str);
        // ,integerr 100
        Assert.assertEquals(100, values[4].getValue());
        // ,integerr_unsign 100
        Assert.assertEquals(100l, values[5].getValue());

        // ,tinyintr, 4
        Assert.assertEquals(Integer.valueOf("4"), values[6].getValue());
        // ,tinyintr_unsign, 4
        Assert.assertEquals(Integer.valueOf("4"), values[7].getValue());
       
        // "smallintr
        Assert.assertEquals(1, values[8].getValue());
        // "smallintr_unsign
        Assert.assertEquals(1, values[9].getValue());
        // ,mediumintr 100
        Assert.assertEquals(100, values[10].getValue());
        // ,mediumintr_unsign 100
        Assert.assertEquals(100, values[11].getValue());
        // ,bitr, 0
        Assert.assertEquals(Byte.valueOf("0").byteValue(), (byte)((byte[])values[12].getValue())[0]);
        // bigintr 1000000
        Assert.assertEquals(1000000l, values[13].getValue());
        // bigintr 1000000
        Assert.assertEquals(BigInteger.valueOf(1000000l), values[14].getValue());
        // ,floatr
        Assert.assertEquals(1.1f, values[15].getValue());
        // ,doubler," 2.2
        Assert.assertEquals(2.2d, values[16].getValue());
        // + "decimalr,
        Assert.assertEquals(BigDecimal.valueOf(1000l), values[17].getValue());
        // dater
        Assert.assertEquals(Date.valueOf("1986-03-22"), values[18].getValue());
        //timer 
        Assert.assertEquals(Time.valueOf("05:33:12"), values[19].getValue());
        //datetimer
        Assert.assertEquals(Timestamp.valueOf("1986-03-22 05:33:12"), values[20].getValue());
        //,timestampr
        Assert.assertEquals(Timestamp.valueOf("1986-03-22 05:33:12"), values[21].getValue());
        //yearr
        Assert.assertEquals(1986, values[22].getValue());
    }
}
