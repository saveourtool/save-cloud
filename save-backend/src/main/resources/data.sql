DROP TABLE IF EXISTS project;

CREATE TABLE project (
  id INT AUTO_INCREMENT  PRIMARY KEY,
  owner VARCHAR(250) NOT NULL,
  name VARCHAR(250) NOT NULL,
  type VARCHAR(250) DEFAULT NULL,
  url VARCHAR(250) DEFAULT NULL,
  description VARCHAR(250) DEFAULT NULL
);

INSERT INTO project (owner, name, type, url, description) VALUES
  ('Google', 'googleName', 'github', 'google.com'),
  ('Test', 'testName', 'generic', 'test.com', 'A test project in SAVE'),
  ('Huawei', 'huaweiName', 'huawei.com', 'generic');