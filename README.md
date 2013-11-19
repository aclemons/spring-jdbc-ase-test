spring-jdbc-ase-test
===============

Simple test case to demonstrate SPR-11097.

https://jira.springsource.org/browse/SPR-11097

Run the tests with (adjust for your environment):
mvn clean install -DjdbcUrl=jdbc:sybase:Tds:localhost:5000/spring -DjdbcUser=spring -DjdbcPassword=spring

