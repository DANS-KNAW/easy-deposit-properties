CREATE TABLE Deposit (
    depositId CHAR(36) NOT NULL PRIMARY KEY,
    bagName TEXT,
    creationTimestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    depositorId VARCHAR(64) NOT NULL
);

CREATE TABLE State (
    stateId SERIAL NOT NULL PRIMARY KEY,
    depositId CHAR(36) NOT NULL,
    label VARCHAR(64) NOT NULL,
    description TEXT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (depositId) REFERENCES Deposit (depositId),
    UNIQUE (depositId, timestamp)
);

CREATE TABLE Identifier (
    identifierId SERIAL NOT NULL PRIMARY KEY,
    depositId CHAR(36) NOT NULL,
    identifierSchema VARCHAR(64) NOT NULL,
    identifierValue VARCHAR(64) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (depositId) REFERENCES Deposit (depositId),
    UNIQUE (depositId, identifierSchema),
    UNIQUE (depositId, timestamp)
);

CREATE TABLE Curation (
    curationId SERIAL NOT NULL PRIMARY KEY,
    depositId CHAR(36) NOT NULL,
    isNewVersion BOOLEAN,
    isRequired BOOLEAN NOT NULL,
    isPerformed BOOLEAN NOT NULL,
    datamanagerUserId VARCHAR(64) NOT NULL,
    datamanagerEmail TEXT NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (depositId) REFERENCES Deposit (depositId),
    UNIQUE (depositId, timestamp)
);

CREATE TABLE Springfield (
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

CREATE TABLE SimpleProperties (
    propertyId SERIAL NOT NULL PRIMARY KEY,
    depositId CHAR(36) NOT NULL,
    key VARCHAR(64) NOT NULL,
    value VARCHAR(64) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (depositId) REFERENCES Deposit (depositId),
    UNIQUE (depositId, key, timestamp)
);

GRANT INSERT, SELECT ON Deposit TO easy_deposit_properties;
GRANT INSERT, SELECT ON State TO easy_deposit_properties;
GRANT INSERT, SELECT ON Identifier TO easy_deposit_properties;
GRANT INSERT, SELECT ON Curation TO easy_deposit_properties;
GRANT INSERT, SELECT ON Springfield TO easy_deposit_properties;
GRANT INSERT, SELECT ON SimpleProperties TO easy_deposit_properties;
GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO easy_deposit_properties;
