DROP TABLE IF EXISTS project;

CREATE TABLE project (
  id INT AUTO_INCREMENT  PRIMARY KEY,
  owner VARCHAR(250) NOT NULL,
  name VARCHAR(250) NOT NULL,
  url VARCHAR(250) DEFAULT NULL
);

INSERT INTO project (owner, name, url) VALUES
  ('Google', 'googleName', 'google.com'),
  ('Test', 'testName', 'test.com'),
  ('Huawei', 'huaweiName', 'huawei.com');