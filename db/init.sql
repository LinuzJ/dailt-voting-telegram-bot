CREATE TABLE polls (
	id INT PRIMARY KEY,
	name TEXT,
	finished BOOLEAN
);

CREATE TABLE poll_results (
	pollId INT PRIMARY KEY,
	option_text TEXT,
	username TEXT,
	msgId INT,
	votes INT
);
