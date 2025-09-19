#!/usr/bin/env python3
"""
Setup WireMock stubs for Gmail API mocking in WCFC Manuals integration tests
"""

import json
import requests
import sys
import time

def setup_wiremock_stubs():
    """Setup WireMock stubs for Gmail API"""
    print("Setting up WireMock stubs for Gmail API...")
    
    wiremock_url = "http://localhost:8080"
    
    # Wait for WireMock to be ready
    for i in range(30):
        try:
            response = requests.get(f"{wiremock_url}/__admin/health")
            if response.status_code == 200:
                break
        except requests.exceptions.ConnectionError:
            pass
        time.sleep(1)
    else:
        raise Exception("WireMock is not ready")
    
    # Clear existing stubs
    requests.delete(f"{wiremock_url}/__admin/mappings")
    
    # Gmail API OAuth token endpoint stub
    oauth_stub = {
        "request": {
            "method": "POST",
            "urlPattern": "/token"
        },
        "response": {
            "status": 200,
            "headers": {
                "Content-Type": "application/json"
            },
            "jsonBody": {
                "access_token": "mock_access_token",
                "token_type": "Bearer",
                "expires_in": 3600
            }
        }
    }
    
    response = requests.post(f"{wiremock_url}/__admin/mappings", json=oauth_stub)
    if response.status_code != 201:
        raise Exception(f"Failed to create OAuth stub: {response.text}")
    
    print("Created OAuth token stub")
    
    # Gmail API send message endpoint stub
    send_email_stub = {
        "request": {
            "method": "POST",
            "urlPattern": "/gmail/v1/users/.*/messages/send"
        },
        "response": {
            "status": 200,
            "headers": {
                "Content-Type": "application/json"
            },
            "jsonBody": {
                "id": "mock_message_id_{{randomValue length=16 type='ALPHANUMERIC'}}",
                "threadId": "mock_thread_id_{{randomValue length=16 type='ALPHANUMERIC'}}",
                "labelIds": ["SENT"]
            }
        },
        "postServeActions": [
            {
                "name": "webhook",
                "parameters": {
                    "url": "http://localhost:8080/__admin/requests/count",
                    "method": "GET"
                }
            }
        ]
    }
    
    response = requests.post(f"{wiremock_url}/__admin/mappings", json=send_email_stub)
    if response.status_code != 201:
        raise Exception(f"Failed to create send email stub: {response.text}")
    
    print("Created Gmail send message stub")
    
    # Gmail API get profile endpoint stub (for service account validation)
    profile_stub = {
        "request": {
            "method": "GET",
            "urlPattern": "/gmail/v1/users/.*/profile"
        },
        "response": {
            "status": 200,
            "headers": {
                "Content-Type": "application/json"
            },
            "jsonBody": {
                "emailAddress": "no-reply@wingsofcarolina.org",
                "messagesTotal": 0,
                "threadsTotal": 0,
                "historyId": "1"
            }
        }
    }
    
    response = requests.post(f"{wiremock_url}/__admin/mappings", json=profile_stub)
    if response.status_code != 201:
        raise Exception(f"Failed to create profile stub: {response.text}")
    
    print("Created Gmail profile stub")
    
    # Slack webhook stub
    slack_stub = {
        "request": {
            "method": "POST",
            "urlPattern": "/services/.*"
        },
        "response": {
            "status": 200,
            "body": "ok"
        }
    }
    
    response = requests.post(f"{wiremock_url}/__admin/mappings", json=slack_stub)
    if response.status_code != 201:
        raise Exception(f"Failed to create Slack stub: {response.text}")
    
    print("Created Slack webhook stub")
    
    print("WireMock stubs setup completed successfully!")

def get_email_requests():
    """Get all email requests sent to WireMock"""
    wiremock_url = "http://localhost:8080"
    
    response = requests.get(f"{wiremock_url}/__admin/requests")
    if response.status_code == 200:
        requests_data = response.json()
        email_requests = []
        
        for request in requests_data.get('requests', []):
            if '/gmail/v1/users/' in request.get('url', '') and '/messages/send' in request.get('url', ''):
                email_requests.append(request)
        
        return email_requests
    else:
        raise Exception(f"Failed to get requests from WireMock: {response.text}")

if __name__ == "__main__":
    try:
        setup_wiremock_stubs()
        print("WireMock setup completed successfully!")
    except Exception as e:
        print(f"Error setting up WireMock: {e}")
        sys.exit(1)
