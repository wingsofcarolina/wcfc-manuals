#!/usr/bin/env python3
"""
Setup test data in MongoDB for WCFC Manuals integration tests
"""

import json
import os
import sys
import uuid

try:
    from pymongo import MongoClient
except ImportError:
    print("PyMongo not available. Please install it with: pip install pymongo")
    sys.exit(1)

def setup_mongodb_data():
    """Setup test data in MongoDB"""
    print("Setting up MongoDB test data...")
    
    # Connect to MongoDB
    client = MongoClient('mongodb://localhost:27017/')
    db = client['wcfc-manuals-test']
    
    # Clear existing data
    db.Members.drop()
    db.VerificationCode.drop()
    
    # Create test member data
    test_members = [
        {
            "id": 1001,
            "memberId": 1001,
            "name": "Test User",
            "email": "test@example.com",
            "level": 3,
            "admin": False,
            "uuid": str(uuid.uuid4())
        },
        {
            "id": 1002,
            "memberId": 1002,
            "name": "John Pilot",
            "email": "john.pilot@example.com",
            "level": 2,
            "admin": False,
            "uuid": str(uuid.uuid4())
        }
    ]
    
    db.Members.insert_many(test_members)
    print(f"Inserted {len(test_members)} test member records")
    
    # Create sequence counters
    db.counters.insert_one({"_id": "members", "seq": 1002})
    
    print("MongoDB test data setup completed successfully!")
    
    client.close()

if __name__ == "__main__":
    try:
        setup_mongodb_data()
        print("Test data setup completed successfully!")
    except Exception as e:
        print(f"Error setting up test data: {e}")
        sys.exit(1)
