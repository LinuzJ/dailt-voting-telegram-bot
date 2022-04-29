CREATE TABLE polls (
	id INT PRIMARY KEY,
	name TEXT,
	finished BOOLEAN DEFAULT FALSE
);
CREATE TABLE poll_results (
	pollId INT PRIMARY KEY,
	option_text TEXT,
	username TEXT,
	msgId INT,
	votes INT
);
INSERT INTO polls (id, name, finished)
VALUES (123, 'test', FALSE);