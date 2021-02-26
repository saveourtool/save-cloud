DROP TABLE IF EXISTS agent_status;

CREATE TABLE agent_status (
                         id INT AUTO_INCREMENT  PRIMARY KEY,
                         state VARCHAR(250) NOT NULL,
                         time DATE NOT NULL,
                         agent_id INT
);

INSERT INTO agent_status (state, time, agent_id) VALUES
('IDLE', '2021-12-31', 0),
('FINISHED', '2021-10-14', 0),
('CLI_FAILED', '2021-05-04', 1);