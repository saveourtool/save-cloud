DROP TABLE IF EXISTS project;
DROP TABLE IF EXISTS result;

CREATE TABLE project (
  id INT AUTO_INCREMENT  PRIMARY KEY,
  owner VARCHAR(250) NOT NULL,
  name VARCHAR(250) NOT NULL,
  url VARCHAR(250) DEFAULT NULL
);

CREATE TABLE result (
 id INT PRIMARY KEY,
 status VARCHAR(250) NOT NULL,
 date VARCHAR(250) NOT NULL
);

INSERT INTO project (owner, name, url) VALUES
  ('Google', 'googleName', 'google.com'),
  ('Test', 'testName', 'test.com'),
  ('Huawei', 'huaweiName', 'huawei.com');

INSERT INTO result (id, status, date) VALUES
  (1, 'q', '12'),
  (2, 'w', '123'),
  (3, 'e', '124');