CREATE TABLE IF NOT EXISTS Deposit (
    depositId CHAR(36) NOT NULL PRIMARY KEY,
    bagName TEXT,
    creationTimestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    depositorId VARCHAR(64) NOT NULL,
    origin VARCHAR(6) NOT NULL
);

CREATE TABLE IF NOT EXISTS State (
    stateId SERIAL NOT NULL PRIMARY KEY,
    depositId CHAR(36) NOT NULL,
    label VARCHAR(64) NOT NULL,
    description TEXT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (depositId) REFERENCES Deposit (depositId),
    UNIQUE (depositId, timestamp)
);

CREATE TABLE IF NOT EXISTS Identifier (
    identifierId SERIAL NOT NULL PRIMARY KEY,
    depositId CHAR(36) NOT NULL,
    identifierSchema VARCHAR(64) NOT NULL,
    identifierValue VARCHAR(64) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (depositId) REFERENCES Deposit (depositId),
    UNIQUE (depositId, identifierSchema, timestamp),
    UNIQUE (identifierSchema, identifierValue)
);

CREATE TABLE IF NOT EXISTS Curator (
    curatorEntryId SERIAL NOT NULL PRIMARY KEY,
    depositId CHAR(36) NOT NULL,
    datamanagerUserId VARCHAR(64) NOT NULL,
    datamanagerEmail TEXT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (depositId) REFERENCES Deposit (depositId),
    UNIQUE (depositId, timestamp)
);

CREATE TABLE IF NOT EXISTS Springfield (
    springfieldId SERIAL NOT NULL PRIMARY KEY,
    depositId CHAR(36) NOT NULL,
    domain VARCHAR(32) NOT NULL,
    springfield_user VARCHAR(32) NOT NULL,
    collection VARCHAR(32) NOT NULL,
    playmode VARCHAR(32) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (depositId) REFERENCES Deposit (depositId),
    UNIQUE (depositId, timestamp)
);

CREATE TABLE IF NOT EXISTS SimpleProperties (
    propertyId SERIAL NOT NULL PRIMARY KEY,
    depositId CHAR(36) NOT NULL,
    key VARCHAR(64) NOT NULL,
    value VARCHAR(64) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (depositId) REFERENCES Deposit (depositId),
    UNIQUE (depositId, key, timestamp)
);

CREATE OR REPLACE VIEW LastModified AS (
  SELECT depositId, MAX(max) AS max_timestamp
  FROM (
    (SELECT depositId, MAX(creationTimestamp) AS max FROM Deposit GROUP BY depositId) UNION ALL
    (SELECT depositId, MAX(timestamp) AS max FROM State GROUP BY depositId) UNION ALL
    (SELECT depositId, MAX(timestamp) AS max FROM Identifier GROUP BY depositId) UNION ALL
    (SELECT depositId, MAX(timestamp) AS max FROM Curator GROUP BY depositId) UNION ALL
    (SELECT depositId, MAX(timestamp) AS max FROM Springfield GROUP BY depositId) UNION ALL
    (SELECT depositId, MAX(timestamp) AS max FROM SimpleProperties GROUP BY depositId)
  ) AS max_timestamps
  GROUP BY depositId
);

GRANT INSERT, SELECT, DELETE, UPDATE ON Deposit TO easy_deposit_properties;
GRANT INSERT, SELECT, DELETE ON State TO easy_deposit_properties;
GRANT INSERT, SELECT, DELETE ON Identifier TO easy_deposit_properties;
GRANT INSERT, SELECT, DELETE ON Curator TO easy_deposit_properties;
GRANT INSERT, SELECT, DELETE ON Springfield TO easy_deposit_properties;
GRANT INSERT, SELECT, DELETE ON SimpleProperties TO easy_deposit_properties;
GRANT SELECT ON LastModified TO easy_deposit_properties;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO easy_deposit_properties;
