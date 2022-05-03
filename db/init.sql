CREATE TABLE polls (
	id INT PRIMARY KEY,
	name TEXT,
	finished BOOLEAN DEFAULT FALSE
);
CREATE TABLE poll_results (
	id SERIAL PRIMARY KEY,
	pollId INT,
	option_text TEXT,
	msgId INT,
	votes INT
);
-- INSERT INTO polls (id, name, finished)
-- VALUES (123, 'test', FALSE);
-- INSERT INTO poll_results (pollId, option_text, msgId, votes)
-- VALUES (-1, 'test', 1, 1);