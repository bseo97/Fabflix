
# Fabflix Project 4 - Scaled Web App with Connection Pooling and Master/Slave

- # General

- #### Names: Brian Seo, Lucas Kim

- #### Project 4 Video Demo Link:
link

- #### Instruction of deployment:
1. Start both Master and Slave MySQL servers.
2. Ensure context.xml is configured with connection URLs to both databases with connection pooling.
3. Deploy the .war file to Tomcat 10.1+ using EC2 instances.
4. Set up HTTPS and verify the app runs on the secure endpoint.

- #### Collaborations and Work Distribution:
- Brian Seo: JDBC Connection Pooling integration, Master/Slave Replication, MySQL/Tomcat Load Balancing, AutoComplete, and README. 
- Lucas Kim: Full-text search, testing

- # Connection Pooling

- #### Include the filename/path of all code/configuration files in GitHub of using JDBC Connection Pooling:

- src/main/java/org/example/GenreServlet.java
- src/main/java/org/example/AddMovieServlet.java
- src/main/java/org/example/AddStarServlet.java
- src/main/java/org/example/BrowseServlet.java
- src/main/java/org/example/MovieListServlet.java
- src/main/java/org/example/LoginServlet.java
- src/main/java/org/example/Top20Servlet.java
- src/main/java/org/example/SearchServlet.java
- src/main/java/org/example/MovieServlet.java
- src/main/java/org/example/SingleMovieServlet.java
- src/main/java/org/example/SingleStarServlet.java

- src/main/webapp/META-INF/context.xml
- src/main/webapp/WEB-INF/web.xml

- #### Explain how Connection Pooling is utilized in the Fabflix code:
We configured Tomcat to use JDBC Connection Pooling via context.xml using <Resource> with the maxTotal, maxIdle, minIdle, and maxWaitMillis properties. In the servlet classes, we inject the DataSource via @Resource(name="jdbc/moviedb"), then retrieve connections using dataSource.getConnection().
This avoids repeatedly creating and destroying DB connections, which improves scalability and performance under concurrent load.

- #### Explain how Connection Pooling works with two backend SQL:
In our context.xml, we define one <Resource> named jdbc/moviedb for connection pooling. This connects to the master MySQL instance, which handles both reads and writes. Although we set up MySQL master-slave replication for backend redundancy, our web application connects to the master DB only. This ensures that both read and write queries go to a consistent, up-to-date source. The connection pooling avoids repeated connection creation by reusing database connections efficiently under concurrent load.

- # Master/Slave

- #### Include the filename/path of all code/configuration files in GitHub of routing queries to Master/Slave SQL:
- src/main/java/org/example/GenreServlet.java
- src/main/java/org/example/AddMovieServlet.java
- src/main/java/org/example/AddStarServlet.java
- src/main/java/org/example/BrowseServlet.java
- src/main/java/org/example/MovieListServlet.java
- src/main/java/org/example/LoginServlet.java
- src/main/java/org/example/Top20Servlet.java
- src/main/java/org/example/SearchServlet.java
- src/main/java/org/example/MovieServlet.java
- src/main/java/org/example/SingleMovieServlet.java
- src/main/java/org/example/SingleStarServlet.java

- #### How read/write requests were routed to Master/Slave SQL?
- We configured MySQL master-slave replication. The Tomcat server connects to the master DB (jdbc/moviedb) using connection pooling defined in context.xml. Although our servlets all connect via a single @Resource(name = "jdbc/moviedb"), read queries from the slave are served automatically because the slave syncs with the master using replication. We do not separate reads and writes in code, but the system maintains data consistency via MySQL replication.


