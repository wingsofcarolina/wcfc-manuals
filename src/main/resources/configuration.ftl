######

root: ${ROOT!'./root'}

# Operational mode, DEV or PROD
mode: ${MODE!'PROD'}

# Authentication, ON or OFF
auth: ${AUTH!'ON'}

# Mock user
mockUser: ${MOCKUSER!'none'}

# Slack channels
slackNotify: ${SLACK!'T0H7FTUQK/B02LP23EURJ/d9iiJxMCsr03OBWgYsP4cf9E'}     # Targets #notification
#slackContact: ${SLACK!'REDACTED/REDACTED'}    # Targets #contact

# Configure ports used by DropWizard
server:
    type: simple
    connector:
        type: http
        port: ${SERVER_PORT!'9300'}
    applicationContextPath: /
    adminContextPath: /admin
    requestLog:
        appenders:
          - type: file
            currentLogFilename: log/server-http.log
            threshold: ALL
            archive: true
            archivedLogFilenamePattern: log/server-%i-%d-http.log
            maxFileSize: 500MB
            archivedFileCount: 5
            timeZone: UTC

# SLF4j Logging settings.
logging:
  # The default level of all loggers. Can be OFF, ERROR, WARN, INFO, DEBUG, TRACE, or ALL.
  level: ${DEFAULT_LOGLEVEL!'INFO'}

  # Logger-specific levels.
  loggers:
    "org.wingsofcarolina.manuals": ${LOGLEVEL!'INFO'}
    
  appenders:
      - type: console
        threshold: ALL
        timeZone: UTC
        target: stdout
      - type: file
        currentLogFilename: ./log/server.log
        threshold: ALL
        archive: true
        archivedLogFilenamePattern: ./log/server-%i-%d.log
        maxFileSize: 500MB
        archivedFileCount: 5
        timeZone: UTC
