/**
 * Copyright 2013 Andrew Clemons <andrew.clemons@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nz.geek.caffe.spring.ase;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLErrorCodesFactory;

/**
 * 
 * @author <a href='mailto:andrew.clemons@gmail.com'>Andrew Clemons</a>
 */
public class AseExceptionMappingTest {

    private DriverManagerDataSource datasource;
    private JdbcTemplate jdbcTemplate;

    private void dropTables() {
	try {
	    this.jdbcTemplate.execute("drop table TEST_CHILD");
	} catch (final DataAccessException e) {
	    // ignored
	}

	try {
	    this.jdbcTemplate.execute("drop table TEST_PARENT");
	} catch (final DataAccessException e) {
	    // ignored
	}

    }

    /**
     * Setup.
     */
    @Before
    public void setup() {
	this.datasource = new DriverManagerDataSource(System.getProperty(
		"jdbcUrl", "jdbc:sybase:Tds:localhost:5000/test"),
		System.getProperty("jdbcUser", "spring"), System.getProperty(
			"jdbcPassword", "spring"));

	this.jdbcTemplate = new JdbcTemplate(this.datasource);

	dropTables();

	this.jdbcTemplate
		.execute("create table TEST_PARENT(ID_ int, STR_ varchar(10), primary key (ID_))");
	this.jdbcTemplate
		.execute("create table TEST_CHILD(ID_ int, STR_ varchar(10), PARENT_ int not null, primary key (ID_))");

	this.jdbcTemplate
		.execute("alter table TEST_CHILD add constraint FK_PARENT_CHILD foreign key (PARENT_) references TEST_PARENT(ID_)");
    }

    /**
     * Tear down.
     */
    @After
    public void tearDown() {
	dropTables();
    }

    /**
     * Test for SPR-11097. This test will pass.
     */
    @Test
    public void testForeignKeyConstrainViolation() {
	this.jdbcTemplate.execute("INSERT INTO TEST_PARENT VALUES (1, 'test')");

	try {
	    this.jdbcTemplate
		    .execute("INSERT INTO TEST_CHILD VALUES (1, 'test', 2)");
	    Assert.fail("Insert should have failed");
	} catch (final DataIntegrityViolationException e) {
	    // expected
	}
    }

    /**
     * Test for SPR-11097. This test will fails since the error code 547 is
     * currently not configured in sql-error-codes.xml. Repeating test as a
     * batch since this is what Hibernate will typically do.
     */
    @Test
    public void testForeignKeyConstrainViolationBatch() {
	this.jdbcTemplate.execute("INSERT INTO TEST_PARENT VALUES (1, 'test')");

	// this willl currently fail since the exception is translated as
	// TransientDataAccessResourceExceptio

	try {
	    this.jdbcTemplate
		    .batchUpdate(new String[] { "INSERT INTO TEST_CHILD VALUES (1, 'test', 2)" });
	    Assert.fail("Insert should have failed");
	} catch (final DataIntegrityViolationException e) {
	    // expected
	}
    }

    /**
     * Test for SPR-11097. This test will pass since the error code 547 is
     * currently mapped in the patched config.
     */
    @Test
    public void testForeignKeyConstrainViolationBatchWithPatch() {
	this.jdbcTemplate.execute("INSERT INTO TEST_PARENT VALUES (1, 'test')");

	// override to ues our custom mappings for test

	final SQLErrorCodesFactory factory = new SQLErrorCodesFactory() {

	    /**
	     * @see org.springframework.jdbc.support.SQLErrorCodesFactory#loadResource(java.lang.String)
	     */
	    @Override
	    protected Resource loadResource(String path) {
		return new ClassPathResource("/sql-error-codes-ase.xml",
			getClass().getClassLoader());
	    }

	};

	this.jdbcTemplate
		.setExceptionTranslator(new SQLErrorCodeSQLExceptionTranslator(
			factory.getErrorCodes(this.datasource)));

	try {
	    this.jdbcTemplate
		    .batchUpdate(new String[] { "INSERT INTO TEST_CHILD VALUES (1, 'test', 2)" });
	    Assert.fail("Insert should have failed");
	} catch (final DataIntegrityViolationException e) {
	    // expected
	}
    }
}
