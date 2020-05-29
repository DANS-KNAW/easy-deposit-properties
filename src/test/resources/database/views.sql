CREATE VIEW LastModified (depositId, max_timestamp) AS (
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
