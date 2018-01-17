package internal.org.springframework.content.jpa.operations;

import java.io.*;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.sql.DataSource;

import internal.org.springframework.content.jpa.io.MySQLBlobResource;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.content.commons.annotations.ContentId;
import org.springframework.content.commons.annotations.ContentLength;
import org.springframework.content.commons.io.FileRemover;
import org.springframework.content.commons.io.ObservableInputStream;
import org.springframework.content.commons.utils.BeanUtils;

import internal.org.springframework.content.jpa.utils.InputStreamEx;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;

public class JpaContentTemplate implements InitializingBean {

	private static Log logger = LogFactory.getLog(JpaContentTemplate.class);
	
	private DataSource datasource;

    private JdbcTemplate template;

    @Autowired
    public JpaContentTemplate(DataSource datasource) {
        this.datasource = datasource;
    }

    @Autowired(required=false)
    public void setTemplate(JdbcTemplate template) {
        this.template = template;
    }

    public Resource getResource(String id) {
        return new MySQLBlobResource(id, template);
    }

    @Override
	public void afterPropertiesSet() throws Exception {
        if (this.template == null) {
            this.template = new JdbcTemplate(datasource);
        }

//        this.template.execute(new ConnectionCallback<Integer>() {
//
//            @Override
//            public Integer doInConnection(Connection con) throws SQLException, DataAccessException {
//				ResultSet rs = null;
//				Statement stmt = null;
//				try {
//					rs = con.getMetaData().getTables(null, null, "BLOBS", new String[] {"TABLE"});
//					if (!rs.next()) {
//						logger.info("Creating JPA Content Repository");
//
//						stmt = datasource.getConnection().createStatement();
//						String sql = "CREATE TABLE BLOBS " +
//								"(id INTEGER GENERATED BY DEFAULT AS IDENTITY (START WITH 1), " +
//								" blob BLOB, " +
//								" PRIMARY KEY ( id ))";
//
//						return stmt.executeUpdate(sql);
//					}
//				} finally {
//					if (stmt != null) {
//						stmt.close();
//					}
//					if (rs != null) {
//						rs.close();
//					}
//				}
//                return null;
//            }
//        });
	}

	public <T> void setContent(T metadata, InputStream content) {
		if (BeanUtils.getFieldWithAnnotation(metadata, ContentId.class) == null) {
			String sql = "INSERT INTO BLOBS VALUES(NULL, ?);";
            this.template.execute(new PreparedStatementCreator() {
                @Override
                public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
                    return con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
                }
            }, new PreparedStatementCallback<Integer>() {
                @Override
                public Integer doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
                    ResultSet set = null;
                    int id = 0;
                    int rc = 0;
                    try {
                        InputStreamEx in = new InputStreamEx(content);
                        ps.setBinaryStream(1, in);
                        rc = ps.executeUpdate();
                        set = ps.getGeneratedKeys();
                        set.next();
                        id = set.getInt("ID");
                        BeanUtils.setFieldWithAnnotation(metadata, ContentId.class, id);
                        BeanUtils.setFieldWithAnnotation(metadata, ContentLength.class, in.getLength());
                        return rc;
                    } catch (SQLException sqle) {
                        logger.error("Error inserting content", sqle);
                    } finally {
                        if (set != null) {
                            try {
                                set.close();
                            } catch (SQLException e) {
                                logger.error(String.format("Unexpected error closing result set for content id %s", id));
                            }
                        }
                    }
                    return rc;
                }
            });
		} else {
			String sql = "UPDATE BLOBS SET blob=? WHERE id=" + BeanUtils.getFieldWithAnnotation(metadata, ContentId.class);
            this.template.execute(sql, new PreparedStatementCallback<Object>() {
                @Override
                public Object doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
                    int rc = 0;
                    try {
                        InputStreamEx in = new InputStreamEx(content);
                        ps.setBinaryStream(1, in);
                        rc = ps.executeUpdate();
                        BeanUtils.setFieldWithAnnotation(metadata, ContentLength.class, in.getLength());
                    } catch (SQLException sqle) {
                        logger.error(String.format("Error updating content %s", BeanUtils.getFieldWithAnnotation(metadata, ContentId.class)), sqle);
                    }
                    return rc;
                }
            });
        }
    }

	public <T> void unsetContent(T metadata) {
		String sql = "DELETE FROM BLOBS WHERE id=" + BeanUtils.getFieldWithAnnotation(metadata, ContentId.class);
        this.template.execute(sql, new PreparedStatementCallback<Integer>() {
            @Override
            public Integer doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
                int rc = 0;
                try {
                    rc = ps.executeUpdate();
                    BeanUtils.setFieldWithAnnotation(metadata, ContentId.class, null);
                    BeanUtils.setFieldWithAnnotation(metadata, ContentLength.class, 0);
                } catch (SQLException sqle) {
                    logger.error(String.format("Error deleting content %s", BeanUtils.getFieldWithAnnotation(metadata, ContentId.class)), sqle);
                }
                return rc;
            }
        });
	}

	public <T> InputStream getContent(T metadata) {
		String sql = "SELECT blob FROM BLOBS WHERE id='" + BeanUtils.getFieldWithAnnotation(metadata, ContentId.class) + "'";
        return this.template.execute(sql, new PreparedStatementCallback<InputStream>() {
            @Override
            public InputStream doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
                ResultSet set = null;
                try {
                    set = ps.executeQuery();
                    if(!set.next()) return null;
                    Blob b = set.getBlob("blob");

                    try {
                        File tempFile = File.createTempFile("_sc_jpa_", null);
                        FileOutputStream fos = new FileOutputStream(tempFile);
                        InputStream is = b.getBinaryStream();
                        try {
                            IOUtils.copyLarge(is, fos);
                        } finally {
                            IOUtils.closeQuietly(is);
                            IOUtils.closeQuietly(fos);
                        }
                        return new ObservableInputStream(new FileInputStream(tempFile), new FileRemover(tempFile));
                    } catch (IOException ioe) {
                        return null;
                    }
                } catch (SQLException sqle) {
                    logger.error(String.format("Error getting content %s", BeanUtils.getFieldWithAnnotation(metadata, ContentId.class)), sqle);
                } finally {
                    if (set != null)
                        try {
                            set.close();
                        } catch (SQLException sqle) {
                            logger.error(String.format("Error closing resultset for content %s", BeanUtils.getFieldWithAnnotation(metadata, ContentId.class)), sqle);
                        }
                }
            return null;
            }
        });
	}
}
