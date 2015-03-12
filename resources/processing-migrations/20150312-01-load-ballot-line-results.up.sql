CREATE TABLE ballot_line_results (id INTEGER PRIMARY KEY,
                                  contest_id INTEGER,
                                  jurisdiction_id INTEGER,
                                  entire_district BOOLEAN NOT NULL CHECK (entire_district IN (0,1)),
                                  candidate_id INTEGER,
                                  ballot_response_id INTEGER,
                                  votes INTEGER,
                                  victorious BOOLEAN NOT NULL CHECK (victorious IN (0,1)),
                                  certification TEXT);





