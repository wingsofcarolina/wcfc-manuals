######

root: ${ROOT!'./root'}

# Location of the MongoDB server
# If on a Docker network, just give the name of the MongoDB
# container. Otherwise give the IP:PORT for the MongoDB process.
mongodb: ${MONGODB!'mongodb://mongodb'}

# URL of the server where the groundschool service is running
manualsServer: ${MANUALSSERVER!'https://manuals.wingsofcarolina.org'}

# Operational mode, DEV or PROD
mode: ${MODE!'PROD'}

# Authentication, ON or OFF
auth: ${AUTH!'ON'}

# Mock user
mockUser: ${MOCKUSER!'none'}

# Slack channels
slackNotify: ${SLACK!'T0H7FTUQK/B02LP23EURJ/d9iiJxMCsr03OBWgYsP4cf9E'}     # Targets #notification
slackManuals: ${SLACK!'T0H7FTUQK/B049U4ZG7TL/ct0vai5hEa4a4oA8GJAq97ep'}    # Targets #manuals

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
            currentLogFilename: ${LOG_DIR!'/log'}/manuals-http.log
            threshold: ALL
            archive: true
            archivedLogFilenamePattern: ${LOG_DIR!'/log'}/manuals-%i-%d-http.log
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
        currentLogFilename: ${LOG_DIR!'/log'}/manuals.log
        threshold: ALL
        archive: true
        archivedLogFilenamePattern: ${LOG_DIR!'/log'}/manuals-%i-%d.log
        maxFileSize: 500MB
        archivedFileCount: 5
        timeZone: UTC
