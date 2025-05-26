DROP DATABASE IF EXISTS moviedb;
CREATE DATABASE moviedb;
USE moviedb;

-- Table: movies
CREATE TABLE movies (
    id VARCHAR(10) PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    year INT NOT NULL,
    director VARCHAR(100) NOT NULL
);

-- Table: stars
CREATE TABLE stars (
    id VARCHAR(10) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    birthYear INT
);

-- Table: stars_in_movies
CREATE TABLE stars_in_movies (
    starId VARCHAR(10),
    movieId VARCHAR(10),
    PRIMARY KEY (starId, movieId),
    FOREIGN KEY (starId) REFERENCES stars(id),
    FOREIGN KEY (movieId) REFERENCES movies(id)
);

-- Table: genres
CREATE TABLE genres (
   id INT AUTO_INCREMENT PRIMARY KEY,
   name VARCHAR(32) NOT NULL
);

-- Table: genres_in_movies
CREATE TABLE genres_in_movies (
    genreId INT,
    movieId VARCHAR(10),
    PRIMARY KEY (genreId, movieId),
    FOREIGN KEY (genreId) REFERENCES genres(id),
    FOREIGN KEY (movieId) REFERENCES movies(id)
);

-- Table: creditcards
CREATE TABLE creditcards (
    id VARCHAR(20) PRIMARY KEY,
    firstName VARCHAR(50) NOT NULL,
    lastName VARCHAR(50) NOT NULL,
    expiration DATE NOT NULL
);

-- Table: customers
CREATE TABLE customers (
   id INT AUTO_INCREMENT PRIMARY KEY,
   firstName VARCHAR(50) NOT NULL,
   lastName VARCHAR(50) NOT NULL,
   ccId VARCHAR(20),
   address VARCHAR(200) NOT NULL,
   email VARCHAR(50) NOT NULL,
   password VARCHAR(20) NOT NULL,
   FOREIGN KEY (ccId) REFERENCES creditcards(id)
);

CREATE TABLE employees (
    email varchar(50) primary key,
    password varchar(20) not null,
    fullname varchar(100)
);

-- Table: sales
CREATE TABLE sales (
   id INT AUTO_INCREMENT PRIMARY KEY,
   customerId INT NOT NULL,
   movieId VARCHAR(10) NOT NULL,
   saleDate DATE NOT NULL,
   FOREIGN KEY (customerId) REFERENCES customers(id),
   FOREIGN KEY (movieId) REFERENCES movies(id)
);

-- Table: ratings
CREATE TABLE ratings (
    movieId VARCHAR(10) NOT NULL,
    rating FLOAT NOT NULL,
    numVotes INT NOT NULL,
    FOREIGN KEY (movieId) REFERENCES movies(id)
);
INSERT INTO customers (id, firstName, lastName, ccId, address, email, password)
VALUES (NULL, 'A2', 'Test', NULL, '519 E peltason dr.', 'a2@email.com', 'a2');

INSERT INTO employees (email, password, fullname)
VALUES ('classta@email.edu', 'classta', 'TA CS122B');

