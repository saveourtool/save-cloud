DROP TABLE IF EXISTS agent_status;

CREATE TABLE agent_status (
                         id INT AUTO_INCREMENT  PRIMARY KEY,
                         state VARCHAR(250) NOT NULL,
                         time DATE NOT NULL
);

INSERT INTO agent_status (state, time) VALUES
('IDLE', '2021-12-31'),
('FINISHED', '2021-10-14'),
('CLI_FAILED', '2021-05-04');