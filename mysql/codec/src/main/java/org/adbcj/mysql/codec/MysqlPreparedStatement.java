package org.adbcj.mysql.codec;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.adbcj.DbFuture;
import org.adbcj.DbSession;
import org.adbcj.PreparedStatement;
import org.adbcj.Result;
import org.adbcj.ResultSet;


/**
 * mysql doesn't have the 'real' prepared statement . 
 * so here I just replace all question mark by params 
 * @author Whisper 2013年7月4日 下午1:39:54
 * @since 3.0.1
 */
public class MysqlPreparedStatement implements PreparedStatement{
    protected final String originalSql ;
    protected final DbSession session;
    protected final FastDateFormat fdf;
    public MysqlPreparedStatement(String originalSql,DbSession session,FastDateFormat fdf){
        super();
        this.originalSql = originalSql;
        this.session = session;
        this.fdf = fdf;
    }

    @Override
    public List<Object> getParameterKeys() {
        return null;
    }

    @Override
    public String getNativeSQL() {
        return originalSql;
    }

    @Override
    public DbFuture<ResultSet> executeQuery(Object... params) {
        String newSql = replacePattern(originalSql,fdf, params);
        return session.executeQuery(newSql);
    }
    public static final String replacePattern(String sql,FastDateFormat fdf,Object...params) {
        StringBuilder sb = new StringBuilder();
        int quotationCount = 0;
        int questionMarkCount = 0;
        for(int i = 0; i <sql.length(); i++)
        {
            char c = sql.charAt(i);
            if('\'' == c){
                if(quotationCount == 1)
                {
                    quotationCount --;
                }
                else if(quotationCount == 0)
                {
                    quotationCount++;
                }
                else
                {
                    throw new RuntimeException("should not be here " );
                }
                
            }else if('?' == c && quotationCount == 0){
                Object param = null;
                try {
                    param = params[questionMarkCount];
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new RuntimeException("params' size is less than question mark in sql");
                }
                questionMarkCount ++;
                appendSqlIntoArg(param, sb,fdf);
                continue;
            }
            sb.append(c);
        }
        if(questionMarkCount != params.length)
        {
            throw new RuntimeException("params' size is more than question mark in sql");
        }
        return sb.toString();
    }

    protected static void appendSqlIntoArg(Object arg,StringBuilder tar,FastDateFormat fdf) {
        if(arg instanceof byte[]) {
            throw new UnsupportedOperationException(" bytes not supported yet" );
        }
        if(arg == null)
        {
            tar.append("NULL");
        }
        if(arg instanceof String) {
            String argStr = String.valueOf(arg);
            //argstr may has ' .
            if(argStr.indexOf("'") != -1 && argStr.indexOf("\\'") == -1)
            {
                tar.append(argStr);
            }
            else
            {
                tar.append("'").append(argStr).append("'");
            }
            
        }
        else if(arg instanceof Date) {
            tar.append("'").append(fdf.format((Date)arg)).append("'");
        }
        else {
            tar.append(String.valueOf(arg));
        }
       
    }
    
    @Override
    public <T extends ResultSet> DbFuture<T> executeQuery(Map<Object, Object> params) {
       throw new UnsupportedOperationException("not supported yet !");
    }

    @Override
    public DbFuture<Result> executeUpdate(Object... params) {
        String newSql = replacePattern(originalSql,fdf, params);
        return session.executeUpdate(newSql);
    }

    @Override
    public <T extends ResultSet> DbFuture<T> executeUpdate(Map<Object, Object> params) {
        throw new UnsupportedOperationException("not supported yet !");
    }

}
