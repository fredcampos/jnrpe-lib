package oracle.jdbc.driver;

import it.jnrpe.plugins.mocks.sql.DbConnectionMock;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.util.Properties;

public class OracleDriver implements Driver
{

    public Connection connect(String url, Properties info) throws SQLException
    {
        if (url.equals("jdbc:oracle:thin:@127.0.0.1:1521:mockdb"))
        {
            return new DbConnectionMock(new OracleSQLQueryResolver());
        }
        
        throw new SQLException("Listener refused the connection with the following error: ORA-12505, TNS:listener does not currently know of SID given in connect descriptor");
    }

    public boolean acceptsURL(String url) throws SQLException
    {
        return false;
    }

    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info)
            throws SQLException
    {
        return null;
    }

    public int getMajorVersion()
    {
        return 0;
    }

    public int getMinorVersion()
    {
        return 0;
    }

    public boolean jdbcCompliant()
    {
        return true;
    }

}
