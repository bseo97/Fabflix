
# Fabflix Project 4 - Scaled Web App with Connection Pooling and Master/Slave

- # General

- #### Names: Brian Seo, Lucas Kim

- #### Project 4 Video Demo Link:
[link](https://youtu.be/3QDCMwPkbH4)

- #### Instruction of deployment:
1. Start both Master and Slave MySQL servers.
2. Ensure context.xml is configured with connection URLs to both databases with connection pooling.
3. Deploy the .war file to Tomcat 10.1+ using EC2 instances.
4. Set up HTTPS and verify the app runs on the secure endpoint.

- #### Collaborations and Work Distribution:
- Brian Seo: JDBC Connection Pooling integration, Master/Slave Replication, MySQL/Tomcat Load Balancing, AutoComplete, Fuzzy Search, and README. 
- Lucas Kim: Full-text search, autocomplete, testing

- # Connection Pooling

- #### Include the filename/path of all code/configuration files in GitHub of using JDBC Connection Pooling:
- src/main/java/org/example/AddMovieServlet.java 
- src/main/java/org/example/AddStarServlet.java
- src/main/java/org/example/BrowseServlet.java 
- src/main/java/org/example/CartServlet.java 
- src/main/java/org/example/DashboardLoginServlet.java 
- src/main/java/org/example/GenreServlet.java 
- src/main/java/org/example/LoginServlet.java 
- src/main/java/org/example/MovieListServlet.java 
- src/main/java/org/example/MovieServlet.java 
- src/main/java/org/example/SearchServlet.java
- src/main/java/org/example/SingleMovieServlet.java 
- src/main/java/org/example/SingleStarServlet.java 
- src/main/java/org/example/Top20Servlet.java
- src/main/java/org/example/MetadataServlet.java 

- src/main/webapp/META-INF/context.xml
- src/main/webapp/WEB-INF/web.xml

- #### Explain how Connection Pooling is utilized in the Fabflix code:
We configured Tomcat to use JDBC Connection Pooling via context.xml using <Resource> with the maxTotal, maxIdle, minIdle, and maxWaitMillis properties. In the servlet classes, we inject the DataSource via @Resource(name="jdbc/moviedb"), then retrieve connections using dataSource.getConnection().
This avoids repeatedly creating and destroying DB connections, which improves scalability and performance under concurrent load.

- #### Explain how Connection Pooling works with two backend SQL:
We define two separate <Resource> tags in context.xml:
- One for the master (write + read)
- One for the slave (read-only)
Then in the Java servlets, we use logic to determine which data source to connect to based on whether the query is read or write. For example, SearchServlet uses the slave, while AddMovieServlet uses the master.

- # Master/Slave

- #### Include the filename/path of all code/configuration files in GitHub of routing queries to Master/Slave SQL:
- src/main/java/org/example/AddMovieServlet.java 
- src/main/java/org/example/AddStarServlet.java
- src/main/java/org/example/BrowseServlet.java 
- src/main/java/org/example/CartServlet.java 
- src/main/java/org/example/DashboardLoginServlet.java 
- src/main/java/org/example/GenreServlet.java 
- src/main/java/org/example/LoginServlet.java 
- src/main/java/org/example/MovieListServlet.java 
- src/main/java/org/example/MovieServlet.java 
- src/main/java/org/example/SearchServlet.java
- src/main/java/org/example/SingleMovieServlet.java 
- src/main/java/org/example/SingleStarServlet.java 
- src/main/java/org/example/Top20Servlet.java
- src/main/java/org/example/MetadataServlet.java 


- #### How read/write requests were routed to Master/Slave SQL?
- Read requests (e.g., SELECT) are routed to the slave by injecting @Resource(name="jdbc/moviedb-slave").
- Write requests (e.g., INSERT, UPDATE) are routed to the master via @Resource(name="jdbc/moviedb-master").
- Each servlet is configured to use the appropriate DataSource based on its operation type. This separation ensures high availability and load distribution across DB servers.

- # Fuzzy Search (for extra credit)

**File:**  
- `src/main/java/org/example/SearchServlet.java`
- You can test it out by searching "Love Story", delete 'o', or re-type into "love strey". Similar to example, you can test it out.

**Overview:**  
We implemented fuzzy search using two custom MySQL UDFs from the **Flamingo toolkit**:
- `edth(str1, str2, threshold)` – returns `TRUE` if edit distance ≤ threshold
- `edrec(str1, str2)` – returns actual edit distance

These are used to match movie titles even with minor typos. For example, searching for `Termonator` still returns `Terminator`.

**Example Query:**
```sql
SELECT title FROM movies WHERE edth(title, ?, 2);
