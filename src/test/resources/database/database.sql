-- differences with src/main/assembly/dist/install/db-tables.sql in order to support HSQLDB syntax
-- * SERIAL --> INTEGER IDENTITY
-- * TEXT --> VARCHAR(10000)

CREATE TABLE Deposit (
    depositId CHAR(36) NOT NULL PRIMARY KEY,
    bagName VARCHAR(10000),
    creationTimestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    depositorId VARCHAR(64) NOT NULL
);

CREATE TABLE State (
    stateId INTEGER IDENTITY NOT NULL PRIMARY KEY,
    depositId CHAR(36) NOT NULL,
    label VARCHAR(64) NOT NULL,
    description VARCHAR(10000) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (depositId) REFERENCES Deposit (depositId),
    UNIQUE (depositId, timestamp)
);

CREATE TABLE Identifier (
    identifierId INTEGER IDENTITY NOT NULL PRIMARY KEY,
    depositId CHAR(36) NOT NULL,
    identifierSchema VARCHAR(64) NOT NULL,
    identifierValue VARCHAR(64) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (depositId) REFERENCES Deposit (depositId),
    UNIQUE (depositId, identifierSchema),
    UNIQUE (depositId, timestamp)
);

CREATE TABLE Curation (
    curationId INTEGER IDENTITY NOT NULL PRIMARY KEY,
    depositId CHAR(36) NOT NULL,
    isNewVersion BOOLEAN,
    isRequired BOOLEAN NOT NULL,
    isPerformed BOOLEAN NOT NULL,
    datamanagerUserId VARCHAR(64) NOT NULL,
    datamanagerEmail VARCHAR(10000) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (depositId) REFERENCES Deposit (depositId),
    UNIQUE (depositId, timestamp)
);

CREATE TABLE Springfield (
    springfieldId INTEGER IDENTITY NOT NULL PRIMARY KEY,
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
    propertyId INTEGER IDENTITY NOT NULL PRIMARY KEY,
    depositId CHAR(36) NOT NULL,
    key VARCHAR(64) NOT NULL,
    value VARCHAR(64) NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (depositId) REFERENCES Deposit (depositId),
    UNIQUE (depositId, key, timestamp)
);
